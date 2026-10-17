package com.maxrave.simpmusic.carmode

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.car_mode_offline_done
import simpmusic.composeapp.generated.resources.car_mode_offline_downloading
import simpmusic.composeapp.generated.resources.car_mode_offline_full
import simpmusic.composeapp.generated.resources.car_mode_offline_pool
import simpmusic.composeapp.generated.resources.car_mode_offline_summary

/** "2 431 songs · 18.2 GB of 20 GB", the listened pool, then what Car mode is doing about the rest. */
@Composable
fun carModeOfflineSummary(status: CarModeDownloads.Status): String {
    val summary = stringResource(Res.string.car_mode_offline_summary, status.songs.toString(), gigabytes(status.bytes), gigabytes(status.capBytes))
    val activity =
        when {
            status.full -> stringResource(Res.string.car_mode_offline_full, status.waiting.toString())
            status.downloadingTitle != null ->
                stringResource(Res.string.car_mode_offline_downloading, status.downloadingTitle, status.waiting.toString())
            else -> stringResource(Res.string.car_mode_offline_done)
        }
    val pool = stringResource(Res.string.car_mode_offline_pool, gigabytes(status.poolBytes), gigabytes(status.poolCapBytes))
    return "$summary\n$pool\n$activity"
}

// Compose Resources take %1$s and nothing else, so the number is formatted here.
private fun gigabytes(bytes: Long): String = "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
