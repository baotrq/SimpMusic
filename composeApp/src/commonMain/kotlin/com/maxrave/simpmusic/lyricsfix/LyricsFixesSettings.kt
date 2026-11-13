package com.maxrave.simpmusic.lyricsfix

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.simpmusic.playbacksync.TextValueDialog
import com.maxrave.simpmusic.ui.component.SettingItem
import com.maxrave.simpmusic.ui.icon.Groups
import com.maxrave.simpmusic.ui.icon.Key
import com.maxrave.simpmusic.ui.icon.SimpIcons
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.community_lyrics_admin_key
import simpmusic.composeapp.generated.resources.community_lyrics_fixes
import simpmusic.composeapp.generated.resources.community_lyrics_fixes_description
import simpmusic.composeapp.generated.resources.playback_sync_key_set
import simpmusic.composeapp.generated.resources.playback_sync_not_set

/** Settings → Lyrics: whether this install asks for fixes approved for everyone. On unless turned off. */
@Composable
fun CommunityLyricsFixesItem() {
    val dataStore: DataStoreManager = koinInject()
    val scope = rememberCoroutineScope()
    val flag by remember { dataStore.getString(LyricsFixes.KEY_ENABLED) }.collectAsStateWithLifecycle(null)
    SettingItem(
        icon = SimpIcons.Groups,
        title = stringResource(Res.string.community_lyrics_fixes),
        subtitle = stringResource(Res.string.community_lyrics_fixes_description),
        switch =
            (flag != DataStoreManager.FALSE) to { on ->
                scope.launch { dataStore.putString(LyricsFixes.KEY_ENABLED, if (on) DataStoreManager.TRUE else DataStoreManager.FALSE) }
            },
    )
}

/** The owner's key, in the hidden tools: with it, "Share this fix" publishes at once instead of queueing. */
@Composable
fun LyricsFixesAdminKeyItem() {
    val dataStore: DataStoreManager = koinInject()
    val scope = rememberCoroutineScope()
    val key by remember { dataStore.getString(LyricsFixes.KEY_ADMIN) }.collectAsStateWithLifecycle(null)
    var editing by remember { mutableStateOf(false) }
    SettingItem(
        icon = SimpIcons.Key,
        title = stringResource(Res.string.community_lyrics_admin_key),
        subtitle = stringResource(if (key.isNullOrBlank()) Res.string.playback_sync_not_set else Res.string.playback_sync_key_set),
        onClick = { editing = true },
    )
    if (editing) {
        TextValueDialog(
            title = stringResource(Res.string.community_lyrics_admin_key),
            initial = key.orEmpty(),
            hidden = true,
            onDismiss = { editing = false },
            onSave = { value ->
                scope.launch { dataStore.putString(LyricsFixes.KEY_ADMIN, value.trim()) }
                editing = false
            },
        )
    }
}
