package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.extension.smoothScrimBrush
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PodcastDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.HomeViewModel
import kotlinx.coroutines.launch

/**
 * Fork: YouTube Music's "Speed dial" — the shelf as pages of a 3×3 grid of square covers, each with
 * its title over the bottom of the cover, swiped sideways a page at a time with dots underneath.
 * Used only for a shelf YouTube itself titles "Speed dial" ([isSpeedDialShelf]); every other shelf
 * keeps its row.
 *
 * A tile opens what its card would ([openFromHome]): a song or video starts its radio, anything
 * else opens its page. A long press opens the song sheet, as on the cards.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeSpeedDial(
    data: HomeItem,
    navController: NavController,
    homeViewModel: HomeViewModel,
    // Fork: what the dice draws from — the songs on the rest of Home, not this shelf's own (those
    // are songs you just played; the dice is for something you were not going to pick).
    randomPool: List<Content>,
) {
    val tiles = remember(data.contents) { data.contents.filterNotNull() }
    val playable = remember(randomPool) { randomPool.filter { !it.videoId.isNullOrEmpty() } }
    if (tiles.isEmpty()) return

    var sheetTrack by remember { mutableStateOf<Track?>(null) }
    sheetTrack?.let { track ->
        NowPlayingBottomSheet(
            onDismiss = { sheetTrack = null },
            song = track.toSongEntity(),
            navController = navController,
        )
    }

    // Landscape (a phone in a car mount, a tablet, DeX) is short, so a page there is two rows.
    val windowSize = LocalWindowInfo.current.containerSize
    val rows = if (windowSize.width > windowSize.height) SPEED_DIAL_ROWS_LANDSCAPE else SPEED_DIAL_ROWS_PORTRAIT

    Column {
        HomeSectionHeader(data = data, navController = navController, onPlayAll = { homeViewModel.playAll(data) })
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // Responsive: as many columns as fit at SPEED_DIAL_MIN_TILE, never fewer than three, so
            // a wider screen shows more songs to pick from at once rather than bigger covers.
            val usable = maxWidth - SPEED_DIAL_GUTTER * 2
            val columns =
                ((usable + SPEED_DIAL_GAP) / (SPEED_DIAL_MIN_TILE + SPEED_DIAL_GAP)).toInt().coerceAtLeast(SPEED_DIAL_MIN_COLUMNS)
            val tileSize = (usable - SPEED_DIAL_GAP * (columns - 1)) / columns
            val pageSize = columns * rows
            // YouTube Music's dice sits in the first page's bottom-right corner, so that page carries
            // one tile fewer. No dice when the rest of Home has nothing it could play.
            val pages =
                remember(tiles, playable, pageSize) {
                    if (playable.isEmpty()) {
                        tiles.map { SpeedDialSlot.Item(it) }.chunked(pageSize)
                    } else {
                        val first: List<SpeedDialSlot> = tiles.take(pageSize - 1).map { SpeedDialSlot.Item(it) } + SpeedDialSlot.Dice
                        listOf(first) + tiles.drop(pageSize - 1).map { SpeedDialSlot.Item(it) }.chunked(pageSize)
                    }
                }
            val pagerState = rememberPagerState { pages.size }
            Column {
                HorizontalPager(
                    state = pagerState,
                    pageSpacing = SPEED_DIAL_GAP,
                    modifier = Modifier.fillMaxWidth(),
                ) { page ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(SPEED_DIAL_GAP),
                        modifier = Modifier.padding(horizontal = SPEED_DIAL_GUTTER),
                    ) {
                        // Always the full number of rows, so a short last page keeps the grid's
                        // height and the dots below do not jump when you swipe onto it.
                        repeat(rows) { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(SPEED_DIAL_GAP)) {
                                repeat(columns) { column ->
                                    val slot = pages.getOrNull(page)?.getOrNull(row * columns + column)
                                    Box(modifier = Modifier.size(tileSize)) {
                                        when (slot) {
                                            is SpeedDialSlot.Item -> {
                                                val tile = slot.content
                                                SpeedDialTile(
                                                    content = tile,
                                                    onClick = { tile.openFromHome(navController, homeViewModel) },
                                                    onLongClick = {
                                                        if (!tile.videoId.isNullOrEmpty()) sheetTrack = tile.toTrack()
                                                    },
                                                )
                                            }

                                            SpeedDialSlot.Dice ->
                                                SpeedDialDice(onRoll = { playable.random().playRadio(homeViewModel) })

                                            null -> Unit
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (pages.size > 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    ) {
                        repeat(pages.size) { index ->
                            Box(
                                modifier =
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            MaterialTheme.colorScheme.onBackground.copy(
                                                alpha = if (index == pagerState.currentPage) 1f else 0.3f,
                                            ),
                                        ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpeedDialTile(
    content: Content,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val thumbnail = content.thumbnails.maxByOrNull { it.width * it.height }?.url
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalPlatformContext.current)
                    .data(thumbnail)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(thumbnail)
                    .crossfade(300)
                    .build(),
            placeholder = rememberHolderPainter(),
            error = rememberHolderPainter(),
            contentDescription = content.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.playerOrigin(thumbnail).fillMaxSize(),
        )
        // The title sits on a dark fade at the bottom of the cover, as on YouTube Music.
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(SPEED_DIAL_SCRIM_HEIGHT)
                    .background(smoothScrimBrush(from = Color.Transparent, to = Color.Black.copy(alpha = 0.75f))),
        )
        // Sized like YouTube Music's tile titles: big enough to read at a glance, not a caption, in
        // the app's own font, bold like Quick picks.
        Text(
            text = content.title,
            style = typo().titleSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

/** One cell of a Speed dial page: a shelf item, or the dice. */
private sealed interface SpeedDialSlot {
    data class Item(
        val content: Content,
    ) : SpeedDialSlot

