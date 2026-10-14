package com.maxrave.simpmusic.playbacksync

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.ui.component.SettingItem
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.continue_from_device
import simpmusic.composeapp.generated.resources.continue_listening
import simpmusic.composeapp.generated.resources.dismiss
import simpmusic.composeapp.generated.resources.playback_sync_device_name
import simpmusic.composeapp.generated.resources.playback_sync_failed
import simpmusic.composeapp.generated.resources.playback_sync_incomplete
import simpmusic.composeapp.generated.resources.playback_sync_key
import simpmusic.composeapp.generated.resources.playback_sync_key_set
import simpmusic.composeapp.generated.resources.playback_sync_not_set
import simpmusic.composeapp.generated.resources.playback_sync_ok
import simpmusic.composeapp.generated.resources.playback_sync_server
import simpmusic.composeapp.generated.resources.playback_sync_test
import simpmusic.composeapp.generated.resources.playback_sync_test_hint
import simpmusic.composeapp.generated.resources.playback_sync_testing
import simpmusic.composeapp.generated.resources.playback_sync_unauthorized
import simpmusic.composeapp.generated.resources.save

/** Rows of the "Playback sync" settings group. */
@Composable
fun ColumnScope.PlaybackSyncSettingItems() {
    val dataStore: DataStoreManager = koinInject()
    val scope = rememberCoroutineScope()
    val url by remember { dataStore.getString(PlaybackSync.KEY_URL) }.collectAsStateWithLifecycle(null)
    val secret by remember { dataStore.getString(PlaybackSync.KEY_SECRET) }.collectAsStateWithLifecycle(null)
    val name by remember { dataStore.getString(PlaybackSync.KEY_DEVICE_NAME) }.collectAsStateWithLifecycle(null)

    // The DataStore key being edited, or null while no dialog is open.
    var editing by remember { mutableStateOf<String?>(null) }
    var check by remember { mutableStateOf<PlaybackSync.ConnectionCheck?>(null) }
    var checking by remember { mutableStateOf(false) }
    val notSet = stringResource(Res.string.playback_sync_not_set)

    SettingItem(
        title = stringResource(Res.string.playback_sync_server),
        subtitle = url?.takeIf { it.isNotBlank() } ?: notSet,
        onClick = { editing = PlaybackSync.KEY_URL },
    )
    SettingItem(
        title = stringResource(Res.string.playback_sync_key),
        subtitle = if (secret.isNullOrBlank()) notSet else stringResource(Res.string.playback_sync_key_set),
        onClick = { editing = PlaybackSync.KEY_SECRET },
    )
    SettingItem(
        title = stringResource(Res.string.playback_sync_device_name),
        subtitle = name?.takeIf { it.isNotBlank() } ?: PlaybackSync.defaultDeviceName,
        onClick = { editing = PlaybackSync.KEY_DEVICE_NAME },
    )
    SettingItem(
        title = stringResource(Res.string.playback_sync_test),
        subtitle =
            stringResource(
                when {
                    checking -> Res.string.playback_sync_testing
                    else ->
                        when (check) {
                            null -> Res.string.playback_sync_test_hint
                            PlaybackSync.ConnectionCheck.OK -> Res.string.playback_sync_ok
                            PlaybackSync.ConnectionCheck.NOT_SET -> Res.string.playback_sync_incomplete
                            PlaybackSync.ConnectionCheck.UNAUTHORIZED -> Res.string.playback_sync_unauthorized
                            PlaybackSync.ConnectionCheck.FAILED -> Res.string.playback_sync_failed
                        }
                },
            ),
        onClick = {
            if (!checking) {
                scope.launch {
                    checking = true
                    check = PlaybackSync.checkConnection()
                    checking = false
                }
            }
        },
    )

    editing?.let { key ->
        TextValueDialog(
            title =
                stringResource(
                    when (key) {
                        PlaybackSync.KEY_URL -> Res.string.playback_sync_server
                        PlaybackSync.KEY_SECRET -> Res.string.playback_sync_key
                        else -> Res.string.playback_sync_device_name
                    },
                ),
            initial =
                when (key) {
                    PlaybackSync.KEY_URL -> url
                    PlaybackSync.KEY_SECRET -> secret
                    else -> name
                }.orEmpty(),
            hidden = key == PlaybackSync.KEY_SECRET,
            onDismiss = { editing = null },
            onSave = { value ->
                scope.launch { dataStore.putString(key, value.trim()) }
                check = null
                editing = null
            },
        )
    }
}

@Composable
private fun TextValueDialog(
    title: String,
    initial: String,
    hidden: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = typo().titleMedium) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text(stringResource(Res.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

/**
 * Home's slot for [ContinueFromDeviceCard]: it fades and folds away, so Home slides up into its place
 * instead of jumping. The last snapshot is kept so the card still has content while it leaves.
 */
@Composable
fun ContinueFromDeviceSlot(
    snapshot: PlaybackSnapshot?,
    onContinue: (PlaybackSnapshot) -> Unit,
    onDismiss: (PlaybackSnapshot) -> Unit,
) {
    val shown = remember { mutableStateOf(snapshot) }
    if (snapshot != null) shown.value = snapshot
    val spec = tween<IntSize>(durationMillis = 350, easing = FastOutSlowInEasing)
    AnimatedVisibility(
        visible = snapshot != null,
        enter = fadeIn(tween(350)) + expandVertically(spec),
        exit = fadeOut(tween(250)) + shrinkVertically(spec),
    ) {
        shown.value?.let { last ->
            Column {
                ContinueFromDeviceCard(last, onContinue = { onContinue(last) }, onDismiss = { onDismiss(last) })
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** Home's offer to continue what another phone was playing. */
@Composable
private fun ContinueFromDeviceCard(
    snapshot: PlaybackSnapshot,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    val track = snapshot.track ?: return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f))
                .clickable(onClick = onContinue)
                .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
    ) {
        AsyncImage(
            model = track.thumbnails?.lastOrNull()?.url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.continue_from_device, snapshot.deviceName),
                style = typo().labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.title,
                style = typo().titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    listOfNotNull(
                        track.artists?.joinToString(", ") { it.name }?.takeIf { it.isNotBlank() },
                        formatDuration(snapshot.positionMs),
                    ).joinToString(" · "),
                style = typo().bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onContinue) {
            Icon(SimpIcons.PlayArrow, contentDescription = stringResource(Res.string.continue_listening))
        }
        IconButton(onClick = onDismiss) {
            Icon(SimpIcons.Close, contentDescription = stringResource(Res.string.dismiss))
        }
    }
}
