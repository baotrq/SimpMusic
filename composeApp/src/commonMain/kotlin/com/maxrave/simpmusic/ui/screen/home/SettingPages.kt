package com.maxrave.simpmusic.ui.screen.home

import simpmusic.composeapp.generated.resources.ai_guide_openai_models
import simpmusic.composeapp.generated.resources.ai_guide_gemini_models
import simpmusic.composeapp.generated.resources.ai_guide_step_4
import androidx.compose.material3.OutlinedButton
import simpmusic.composeapp.generated.resources.style_player_nhac_nheo
import simpmusic.composeapp.generated.resources.now_playing_style_nhac_nheo
import simpmusic.composeapp.generated.resources.ai_guide_get_openai_key
import simpmusic.composeapp.generated.resources.ai_guide_get_gemini_key
import simpmusic.composeapp.generated.resources.ai_guide_privacy
import simpmusic.composeapp.generated.resources.ai_guide_gemini_free
import simpmusic.composeapp.generated.resources.ai_guide_step_3
import simpmusic.composeapp.generated.resources.ai_guide_step_2
import simpmusic.composeapp.generated.resources.ai_guide_step_1
import simpmusic.composeapp.generated.resources.ai_guide_title
import simpmusic.composeapp.generated.resources.ai_translation
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.FilledTonalButton
import com.maxrave.simpmusic.ui.screen.player.content.NOW_PLAYING_STYLE_NHAC_NHEO
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.simpmusic.ui.component.DonateQrDialog
import com.maxrave.simpmusic.ui.component.SettingGroup
import com.maxrave.simpmusic.ui.icon.ArrowForwardIos
import com.maxrave.simpmusic.ui.icon.Favorite
import com.maxrave.simpmusic.ui.icon.GraphicEq
import com.maxrave.simpmusic.ui.icon.Info
import com.maxrave.simpmusic.ui.icon.LogoDev
import com.maxrave.simpmusic.ui.icon.Lyrics
import com.maxrave.simpmusic.ui.icon.Palette
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Storage
import com.maxrave.simpmusic.ui.icon.Sync
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.about
import simpmusic.composeapp.generated.resources.developer_option
import simpmusic.composeapp.generated.resources.donate
import simpmusic.composeapp.generated.resources.donate_summary
import simpmusic.composeapp.generated.resources.lyrics_style
import simpmusic.composeapp.generated.resources.lyrics_style_apple_music
import simpmusic.composeapp.generated.resources.lyrics_style_classic
import simpmusic.composeapp.generated.resources.now_playing_style
import simpmusic.composeapp.generated.resources.now_playing_style_apple_music
import simpmusic.composeapp.generated.resources.now_playing_style_m3_expressive
import simpmusic.composeapp.generated.resources.now_playing_style_spotify
import simpmusic.composeapp.generated.resources.requires_android_12
import simpmusic.composeapp.generated.resources.settings_about_summary
import simpmusic.composeapp.generated.resources.settings_accounts_sync
import simpmusic.composeapp.generated.resources.settings_accounts_sync_summary
import simpmusic.composeapp.generated.resources.settings_appearance
import simpmusic.composeapp.generated.resources.settings_appearance_summary
import simpmusic.composeapp.generated.resources.settings_audio_playback
import simpmusic.composeapp.generated.resources.settings_audio_playback_summary
import simpmusic.composeapp.generated.resources.settings_developer_summary
import simpmusic.composeapp.generated.resources.settings_lyrics_ai
import simpmusic.composeapp.generated.resources.settings_lyrics_ai_summary
import simpmusic.composeapp.generated.resources.settings_storage_backup
import simpmusic.composeapp.generated.resources.settings_storage_backup_summary
import simpmusic.composeapp.generated.resources.settings_style_hint
import simpmusic.composeapp.generated.resources.style_lyrics_apple_music
import simpmusic.composeapp.generated.resources.style_lyrics_classic
import simpmusic.composeapp.generated.resources.style_player_m3_expressive
import simpmusic.composeapp.generated.resources.style_player_spotify

/**
 * Fork: the pages Settings is split into, phone-settings style. The main page lists the top-level
 * ones; [parent] marks a page reached from inside another, which Back returns to.
 */
internal enum class SettingPage(
    val parent: SettingPage? = null,
) {
    APPEARANCE,
    AUDIO_PLAYBACK,
    LYRICS_AI,
    ACCOUNTS_SYNC,
    STORAGE_BACKUP,
    ABOUT,
    DEVELOPER,
    PLAYER_STYLE(APPEARANCE),
    LYRICS_STYLE(APPEARANCE),
    AI_TRANSLATION(LYRICS_AI),
}

// Depth decides the direction: the main page is 0, a top-level page 1, a page inside one 2.
private val SettingPage?.depth: Int get() = if (this == null) 0 else if (parent == null) 1 else 2

