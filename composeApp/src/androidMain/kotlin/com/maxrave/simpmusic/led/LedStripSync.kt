package com.maxrave.simpmusic.led

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

private const val TAG = "LedStrip"

/**
 * Drives a car LED strip's BLE controller (the `LEDDMX-…` family that the "LED LAMP" app's LED DMX
 * mode talks to) with the vivid colour of the playing song's artwork.
 *
 * The protocol was read off the phone's own HCI snoop log on 2026-10-13 (LEDDMX-00-4909): every
 * command is a write-without-response to characteristic FFE1 of service FFE0, framed `7B … BF`.
 * Colour is `7B FF 07 RR GG BB 00 FF BF`, power `7B FF 04 0x FF FF FF FF BF`, and the app sends
 * `2A 02 A1 23 45 67 56 02 AF` once after connecting, which is replayed verbatim.
 *
 * Brightness is the controller's own command, `7B FF 01 LL PP 00 FF FF BF` (see [sendBrightness]);
 * scaling the colour instead read as "more white" at higher levels. The controller accepts the
 * LED LAMP app alongside this (checked on 2026-10-13), so neither has to be closed for the other.
 */
object LedStripSync {
    const val KEY_ENABLED = "led_strip_enabled"
    const val KEY_ADDRESS = "led_strip_address"
    const val KEY_NAME = "led_strip_name"
    const val KEY_BRIGHTNESS = "led_strip_brightness"
    const val DEFAULT_BRIGHTNESS = 100

    /**
     * Lowest brightness ever sent: 4 % is the controller's level byte 01. Byte 00 (anything under
     * 4 %) makes the strip jump bright instead of going dark (laptop tests, 2026-10-14).
     */
    const val MIN_BRIGHTNESS = 4

    private val SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val WRITE_UUID: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
    private val HANDSHAKE = bytes(0x2A, 0x02, 0xA1, 0x23, 0x45, 0x67, 0x56, 0x02, 0xAF)

    /** Pause between colour-walk steps (~30 per second, the rate the strip took without dropping). */
    private const val WALK_STEP_MS = 33L

    /** LED LAMP sends brightness every ~115 ms while its slider moves. */
    private const val BRIGHTNESS_INTERVAL_MS = 100L

    enum class Status { OFF, NO_PERMISSION, NO_DEVICE, CONNECTING, CONNECTED }

