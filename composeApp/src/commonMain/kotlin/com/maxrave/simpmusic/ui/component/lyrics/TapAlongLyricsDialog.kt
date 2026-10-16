package com.maxrave.simpmusic.ui.component.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.maxrave.simpmusic.ui.component.rememberSurfaceDarkColors
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.delete
import simpmusic.composeapp.generated.resources.save
import simpmusic.composeapp.generated.resources.tap_along
import simpmusic.composeapp.generated.resources.tap_along_add_after
import simpmusic.composeapp.generated.resources.tap_along_add_line
import simpmusic.composeapp.generated.resources.tap_along_edit_line
import simpmusic.composeapp.generated.resources.tap_along_hint
import simpmusic.composeapp.generated.resources.tap_along_new_line
import simpmusic.composeapp.generated.resources.tap_along_restart
import simpmusic.composeapp.generated.resources.tap_along_start
import simpmusic.composeapp.generated.resources.tap_along_tap
import simpmusic.composeapp.generated.resources.tap_along_undo

/**
 * Times a song's lyrics by hand: the song restarts, and each tap stamps the highlighted line with
 * the moment it was tapped. The global lyrics offset is taken off each stamp, because the display
 * adds it back when it reads them — so a tap made while hearing the line through a lagging
 * Bluetooth path lands where the display expects it, and so is [TAP_REACTION_MS], since a tap
 * lands a beat after the line is heard.
 *
 * Lines can be fixed while listening: tap a line to edit or delete it, or add a missing one where
 * the playhead is ("Add line here" goes in front of the line waiting for its tap).
 */
