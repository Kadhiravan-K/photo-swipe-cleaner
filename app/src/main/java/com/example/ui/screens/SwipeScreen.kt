package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.PhotoEntity
import com.example.data.PhotoWithAnalysis
import com.example.repository.SwipeAction
import com.example.ui.PhotoFlowViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SwipeScreen(
    viewModel: PhotoFlowViewModel,
    modifier: Modifier = Modifier
) {
    val photos by viewModel.galleryPhotos.collectAsStateWithLifecycle()
    val swipeLeftEnabled by viewModel.swipeLeftEnabled.collectAsStateWithLifecycle()
    val swipeRightEnabled by viewModel.swipeRightEnabled.collectAsStateWithLifecycle()
    val swipeUpEnabled by viewModel.swipeUpEnabled.collectAsStateWithLifecycle()
    val swipeDownEnabled by viewModel.swipeDownEnabled.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val mediaTypeFilter by viewModel.mediaTypeFilter.collectAsStateWithLifecycle()
    val aiFilter by viewModel.aiFilter.collectAsStateWithLifecycle()
    val dateFilter by viewModel.dateFilter.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Minimalist Expandable Filter Panel state
    var filtersExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black) // Ultimate photo-first focus format (pure dark theme)
    ) {
        // Core Card Stack & Photo View Area (Edge-to-Edge)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Screen Small Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Slide to Sort",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "${photos.size} photos found",
                        fontSize = 11.sp,
                        color = Color.LightGray.copy(alpha = 0.8f)
                    )
                }

                // Collapsible Filter Trigger button (Distraction-free)
                IconButton(
                    onClick = { filtersExpanded = !filtersExpanded },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (filtersExpanded) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
                        contentColor = if (filtersExpanded) MaterialTheme.colorScheme.onPrimary else Color.White
                    ),
                    modifier = Modifier.testTag("expand_filters_fab")
                ) {
                    Icon(
                        imageVector = if (filtersExpanded) Icons.Default.Close else Icons.Default.Tune,
                        contentDescription = "Filters"
                    )
                }
            }

            // Interactive Card Stack
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (photos.isEmpty()) {
                    EmptySwipeState()
                } else {
                    val topPhoto = photos.first()
                    val nextPhoto = if (photos.size > 1) photos[1] else null

                    // Background next card
                    nextPhoto?.let {
                        SwipeCardBackground(photo = it.photo)
                    }

                    // Foreground interactive card
                    val topPhotoId = topPhoto.photo.id
                    androidx.compose.runtime.key(topPhotoId) {
                        SwipeCard(
                            item = topPhoto,
                            viewModel = viewModel,
                            swipeLeftEnabled = swipeLeftEnabled,
                            swipeRightEnabled = swipeRightEnabled,
                            swipeUpEnabled = swipeUpEnabled,
                            swipeDownEnabled = swipeDownEnabled,
                            onSwipe = { action ->
                                viewModel.swipeAction(topPhoto.photo.id, action)
                            }
                        )
                    }
                }
            }

            // Margin space
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Minimalist Floating Expandable Filters overlay (Smooth animation)
        androidx.compose.animation.AnimatedVisibility(
            visible = filtersExpanded,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 56.dp)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(0.95f)
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(30.dp)),
                color = Color(0xFF2B2930),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sort and Filter",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        TextButton(
                            onClick = {
                                viewModel.updateSearchQuery("")
                                viewModel.selectCategory(null)
                                viewModel.setMediaTypeFilter("All")
                                viewModel.setAiFilter("All")
                                viewModel.setDateFilter("Any Time")
                                Toast.makeText(context, "Filters cleared", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Clear filters", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 1. Natural Language Search Option
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search by faces, text, words...", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.LightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    )

                    // 2. Category selection chips
                    Text("Photo Group:", style = MaterialTheme.typography.labelSmall, color = Color.LightGray, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val categories = listOf("All", "Camera Photos", "Screenshots", "Downloads", "WhatsApp Images", "Videos", "Favorites")
                        categories.forEach { cat ->
                            val isSelected = (selectedCategory == null && cat == "All") || (selectedCategory == cat)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f))
                                    .clickable {
                                        viewModel.selectCategory(if (cat == "All") null else cat)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(cat, fontSize = 10.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 3. AI scoring / Quality criteria filters
                    Text("Photo Smart Check:", style = MaterialTheme.typography.labelSmall, color = Color.LightGray, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val aiFilters = listOf("All", "Blurry", "Dark/Light", "Screenshots", "With Faces", "Documents with text")
                        aiFilters.forEach { fit ->
                            val isSelected = (aiFilter == fit)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f))
                                    .clickable { viewModel.setAiFilter(fit) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(fit, fontSize = 10.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 4. Date ranges
                    Text("Date Range:", style = MaterialTheme.typography.labelSmall, color = Color.LightGray, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val dates = listOf("Any Time", "Today", "Last 7 Days", "Last 30 Days")
                        dates.forEach { dt ->
                            val isSelected = (dateFilter == dt)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f))
                                    .clickable { viewModel.setDateFilter(dt) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(dt, fontSize = 10.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = { filtersExpanded = false },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Show Photos", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeCard(
    item: PhotoWithAnalysis,
    viewModel: PhotoFlowViewModel,
    swipeLeftEnabled: Boolean = true,
    swipeRightEnabled: Boolean = true,
    swipeUpEnabled: Boolean = true,
    swipeDownEnabled: Boolean = true,
    onSwipe: (SwipeAction) -> Unit
) {
    val photo = item.photo
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val dragThresholdX = 130.dp
    val dragThresholdY = 130.dp

    // Animatable offsets for card position
    val offsetX = remember(photo.id) { Animatable(0f) }
    val offsetY = remember(photo.id) { Animatable(0f) }

    var dragJobX by remember { mutableStateOf<Job?>(null) }
    var dragJobY by remember { mutableStateOf<Job?>(null) }
    var swipeOverlayState by remember { mutableStateOf<SwipeAction?>(null) }

    // ZOOM & PAN logic states (Pinch-to-zoom, pan, double-tap zoom)
    var zoomScale by remember(photo.id) { mutableStateOf(1f) }
    var zoomOffset by remember(photo.id) { mutableStateOf(Offset.Zero) }

    val rotationFraction = (offsetX.value / 350f).coerceIn(-1f, 1f)
    val cardRotation = rotationFraction * 12f

    // Metadata overlay request expand/collapse state (Hidden by default for photo-first)
    var metadataRequested by remember(photo.id) { mutableStateOf(false) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        zoomScale = (zoomScale * zoomChange).coerceIn(1f, 5f)
        if (zoomScale > 1f) {
            zoomOffset += panChange
        } else {
            zoomOffset = Offset.Zero
        }
    }

    // Direct actions link
    val triggerActionAnimated: (SwipeAction) -> Unit = { action ->
        coroutineScope.launch {
            when (action) {
                SwipeAction.KEEP -> {
                    offsetX.animateTo(screenWidth.value * 2.5f, spring())
                    onSwipe(SwipeAction.KEEP)
                }
                SwipeAction.DELETE -> {
                    offsetX.animateTo(-screenWidth.value * 2.5f, spring())
                    onSwipe(SwipeAction.DELETE)
                }
                SwipeAction.ARCHIVE -> {
                    offsetY.animateTo(screenHeight.value * 2.5f, spring())
                    onSwipe(SwipeAction.ARCHIVE)
                }
                SwipeAction.FAVORITE -> {
                    offsetY.animateTo(-screenHeight.value * 2.5f, spring())
                    onSwipe(SwipeAction.FAVORITE)
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper Card View
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(photo.id) {
                    detectDragGestures(
                        onDragEnd = {
                            if (zoomScale == 1f) {
                                dragJobX?.cancel()
                                dragJobY?.cancel()
                                val currentX = offsetX.value
                                val currentY = offsetY.value

                                coroutineScope.launch {
                                    val thresholdXPx = dragThresholdX.toPx()
                                    val thresholdYPx = dragThresholdY.toPx()

                                    when {
                                        currentX > thresholdXPx && swipeRightEnabled -> {
                                            offsetX.animateTo(screenWidth.toPx() * 1.5f, spring())
                                            onSwipe(SwipeAction.KEEP)
                                        }
                                        currentX < -thresholdXPx && swipeLeftEnabled -> {
                                            offsetX.animateTo(-screenWidth.toPx() * 1.5f, spring())
                                            onSwipe(SwipeAction.DELETE)
                                        }
                                        currentY > thresholdYPx && swipeDownEnabled -> {
                                            offsetY.animateTo(screenHeight.toPx() * 1.5f, spring())
                                            onSwipe(SwipeAction.ARCHIVE)
                                        }
                                        currentY < -thresholdYPx && swipeUpEnabled -> {
                                            offsetY.animateTo(-screenHeight.toPx() * 1.5f, spring())
                                            onSwipe(SwipeAction.FAVORITE)
                                        }
                                        else -> {
                                            launch { offsetX.animateTo(0f, spring()) }
                                            launch { offsetY.animateTo(0f, spring()) }
                                        }
                                    }
                                    swipeOverlayState = null
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (zoomScale == 1f) {
                                // Swiping gesture
                                change.consume()
                                dragJobX?.cancel()
                                dragJobY?.cancel()
                                dragJobX = coroutineScope.launch {
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                }
                                dragJobY = coroutineScope.launch {
                                    offsetY.snapTo(offsetY.value + dragAmount.y)
                                }

                                swipeOverlayState = when {
                                    abs(offsetX.value) > abs(offsetY.value) -> {
                                        if (offsetX.value > 0) {
                                            if (swipeRightEnabled) SwipeAction.KEEP else null
                                        } else {
                                            if (swipeLeftEnabled) SwipeAction.DELETE else null
                                        }
                                    }
                                    else -> {
                                        if (offsetY.value > 0) {
                                            if (swipeDownEnabled) SwipeAction.ARCHIVE else null
                                        } else {
                                            if (swipeUpEnabled) SwipeAction.FAVORITE else null
                                        }
                                    }
                                }
                            } else {
                                // Zoom Panning gesture
                                change.consume()
                                zoomOffset = Offset(
                                    x = zoomOffset.x + dragAmount.x,
                                    y = zoomOffset.y + dragAmount.y
                                )
                            }
                        }
                    )
                }
                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .rotate(cardRotation)
                .graphicsLayer {
                    val sc = (1f - abs(rotationFraction) * 0.05f).coerceIn(0.95f, 1f)
                    scaleX = sc
                    scaleY = sc
                }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .testTag("swipe_photo_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                ) {
                    // Actual photo - Aspect ratio strictly preserved, double-tap & pinch zoom supported
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photo.uriString)
                            .crossfade(true)
                            .build(),
                        contentDescription = photo.fileName,
                        contentScale = ContentScale.Fit, // Aspect ratio preserved (no crop!)
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(photo.id) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (zoomScale > 1f) {
                                            zoomScale = 1f
                                            zoomOffset = Offset.Zero
                                        } else {
                                            zoomScale = 2.5f
                                        }
                                    }
                                )
                            }
                            .transformable(state = transformState)
                            .graphicsLayer {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = zoomOffset.x
                                translationY = zoomOffset.y
                            }
                    )

                    // Minimal semi-transparent Overlay top button triggers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Info / Metadata Trigger toggle button
                        IconButton(
                            onClick = { metadataRequested = !metadataRequested },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (metadataRequested) MaterialTheme.colorScheme.primaryContainer else Color.Black.copy(alpha = 0.45f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("toggle_metadata_button")
                        ) {
                            Icon(
                                imageVector = if (metadataRequested) Icons.Default.Info else Icons.Outlined.Info,
                                contentDescription = "Show photo details"
                            )
                        }

                        // 2. Lock / Vault Button directly on card
                        IconButton(
                            onClick = {
                                viewModel.toggleVaultLock(photo.id)
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.45f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Move to Safe Folder",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Metadata Overlay (semi-transparent sheet - requested details only!)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = metadataRequested,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.75f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .navigationBarsPadding(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = photo.fileName,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = viewModel.formatBytes(photo.size),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = "Folder: ${photo.category} • Size: ${photo.width}x${photo.height}",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )

                                Divider(color = Color.White.copy(alpha = 0.15f))

                                // Dynamic Quality Blur metrics
                                item.analysis?.let { ans ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Clear Image", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                            LinearProgressIndicator(
                                                progress = ans.sharpnessScore,
                                                color = Color(0xFF81C784),
                                                trackColor = Color.White.copy(alpha = 0.1f),
                                                modifier = Modifier.fillMaxWidth().height(4.dp).padding(vertical = 2.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Blurry", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                            LinearProgressIndicator(
                                                progress = ans.blurScore,
                                                color = Color(0xFFE57373),
                                                trackColor = Color.White.copy(alpha = 0.1f),
                                                modifier = Modifier.fillMaxWidth().height(4.dp).padding(vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Local face markers
                                    if (ans.detectedFacesCount > 0) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF81C784).copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Face, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(14.dp))
                                            Text("People identified: ${ans.detectedFaceNames}", fontSize = 10.sp, color = Color(0xFFE8F5E9), fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // OCR Content
                                    if (ans.extractedText.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF64B5F6).copy(alpha = 0.12f))
                                                .padding(8.dp)
                                        ) {
                                            Text("Text in photo:", fontSize = 9.sp, color = Color(0xFF90CAF9), fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(ans.extractedText, fontSize = 10.sp, color = Color(0xFFE3F2FD), maxLines = 2)
                                        }
                                    }
                                }

                                // Security block shield memory
                                val memoryInfo = viewModel.checkMemoryProtection(item)
                                if (memoryInfo != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFF2B8B5).copy(alpha = 0.15f))
                                            .padding(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Color(0xFFF2B8B5), modifier = Modifier.size(16.dp))
                                        Text("Kept safe: ${memoryInfo.category}", fontSize = 10.sp, color = Color(0xFFF2B8B5), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Floating gesture state badge
                    swipeOverlayState?.let { act ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    when (act) {
                                        SwipeAction.DELETE -> Color.Red.copy(alpha = 0.18f)
                                        SwipeAction.KEEP -> Color.Green.copy(alpha = 0.18f)
                                        SwipeAction.FAVORITE -> Color.Magenta.copy(alpha = 0.18f)
                                        SwipeAction.ARCHIVE -> Color.Blue.copy(alpha = 0.18f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when (act) {
                                            SwipeAction.DELETE -> Color.Red
                                            SwipeAction.KEEP -> Color.Green
                                            SwipeAction.FAVORITE -> Color.Magenta
                                            SwipeAction.ARCHIVE -> Color.Blue
                                        }
                                    )
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text(act.name, fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }

        // Complete Bottom Action Bar (Undo, Delete, Keep, Favorite, Archive)
        Surface(
            color = Color.Black.copy(alpha = 0.4f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. UNDO Action Button (Accessible)
                IconButton(
                    onClick = {
                        viewModel.undoLastAction()
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("swipe_action_undo")
                ) {
                    Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo last choice", modifier = Modifier.size(20.dp))
                }

                // 2. DELETE Action Button (Swipe Left link)
                IconButton(
                    onClick = { triggerActionAnimated(SwipeAction.DELETE) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFFFFD8D6).copy(alpha = 0.9f),
                        contentColor = Color(0xFF601410)
                    ),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("swipe_action_delete")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(26.dp))
                }

                // 3. FAVORITE Action Button (Swipe Up link)
                IconButton(
                    onClick = { triggerActionAnimated(SwipeAction.FAVORITE) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFFFFD8E4).copy(alpha = 0.9f),
                        contentColor = Color(0xFF492532)
                    ),
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("swipe_action_favorite")
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = "Favorite", modifier = Modifier.size(22.dp))
                }

                // 4. ARCHIVE Action Button (Swipe Down link)
                IconButton(
                    onClick = { triggerActionAnimated(SwipeAction.ARCHIVE) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFFE8DEF8).copy(alpha = 0.9f),
                        contentColor = Color(0xFF1D192B)
                    ),
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("swipe_action_archive")
                ) {
                    Icon(imageVector = Icons.Default.Archive, contentDescription = "Move Out of View", modifier = Modifier.size(22.dp))
                }

                // 5. KEEP Action Button (Swipe Right link)
                IconButton(
                    onClick = { triggerActionAnimated(SwipeAction.KEEP) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFFD0BCFF).copy(alpha = 0.9f),
                        contentColor = Color(0xFF381E72)
                    ),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("swipe_action_keep")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Keep", modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

@Composable
fun SwipeCardBackground(photo: PhotoEntity) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val sc = 0.94f
                scaleX = sc
                scaleY = sc
                translationY = 16f
            }
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photo.uriString)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )
        }
    }
}

@Composable
fun EmptySwipeState() {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "All Cleaned Up!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You have checked all the photos! There are no pictures left to sort. You can find more pictures on the main screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}