    data object Dice : SpeedDialSlot
}

/**
 * YouTube Music's dice: a die showing three, on a dark violet tile. Tapping it plays a random song
 * from the rest of Home, and the die gives a quick roll.
 */
@Composable
private fun SpeedDialDice(onRoll: () -> Unit) {
    val spin = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(DiceTileTop, DiceTileBottom)))
                .clickable {
                    onRoll()
                    scope.launch {
                        spin.snapTo(0f)
                        spin.animateTo(1f, tween(DICE_ROLL_MS))
                    }
                },
    ) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .graphicsLayer { rotationZ = spin.value * 360f },
        ) {
            val radius = size.minDimension * 0.15f
            listOf(0.18f, 0.5f, 0.82f).forEach { t ->
                drawCircle(color = DiceDot, radius = radius, center = Offset(size.width * t, size.height * t))
            }
        }
    }
}

private val DiceTileTop = Color(0xFF2A1F3D)
private val DiceTileBottom = Color(0xFF14121C)
private val DiceDot = Color(0xFFB9B4EA)
private const val DICE_ROLL_MS = 450

/**
 * The shelf YouTube Music's app shows as "Speed dial". The web data this app reads still calls it
 * "Listen again" — same songs, older name — so it is recognised either by the new title or by what
 * marks Listen again in any language: the signed-in account's name as its strapline, the same test
 * Home already uses to decide whether to draw the account row.
 */
fun HomeItem.isSpeedDialShelf(accountName: String?): Boolean =
    title.trim().equals(SPEED_DIAL_TITLE, ignoreCase = true) ||
        (!accountName.isNullOrBlank() && subtitle == accountName)

/**
 * Opens [this] the way its Home card does (see [HomeContentCard]'s dispatch): a song or video
 * starts its radio; a channel id opens the artist; a podcast, album or playlist opens its page.
 */
internal fun Content.openFromHome(
    navController: NavController,
    viewModel: HomeViewModel,
) {
    val browseId = browseId
    val playlistId = playlistId
    when {
        !videoId.isNullOrEmpty() -> playRadio(viewModel)
        playlistId != null && playlistId.startsWith("UC") -> navController.navigate(ArtistDestination(channelId = playlistId))
        playlistId != null -> navController.navigate(PlaylistDestination(playlistId = playlistId))
        browseId != null && browseId.startsWith("UC") -> navController.navigate(ArtistDestination(channelId = browseId))
        browseId != null && browseId.startsWith("MPSP") -> navController.navigate(PodcastDestination(podcastId = browseId))
        browseId != null -> navController.navigate(AlbumDestination(browseId = browseId))
    }
}

private const val SPEED_DIAL_TITLE = "Speed dial"
// The smallest a cover may get before another column is added, and the fewest columns there are.
private val SPEED_DIAL_MIN_TILE = 112.dp
private const val SPEED_DIAL_MIN_COLUMNS = 3
private const val SPEED_DIAL_ROWS_PORTRAIT = 3
private const val SPEED_DIAL_ROWS_LANDSCAPE = 2
private val SPEED_DIAL_GAP = 6.dp
private val SPEED_DIAL_GUTTER = 12.dp
private val SPEED_DIAL_SCRIM_HEIGHT = 44.dp
