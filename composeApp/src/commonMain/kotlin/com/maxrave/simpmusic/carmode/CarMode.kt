package com.maxrave.simpmusic.carmode

import com.maxrave.domain.manager.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Fork: Car mode (Settings → Car mode), for a phone that lives in the car with mobile data and
 * storage to spare. The setting itself is [DataStoreManager.carMode]; this mirrors it for code that
 * cannot wait on a Flow, such as the image loader's per-request cache check. The player and
 * playback sync read the setting directly.
 */
object CarMode {
    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** Covers are re-checked online after this; in Car mode they stay put for a month. */
    private const val CAR_MODE_IMAGE_TTL_MS = 30 * DAY_MS

    @Volatile
    var enabled: Boolean = false
        private set

    fun start(
        dataStoreManager: DataStoreManager,
        scope: CoroutineScope,
    ) {
        scope.launch {
            dataStoreManager.carMode
                .map { it == DataStoreManager.TRUE }
                .distinctUntilChanged()
                .onEach { enabled = it }
                .collect()
        }
    }

    fun imageTtlMillis(): Long = if (enabled) CAR_MODE_IMAGE_TTL_MS else DAY_MS
}
