package com.example.ui

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ReviewQueueScreen
import com.example.ui.screens.SmartCleanupScreen
import com.example.ui.screens.SwipeScreen

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PhotoFlowMain(
    viewModel: PhotoFlowViewModel
) {
    val activeRoute by viewModel.activeRoute.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val remainingPhotos by viewModel.remainingPhotosCount.collectAsStateWithLifecycle()
    val deletedCount by viewModel.deletedCandidatesCount.collectAsStateWithLifecycle()

    val mediaTypeFilter by viewModel.mediaTypeFilter.collectAsStateWithLifecycle()
    val aiFilter by viewModel.aiFilter.collectAsStateWithLifecycle()
    val dateFilter by viewModel.dateFilter.collectAsStateWithLifecycle()

    // 1. Setup multi-permission handling for different OS levels (Read Photos / Videos)
    val mediaPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val permissionsState = rememberMultiplePermissionsState(permissions = mediaPermissions)

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            viewModel.triggerMediaScan()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        bottomBar = {
            AppBottomNavigation(
                activeRoute = activeRoute,
                remainingSwipeCount = remainingPhotos,
                deletedCandidatesCount = deletedCount,
                onNavigate = { viewModel.setRoute(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!permissionsState.allPermissionsGranted) {
                // Renders Request permissions guide screen
                PermissionsRequestScreen(
                    allGranted = permissionsState.allPermissionsGranted,
                    onRequestPermissions = { permissionsState.launchMultiplePermissionRequest() }
                )
            } else {
                // Navigate dynamically to standard view frames
                when (activeRoute) {
                    "dashboard" -> {
                        DashboardScreen(viewModel = viewModel)
                    }
                    "chat" -> {
                        ChatScreen(viewModel = viewModel)
                    }
                    "swipe" -> {
                        SwipeScreen(viewModel = viewModel)
                    }
                    "cleanup" -> {
                        SmartCleanupScreen(viewModel = viewModel)
                    }
                    "queue" -> {
                        ReviewQueueScreen(viewModel = viewModel)
                    }
                    "settings" -> {
                        com.example.ui.screens.SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryFilterRow(
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit
) {
    val categories = listOf(
        "All Items",
        "Camera Photos",
        "Screenshots",
        "Downloads",
        "WhatsApp Images",
        "Videos",
        "Favorites"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(categories) { category ->
            val isSelected = (selectedCategory == null && category == "All Items") || 
                             (selectedCategory != null && category == selectedCategory)

            val chipColor = if (isSelected) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                            else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(chipColor)
                    .clickable {
                        if (category == "All Items") {
                            onCategorySelect(null)
                        } else {
                            onCategorySelect(category)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("category_chip_$category")
            ) {
                Text(
                    text = category,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AppBottomNavigation(
    activeRoute: String,
    remainingSwipeCount: Int,
    deletedCandidatesCount: Int,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("app_navigation_bar")
    ) {
        NavigationBarItem(
            selected = activeRoute == "dashboard",
            onClick = { onNavigate("dashboard") },
            icon = { Icon(imageVector = Icons.Default.Dashboard, contentDescription = null) },
            label = { Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            modifier = Modifier.testTag("nav_dashboard")
        )

        NavigationBarItem(
            selected = activeRoute == "chat",
            onClick = { onNavigate("chat") },
            icon = { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null) },
            label = { Text("Chat", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            modifier = Modifier.testTag("nav_chat")
        )

        NavigationBarItem(
            selected = activeRoute == "swipe",
            onClick = { onNavigate("swipe") },
            icon = {
                Box {
                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null)
                    if (remainingSwipeCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            },
            label = { Text("Swipe", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            modifier = Modifier.testTag("nav_swipe")
        )

        NavigationBarItem(
            selected = activeRoute == "cleanup",
            onClick = { onNavigate("cleanup") },
            icon = { Icon(imageVector = Icons.Default.CleanHands, contentDescription = null) },
            label = { Text("Cleanup", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            modifier = Modifier.testTag("nav_cleanup")
        )

        NavigationBarItem(
            selected = activeRoute == "queue",
            onClick = { onNavigate("queue") },
            icon = {
                Box {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null)
                    if (deletedCandidatesCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                                .align(Alignment.TopEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = deletedCandidatesCount.toString(),
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            },
            label = { Text("Queue", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            modifier = Modifier.testTag("nav_queue")
        )

        NavigationBarItem(
            selected = activeRoute == "settings",
            onClick = { onNavigate("settings") },
            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            modifier = Modifier.testTag("nav_settings")
        )
    }
}

@Composable
fun PermissionsRequestScreen(
    allGranted: Boolean,
    onRequestPermissions: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Give Access to Photos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To let PhotoFlow find blurry photos, group similar pictures, and help you tidy up your gallery offline, we need your permission to see your photos and videos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRequestPermissions,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("grant_permissions_button")
            ) {
                Text("Give Permission", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuickFiltersRow(
    mediaType: String,
    aiCategory: String,
    dateRange: String,
    onMediaTypeSelect: (String) -> Unit,
    onAiCategorySelect: (String) -> Unit,
    onDateRangeSelect: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Media types
        item {
            FilterGroupTitle("Type")
        }
        val mediaTypes = listOf("All", "Images", "Videos")
        items(mediaTypes) { type ->
            val isSelected = mediaType == type
            FilterChipItem(
                text = type,
                isSelected = isSelected,
                onClick = { onMediaTypeSelect(type) },
                testTag = "media_type_chip_$type"
            )
        }

        // AI Quality/Categories
        item {
            FilterGroupTitle("Quality Check")
        }
        val aiCategories = listOf("All", "Blurry", "Dark/Light", "Screenshots", "With Faces", "OCR / Documents")
        items(aiCategories) { cat ->
            val isSelected = aiCategory == cat
            val displayTitle = when (cat) {
                "Dark/Light" -> "Too Dark/Bright"
                "OCR / Documents" -> "With Text"
                "With Faces" -> "With People"
                else -> cat
            }
            FilterChipItem(
                text = displayTitle,
                isSelected = isSelected,
                onClick = { onAiCategorySelect(cat) },
                testTag = "ai_category_chip_$cat"
            )
        }

        // Dates
        item {
            FilterGroupTitle("Time")
        }
        val dateRanges = listOf("Any Time", "Today", "Last 7 Days", "Last 30 Days")
        items(dateRanges) { range ->
            val isSelected = dateRange == range
            FilterChipItem(
                text = range,
                isSelected = isSelected,
                onClick = { onDateRangeSelect(range) },
                testTag = "date_range_chip_$range"
            )
        }
    }
}

@Composable
fun FilterGroupTitle(text: String) {
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .padding(end = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun FilterChipItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val chipColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
                      else Color.Transparent

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(chipColor)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(12.dp))
                } else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