    data class FoundStrip(
        val name: String,
        val address: String,
        val rssi: Int,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var appContext: Context

    private val _status = MutableStateFlow(Status.OFF)
    val status: StateFlow<Status> = _status.asStateFlow()

    /** Where the strip is heading: the artwork's colour on the wheel; null until one is known. */
    private val target = MutableStateFlow<WheelColor?>(null)

    /** What the strip shows now, as a screen colour (0xRRGGBB) for the Settings swatch. */
    private val _color = MutableStateFlow<Int?>(null)
    val color: StateFlow<Int?> = _color.asStateFlow()

    private val _previewBrightness = MutableStateFlow<Int?>(null)

    // Touched from the scope and from the binder thread GATT callbacks arrive on.
    @Volatile private var gatt: BluetoothGatt? = null

    @Volatile private var gattAddress: String? = null

    @Volatile private var writeChar: BluetoothGattCharacteristic? = null

    /** The colour last written to the strip; null after (re)connecting, when it is unknown. */
    @Volatile private var shown: WheelColor? = null

    fun start(
        context: Context,
        dataStoreManager: DataStoreManager,
        handler: MediaPlayerHandler,
    ) {
        appContext = context.applicationContext
        val enabled = dataStoreManager.getString(KEY_ENABLED).map { it == "true" }
        val address = dataStoreManager.getString(KEY_ADDRESS)
        val brightness = dataStoreManager.getString(KEY_BRIGHTNESS).map { it?.toIntOrNull() ?: DEFAULT_BRIGHTNESS }

        // Connection follows the switch and the chosen strip.
        scope.launch {
            combine(enabled, address) { on, addr -> on to addr }
                .distinctUntilChanged()
                .collect { (on, addr) ->
                    when {
                        !on -> disconnect(Status.OFF)
                        addr.isNullOrBlank() -> disconnect(Status.NO_DEVICE)
                        !hasConnectPermission() -> disconnect(Status.NO_PERMISSION)
                        else -> connect(addr)
                    }
                }
        }

        // Colour follows the artwork; only the URL matters, so skips within one album send nothing new.
        scope.launch {
            handler.nowPlaying
                .map { it?.metadata?.artworkUri?.takeIf(String::isNotBlank) }
                .distinctUntilChanged()
                .collect { url ->
                    val picked = url?.let { vividColorOf(it) } ?: return@collect
                    // Written to the app log (Settings → Developer option) so a surprising colour
                    // can be traced back to the artwork that produced it.
                    Logger.i(TAG, "Target hue=${picked.hue.toInt()} sat=${"%.2f".format(picked.saturation)} from $url")
                    target.value = picked
                }
        }

        val ready = combine(enabled, _status) { on, st -> on && st == Status.CONNECTED }.distinctUntilChanged()

        // Walks the wheel toward the target one small step at a time. A new song mid-walk cancels
        // the loop, and the next one simply carries on from wherever the strip is — never a jump.
        // (A "fade through dark" mode was tried and removed on 2026-10-14: this controller has 32
        // coarse brightness levels, no level below 4 % that is dark, and no fade of its own, so
        // every dim looked stepped.)
        scope.launch {
            combine(ready, target) { r, t -> if (r) t else null }
                .distinctUntilChanged()
                .collectLatest { goal -> if (goal != null) walkTo(goal) }
        }

        // Brightness is the controller's own command, so the colour always goes out at full value
        // and stays the same hue at any level. A drag in Settings previews live, like LED LAMP's
        // slider, at most every BRIGHTNESS_INTERVAL_MS.
        scope.launch {
            combine(ready, brightness, _previewBrightness) { r, stored, preview -> if (r) preview ?: stored else null }
                .distinctUntilChanged()
                .conflate()
                .collect { level ->
                    if (level != null) {
                        sendBrightness(level)
                        delay(BRIGHTNESS_INTERVAL_MS)
                    }
                }
        }
    }

    /** Live level while the Settings slider is held; null hands back to the stored setting. */
    fun previewBrightness(percent: Int?) {
        _previewBrightness.value = percent
    }

    /** Re-reads the settings path after the user grants Bluetooth permission. */
    fun retry(dataStoreManager: DataStoreManager) {
        scope.launch {
            val on = dataStoreManager.getString(KEY_ENABLED).first() == "true"
            val addr = dataStoreManager.getString(KEY_ADDRESS).first()
            if (on && !addr.isNullOrBlank() && hasConnectPermission()) connect(addr)
        }
    }

    fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun hasPermissions(context: Context): Boolean =
        requiredPermissions().all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    private fun hasConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    /** Strips in range, nearest first. LEDDMX controllers are listed before anything else with a name. */
    @SuppressLint("MissingPermission")
    fun scan(context: Context): Flow<List<FoundStrip>> =
        callbackFlow {
            val scanner =
                context
                    .getSystemService(BluetoothManager::class.java)
                    ?.adapter
                    ?.takeIf { it.isEnabled }
                    ?.bluetoothLeScanner
            if (scanner == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val found = linkedMapOf<String, FoundStrip>()
            val callback =
                object : ScanCallback() {
                    override fun onScanResult(
                        callbackType: Int,
                        result: ScanResult,
                    ) {
                        val name = result.scanRecord?.deviceName ?: result.device.name ?: return
                        found[result.device.address] = FoundStrip(name, result.device.address, result.rssi)
                        trySend(
                            found.values.sortedWith(
                                compareByDescending<FoundStrip> { it.name.startsWith("LEDDMX", ignoreCase = true) }
                                    .thenByDescending { it.rssi },
                            ),
                        )
                    }

                    override fun onScanFailed(errorCode: Int) {
                        Logger.w(TAG, "Scan failed: $errorCode")
                    }
                }
            scanner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback)
            awaitClose { runCatching { scanner.stopScan(callback) } }
        }

    @SuppressLint("MissingPermission")
    private fun connect(address: String) {
        if (gattAddress == address && gatt != null) return
        disconnect(Status.CONNECTING)
        val adapter = appContext.getSystemService(BluetoothManager::class.java)?.adapter ?: return
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: return disconnect(Status.NO_DEVICE)
        Logger.i(TAG, "Connecting to $address")
        _status.value = Status.CONNECTING
        gattAddress = address
        // autoConnect: Android keeps the request open and links up whenever the strip comes into
        // range (car turned on), instead of failing after ~30 s.
        gatt = device.connectGatt(appContext, true, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    private fun disconnect(next: Status) {
        gatt?.let {
            runCatching { it.disconnect() }
            runCatching { it.close() }
        }
        gatt = null
        gattAddress = null
        writeChar = null
        _status.value = next
    }

    private val gattCallback =
        object : BluetoothGattCallback() {
            @SuppressLint("MissingPermission")
            override fun onConnectionStateChange(
                g: BluetoothGatt,
                status: Int,
                newState: Int,
            ) {
                if (g != gatt) return
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Logger.i(TAG, "Connected, discovering services")
                    g.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Logger.i(TAG, "Disconnected (status $status); waiting for the strip to return")
                    writeChar = null
                    _status.value = Status.CONNECTING
                    // With autoConnect the same GATT object reconnects by itself once in range.
                    runCatching { g.connect() }
                }
            }

            override fun onServicesDiscovered(
                g: BluetoothGatt,
                status: Int,
            ) {
                if (g != gatt) return
                val ch = g.getService(SERVICE_UUID)?.getCharacteristic(WRITE_UUID)
                if (ch == null) {
                    Logger.w(TAG, "Service FFE0/FFE1 not found on this device")
                    return
                }
                writeChar = ch
                shown = null
                scope.launch {
                    write(HANDSHAKE)
                    // Let the handshake leave before the first colour, as the LED LAMP app does.
                    delay(200)
                    if (g == gatt) _status.value = Status.CONNECTED
                }
            }
        }

    private val writeLock = Mutex()

    /**
     * Android allows one GATT operation in flight, write-without-response included, and refuses
     * the next with "busy" until the stack has sent it; so a refused write is retried briefly.
     */
    private suspend fun write(value: ByteArray) = writeLock.withLock { writeLocked(value) }

    @SuppressLint("MissingPermission")
    private suspend fun writeLocked(value: ByteArray) {
        repeat(5) {
            val g = gatt ?: return
            val ch = writeChar ?: return
            val sent =
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        g.writeCharacteristic(ch, value, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) == 0
                    } else {
                        @Suppress("DEPRECATION")
                        ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                        @Suppress("DEPRECATION")
                        ch.value = value
                        @Suppress("DEPRECATION")
                        g.writeCharacteristic(ch)
                    }
                }.getOrElse {
                    Logger.w(TAG, "Write failed: ${it.message}")
                    false
                }
            if (sent) return
            delay(20)
        }
    }

