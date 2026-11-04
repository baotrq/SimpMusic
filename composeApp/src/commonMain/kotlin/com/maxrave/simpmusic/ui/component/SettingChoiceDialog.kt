package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.Remove
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.lyrics_offset_reset
import simpmusic.composeapp.generated.resources.lyrics_offset_value
import simpmusic.composeapp.generated.resources.search
import java.text.Normalizer
import java.util.Locale
import kotlin.math.roundToInt

/** Lists longer than this get a search box. */
private const val SEARCH_THRESHOLD = 12

/**
 * Fork: the pick-one list for a setting (Language, Country, Lyrics provider…), as a popup in the
 * Settings groups' card style. A tap applies the option at once and the popup stays, showing the new
 * choice; tapping outside closes it. [closeOnSelect] closes it instead, for a choice that opens
 * something of its own (the app language asks to restart). [label] is what a row shows and what
 * search matches, [secondary] a smaller line beside it, [emoji] a mark in front, such as a flag.
 */
@Composable
fun SettingChoiceDialog(
    title: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    label: (String) -> String = { it },
    secondary: (String) -> String? = { null },
    emoji: (String) -> String? = { null },
    closeOnSelect: Boolean = false,
) {
    var current by remember { mutableStateOf(selected) }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(options, query) { options.matching(query, label, secondary) }
    // Opens with the current choice in view, a couple of rows down from the top.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (options.indexOf(selected) - 2).coerceAtLeast(0))

    ChoiceDialogFrame(title = title, onDismiss = onDismiss) {
        if (options.size > SEARCH_THRESHOLD) ChoiceSearchField(query = query, onQueryChange = { query = it })
        ChoiceCard {
            LazyColumn(state = listState) {
                itemsIndexed(shown) { index, option ->
                    ChoiceRow(
                        text = label(option),
                        secondary = secondary(option),
                        emoji = emoji(option),
                        selected = option == current,
                        showDivider = index > 0,
                        trailing = { isSelected ->
                            if (isSelected) {
                                Icon(SimpIcons.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        },
                        onClick = {
                            if (option != current) {
                                current = option
                                onSelect(option)
                            }
                            if (closeOnSelect) onDismiss()
                        },
                    )
                }
            }
        }
    }
}

/**
 * Fork: the pick-several list for a setting (Lyrics romanization, SponsorBlock categories), in the
 * same popup as [SettingChoiceDialog]. Every tap toggles a row and applies the whole selection.
 */
@Composable
fun SettingMultiChoiceDialog(
    title: String,
    options: List<String>,
    initiallySelected: List<String>,
    onChange: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var chosen by remember { mutableStateOf(initiallySelected.toSet()) }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(options, query) { options.matching(query, { it }, { null }) }

    ChoiceDialogFrame(title = title, onDismiss = onDismiss) {
        if (options.size > SEARCH_THRESHOLD) ChoiceSearchField(query = query, onQueryChange = { query = it })
        ChoiceCard {
            LazyColumn {
                itemsIndexed(shown) { index, option ->
                    ChoiceRow(
                        text = option,
                        secondary = null,
                        emoji = null,
                        selected = option in chosen,
                        showDivider = index > 0,
                        trailing = { isSelected -> CheckMark(isSelected) },
                        onClick = {
                            chosen = if (option in chosen) chosen - option else chosen + option
                            // Keeps the list's own order, not the order the rows were tapped in.
                            onChange(options.filter { it in chosen })
                        },
                    )
                }
            }
        }
    }
}

/**
 * Fork: the lyrics timing offset as a dial instead of a text box: the value large, ±10 ms buttons and a
 * slider for ±2 s, and Reset. Applies as it changes (the slider when let go); tapping outside closes.
 */
@Composable
fun LyricsOffsetDialog(
    title: String,
    message: String,
    valueMs: Int,
    onChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableIntStateOf(valueMs) }
    val apply: (Int) -> Unit = { next ->
        value = next
        onChange(next)
    }
    ChoiceDialogFrame(title = title, onDismiss = onDismiss) {
        Text(
            text = stringResource(Res.string.lyrics_offset_value, if (value > 0) "+$value" else value.toString()),
            style = typo().titleLarge.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { apply(value - 10) }) {
                Icon(SimpIcons.Remove, contentDescription = "-10 ms")
            }
            Slider(
                value = value.coerceIn(-OFFSET_SLIDER_RANGE, OFFSET_SLIDER_RANGE).toFloat(),
                onValueChange = { value = (it / 10f).roundToInt() * 10 },
                onValueChangeFinished = { onChange(value) },
                valueRange = -OFFSET_SLIDER_RANGE.toFloat()..OFFSET_SLIDER_RANGE.toFloat(),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            FilledTonalIconButton(onClick = { apply(value + 10) }) {
                Icon(SimpIcons.Add, contentDescription = "+10 ms")
            }
        }
        Text(
            text = message,
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp),
        )
        TextButton(
            onClick = { apply(0) },
            enabled = value != 0,
            modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
        ) {
            Text(stringResource(Res.string.lyrics_offset_reset), color = LocalContentColor.current)
        }
    }
}

