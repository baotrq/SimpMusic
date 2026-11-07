package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.ForkBuildCheck
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.fork_update_available
import simpmusic.composeapp.generated.resources.fork_update_not_now
import simpmusic.composeapp.generated.resources.fork_update_now

private const val KEY_DISMISSED_TAG = "fork_update_dismissed_tag"

/** Fork: a new build's soft notice on Home. "Not now" hides it until a newer release appears. */
@Composable
fun ForkUpdateCard(modifier: Modifier = Modifier) {
    val available by ForkBuildCheck.available.collectAsStateWithLifecycle()
    val build = available ?: return
    val dataStore: DataStoreManager = koinInject()
    val dismissedTag by remember(dataStore) { dataStore.getString(KEY_DISMISSED_TAG) }
        .collectAsStateWithLifecycle(build.tag)
    if (dismissedTag == build.tag) return
    val scope = rememberCoroutineScope()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth().padding(bottom = 12.dp),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Text(stringResource(Res.string.fork_update_available), style = typo().titleSmall)
            Text(
                build.title,
                style = typo().bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, end = 8.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                TextButton(onClick = { scope.launch { dataStore.putString(KEY_DISMISSED_TAG, build.tag) } }) {
                    Text(stringResource(Res.string.fork_update_not_now), color = LocalContentColor.current)
                }
                FilledTonalButton(onClick = { ForkBuildCheck.install?.invoke() }) {
                    Text(stringResource(Res.string.fork_update_now), color = LocalContentColor.current)
                }
            }
        }
    }
}
