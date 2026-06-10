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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.BuildConfig
import com.example.api.BestShotResponse
import com.example.data.PhotoWithAnalysis
import com.example.repository.SwipeAction
import com.example.ui.PhotoFlowViewModel

@Composable
fun SmartCleanupScreen(
    viewModel: PhotoFlowViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.smartCleanupPhotos.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val bestShotResult by viewModel.aiBestShotResult.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    val selectedItems = remember { mutableStateListOf<Long>() }

    // Grouping duplicates: find photos with very close similarity hash
    val duplicateGroups = remember(items) {
        findDuplicateGroups(items)
    }

    // Checking if Gemini key is available for advanced Best Shot analysis
    val hasGemini = BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Smart Cleanup",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ranked by local deletion probability score",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selectedItems.isNotEmpty()) {
                Button(
                    onClick = {
                        // Cue selected for deletion review
                        selectedItems.forEach { id ->
                            viewModel.swipeAction(id, SwipeAction.DELETE)
                        }
                        selectedItems.clear()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Prune (${selectedItems.size})", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Duplicate Group Showcase ---
        if (duplicateGroups.isNotEmpty()) {
            DuplicateGroupBanner(
                groupsCount = duplicateGroups.size,
                hasGeminiKey = hasGemini,
                isAiLoading = isAiLoading,
                bestShotResult = bestShotResult,
                onRunBestShotAnalysis = {
                    val firstGroup = duplicateGroups.first()
                    viewModel.analyzeSimilarBestShot(firstGroup)
                },
                onClearBestShot = { viewModel.clearBestShotResult() }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (items.isEmpty()) {
            EmptyCleanupState(isScanning = isScanning)
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { item ->
                    val isChecked = selectedItems.contains(item.photo.id)
                    SmartCleanupRow(
                        item = item,
                        viewModel = viewModel,
                        isChecked = isChecked,
                        onCheckChange = {
                            if (isChecked) {
                                selectedItems.remove(item.photo.id)
                            } else {
                                selectedItems.add(item.photo.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SmartCleanupRow(
    item: PhotoWithAnalysis,
    viewModel: PhotoFlowViewModel,
    isChecked: Boolean,
    onCheckChange: () -> Unit
) {
    val scorePercent = (viewModel.calculateDeletionProbability(item) * 100f).roundAtDecimal(0)
    val analysis = item.analysis

    val reasons = remember(analysis) {
        val list = mutableListOf<String>()
        if (analysis != null) {
            if (analysis.blurScore > 0.6f) list.add("High Blur")
            if (analysis.brightnessScore < 0.2f) list.add("Very Dark")
            if (analysis.brightnessScore > 0.8f) list.add("Overexposed")
            if (analysis.screenshotProbabilityScore > 0.8f) list.add("Screenshot")
        }
        list
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckChange() }
            .border(
                width = 1.dp,
                color = if (isChecked) Color(0xFFD0BCFF).copy(alpha = 0.45f) 
                        else Color(0xFF938F99).copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("cleanup_item_card_${item.photo.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) Color(0xFFD0BCFF).copy(alpha = 0.12f)
                             else Color(0xFF2B2930)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Check indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isChecked) Color(0xFFD0BCFF)
                        else Color.Transparent
                    )
                    .border(
                        1.5.dp, 
                        if (isChecked) Color.Transparent 
                        else Color(0xFF938F99).copy(alpha = 0.6f), 
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isChecked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color(0xFF381E72),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Thumbnail
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.photo.uriString)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.photo.fileName,
                    maxLines = 1,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE6E1E5)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (scorePercent >= 75) Color(0xFFF2B8B5).copy(alpha = 0.12f)
                                else Color(0xFFD0BCFF).copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$scorePercent% Prunes",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (scorePercent >= 75) Color(0xFFF2B8B5)
                                    else Color(0xFFD0BCFF)
                        )
                    }

                    reasons.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFCAC4D0).copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 9.sp,
                                color = Color(0xFFCAC4D0)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Score Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (scorePercent >= 75) Color(0xFFF2B8B5).copy(alpha = 0.15f)
                        else Color(0xFFD0BCFF).copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = scorePercent.toString(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (scorePercent >= 75) Color(0xFFF2B8B5)
                            else Color(0xFFD0BCFF)
                )
            }
        }
    }
}

@Composable
fun DuplicateGroupBanner(
    groupsCount: Int,
    hasGeminiKey: Boolean,
    isAiLoading: Boolean,
    bestShotResult: BestShotResponse?,
    onRunBestShotAnalysis: () -> Unit,
    onClearBestShot: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF938F99).copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFD0BCFF).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Balance,
                        contentDescription = null,
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "Duplicate Candidates",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFE6E1E5)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "We configured similar pixel visual hashes locally and identified $groupsCount groups of duplicate photo clusters on your device locally.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCAC4D0),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (bestShotResult != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    color = Color(0xFFCAC4D0).copy(alpha = 0.08f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Gemini Best Shot recommendation:", 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE6E1E5)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Recommend keeping Image ${bestShotResult.bestIndex + 1}: ${bestShotResult.explanation}",
                            fontSize = 11.sp,
                            color = Color(0xFFCAC4D0),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onClearBestShot,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFCAC4D0).copy(alpha = 0.15f),
                                contentColor = Color(0xFFE6E1E5)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Acknowledge", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                if (hasGeminiKey) {
                    Button(
                        onClick = onRunBestShotAnalysis,
                        enabled = !isAiLoading,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0BCFF),
                            contentColor = Color(0xFF381E72)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isAiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF381E72), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Best Shot with Gemini", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { },
                        enabled = false,
                        shape = RoundedCornerShape(10.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add API key to unlock Best Shot Choice", fontSize = 11.sp, color = Color(0xFFCAC4D0).copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCleanupState(isScanning: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Perfect local score!",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isScanning) "Performing local pixel computations..." 
                      else "Your local analysis returned 0 high-risk gallery elements. Enjoy your tidy photo collection!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helpers
private fun findDuplicateGroups(items: List<PhotoWithAnalysis>): List<List<PhotoWithAnalysis>> {
    val result = mutableListOf<List<PhotoWithAnalysis>>()
    val visited = mutableSetOf<Long>()

    for (i in items.indices) {
        val item1 = items[i]
        if (visited.contains(item1.photo.id)) continue
        val hash1 = item1.analysis?.duplicateSimilarityHash ?: continue

        val currentGroup = mutableListOf(item1)
        for (j in i + 1 until items.size) {
            val item2 = items[j]
            if (visited.contains(item2.photo.id)) continue
            val hash2 = item2.analysis?.duplicateSimilarityHash ?: continue

            // Hamming distance of average hashes (clamped standard difference <= 8 beats duplicate clusters)
            val distance = calculateHammingDistance(hash1, hash2)
            if (distance <= 8) {
                currentGroup.add(item2)
                visited.add(item2.photo.id)
            }
        }

        if (currentGroup.size > 1) {
            visited.add(item1.photo.id)
            result.add(currentGroup)
        }
    }
    return result
}

private fun calculateHammingDistance(hash1: String, hash2: String): Int {
    return try {
        val h1 = java.lang.Long.parseUnsignedLong(hash1, 16)
        val h2 = java.lang.Long.parseUnsignedLong(hash2, 16)
        java.lang.Long.bitCount(h1 xor h2)
    } catch (e: Exception) {
        64
    }
}

private fun Float.roundAtDecimal(decimals: Int): Float {
    var multiplier = 1.0f
    repeat(decimals) { multiplier *= 10f }
    return kotlin.math.round(this * multiplier) / multiplier
}
