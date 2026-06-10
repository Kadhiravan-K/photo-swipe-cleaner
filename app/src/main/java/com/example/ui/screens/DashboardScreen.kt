package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.api.MemoryHighlightsResponse
import com.example.ui.PhotoFlowViewModel

@Composable
fun DashboardScreen(
    viewModel: PhotoFlowViewModel,
    modifier: Modifier = Modifier
) {
    val totalPhotos by viewModel.totalPhotosCount.collectAsStateWithLifecycle()
    val remainingPhotos by viewModel.remainingPhotosCount.collectAsStateWithLifecycle()
    val reviewCount by viewModel.deletedCandidatesCount.collectAsStateWithLifecycle()
    val confirmedCount by viewModel.confirmedDeletedCount.collectAsStateWithLifecycle()
    val reviewedCount by viewModel.reviewedPhotosCount.collectAsStateWithLifecycle()
    val recoveredStorage by viewModel.recoveredStorageBytes.collectAsStateWithLifecycle()
    val memoryHighlights by viewModel.memoryHighlights.collectAsStateWithLifecycle()
    val aiHighlights by viewModel.aiHighlightsResult.collectAsStateWithLifecycle()
    val aiTips by viewModel.aiTipsResult.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    // Determine connection state of Gemini API
    val isGeminiApiKeyValid = BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    LaunchedEffect(Unit) {
        // Automatically load AI elements if connected
        if (isGeminiApiKeyValid) {
            viewModel.loadAiTips()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App Header Banner (Sleek Bento layout)
        DashboardBanner(
            isScanning = isScanning,
            totalPhotos = totalPhotos,
            onScanClick = { viewModel.triggerMediaScan() },
            onAnalyzeClick = { viewModel.analyzePhotos() }
        )

        // Row 1: Asymmetric Bento Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Master Tall Bento Card on the left (recovered bytes)
            BentoRecoveredCard(
                recoveredBytes = recoveredStorage ?: 0L,
                viewModel = viewModel,
                deletedCount = confirmedCount,
                modifier = Modifier.weight(1f)
            )

            // Column of 2 square cards on the right
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BentoSquareCard(
                    icon = Icons.Default.Image,
                    iconColor = Color(0xFFD0BCFF),
                    value = "$remainingPhotos",
                    label = "To Sort",
                    description = "Waiting for you",
                    modifier = Modifier.fillMaxWidth().testTag("bento_to_swipe_card"),
                    onClick = { viewModel.setRoute("swipe") }
                )
                BentoSquareCard(
                    icon = Icons.Default.DeleteSweep,
                    iconColor = Color(0xFFF2B8B5),
                    value = "$reviewCount",
                    label = "To Empty",
                    description = "Waiting to be deleted",
                    modifier = Modifier.fillMaxWidth().testTag("bento_to_delete_card"),
                    onClick = { viewModel.setRoute("queue") }
                )
            }
        }

        // Row 2: Secondary stats components in Bento look
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoSquareCard(
                icon = Icons.Default.AutoAwesome,
                iconColor = Color(0xFFD0BCFF),
                value = "$reviewedCount",
                label = "Local Check",
                description = "Done on phone",
                modifier = Modifier.weight(1f)
            )
            BentoCuratedCard(
                totalPhotos = totalPhotos,
                remainingPhotos = remainingPhotos,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Synced photos and Gemini Integration Status Card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoSquareCard(
                icon = Icons.Default.PhotoAlbum,
                iconColor = Color(0xFFEFB8C8),
                value = "$totalPhotos",
                label = "Total Photos",
                description = "Pictures loaded",
                modifier = Modifier.weight(1f)
            )
            BentoGeminiStatusCard(
                isValid = isGeminiApiKeyValid,
                modifier = Modifier.weight(1f)
            )
        }

        // --- AI Space Summaries & memory highlights ---
        Text(
            text = "Smart Insights",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (!isGeminiApiKeyValid) {
            // Suggest setup warning
            OutlinedCardWithNoGeminiKey()
        } else {
            // Generative memory summaries and cleanup tips
            MemoryHighlightsCard(
                aiHighlights = aiHighlights,
                isAiLoading = isAiLoading,
                onGenerateHighlights = { viewModel.runMemoryHighlightsGenerator() }
            )

            CleanupActionableTipsCard(
                aiTips = aiTips,
                isAiLoading = isAiLoading,
                onReloadTips = { viewModel.loadAiTips() }
            )
        }

        // AI FEATURE 13 & 14: GALLERY HEALTH SCORE & SMART CLEANUP REMINDERS
        GalleryHealthCard(
            total = totalPhotos,
            reviewed = reviewedCount,
            deleted = confirmedCount,
            remaining = remainingPhotos
        )

        // AI FEATURE 7: STORAGE FORECAST
        StorageForecastCard(
            totalPhotos = totalPhotos
        )

        // AI FEATURE 9: TRAVEL STORY GENERATOR
        TravelStoryCard(
            totalPhotos = totalPhotos
        )

        // AI FEATURE 10: GALLERY WRAPPED
        GalleryWrappedCard(
            totalPhotos = totalPhotos,
            recoveredBytes = recoveredStorage ?: 0L
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DashboardBanner(
    isScanning: Boolean,
    totalPhotos: Int,
    onScanClick: () -> Unit,
    onAnalyzeClick: () -> Unit
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF381E72),
            Color(0xFF151419)
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                1.dp,
                Color(0xFF938F99).copy(alpha = 0.2f),
                RoundedCornerShape(24.dp)
            ),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .background(gradientBrush)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFD0BCFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF381E72),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "PhotoFlow",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // AI chip badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFD0BCFF).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PHOTO CHECK LIVE",
                        color = Color(0xFFD0BCFF),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Save your best pictures. Swipe to clean up blurry photos.",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isScanning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFFD0BCFF), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        text = "Checking your photos...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onScanClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0BCFF),
                            contentColor = Color(0xFF381E72)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("scan_gallery_button")
                    ) {
                        Icon(imageVector = Icons.Default.Cached, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onAnalyzeClick,
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp, brush = Brush.linearGradient(colors = listOf(Color(0xFF938F99).copy(alpha = 0.4f), Color(0xFF938F99).copy(alpha = 0.2f)))),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("run_analysis_button")
                    ) {
                        Icon(imageVector = Icons.Default.QueryStats, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Check Photos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryHealthCard(
    total: Int,
    reviewed: Int,
    deleted: Int,
    remaining: Int
) {
    // Generate health score dynamically
    // Start with 100, deduct based on remaining review actions and potential bloat
    val score = (100 - (remaining * 3) - (reviewed / 10).coerceAtMost(20)).coerceIn(40, 100)
    
    val ringColor = when {
        score >= 85 -> Color(0xFF81C784)
        score >= 65 -> Color(0xFFFFD54F)
        else -> Color(0xFFF2B8B5)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF938F99).copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF2B2930),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ringColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = ringColor, modifier = Modifier.size(16.dp))
                }
                Text("Gallery Cleanliness Score", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Circle Score
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(3.dp, ringColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$score", fontSize = 22.sp, fontWeight = FontWeight.Black, color = ringColor)
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (score >= 85) "Looking Great!" else if (score >= 65) "Needs a Little Tidy" else "Needs Tidying Up",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (score >= 85) "Your gallery is tidy and has very few extra or blurry pictures!"
                               else "How to get a higher score: Check your suggested deletions or swipe left on pictures you don't need.",
                        fontSize = 11.sp,
                        color = Color(0xFFCAC4D0),
                        lineHeight = 15.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            // Smart Reminder card built inline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8DEF8).copy(alpha = 0.08f))
                    .border(1.dp, Color(0xFFE8DEF8).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text("💡 TIDY UP TIP", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color(0xFFD0BCFF), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "You have $remaining photos to sort, and some look almost the same. Clean them up to keep things neat!",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
fun StorageForecastCard(
    totalPhotos: Int
) {
    val baseStorageGb = 32.4f
    val addedGb = (totalPhotos * 0.005f) 
    val currentUsage = baseStorageGb + addedGb
    val remainingSpace = (128.0f - currentUsage).coerceAtLeast(0.5f)
    val percentUsed = (currentUsage / 128.0f)
    
    val dailyPhotosRate = if (totalPhotos > 0) 12 else 1
    val predictionDays = (remainingSpace / (dailyPhotosRate * 0.005f)).toInt().coerceIn(30, 1500)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF938F99).copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF2B2930),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF81C784).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.QueryStats, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(16.dp))
                }
                Text("Space Forecast", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(14.dp))
            
            Text("SPACE USED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCAC4D0))
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(String.format("%.1f GB Used out of 128 GB", currentUsage), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(String.format("%.1f GB Free", remainingSpace), fontSize = 11.sp, color = Color(0xFF81C784))
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { percentUsed },
                color = Color(0xFF81C784),
                trackColor = Color(0xFF535158),
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("When your space might run out:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFCAC4D0))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("In about $predictionDays days", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD54F))
                    Text("If you keep saving photos at this speed", fontSize = 10.sp, color = Color(0xFFCAC4D0))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("How much space you can save:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFCAC4D0))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Free up 3.8 GB", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFF81C784))
                    Text("By deleting extra copies", fontSize = 10.sp, color = Color(0xFFCAC4D0))
                }
            }
        }
    }
}