@Composable
fun TapAlongLyricsDialog(
    videoId: String,
    sourceLines: List<String>,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val colors = rememberSurfaceDarkColors()
    val offset by sharedViewModel.getLyricsOffsetMs().collectAsState(0)
    val lines = remember { sourceLines.toMutableStateList() }
    val stamps = remember { mutableStateListOf<Long>() }
    var started by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val done = lines.isNotEmpty() && stamps.size == lines.size
    // The line being edited: (index, isNew). A new line is inserted at index only when saved.
    var editing by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }

    // Where the song was paused for the line editor, or null if it was not playing.
    var pausedAt by remember { mutableStateOf<Long?>(null) }

    // The song waits while a line is being typed, so nothing goes by unheard.
    fun openEditor(target: Pair<Int, Boolean>) {
        if (editing == null && sharedViewModel.controllerState.value.isPlaying) {
            pausedAt = sharedViewModel.playerPositionMs()
            sharedViewModel.onUIEvent(UIEvent.PlayPause)
        }
        editing = target
    }

    // Resumes a few seconds back so the line being worked on can be caught again — or from the last
    // tap still standing, when taps after it were just dropped. [rewindTo] null = the default.
    fun closeEditor(rewindTo: Long? = null) {
        editing = null
        val at = pausedAt ?: return
        pausedAt = null
        if (started) sharedViewModel.seekToMs(rewindTo ?: maxOf(stamps.lastOrNull() ?: 0, at - 3_000))
        sharedViewModel.onUIEvent(UIEvent.PlayPause)
    }

    fun start() {
        stamps.clear()
        sharedViewModel.seekToMs(0)
        if (!sharedViewModel.controllerState.value.isPlaying) sharedViewModel.onUIEvent(UIEvent.PlayPause)
        started = true
    }

    LaunchedEffect(stamps.size) {
        listState.animateScrollToItem((stamps.size - 2).coerceAtLeast(0))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(colors.container)
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(SimpIcons.Close, contentDescription = stringResource(Res.string.cancel), tint = colors.content)
                }
                Text(
                    text = stringResource(Res.string.tap_along),
                    style = typo().titleMedium,
                    color = colors.content,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    enabled = done,
                    onClick = {
                        sharedViewModel.saveCustomLyrics(videoId, stamps.zip(lines))
                        onSaved()
                    },
                ) {
                    Text(stringResource(Res.string.save), style = typo().labelMedium, color = LocalContentColor.current)
                }
            }
            Text(
                text = stringResource(Res.string.tap_along_hint),
                style = typo().bodySmall,
                color = colors.subtitle,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(lines) { index, line ->
                    val current = started && index == stamps.size
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { openEditor(index to false) },
                    ) {
                        Text(
                            text = stamps.getOrNull(index)?.let(::formatStamp) ?: "",
                            style = typo().bodySmall,
                            color = colors.subtitle,
                            modifier = Modifier.width(64.dp),
                        )
                        Text(
                            text = line,
                            style = if (current) typo().titleMedium else typo().bodyLarge,
                            fontWeight = if (current) FontWeight.Bold else null,
                            color =
                                when {
                                    current -> MaterialTheme.colorScheme.primary
                                    index < stamps.size -> colors.subtitle
                                    else -> colors.content
                                },
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                OutlinedButton(
                    enabled = stamps.isNotEmpty(),
                    onClick = {
                        stamps.removeAt(stamps.lastIndex)
                        // Back to where the line before starts, so the undone one can be tapped again.
                        sharedViewModel.seekToMs(stamps.lastOrNull() ?: 0)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.tap_along_undo), color = LocalContentColor.current)
                }
                OutlinedButton(
                    onClick = { openEditor(stamps.size to true) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.tap_along_add_line), color = LocalContentColor.current)
                }
                OutlinedButton(
                    enabled = started,
                    onClick = { start() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.tap_along_restart), color = LocalContentColor.current)
                }
            }
            Button(
                enabled = !started || stamps.size < lines.size,
                onClick = {
                    if (!started) {
                        start()
                    } else {
                        val at = sharedViewModel.playerPositionMs() - offset - TAP_REACTION_MS
                        stamps.add(at.coerceAtLeast(stamps.lastOrNull() ?: 0))
                    }
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(112.dp).padding(bottom = 16.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(if (started) Res.string.tap_along_tap else Res.string.tap_along_start),
                        style = typo().titleLarge,
                        color = LocalContentColor.current,
                    )
                }
            }
        }
    }

    editing?.let { (index, isNew) ->
        key(index, isNew) {
            EditLineDialog(
                initial = if (isNew) "" else lines.getOrElse(index) { "" },
                isNew = isNew,
                onDismiss = { closeEditor() },
                onSave = { text ->
                    if (isNew) lines.add(index, text) else lines[index] = text
                    // A line added among the ones already tapped has no time, and neither does
                    // anything after it any more: those taps go, and the song picks up from the
                    // last one still standing.
                    if (isNew && index < stamps.size) {
                        stamps.removeRange(index, stamps.size)
                        closeEditor(rewindTo = stamps.lastOrNull() ?: 0)
                    } else {
                        closeEditor()
                    }
                },
                onDelete = {
                    lines.removeAt(index)
                    if (index < stamps.size) stamps.removeAt(index)
                    closeEditor()
                },
                onAddAfter = { openEditor(index + 1 to true) },
            )
        }
    }
}

@Composable
private fun EditLineDialog(
    initial: String,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onAddAfter: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = rememberSurfaceDarkColors().container,
        title = {
            Text(
                text = stringResource(if (isNew) Res.string.tap_along_new_line else Res.string.tap_along_edit_line),
                style = typo().titleMedium,
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.replace("\n", " ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!isNew) {
                    Row {
                        TextButton(onClick = onDelete) {
                            Text(stringResource(Res.string.delete), color = LocalContentColor.current)
                        }
                        TextButton(onClick = onAddAfter) {
                            Text(stringResource(Res.string.tap_along_add_after), color = LocalContentColor.current)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onSave(text.trim()) }) {
                Text(stringResource(Res.string.save), color = LocalContentColor.current)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel), color = LocalContentColor.current)
            }
        },
    )
}

/** How long after hearing a line a tap typically lands; taken off every stamp. */
private const val TAP_REACTION_MS = 250L

private fun formatStamp(ms: Long): String {
    val tenths = (ms / 100) % 10
    val seconds = (ms / 1000) % 60
    val minutes = ms / 60_000
    return "$minutes:${seconds.toString().padStart(2, '0')}.$tenths"
}
