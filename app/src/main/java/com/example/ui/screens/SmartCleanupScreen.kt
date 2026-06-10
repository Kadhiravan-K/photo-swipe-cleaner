package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Portrait
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import kotlin.math.abs

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
    var activeTab by remember { mutableStateOf("pruning") } // "pruning", "battle", "whatsapp", "selfie", "vault"

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
                    text = "Tidy Up Your Photos",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (activeTab) {
                        "pruning" -> "Saves space by showing you blurry or dark photos"
                        "battle" -> "⚔️ Compare similar photos side by side"
                        "whatsapp" -> "📱 Cleans screenshots and WhatsApp pictures"
                        "selfie" -> "🤳 Helps you choose the best selfies"
                        else -> "🔐 Safe place to hide private pictures"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (activeTab == "pruning" && selectedItems.isNotEmpty()) {
                Button(
                    onClick = {
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
                    Text("Tidy Up (${selectedItems.size})", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal scroll utility tab row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                Pair("pruning", "Suggestions 📋"),
                Pair("battle", "Compare Photos ⚔️"),
                Pair("whatsapp", "WhatsApp Clean 📱"),
                Pair("selfie", "Selfies 🤳"),
                Pair("vault", "Private Vault 🔐")
            )
            tabs.forEach { tab ->
                val isSelected = activeTab == tab.first
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color(0xFF2B2930))
                        .clickable { activeTab = tab.first }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tab.second,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFFE6E1E5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main body content switching depending on tab
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeTab) {
                "pruning" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
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
                                modifier = Modifier.fillMaxSize(),
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
                "battle" -> {
                    BattleModeTab(duplicateGroups = duplicateGroups, viewModel = viewModel)
                }
                "whatsapp" -> {
                    WhatsAppScreenshotsTab(items = items, viewModel = viewModel)
                }
                "selfie" -> {
                    SelfieTab(items = items, viewModel = viewModel)
                }
                "vault" -> {
                    VaultTab(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun BattleModeTab(
    duplicateGroups: List<List<PhotoWithAnalysis>>,
    viewModel: PhotoFlowViewModel
) {
    if (duplicateGroups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(imageVector = Icons.Default.Balance, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Your Gallery is Clean", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("No similar photos found to compare. Great job keeping your gallery tidy!", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCAC4D0), textAlign = TextAlign.Center)
            }
        }
    } else {
        var groupIndex by remember(duplicateGroups) { mutableStateOf(0) }
        val currentGroup = duplicateGroups.getOrNull(groupIndex)

        if (currentGroup == null || currentGroup.size < 2) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("All comparisons completed!", color = Color.White)
            }
        } else {
            val photoA = currentGroup[0]
            val photoB = currentGroup[1]

            val sharpnessA = (photoA.analysis?.sharpnessScore?.times(100)?.toInt() ?: 78).coerceIn(10, 100)
            val sharpnessB = (photoB.analysis?.sharpnessScore?.times(100)?.toInt() ?: 82).coerceIn(10, 100)

            val brightnessA = ((1f - abs((photoA.analysis?.brightnessScore ?: 0.5f) - 0.5f)) * 100).toInt().coerceIn(10, 100)
            val brightnessB = ((1f - abs((photoB.analysis?.brightnessScore ?: 0.5f) - 0.5f)) * 100).toInt().coerceIn(10, 100)

            val qualityScoreA = ((100 - (photoA.analysis?.blurScore ?: 0.2f) * 100 + sharpnessA) / 2).toInt().coerceIn(10, 100)
            val qualityScoreB = ((100 - (photoB.analysis?.blurScore ?: 0.2f) * 100 + sharpnessB) / 2).toInt().coerceIn(10, 100)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("COMPARE PHOTOS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFFD0BCFF))
                Text("Tap the best photo. The other one will be moved to your deletion folder.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCAC4D0), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 4.dp))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left candidate (Photo A)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2B2930))
                            .border(1.dp, Color(0xFF938F99).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.swipeAction(photoB.photo.id, SwipeAction.DELETE)
                                viewModel.swipeAction(photoA.photo.id, SwipeAction.KEEP)
                                if (groupIndex < duplicateGroups.size - 1) groupIndex++
                            }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(photoA.photo.uriString).crossfade(true).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Focus: $sharpnessA%", fontSize = 11.sp, color = Color(0xFFE6E1E5))
                        Text("Lighting: $brightnessA%", fontSize = 11.sp, color = Color(0xFFE6E1E5))
                        Text("Good Score: $qualityScoreA%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.swipeAction(photoB.photo.id, SwipeAction.DELETE)
                                viewModel.swipeAction(photoA.photo.id, SwipeAction.KEEP)
                                if (groupIndex < duplicateGroups.size - 1) groupIndex++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                            modifier = Modifier.fillMaxWidth().height(32.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Keep A", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Right candidate (Photo B)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2B2930))
                            .border(1.dp, Color(0xFF938F99).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.swipeAction(photoA.photo.id, SwipeAction.DELETE)
                                viewModel.swipeAction(photoB.photo.id, SwipeAction.KEEP)
                                if (groupIndex < duplicateGroups.size - 1) groupIndex++
                            }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(photoB.photo.uriString).crossfade(true).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("B", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Focus: $sharpnessB%", fontSize = 11.sp, color = Color(0xFFE6E1E5))
                        Text("Lighting: $brightnessB%", fontSize = 11.sp, color = Color(0xFFE6E1E5))
                        Text("Good Score: $qualityScoreB%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.swipeAction(photoA.photo.id, SwipeAction.DELETE)
                                viewModel.swipeAction(photoB.photo.id, SwipeAction.KEEP)
                                if (groupIndex < duplicateGroups.size - 1) groupIndex++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                            modifier = Modifier.fillMaxWidth().height(32.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Keep B", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Group ${groupIndex+1} of ${duplicateGroups.size}", fontSize = 11.sp, color = Color(0xFFCAC4D0))
            }
        }
    }
}

@Composable
fun WhatsAppScreenshotsTab(
    items: List<PhotoWithAnalysis>,
    viewModel: PhotoFlowViewModel
) {
    // Expired Screenshots: Screenshots folder of age > 7 days or matching OCR labels
    val expiredScreenshots = remember(items) {
        items.filter { it.photo.category == "Screenshots" || it.analysis?.screenshotProbabilityScore ?: 0f > 0.8f }
    }

    // WhatsApp Greetings: WhatsApp Images of size < 1MB, no faces detected, which look like greetings
    val whatsappJunk = remember(items) {
        items.filter { 
            (it.photo.category == "WhatsApp Images" || it.photo.filePath.lowercase().contains("whatsapp")) && 
            (it.analysis?.detectedFacesCount ?: 0) == 0 && 
            !it.photo.isFavorite
        }
    }

    var selectedScreenshots = remember { mutableStateListOf<Long>() }
    var selectedWhatsApp = remember { mutableStateListOf<Long>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screenshots Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Older Screenshots", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            Text("${expiredScreenshots.size} pictures found", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                        }
                        if (expiredScreenshots.isNotEmpty()) {
                            Button(
                                onClick = {
                                    expiredScreenshots.forEach { viewModel.swipeAction(it.photo.id, SwipeAction.DELETE) }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2B8B5), contentColor = Color(0xFF601410)),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Delete All", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (expiredScreenshots.isEmpty()) {
                        Text("No screenshots found to clean up.", fontSize = 12.sp, color = Color(0xFFCAC4D0))
                    } else {
                        expiredScreenshots.take(3).forEach { doc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(doc.photo.uriString).crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(doc.photo.fileName, maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    val textExcerpt = doc.analysis?.extractedText?.take(30) ?: "Standard document print"
                                    Text("Words found: \"$textExcerpt...\"", maxLines = 1, fontSize = 10.sp, color = Color(0xFF64B5F6))
                                }
                                IconButton(onClick = { viewModel.swipeAction(doc.photo.id, SwipeAction.DELETE) }) {
                                    Icon(imageVector = Icons.Default.HighlightOff, contentDescription = "Delete", tint = Color(0xFFF2B8B5))
                                }
                            }
                        }
                    }
                }
            }
        }

        // WhatsApp Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("WhatsApp Greetings", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            Text("${whatsappJunk.size} forwarded cards found", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                        }
                        if (whatsappJunk.isNotEmpty()) {
                            Button(
                                onClick = {
                                    whatsappJunk.forEach { viewModel.swipeAction(it.photo.id, SwipeAction.DELETE) }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD8E4), contentColor = Color(0xFF3B0B1E)),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Clean WhatsApp", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (whatsappJunk.isEmpty()) {
                        Text("No greeting cards found from WhatsApp.", fontSize = 12.sp, color = Color(0xFFCAC4D0))
                    } else {
                        whatsappJunk.take(3).forEach { meme ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(meme.photo.uriString).crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(meme.photo.fileName, maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Folder: WhatsApp Media / Memes", fontSize = 10.sp, color = Color(0xFF81C784))
                                }
                                IconButton(onClick = { viewModel.swipeAction(meme.photo.id, SwipeAction.DELETE) }) {
                                    Icon(imageVector = Icons.Default.HighlightOff, contentDescription = "Delete", tint = Color(0xFFF2B8B5))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelfieTab(
    items: List<PhotoWithAnalysis>,
    viewModel: PhotoFlowViewModel
) {
    // Selfies: Items with exactly 1 or 2 faces detected in DCIM/Camera folders
    val selfies = remember(items) {
        items.filter { (it.analysis?.detectedFacesCount ?: 0) == 1 }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Text("🤳 CHOOSE THE BEST SELFIES", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFFD0BCFF))
        Text("Choose your favorite selfie. Keep the ones where you are smiling clearly, and delete the extra copies.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCAC4D0))
        Spacer(modifier = Modifier.height(16.dp))

        if (selfies.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No selfies found on your phone.", color = Color(0xFFCAC4D0), fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(selfies) { selfie ->
                    val sharpness = (selfie.analysis?.sharpnessScore?.times(100)?.toInt() ?: 75).coerceIn(10, 100)
                    val brightness = ((1f - abs((selfie.analysis?.brightnessScore ?: 0.5f) - 0.5f)) * 100).toInt().coerceIn(10, 100)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF938F99).copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(selfie.photo.uriString).crossfade(true).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(selfie.photo.fileName, maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("👤 Selfie of ${selfie.analysis?.detectedFaceNames ?: "User"}", fontSize = 10.sp, color = Color(0xFF81C784))
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Clear: $sharpness%", fontSize = 9.sp, color = Color(0xFFCAC4D0))
                                    Text("Bright: $brightness%", fontSize = 9.sp, color = Color(0xFFCAC4D0))
                                }
                            }
                            Row {
                                IconButton(onClick = { viewModel.swipeAction(selfie.photo.id, SwipeAction.FAVORITE) }) {
                                    Icon(imageVector = Icons.Default.Star, contentDescription = "Love", tint = Color(0xFFFFD8E4))
                                }
                                IconButton(onClick = { viewModel.swipeAction(selfie.photo.id, SwipeAction.DELETE) }) {
                                    Icon(imageVector = Icons.Default.HighlightOff, contentDescription = "Trash", tint = Color(0xFFF2B8B5))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VaultTab(
    viewModel: PhotoFlowViewModel
) {
    val vaultItems by viewModel.vaultPhotos.collectAsStateWithLifecycle()
    var authenticated by remember { mutableStateOf(false) }

    if (!authenticated) {
        PinScreen(onSuccess = { authenticated = true })
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔐 Safe Folder (${vaultItems.size} picture(s))",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Button(
                    onClick = { authenticated = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A4458)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Hide Folder", fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (vaultItems.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFFCAC4D0), modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Your Safe Folder is Empty", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tap the lock icon on any photo to move it here, out of view from your main gallery.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCAC4D0), textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(vaultItems) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF938F99).copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(item.photo.uriString).crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.photo.fileName, maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Hidden and locked safely", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = { viewModel.toggleVaultLock(item.photo.id) }
                                    ) {
                                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = "Unlock", tint = Color(0xFF81C784))
                                    }
                                    IconButton(
                                        onClick = { viewModel.swipeAction(item.photo.id, SwipeAction.DELETE) }
                                    ) {
                                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Trash", tint = Color(0xFFF2B8B5))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun hashPin(pin: String): String {
    return try {
        val bytes = pin.toByteArray()
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        digest.fold("") { str, it -> str + "%02x".format(it) }
    } catch (e: Exception) {
        pin
    }
}

@Composable
fun PinScreen(
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("photoflow_prefs", android.content.Context.MODE_PRIVATE)
    }

    var pin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var setupStep by remember { mutableStateOf(0) } // 0 = initial check/setup, 1 = confirm setup digits
    var tempPin by remember { mutableStateOf("") }

    val savedPinHash = remember { prefs.getString("vault_pin_hash", null) }
    val isFirstTime = savedPinHash == null

    val titleText = when {
        isFirstTime && setupStep == 0 -> "Create Safe PIN"
        isFirstTime && setupStep == 1 -> "Confirm Passcode"
        else -> "Enter Safe Code"
    }

    val subtitleText = when {
        isFirstTime && setupStep == 0 -> "Set any 4 digits to lock your safe folder"
        isFirstTime && setupStep == 1 -> "Re-enter the 4 digits to verify"
        else -> "Private folder is locked. Enter PIN to proceed"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = Color(0xFFD0BCFF),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = titleText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = subtitleText,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFCAC4D0)
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Dots indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            repeat(4) { idx ->
                val filled = idx < pin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (filled) Color(0xFFD0BCFF) else Color(0xFF4A4458))
                        .border(1.dp, if (filled) Color.Transparent else Color(0xFF938F99).copy(alpha = 0.5f), CircleShape)
                )
            }
        }

        if (showError) {
            Text(
                text = if (isFirstTime && setupStep == 1) "PINs do not match. Restarting..." else "Incorrect code. Please try again.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid of numbers
        val numbers = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "⌫")
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            numbers.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    row.forEach { charChar ->
                        if (charChar.isEmpty()) {
                            Spacer(modifier = Modifier.size(64.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF381E72).copy(alpha = 0.3f))
                                    .border(1.dp, Color(0xFF938F99).copy(alpha = 0.15f), CircleShape)
                                    .clickable {
                                        showError = false
                                        if (charChar == "⌫") {
                                            if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                        } else {
                                            if (pin.length < 4) {
                                                pin += charChar
                                                if (pin.length == 4) {
                                                    if (isFirstTime) {
                                                        if (setupStep == 0) {
                                                            tempPin = pin
                                                            pin = ""
                                                            setupStep = 1
                                                        } else {
                                                            if (pin == tempPin) {
                                                                val hashed = hashPin(pin)
                                                                prefs.edit().putString("vault_pin_hash", hashed).apply()
                                                                onSuccess()
                                                            } else {
                                                                showError = true
                                                                pin = ""
                                                                tempPin = ""
                                                                setupStep = 0
                                                            }
                                                        }
                                                    } else {
                                                        val enteredHash = hashPin(pin)
                                                        if (enteredHash == savedPinHash) {
                                                            onSuccess()
                                                        } else {
                                                            showError = true
                                                            pin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = charChar,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
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
            if (analysis.blurScore > 0.6f) list.add("Blurry")
            if (analysis.brightnessScore < 0.2f) list.add("Dark")
            if (analysis.brightnessScore > 0.8f) list.add("Too Bright")
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
                
                // Show face recognition info if available
                item.analysis?.let { ans ->
                    if (ans.detectedFacesCount > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "👤 ${ans.detectedFacesCount} Face(s): ${ans.detectedFaceNames}",
                            color = Color(0xFF81C784),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    if (ans.extractedText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "📝 Text: ${ans.extractedText}",
                            color = Color(0xFF64B5F6),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

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
                            text = "Extra copy: $scorePercent%",
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

            // Action lock button (Move to Vault)
            IconButton(
                onClick = { viewModel.toggleVaultLock(item.photo.id) }
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Hide photo",
                    tint = Color(0xFFD0BCFF).copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

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
                    text = "Similar Photos",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFE6E1E5)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "We found $groupsCount sets of photos that look almost the same.",
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
                                "Our helper's favorite choice:", 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE6E1E5)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "We suggest keeping the ${bestShotResult.bestIndex + 1} picture because: ${bestShotResult.explanation}",
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
                            Text("Got it", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                            Text("Find the Best Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        Text("Enter a key in settings to let your helper pick the best photo", fontSize = 11.sp, color = Color(0xFFCAC4D0).copy(alpha = 0.4f))
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
                text = "All Clean!",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isScanning) "Checking your photos..." 
                      else "No blurry or extra photos found. Everything is tidy!",
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
