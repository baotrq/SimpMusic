package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import com.maxrave.domain.data.model.loginsync.LoginSyncService
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.login_sync_item_ai
import simpmusic.composeapp.generated.resources.login_sync_item_app
import simpmusic.composeapp.generated.resources.login_sync_item_playback

/** Desktop shows the pairing QR and receives sign-ins and settings; Android scans it and sends them. */
@Composable
expect fun LoginSyncDialog(onDismiss: () -> Unit)

/** Service names stay as written; the settings groups are words, so they are translated. */
@Composable
internal fun LoginSyncService.displayName(): String =
    when (this) {
        LoginSyncService.AI_SETTINGS -> stringResource(Res.string.login_sync_item_ai)
        LoginSyncService.APP_SETTINGS -> stringResource(Res.string.login_sync_item_app)
        LoginSyncService.PLAYBACK_SYNC -> stringResource(Res.string.login_sync_item_playback)
        else -> label
    }