/**
 * The app's own page motion (AppNavigationGraph): going deeper slides in from the right, Back slides
 * out to the right, with the same fade from half. Between pages of one level (Desktop's right
 * pane) the one further down the list counts as "deeper".
 */
internal fun AnimatedContentTransitionScope<SettingPage?>.settingPageTransition(): ContentTransform {
    val forward =
        targetState.depth > initialState.depth ||
            (targetState.depth == initialState.depth && (targetState?.ordinal ?: -1) > (initialState?.ordinal ?: -1))
    val direction = if (forward) 1 else -1
    return (fadeIn(initialAlpha = PAGE_FADE) + slideInHorizontally { it * direction }) togetherWith
        (fadeOut(targetAlpha = PAGE_FADE) + slideOutHorizontally { -it * direction })
}

private const val PAGE_FADE = 0.5f

@Composable
internal fun SettingPage.title(): String =
    stringResource(
        when (this) {
            SettingPage.APPEARANCE -> Res.string.settings_appearance
            SettingPage.AUDIO_PLAYBACK -> Res.string.settings_audio_playback
            SettingPage.LYRICS_AI -> Res.string.settings_lyrics_ai
            SettingPage.ACCOUNTS_SYNC -> Res.string.settings_accounts_sync
            SettingPage.STORAGE_BACKUP -> Res.string.settings_storage_backup
            SettingPage.ABOUT -> Res.string.about
            SettingPage.DEVELOPER -> Res.string.developer_option
            SettingPage.PLAYER_STYLE -> Res.string.now_playing_style
            SettingPage.LYRICS_STYLE -> Res.string.lyrics_style
            SettingPage.AI_TRANSLATION -> Res.string.ai_translation
        },
    )

/** The main page: one row per top-level page; [selected] is highlighted (Desktop's left pane). */
@Composable
internal fun SettingHome(
    selected: SettingPage?,
    showDonate: Boolean,
    onOpen: (SettingPage) -> Unit,
) {
    SettingGroup(title = null) {
        SettingCategoryRow(SettingPage.APPEARANCE, SimpIcons.Palette, Res.string.settings_appearance_summary, selected, onOpen)
        SettingCategoryRow(SettingPage.AUDIO_PLAYBACK, SimpIcons.GraphicEq, Res.string.settings_audio_playback_summary, selected, onOpen)
        SettingCategoryRow(SettingPage.LYRICS_AI, SimpIcons.Lyrics, Res.string.settings_lyrics_ai_summary, selected, onOpen)
        SettingCategoryRow(SettingPage.ACCOUNTS_SYNC, SimpIcons.Sync, Res.string.settings_accounts_sync_summary, selected, onOpen)
        SettingCategoryRow(SettingPage.STORAGE_BACKUP, SimpIcons.Storage, Res.string.settings_storage_backup_summary, selected, onOpen)
        SettingCategoryRow(SettingPage.ABOUT, SimpIcons.Info, Res.string.settings_about_summary, selected, onOpen)
    }
    var showDonateQr by rememberSaveable { mutableStateOf(false) }
    if (showDonate) {
        SettingGroup(title = null) {
            SettingHomeRow(
                title = stringResource(Res.string.donate),
                icon = SimpIcons.Favorite,
                summary = Res.string.donate_summary,
                highlighted = false,
                onClick = { showDonateQr = true },
            )
        }
    }
    if (showDonateQr) DonateQrDialog(onDismiss = { showDonateQr = false })
    SettingGroup(title = null) {
        SettingCategoryRow(SettingPage.DEVELOPER, SimpIcons.LogoDev, Res.string.settings_developer_summary, selected, onOpen)
    }
}

@Composable
private fun SettingCategoryRow(
    page: SettingPage,
    icon: ImageVector,
    summary: org.jetbrains.compose.resources.StringResource,
    selected: SettingPage?,
    onOpen: (SettingPage) -> Unit,
) = SettingHomeRow(page.title(), icon, summary, page == selected) { onOpen(page) }

@Composable
private fun SettingHomeRow(
    title: String,
    icon: ImageVector,
    summary: org.jetbrains.compose.resources.StringResource,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                // Hairline above each row from where the text starts, like SettingItem's; the card
                // clips the first row's away.
                .drawBehind {
                    val inset = (16 + 36 + 16).dp.toPx()
                    val rtl = layoutDirection == LayoutDirection.Rtl
                    drawLine(
                        color = dividerColor,
                        start = Offset(if (rtl) 0f else inset, 0f),
                        end = Offset(if (rtl) size.width - inset else size.width, 0f),
                        strokeWidth = 0.5.dp.toPx(),
                    )
                }.background(if (highlighted) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = typo().titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(summary), style = typo().bodySmall)
        }
        Icon(
            SimpIcons.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
    }
}

internal class StyleOption(
    val key: String,
    val label: String,
    val image: DrawableResource?,
    val note: String? = null,
)

