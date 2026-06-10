package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.key
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.example.repository.SwipeAction
import com.example.ui.PhotoFlowViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SwipeScreen(
    viewModel: PhotoFlowViewModel,
    modifier: Modifier = Modifier
) {
    val photos by viewModel.galleryPhotos.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Swipe Cleaner",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${photos.size} items in queue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalButton(
                onClick = { viewModel.undoLastAction() },
                modifier = Modifier.testTag("undo_action_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = "Undo Action",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Undo")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (photos.isEmpty()) {
                EmptySwipeState()
            } else {
                // Stack cards: show current top photo and the next one underneath
                val topPhoto = photos.firstOrNull()
                val nextPhoto = if (photos.size > 1) photos[1] else null

                // Underneath Card
                nextPhoto?.let { photo ->
                    SwipeCardBackground(photo = photo)
                }

                // Interactive Top Card
                topPhoto?.let { photo ->
                    key(photo.id) {
                        SwipeCard(
                            photo = photo,
                            onSwipe = { action ->
                                viewModel.swipeAction(photo.id, action)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Gesture guide legend
        GestureGuideLegend()
    }
}

@Composable
fun SwipeCard(
    photo: PhotoEntity,
    onSwipe: (SwipeAction) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val dragThresholdX = 120.dp
    val dragThresholdY = 120.dp

    // Animatable offsets for smooth swipe transitions
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    var swipeOverlayState by remember { mutableStateOf<SwipeAction?>(null) }

    val rotationFraction = (offsetX.value / 300f).coerceIn(-1f, 1f)
    val cardRotation = rotationFraction * 15f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(photo.id) {
                detectDragGestures(
                    onDragEnd = {
                        val currentX = offsetX.value
                        val currentY = offsetY.value
                        val density = this.density

                        coroutineScope.launch {
                            val dragThresholdXPx = dragThresholdX.toPx()
                            val dragThresholdYPx = dragThresholdY.toPx()

                            when {
                                currentX > dragThresholdXPx -> {
                                    // Swipe Right (Keep)
                                    offsetX.animateTo(screenWidth.toPx() * 1.5f, spring())
                                    onSwipe(SwipeAction.KEEP)
                                }
                                currentX < -dragThresholdXPx -> {
                                    // Swipe Left (Delete)
                                    offsetX.animateTo(-screenWidth.toPx() * 1.5f, spring())
                                    onSwipe(SwipeAction.DELETE)
                                }
                                currentY > dragThresholdYPx -> {
                                    // Swipe Down (Archive)
                                    offsetY.animateTo(screenHeight.toPx() * 1.5f, spring())
                                    onSwipe(SwipeAction.ARCHIVE)
                                }
                                currentY < -dragThresholdYPx -> {
                                    // Swipe Up (Favorite)
                                    offsetY.animateTo(-screenHeight.toPx() * 1.5f, spring())
                                    onSwipe(SwipeAction.FAVORITE)
                                }
                                else -> {
                                    // Snap back
                                    launch { offsetX.animateTo(0f, spring()) }
                                    launch { offsetY.animateTo(0f, spring()) }
                                }
                            }
                            swipeOverlayState = null
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)

                            // Update overlays dynamically
                            swipeOverlayState = when {
                                abs(offsetX.value) > abs(offsetY.value) -> {
                                    if (offsetX.value > 0) SwipeAction.KEEP else SwipeAction.DELETE
                                }
                                else -> {
                                    if (offsetY.value > 0) SwipeAction.ARCHIVE else SwipeAction.FAVORITE
                                }
                            }
                        }
                    }
                )
            }
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .rotate(cardRotation)
            .graphicsLayer {
                val scale = (1f - abs(rotationFraction) * 0.05f).coerceIn(0.95f, 1f)
                scaleX = scale
                scaleY = scale
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Color(0xFF938F99).copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                .testTag("swipe_photo_card"),
            shape = RoundedCornerShape(20.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Photo content
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photo.uriString)
                        .crossfade(true)
                        .build(),
                    contentDescription = photo.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Visual gradient dark cover at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.4f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                            )
                        )
                )

                // Photo overlay metadatas
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                ) {
                    Text(
                        text = photo.fileName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${photo.category} • ${photo.mimeType.substringAfter('/')}",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Temporary overlay sticker indicator
                swipeOverlayState?.let { action ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                when (action) {
                                    SwipeAction.DELETE -> Color.Red.copy(alpha = 0.15f)
                                    SwipeAction.KEEP -> Color.Green.copy(alpha = 0.15f)
                                    SwipeAction.FAVORITE -> Color.Magenta.copy(alpha = 0.15f)
                                    SwipeAction.ARCHIVE -> Color.Blue.copy(alpha = 0.15f)
                                }
                            )
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (action) {
                                    SwipeAction.DELETE -> Color.Red
                                    SwipeAction.KEEP -> Color.Green
                                    SwipeAction.FAVORITE -> Color.Magenta
                                    SwipeAction.ARCHIVE -> Color.Blue
                                }
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = action.name,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        )
                    }
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
            .border(1.dp, Color(0xFF938F99).copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
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
            .border(1.dp, Color(0xFF938F99).copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
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
                    .background(Color(0xFFD0BCFF).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "All Cleaned Up!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE6E1E5),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No images or videos remaining in your queue. Scan for more items or browse your smart cleanup suggestions.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCAC4D0),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun GestureGuideLegend() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF938F99).copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        color = Color(0xFF2B2930)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF2B8B5).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Swipe Left to Delete",
                        tint = Color(0xFFF2B8B5),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Delete", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFE6E1E5))
                Text("Swipe Left", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCAC4D0), fontSize = 8.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCBD2FF).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Swipe Right to Keep",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Keep", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFE6E1E5))
                Text("Swipe Right", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCAC4D0), fontSize = 8.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD8E4).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Swipe Up to Favorite",
                        tint = Color(0xFFFFD8E4),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Favorite", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFE6E1E5))
                Text("Swipe Up", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCAC4D0), fontSize = 8.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8DEF8).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "Swipe Down to Archive",
                        tint = Color(0xFFE8DEF8),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Archive", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFE6E1E5))
                Text("Swipe Down", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCAC4D0), fontSize = 8.sp)
            }
        }
    }
}
