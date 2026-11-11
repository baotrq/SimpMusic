package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.simpmusic.utils.ForkBuildCheck
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.fork_update_available
import simpmusic.composeapp.generated.resources.fork_update_not_now
import simpmusic.composeapp.generated.resources.fork_update_now

private const val KEY_DISMISSED_TAG = "fork_update_dismissed_tag"
private const val CLEAR_MS = 400L

/**
 * Fork: a new build's notice, a window of its own in the middle of the screen so no button can sit over it.
 * It waits for [blocked] (another dialog, Now Playing, Home not in front) to stay false for a moment,
 * then stays up until the user answers. "Not now" hides it until a newer release appears; tapping outside
 * or Back only closes it for this launch. [onShowing] tells Home, so the launch banners wait behind it.
 */
@Composable
fun ForkUpdateDialog(
    blocked: Boolean,
    onShowing: (Boolean) -> Unit,
) {
    val available by ForkBuildCheck.available.collectAsStateWithLifecycle()
    val build = available
    val dataStore: DataStoreManager = koinInject()
    val dismissedTag by remember(dataStore) { dataStore.getString(KEY_DISMISSED_TAG) }
        .collectAsStateWithLifecycle(build?.tag)
    var closedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var shownTag by rememberSaveable { mutableStateOf<String?>(null) }
    val wanted = build != null && dismissedTag != build.tag && closedTag != build.tag

    LaunchedEffect(wanted, blocked, build?.tag) {
        if (!wanted || blocked || shownTag == build?.tag) return@LaunchedEffect
        // Not while another window is about to open: it must stay clear for a moment first.
        delay(CLEAR_MS)
        shownTag = build?.tag
    }
    val showing = wanted && shownTag == build?.tag
    LaunchedEffect(showing) { onShowing(showing) }
    if (!showing || build == null) return

    val status by ForkBuildCheck.status.collectAsStateWithLifecycle()
    val downloading by ForkBuildCheck.downloading.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!downloading) closedTag = build.tag },
        title = { Text(stringResource(Res.string.fork_update_available)) },
        text = {
            Column {
                Text(build.title)
                status?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !downloading,
                onClick = { scope.launch { dataStore.putString(KEY_DISMISSED_TAG, build.tag) } },
            ) {
                Text(stringResource(Res.string.fork_update_not_now), color = LocalContentColor.current)
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = { ForkBuildCheck.install?.invoke() }, enabled = !downloading) {
                Text(stringResource(Res.string.fork_update_now), color = LocalContentColor.current)
            }
        },
    )
}
