package com.suvojeet.suvmusic.ui.screens.wrapped

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.recommendation.TopArtist
import com.suvojeet.suvmusic.recommendation.WrappedCardSong
import com.suvojeet.suvmusic.recommendation.WrappedGenerator
import com.suvojeet.suvmusic.recommendation.WrappedReport
import com.suvojeet.suvmusic.recommendation.WrappedWindow
import com.suvojeet.suvmusic.ui.components.glass.LiquidGlassSurface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class WrappedViewModel @Inject constructor(
    private val generator: WrappedGenerator
) : ViewModel() {
    private val _report = MutableStateFlow<WrappedReport?>(null)
    val report: StateFlow<WrappedReport?> = _report.asStateFlow()

    fun load(window: WrappedWindow) {
        viewModelScope.launch { _report.value = generator.generate(window) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WrappedScreen(
    onBack: () -> Unit,
    initialWindow: WrappedWindow = WrappedWindow.LAST_365_DAYS,
    viewModel: WrappedViewModel = koinViewModel()
) {
    val isDark = isSystemInDarkTheme()
    val window by remember { mutableStateOf(initialWindow) }

    LaunchedEffect(window) { viewModel.load(window) }

    val report by viewModel.report.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (isDark) listOf(Color(0xFF1B0030), Color(0xFF002240), Color(0xFF000000))
                    else listOf(Color(0xFFFFE4F2), Color(0xFFE4EEFF), Color(0xFFFFFFFF))
                )
            )
    ) {
        val current = report

        if (current == null || !current.hasData) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (current == null) "Loading your wrapped…"
                    else "Not enough listening yet — play a few more songs to unlock your wrapped.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            val cards = buildCards(current)
            val pagerState = rememberPagerState(pageCount = { cards.size })
            VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                WrappedCard(cards[page], isDark)
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier.statusBarsPadding().padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

private sealed class WrappedCardContent(val title: String) {
    class TotalMinutes(val minutes: Long) : WrappedCardContent("You listened for…")
    class TotalSongs(val songs: Int, val plays: Int) : WrappedCardContent("Your library in motion")
    class TopGenre(val genre: String) : WrappedCardContent("Your top genre")
    class TopSongs(val songs: List<WrappedCardSong>) : WrappedCardContent("Your top songs")
    class TopArtists(val artists: List<TopArtist>) : WrappedCardContent("Your top artists")
    class MostSkipped(val song: WrappedCardSong) : WrappedCardContent("The one you kept skipping")
    class FirstEver(val song: WrappedCardSong) : WrappedCardContent("It all started with…")
}

private fun buildCards(r: WrappedReport): List<WrappedCardContent> {
    val out = mutableListOf<WrappedCardContent>()
    out.add(WrappedCardContent.TotalMinutes(r.totalMinutesListened))
    out.add(WrappedCardContent.TotalSongs(r.totalSongs, r.totalPlays))
    if (r.topSongs.isNotEmpty()) out.add(WrappedCardContent.TopSongs(r.topSongs))
    if (r.topArtists.isNotEmpty()) out.add(WrappedCardContent.TopArtists(r.topArtists))
    out.add(WrappedCardContent.TopGenre(r.topGenre))
    r.mostSkipped?.let { out.add(WrappedCardContent.MostSkipped(it)) }
    r.firstEverTrack?.let { out.add(WrappedCardContent.FirstEver(it)) }
    return out
}

@Composable
private fun WrappedCard(card: WrappedCardContent, isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassSurface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(36.dp),
            blurAmount = 55f,
            isDarkTheme = isDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                when (card) {
                    is WrappedCardContent.TotalMinutes -> HeadlineNumber("${card.minutes}", "minutes")
                    is WrappedCardContent.TotalSongs -> HeadlineNumber("${card.songs}", "unique songs • ${card.plays} plays")
                    is WrappedCardContent.TopGenre -> HeadlineNumber(
                        card.genre.replaceFirstChar { it.titlecase() },
                        "is your vibe"
                    )
                    is WrappedCardContent.TopSongs -> SongList(card.songs)
                    is WrappedCardContent.TopArtists -> ArtistList(card.artists)
                    is WrappedCardContent.MostSkipped -> SongList(listOf(card.song))
                    is WrappedCardContent.FirstEver -> SongList(listOf(card.song))
                }
            }
        }
    }
}

@Composable
private fun HeadlineNumber(headline: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = headline,
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SongList(songs: List<WrappedCardSong>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        songs.forEachIndexed { index, song ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${index + 1}.",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(28.dp)
                )
                if (!song.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(modifier = Modifier.height(48.dp)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${song.artist} • ${song.playCount} plays",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistList(artists: List<TopArtist>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        artists.forEachIndexed { index, a ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${index + 1}.",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(28.dp)
                )
                Column {
                    Text(
                        text = a.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${a.totalPlays} plays",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