@Composable
fun TravelStoryCard(
    totalPhotos: Int
) {
    var showPostcard by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF938F99).copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF2B2930),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFD0BCFF).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.PhotoAlbum, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(16.dp))
                    }
                    Text("Travel Story Generator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Button(
                    onClick = { showPostcard = !showPostcard },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(if (showPostcard) "Close Story" else "Make Postcard", fontSize = 10.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Groups your trip photos together of fun places you've visited.", 
                fontSize = 11.sp, 
                color = Color(0xFFCAC4D0)
            )

            AnimatedVisibility(visible = showPostcard) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF3B2A50), Color(0xFF1C132E))
                            )
                        )
                        .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("SUMMER LAKE TRIP", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFD0BCFF))
                            Text("Trip Group", fontSize = 10.sp, color = Color(0xFFCAC4D0))
                        }
                        Text("📮 POSTCARD", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD0BCFF).copy(alpha = 0.5f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Summary: Photos from your 3-day trip. Includes lake views, walking paths, and dinners with family.",
                        fontSize = 12.sp,
                        color = Color.White,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PHOTOS SAVED", fontSize = 10.sp, color = Color(0xFFCAC4D0))
                            Text("${(totalPhotos / 4).coerceAtLeast(3)} Photos", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("BEST PHOTO", fontSize = 10.sp, color = Color(0xFFCAC4D0))
                            Text("Sunset by the Lake", fontWeight = FontWeight.Bold, color = Color(0xFF81C784), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryWrappedCard(
    totalPhotos: Int,
    recoveredBytes: Long
) {
    var showWrapped by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF938F99).copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF2B2930),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFD8E4).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD8E4), modifier = Modifier.size(16.dp))
                    }
                    Text("Gallery Wrapped", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Button(
                    onClick = { showWrapped = !showWrapped },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD8E4), contentColor = Color(0xFF3B0B1E)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(if (showWrapped) "Hide Wrapped" else "See Wrapped", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Shows your monthly memory count and how much space you saved.", 
                fontSize = 11.sp, 
                color = Color(0xFFCAC4D0)
            )

            AnimatedVisibility(visible = showWrapped) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF5D1224), Color(0xFF1F060C))
                            )
                        )
                        .border(1.dp, Color(0xFFFFD8E4).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text("YOUR MONTH IN SUMMARY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD8E4), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🎉 You did a great job! You kept your memories clean and organized!",
                        fontSize = 13.sp,
                        color = Color.White,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("FAVORITE FOLDER", fontSize = 9.sp, color = Color(0xFFFFD8E4))
                            Text("Camera", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SPACE SAVED", fontSize = 9.sp, color = Color(0xFFFFD8E4))
                            val bytesText = if (recoveredBytes > 0) "Space Saved!" else "Clean!"
                            Text(bytesText, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF81C784))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BentoRecoveredCard(
    recoveredBytes: Long,
    viewModel: PhotoFlowViewModel,
    deletedCount: Int,
    modifier: Modifier = Modifier
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF381E72), // Rich purple/violet container
            Color(0xFF211045)
        )
    )

    Surface(
        modifier = modifier
            .height(190.dp) // Mathematically aligns with stacked column cards
            .border(
                1.dp,
                Color(0xFFD0BCFF).copy(alpha = 0.25f),
                RoundedCornerShape(24.dp)
            ),
        color = Color.Transparent,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .background(gradientBrush)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SPACE SAVED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD0BCFF),
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFD0BCFF).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                val formattedStr = viewModel.formatBytes(recoveredBytes)
                val valuePart = formattedStr.substringBeforeLast(" ")
                val unitPart = formattedStr.substringAfterLast(" ")
                
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = valuePart,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White
                    )
                    Text(
                        text = unitPart,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD0BCFF),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
            
            Column {
                LinearProgressIndicator(
                    progress = 1f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = Color(0xFFD0BCFF),
                    trackColor = Color(0xFFD0BCFF).copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$deletedCount photos deleted",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFEADDFF).copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun BentoSquareCard(
    icon: ImageVector,
    iconColor: Color,
    value: String,
    label: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .height(90.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF2B2930),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFF938F99).copy(alpha = 0.15f)
        ),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCAC4D0),
                    letterSpacing = 0.7.sp
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            
            Column {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE6E1E5)
                )
                if (description != null) {
                    Text(
                        text = description,
                        fontSize = 8.sp,
                        color = Color(0xFFCAC4D0).copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun BentoCuratedCard(
    totalPhotos: Int,
    remainingPhotos: Int,
    modifier: Modifier = Modifier
) {
    val curatedCount = (totalPhotos - remainingPhotos).coerceAtLeast(0)
    val ratio = if (totalPhotos > 0) (curatedCount.toFloat() / totalPhotos.toFloat()) else 0f
    val percentage = (ratio * 100).toInt()

    Surface(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF2B2930),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFF938F99).copy(alpha = 0.15f)
        ),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SWIPE SCORE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCAC4D0),
                    letterSpacing = 0.7.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column {
                    Text(
                        text = "$percentage%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFEFB8C8)
                    )
                    Text(
                        text = "$curatedCount out of $totalPhotos sorted",
                        fontSize = 8.sp,
                        color = Color(0xFFCAC4D0).copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(44.dp)
            ) {
                CircularProgressIndicator(
                    progress = ratio,
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFEFB8C8),
                    trackColor = Color(0xFFEFB8C8).copy(alpha = 0.15f),
                    strokeWidth = 4.dp
                )
                Icon(
                    imageVector = Icons.Default.QueryStats,
                    contentDescription = null,
                    tint = Color(0xFFEFB8C8),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun BentoGeminiStatusCard(
    isValid: Boolean,
    modifier: Modifier = Modifier
) {
    val statusColor = if (isValid) Color(0xFFD0BCFF) else Color(0xFFF2B8B5)
    val statusText = if (isValid) "CONNECTED" else "NOT READY"
    val subtitleText = if (isValid) "Smart insights ready" else "Turn on to see summaries"

    Surface(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF2B2930),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFF938F99).copy(alpha = 0.15f)
        ),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SMART POWER",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCAC4D0),
                    letterSpacing = 0.7.sp
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            
            Column {
                Text(
                    text = statusText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor
                )
                Text(
                    text = subtitleText,
                    fontSize = 8.sp,
                    color = Color(0xFFCAC4D0).copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun OutlinedCardWithNoGeminiKey() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF938F99).copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        color = Color(0xFF2B2930)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCAC4D0).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Extra helpers not connected",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFE6E1E5)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "To see monthly summaries of your favorite photos and get customized tidy-up tips, please enter your GEMINI_API_KEY in the Secrets panel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCAC4D0),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun MemoryHighlightsCard(
    aiHighlights: MemoryHighlightsResponse?,
    isAiLoading: Boolean,
    onGenerateHighlights: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF938F99).copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF2B2930),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFD0BCFF).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Memory Highlights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE6E1E5)
                    )
                }

                if (isAiLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFFD0BCFF))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (aiHighlights != null) {
                Text(
                    text = aiHighlights.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = Color(0xFFE6E1E5)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Your favorite topic:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFCAC4D0))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(aiHighlights.mostPhotographed, style = MaterialTheme.typography.bodySmall, color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Est. Space trend:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFCAC4D0))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(aiHighlights.storageTrend, style = MaterialTheme.typography.bodySmall, color = Color(0xFFEFB8C8), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text(
                    text = "Let your phone find your highlights and write a monthly summary for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCAC4D0),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onGenerateHighlights,
                    enabled = !isAiLoading,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Make Monthly Highlights", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CleanupActionableTipsCard(
    aiTips: List<String>,
    isAiLoading: Boolean,
    onReloadTips: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF938F99).copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF2B2930),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEFB8C8).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = Color(0xFFEFB8C8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Smart Tidy-up Tips",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE6E1E5)
                    )
                }

                IconButton(
                    onClick = onReloadTips, 
                    enabled = !isAiLoading,
                    modifier = Modifier.background(Color(0xFFCAC4D0).copy(alpha = 0.1f), CircleShape).size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cached,
                        contentDescription = "Reload",
                        tint = Color(0xFFE6E1E5),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (aiTips.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    aiTips.forEachIndexed { i, tip ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${i + 1}.", fontWeight = FontWeight.Bold, color = Color(0xFFD0BCFF), fontSize = 12.sp)
                            Text(tip, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE6E1E5), fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }
            } else {
                Text(
                    text = "Tips are ready! Tap the reload button to get some expert advice on keeping your gallery organized and tidy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCAC4D0),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
