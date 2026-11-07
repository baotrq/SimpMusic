package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.saveImageToDevice
import com.maxrave.simpmusic.expect.ui.rememberSaveImagePermission
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.screen.home.PERSONAL_TOOLS_KEY
import com.maxrave.simpmusic.ui.theme.typo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.donate
import simpmusic.composeapp.generated.resources.donate_close
import simpmusic.composeapp.generated.resources.donate_prompt_message
import simpmusic.composeapp.generated.resources.donate_prompt_title
import simpmusic.composeapp.generated.resources.donate_qr
import simpmusic.composeapp.generated.resources.donate_qr_description
import simpmusic.composeapp.generated.resources.donate_qr_hint
import simpmusic.composeapp.generated.resources.donate_save_qr
import simpmusic.composeapp.generated.resources.later
import simpmusic.composeapp.generated.resources.share_lyrics_permission_denied
import simpmusic.composeapp.generated.resources.share_lyrics_save_failed
import simpmusic.composeapp.generated.resources.share_lyrics_saved
import simpmusic.composeapp.generated.resources.share_lyrics_saved_desktop

private const val KEY_NEXT_PROMPT_AT = "donate_next_prompt_at"
private const val DAY_MS = 24L * 60 * 60 * 1000
private const val FIRST_PROMPT_AFTER_MS = 7 * DAY_MS
private const val REPEAT_PROMPT_AFTER_MS = 14 * DAY_MS
private const val SETTLE_MS = 1_000L
private const val CLEAR_MS = 400L

/**
 * Fork: the occasional donate box on Home, 7 days after this feature first runs and at most every 14
 * days after that. It follows the launch banners' manners: one decision per launch, never over another
 * dialog or Now Playing, and a launch where the user has already started leaves it for a later one.
 * The next date is stored as soon as the box appears, so dismissing it any way counts as "later".
 */
@Composable
fun DonatePrompt(
    blocked: Boolean,
    userStarted: Boolean,
    awaitLaunchSettled: suspend () -> Unit,
) {
    val dataStore: DataStoreManager = koinInject()
    val started by rememberUpdatedState(userStarted)
    var decided by rememberSaveable { mutableStateOf(false) }
    var due by rememberSaveable { mutableStateOf(false) }
    var showPrompt by rememberSaveable { mutableStateOf(false) }
    var showQr by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (decided) return@LaunchedEffect
        decided = true
        awaitLaunchSettled()
        delay(SETTLE_MS)
        // The owner's own build, with the personal tools shown, is never asked.
        if (dataStore.getString(PERSONAL_TOOLS_KEY).first() == DataStoreManager.TRUE) return@LaunchedEffect
        val now = System.currentTimeMillis()
        val nextAt = dataStore.getString(KEY_NEXT_PROMPT_AT).first()?.toLongOrNull()
        if (nextAt == null) {
            dataStore.putString(KEY_NEXT_PROMPT_AT, (now + FIRST_PROMPT_AFTER_MS).toString())
        } else {
            due = now >= nextAt
        }
    }
    LaunchedEffect(blocked, due) {
        if (blocked || !due) return@LaunchedEffect
        delay(CLEAR_MS)
        if (!started) {
            dataStore.putString(KEY_NEXT_PROMPT_AT, (System.currentTimeMillis() + REPEAT_PROMPT_AFTER_MS).toString())
            showPrompt = true
        }
        // Last: `due` keys this effect, so changing it any earlier would cancel the work above.
        due = false
    }

    if (showPrompt) {
        AlertDialog(
            onDismissRequest = { showPrompt = false },
            title = { Text(stringResource(Res.string.donate_prompt_title)) },
            text = { Text(stringResource(Res.string.donate_prompt_message)) },
            confirmButton = {
                FilledTonalButton(onClick = {
                    showPrompt = false
                    showQr = true
                }) {
                    Text(stringResource(Res.string.donate), color = LocalContentColor.current)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrompt = false }) {
                    Text(stringResource(Res.string.later), color = LocalContentColor.current)
                }
            },
        )
    }
    if (showQr) DonateQrDialog(onDismiss = { showQr = false })
}

/** The QR, with Save QR because a phone cannot scan its own screen: MoMo and bank apps read it from the gallery. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateQrDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    val savedMessage =
        stringResource(
            if (getPlatform() == Platform.Desktop) Res.string.share_lyrics_saved_desktop else Res.string.share_lyrics_saved,
        )
    val saveFailedMessage = stringResource(Res.string.share_lyrics_save_failed)
    val permissionDeniedMessage = stringResource(Res.string.share_lyrics_permission_denied)
    val savePermission =
        rememberSaveImagePermission { granted ->
            if (!granted) {
                showToast(permissionDeniedMessage, ToastGravity.Bottom)
                saving = false
                return@rememberSaveImagePermission
            }
            scope.launch {
                val ok =
                    runCatching { saveImageToDevice(Res.readBytes("drawable/donate_qr.png"), "NhacNheo_donate_QR.png") }
                        .getOrDefault(false)
                showToast(if (ok) savedMessage else saveFailedMessage, ToastGravity.Bottom)
                saving = false
            }
        }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(Res.string.donate),
                    style = typo().titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                // The tile is white in the picture itself: a QR needs dark modules on a light ground to
                // scan, whatever colour the dialog is.
                Image(
                    painterResource(Res.drawable.donate_qr),
                    contentDescription = stringResource(Res.string.donate_qr_description),
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(Res.string.donate_qr_hint),
                    style = typo().bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                )
                Button(
                    enabled = !saving,
                    onClick = {
                        saving = true
                        savePermission.requestIfNeeded()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.donate_save_qr), color = LocalContentColor.current)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.donate_close), color = LocalContentColor.current)
                }
            }
        }
    }
}