    /** Sends [c] as PWM duty with its strongest channel at FF ([VividColor.toDuty]). */
    private suspend fun sendColor(c: WheelColor) {
        val duty = VividColor.toDuty(c)
        write(bytes(0x7B, 0xFF, 0x07, (duty shr 16) and 0xFF, (duty shr 8) and 0xFF, duty and 0xFF, 0x00, 0xFF, 0xBF))
        shown = c
        _color.value = VividColor.toScreen(c)
    }

    /**
     * `7B FF 01 LL PP 00 FF FF BF`: PP is the percentage, LL the same level on the controller's
     * 0..32 scale, rounded down — exactly what LED LAMP sends (max `20 64`, half `10 32`,
     * lowest `00 01`; captured 2026-10-13).
     */
    private suspend fun sendBrightness(percent: Int) {
        // Never controller byte 00: the strip answers it by jumping bright.
        val p = percent.coerceIn(MIN_BRIGHTNESS, 100)
        write(bytes(0x7B, 0xFF, 0x01, p * 32 / 100, p, 0x00, 0xFF, 0xFF, 0xBF))
    }

    /**
     * Moves the strip to [goal] around the colour wheel in [VividColor.step]-sized steps, one every
     * [WALK_STEP_MS]. It takes as long as the distance needs (≈2.4 s for half the wheel) and never
     * skips ahead: each step waits for its own write. Right after connecting the strip's colour is
     * unknown, so the goal is sent directly.
     */
    private suspend fun walkTo(goal: WheelColor) {
        var current = shown ?: return sendColor(goal)
        while (current != goal) {
            current = VividColor.step(current, goal)
            sendColor(current)
            delay(WALK_STEP_MS)
        }
    }

    private suspend fun vividColorOf(url: String): WheelColor? {
        val request =
            ImageRequest
                .Builder(appContext)
                .data(url)
                .size(64)
                .allowHardware(false)
                .build()
        val bitmap = SingletonImageLoader.get(appContext).execute(request).image?.toBitmap() ?: return null
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return VividColor.pick(pixels)
    }

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }
}
