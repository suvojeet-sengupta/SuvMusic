package com.suvojeet.suvmusic.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suvojeet.suvmusic.core.data.local.dao.ArtistStats
import com.suvojeet.suvmusic.core.data.local.entity.ListeningHistory
import com.suvojeet.suvmusic.ui.viewmodel.DailyListening
import com.suvojeet.suvmusic.ui.viewmodel.ListeningStatsUiState
import com.suvojeet.suvmusic.ui.viewmodel.ListeningStatsViewModel
import com.suvojeet.suvmusic.ui.viewmodel.MusicPersonality
import com.suvojeet.suvmusic.ui.viewmodel.TimeOfDay
import java.util.concurrent.TimeUnit
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningStatsScreen(
    onBackClick: () -> Unit,
    viewModel: ListeningStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    val shareStats = {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out my music personality on SuvMusic: ${uiState.musicPersonality.title}! I've listened to ${uiState.totalSongsPlayed} songs.")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share your insights"))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Your Insights", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = shareStats) {
                        Icon(Icons.Default.Share, "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.totalSongsPlayed == 0) {
            EmptyStatsState(padding)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Music Personality Hero Section
                item {
                    AnimatedEntry {
                        MusicPersonalityHero(uiState.musicPersonality)
                    }
                }

                // 2. Global Key Metrics
                item {
                    AnimatedEntry(delay = 100) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            GlobalStatsRow(uiState)
                        }
                    }
                }

                // 3. Weekly Activity Chart (Premium Bezier)
                item {
                    AnimatedEntry(delay = 200) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            WeeklyActivitySection(uiState.weeklyTrends)
                        }
                    }
                }

                // 4. Monthly Highlight
                if (uiState.topArtistThisMonth != null) {
                    item {
                        AnimatedEntry(delay = 300) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                MonthlyHighlightSection(uiState.topArtistThisMonth!!)
                            }
                        }
                    }
                }

                // 5. Top Content
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        if (uiState.topSongs.isNotEmpty()) {
                            AnimatedEntry(delay = 400) {
                                Column {
                                    SectionHeaderWithPadding("Most Played Songs", Icons.Default.Audiotrack)
                                    Spacer(Modifier.height(12.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        items(uiState.topSongs) { song ->
                                            TopSongCard(song)
                                        }
                                    }
                                }
                            }
                        }

                        if (uiState.topArtists.isNotEmpty()) {
                            AnimatedEntry(delay = 500) {
                                Column {
                                    SectionHeaderWithPadding("Top Artists", Icons.Default.Person)
                                    Spacer(Modifier.height(12.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        items(uiState.topArtists) { artist ->
                                            TopArtistCard(artist)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Time of Day Breakdown
                item {
                    AnimatedEntry(delay = 600) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            TimeOfDaySection(uiState.timeOfDayStats)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedEntry(
    delay: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(800)) + 
                slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(800))
    ) {
        content()
    }
}

@Composable
private fun SectionHeaderWithPadding(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MusicPersonalityHero(personality: MusicPersonality) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(32.dp))
    ) {
        com.suvojeet.suvmusic.ui.components.MeshGradientBackground(
            dominantColors = com.suvojeet.suvmusic.ui.components.DominantColors(
                primary = MaterialTheme.colorScheme.primaryContainer,
                secondary = MaterialTheme.colorScheme.tertiaryContainer,
                accent = MaterialTheme.colorScheme.secondaryContainer,
                onBackground = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "YOUR PERSONALITY",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = personality.title,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = personality.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WeeklyActivitySection(trends: List<DailyListening>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Weekly Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Listening minutes per day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.Timeline, null, tint = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(Modifier.height(32.dp))
            
            BezierChart(trends)
        }
    }
}

@Composable
fun BezierChart(trends: List<DailyListening>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    
    val maxMinutes = trends.maxOfOrNull { it.minutesListen }?.toFloat() ?: 1f
    val chartMax = maxOf(maxMinutes, 30f) * 1.2f

    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "ChartAnimation"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val width = size.width
        val height = size.height
        val spacing = width / (trends.size - 1)
        
        val points = trends.mapIndexed { index, daily ->
            Offset(
                x = index * spacing,
                y = height - ((daily.minutesListen / chartMax) * height * animationProgress)
            )
        }

        val path = Path()
        val fillPath = Path()
        
        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            fillPath.moveTo(0f, height)
            fillPath.lineTo(points[0].x, points[0].y)
            
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                
                val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2, p0.y)
                val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2, p1.y)
                
                path.cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    p1.x, p1.y
                )
                fillPath.cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    p1.x, p1.y
                )
            }
            
            fillPath.lineTo(width, height)
            fillPath.close()
            
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.3f * animationProgress), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )
            
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
            
            trends.forEachIndexed { index, daily ->
                val point = points[index]
                
                if ((daily.minutesListen > 0 || index == 0 || index == trends.size - 1) && animationProgress > 0.8f) {
                    drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = point)
                    drawCircle(color = surfaceColor, radius = 2.dp.toPx(), center = point)
                }
                
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = labelColor
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        alpha = (255 * animationProgress).toInt()
                    }
                    drawText(daily.dayName, point.x, height + 40f, paint)
                }
            }
        }
    }
}

@Composable
fun MonthlyHighlightSection(artist: ArtistStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    artist.artist.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(Modifier.width(20.dp))
            
            Column {
                Text(
                    "MONTHLY STAR",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    artist.artist,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "You've played them ${artist.totalPlays} times this month!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun GlobalStatsRow(uiState: ListeningStatsUiState) {
    val totalHours = TimeUnit.MILLISECONDS.toHours(uiState.totalListeningTimeMs)
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(uiState.totalListeningTimeMs) % 60
    
    val avgHours = TimeUnit.MILLISECONDS.toHours(uiState.averageDailyMs)
    val avgMinutes = TimeUnit.MILLISECONDS.toMinutes(uiState.averageDailyMs) % 60
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCardSmall(
                modifier = Modifier.weight(1f),
                title = "Total Songs",
                value = uiState.totalSongsPlayed.toString(),
                icon = Icons.Default.GraphicEq
            )
            StatCardSmall(
                modifier = Modifier.weight(1f),
                title = "Total Time",
                value = "${totalHours}h ${totalMinutes}m",
                icon = Icons.Default.AccessTime
            )
        }
        
        StatCardWide(
            title = "Daily Average",
            value = if (avgHours > 0) "${avgHours}h ${avgMinutes}m" else "${avgMinutes}m",
            subtitle = "Based on your activity this week",
            icon = Icons.Default.Timeline
        )
    }
}

@Composable
private fun StatCardWide(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column {
                Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun StatCardSmall(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TopSongCard(song: ListeningHistory) {
    Card(
        modifier = Modifier.width(140.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = song.songTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.playCount} plays",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TopArtistCard(artist: ArtistStats) {
    Card(
        modifier = Modifier.width(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = artist.artist.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(Modifier.height(8.dp))
            Text(
                text = artist.artist,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${artist.totalPlays} plays",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TimeOfDaySection(stats: Map<TimeOfDay, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "When do you listen?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            
            val max = stats.values.maxOrNull()?.toFloat() ?: 1f
            
            TimeOfDay.entries.forEach { time ->
                val count = stats[time] ?: 0
                val progress = if (max > 0) count / max else 0f
                
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                    label = "BarAnimation"
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = time.name.lowercase().replaceFirstChar { it.uppercase() },
                        modifier = Modifier.width(80.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStatsState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Audiotrack,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No listening history yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Start playing songs to see your stats!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