@Composable
internal fun playerStyleOptions(): List<StyleOption> =
    listOf(
        StyleOption(
            DataStoreManager.NOW_PLAYING_STYLE_SPOTIFY,
            stringResource(Res.string.now_playing_style_spotify),
            Res.drawable.style_player_spotify,
        ),
        StyleOption(
            DataStoreManager.NOW_PLAYING_STYLE_M3_EXPRESSIVE,
            stringResource(Res.string.now_playing_style_m3_expressive),
            Res.drawable.style_player_m3_expressive,
        ),
        StyleOption(
            NOW_PLAYING_STYLE_NHAC_NHEO,
            stringResource(Res.string.now_playing_style_nhac_nheo),
            Res.drawable.style_player_nhac_nheo,
            stringResource(Res.string.requires_android_12),
        ),
        StyleOption(
            DataStoreManager.NOW_PLAYING_STYLE_APPLE_MUSIC,
            stringResource(Res.string.now_playing_style_apple_music),
            null,
            stringResource(Res.string.requires_android_12),
        ),
    )

@Composable
internal fun lyricsStyleOptions(): List<StyleOption> =
    listOf(
        StyleOption(
            DataStoreManager.LYRICS_STYLE_CLASSIC,
            stringResource(Res.string.lyrics_style_classic),
            Res.drawable.style_lyrics_classic,
        ),
        StyleOption(
            DataStoreManager.LYRICS_STYLE_APPLE_MUSIC,
            stringResource(Res.string.lyrics_style_apple_music),
            Res.drawable.style_lyrics_apple_music,
            stringResource(Res.string.requires_android_12),
        ),
    )

/** A page of style previews (real screenshots) side by side; tapping one applies it at once. */
@Composable
internal fun StylePreviewPage(
    options: List<StyleOption>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    Text(
        stringResource(Res.string.settings_style_hint),
        style = typo().bodySmall,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        options.forEach { option ->
            val isSelected = option.key == selected
            val borderWidth by animateDpAsState(if (isSelected) 3.dp else 1.dp)
            val borderColor by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            )
            val scale by animateFloatAsState(if (isSelected) 1f else 0.96f)
            val shape = RoundedCornerShape(20.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .width(150.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }.clip(shape)
                        .clickable { onSelect(option.key) },
            ) {
                val frame =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(SCREENSHOT_ASPECT)
                        .clip(shape)
                        .border(width = borderWidth, color = borderColor, shape = shape)
                if (option.image != null) {
                    Image(
                        painter = painterResource(option.image),
                        contentDescription = option.label,
                        contentScale = ContentScale.Crop,
                        modifier = frame,
                    )
                } else {
                    Box(frame.background(MaterialTheme.colorScheme.surfaceContainerHigh))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    RadioButton(selected = isSelected, onClick = { onSelect(option.key) })
                    Text(option.label, style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                option.note?.let { Text(it, style = typo().bodySmall) }
            }
        }
    }
}

// The screenshots are full phone screens (1080×2354).
private const val SCREENSHOT_ASPECT = 1080f / 2354f

/** Fork: how to get a key for the chosen provider, above the AI settings. */
@Composable
internal fun AiKeyGuide(provider: String) {
    val uriHandler = LocalUriHandler.current
    val keyPage =
        when (provider) {
            DataStoreManager.AI_PROVIDER_GEMINI -> "https://aistudio.google.com/apikey" to Res.string.ai_guide_get_gemini_key
            DataStoreManager.AI_PROVIDER_OPENAI -> "https://platform.openai.com/api-keys" to Res.string.ai_guide_get_openai_key
            else -> null
        }
    val modelsPage =
        when (provider) {
            DataStoreManager.AI_PROVIDER_GEMINI -> "https://ai.google.dev/gemini-api/docs/models" to Res.string.ai_guide_gemini_models
            DataStoreManager.AI_PROVIDER_OPENAI -> "https://platform.openai.com/docs/models" to Res.string.ai_guide_openai_models
            else -> null
        }
    SettingGroup(title = stringResource(Res.string.ai_guide_title)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.ai_guide_step_1), style = typo().bodyMedium)
            Text(stringResource(Res.string.ai_guide_step_2), style = typo().bodyMedium)
            Text(stringResource(Res.string.ai_guide_step_3), style = typo().bodyMedium)
            Text(stringResource(Res.string.ai_guide_step_4), style = typo().bodyMedium)
            if (provider == DataStoreManager.AI_PROVIDER_GEMINI) {
                Text(stringResource(Res.string.ai_guide_gemini_free), style = typo().bodySmall)
            }
            Text(stringResource(Res.string.ai_guide_privacy), style = typo().bodySmall)
            keyPage?.let { (url, label) ->
                FilledTonalButton(onClick = { uriHandler.openUri(url) }) {
                    Text(stringResource(label), color = LocalContentColor.current)
                }
            }
            modelsPage?.let { (url, label) ->
                OutlinedButton(onClick = { uriHandler.openUri(url) }) {
                    Text(stringResource(label), color = LocalContentColor.current)
                }
            }
        }
    }
}