private const val OFFSET_SLIDER_RANGE = 2000

/** The flag emoji for a two-letter country code, built from regional indicator letters. */
fun countryFlag(code: String): String? {
    if (code.length != 2 || !code.all { it in 'A'..'Z' }) return null
    return buildString { code.forEach { appendCodePoint(0x1F1E6 + (it - 'A')) } }
}

/** A country code's name in the app's language ("VN" → "Vietnam" / "Việt Nam"). */
fun countryName(code: String): String = Locale("", code).getDisplayCountry(Locale.getDefault()).ifBlank { code }

/** A two-letter language code's name in the app's language ("ja" → "Japanese" / "Tiếng Nhật"). */
fun languageName(code: String): String =
    Locale(code).getDisplayLanguage(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }.ifBlank { code }

/** The language's own name for itself ("ja" → "日本語"), or null when it reads the same as [languageName]. */
fun languageNativeName(code: String): String? =
    Locale(code).let { it.getDisplayLanguage(it) }.replaceFirstChar { it.titlecase(Locale(code)) }.takeIf {
        it.isNotBlank() && it != code && it != languageName(code)
    }

/** Every two-letter language code the system can name, sorted by that name. */
fun namedLanguageCodes(): List<String> =
    Locale
        .getISOLanguages()
        .filter { Locale(it).getDisplayLanguage(Locale.getDefault()).let { name -> name.isNotBlank() && name != it } }
        .sortedBy { languageName(it).searchKey() }

@Composable
private fun ChoiceDialogFrame(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(0.9f).widthIn(max = 440.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    style = typo().titleLarge.copy(fontSize = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp, bottom = 16.dp),
                )
                content()
            }
        }
    }
}

@Composable
private fun ChoiceSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val fieldColor = MaterialTheme.colorScheme.surfaceContainerHighest
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(Res.string.search)) },
        leadingIcon = { Icon(SimpIcons.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(SimpIcons.Close, contentDescription = null)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = fieldColor,
                unfocusedContainerColor = fieldColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    )
}

@Composable
private fun ChoiceCard(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 440.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) { content() }
}

@Composable
private fun ChoiceRow(
    text: String,
    secondary: String?,
    emoji: String?,
    selected: Boolean,
    showDivider: Boolean,
    trailing: @Composable (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    Column {
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (emoji != null) 56.dp else 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (emoji != null) {
                Text(text = emoji, fontSize = 22.sp, modifier = Modifier.width(40.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = typo().bodyLarge.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                if (secondary != null) {
                    Text(
                        text = secondary,
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            trailing(selected)
        }
    }
}

/** A round tick: filled with the accent when on, an empty ring when off. */
@Composable
private fun CheckMark(checked: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier =
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .then(
                    if (checked) {
                        Modifier.background(primary)
                    } else {
                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(SimpIcons.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

private fun List<String>.matching(
    query: String,
    label: (String) -> String,
    secondary: (String) -> String?,
): List<String> {
    val key = query.searchKey()
    if (key.isEmpty()) return this
    return filter { option ->
        listOfNotNull(label(option), secondary(option), option).any { it.searchKey().contains(key) }
    }
}

// Case and accents ignored, so "tieng viet" finds "Tiếng Việt".
private fun String.searchKey(): String =
    Normalizer
        .normalize(trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace('đ', 'd')
