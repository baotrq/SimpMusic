package com.maxrave.simpmusic.expect.ui

import android.bluetooth.BluetoothManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.simpmusic.led.LedStripSync
import com.maxrave.simpmusic.ui.component.SettingItem
import com.maxrave.simpmusic.ui.theme.typo
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.led_strip_bluetooth_off
import simpmusic.composeapp.generated.resources.led_strip_brightness
import simpmusic.composeapp.generated.resources.led_strip_choose
import simpmusic.composeapp.generated.resources.led_strip_device
import simpmusic.composeapp.generated.resources.led_strip_none_found
import simpmusic.composeapp.generated.resources.led_strip_not_selected
import simpmusic.composeapp.generated.resources.led_strip_searching
import simpmusic.composeapp.generated.resources.led_strip_status_connected
import simpmusic.composeapp.generated.resources.led_strip_status_connecting
import simpmusic.composeapp.generated.resources.led_strip_status_no_permission
import simpmusic.composeapp.generated.resources.led_strip_sync
import simpmusic.composeapp.generated.resources.led_strip_sync_info

@Composable
actual fun ColumnScope.LedStripSettingItems() {
    val context = LocalContext.current
    val dataStore: DataStoreManager = koinInject()
    val scope = rememberCoroutineScope()

    val enabled by remember { dataStore.getString(LedStripSync.KEY_ENABLED).map { it == "true" } }
        .collectAsStateWithLifecycle(false)
    val stripName by remember { dataStore.getString(LedStripSync.KEY_NAME) }.collectAsStateWithLifecycle(null)
    val storedBrightness by remember {
        dataStore.getString(LedStripSync.KEY_BRIGHTNESS).map { it?.toIntOrNull() ?: LedStripSync.DEFAULT_BRIGHTNESS }
    }.collectAsStateWithLifecycle(LedStripSync.DEFAULT_BRIGHTNESS)
    val status by LedStripSync.status.collectAsStateWithLifecycle()
    val color by LedStripSync.color.collectAsStateWithLifecycle()

    var showPicker by remember { mutableStateOf(false) }
    // What to do once permission is granted: open the picker, or just reconnect.
    var afterGrant by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.all { it }) {
                LedStripSync.retry(dataStore)
                afterGrant?.invoke()
            }
            afterGrant = null
        }
    fun withPermission(then: () -> Unit) {
        if (LedStripSync.hasPermissions(context)) {
            then()
        } else {
            afterGrant = then
            permissionLauncher.launch(LedStripSync.requiredPermissions())
        }
    }

    SettingItem(
        title = stringResource(Res.string.led_strip_sync),
        subtitle = stringResource(Res.string.led_strip_sync_info),
        smallSubtitle = true,
        switch = (
            enabled to { on: Boolean ->
                val write = { scope.launch { dataStore.putString(LedStripSync.KEY_ENABLED, on.toString()) } }
                if (on) withPermission { write() } else write()
                Unit
            }
        ),
    )

    val deviceSubtitle =
        when {
            stripName.isNullOrBlank() -> stringResource(Res.string.led_strip_not_selected)
            !enabled -> stripName!!
            status == LedStripSync.Status.NO_PERMISSION -> stringResource(Res.string.led_strip_status_no_permission)
            status == LedStripSync.Status.CONNECTED -> "$stripName · ${stringResource(Res.string.led_strip_status_connected)}"
            else -> "$stripName · ${stringResource(Res.string.led_strip_status_connecting)}"
        }
    SettingItem(
        title = stringResource(Res.string.led_strip_device),
        subtitle = deviceSubtitle,
        onClick = { withPermission { showPicker = true } },
    )

    // Held only while the thumb is moving; otherwise the slider shows the stored value.
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val sliderValue = dragValue ?: storedBrightness.toFloat()
    SettingItem(
        title = stringResource(Res.string.led_strip_brightness),
        subtitle = "${sliderValue.toInt()}%",
        isEnable = enabled,
        otherView = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The colour the strip is showing, at the chosen brightness.
                val shown = color?.let { scaled(it, sliderValue.toInt()) }
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(shown?.let { Color(0xFF000000.toInt() or it) } ?: MaterialTheme.colorScheme.surfaceVariant),
                )
                Spacer(Modifier.width(12.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        dragValue = it
                        LedStripSync.previewBrightness(it.toInt())
                    },
                    onValueChangeFinished = {
                        val level = sliderValue.toInt()
                        scope.launch {
                            dataStore.putString(LedStripSync.KEY_BRIGHTNESS, level.toString())
                            LedStripSync.previewBrightness(null)
                            dragValue = null
                        }
                    },
                    // The controller's lowest level that is not byte 00 (see LedStripSync).
                    valueRange = LedStripSync.MIN_BRIGHTNESS.toFloat()..100f,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    )

    if (showPicker) {
        StripPickerDialog(
            onDismiss = { showPicker = false },
            onPick = { strip ->
                showPicker = false
                scope.launch {
                    dataStore.putString(LedStripSync.KEY_NAME, strip.name)
                    dataStore.putString(LedStripSync.KEY_ADDRESS, strip.address)
                    dataStore.putString(LedStripSync.KEY_ENABLED, "true")
                }
            },
        )
    }
}

@Composable
private fun StripPickerDialog(
    onDismiss: () -> Unit,
    onPick: (LedStripSync.FoundStrip) -> Unit,
) {
    val context = LocalContext.current
    val bluetoothOn =
        remember { context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true }
    val strips by remember { LedStripSync.scan(context) }.collectAsStateWithLifecycle(emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.led_strip_choose), style = typo().titleSmall) },
        text = {
            Column {
                when {
                    !bluetoothOn -> Text(stringResource(Res.string.led_strip_bluetooth_off), style = typo().bodyMedium)
                    strips.isEmpty() ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(stringResource(Res.string.led_strip_searching), style = typo().bodyMedium)
                                Text(stringResource(Res.string.led_strip_none_found), style = typo().bodySmall)
                            }
                        }
                    else ->
                        LazyColumn(Modifier.heightIn(max = 320.dp)) {
                            items(strips, key = { it.address }) { strip ->
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onPick(strip) }
                                        .padding(vertical = 10.dp),
                                ) {
                                    Text(strip.name, style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text("${strip.address} · ${strip.rssi} dBm", style = typo().bodySmall)
                                }
                            }
                        }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
    )
}

private fun scaled(
    rgb: Int,
    percent: Int,
): Int {
    val f = percent.coerceIn(1, 100) / 100f
    return ((((rgb shr 16) and 0xFF) * f).toInt() shl 16) or
        ((((rgb shr 8) and 0xFF) * f).toInt() shl 8) or
        (((rgb and 0xFF) * f).toInt())
}
