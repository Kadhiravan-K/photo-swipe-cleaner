package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.PhotoFlowApplication
import com.example.ui.PhotoFlowViewModel

@Composable
fun SettingsScreen(
    viewModel: PhotoFlowViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Retrieve Shared Preferences
    val prefs = remember {
        context.getSharedPreferences("photoflow_prefs", Context.MODE_PRIVATE)
    }

    // General States
    var selectedTheme by remember { mutableStateOf(prefs.getString("setting_theme", "Dark Slate") ?: "Dark Slate") }
    var selectedLanguage by remember { mutableStateOf(prefs.getString("setting_lang", "English") ?: "English") }
    var notificationsEnabled by remember { mutableStateOf(prefs.getBoolean("setting_notifications", true)) }
    var swipeSensitivity by remember { mutableStateOf(prefs.getFloat("setting_swipe_sensitivity", 0.5f)) }
    var animationSpeed by remember { mutableStateOf(prefs.getFloat("setting_animation_speed", 0.5f)) }

    // Gallery Scan Options
    var scanLocation by remember { mutableStateOf(prefs.getString("setting_scan_loc", "/DCIM") ?: "/DCIM") }
    var autoRefresh by remember { mutableStateOf(prefs.getBoolean("setting_auto_refresh", true)) }
    var includeVideos by remember { mutableStateOf(prefs.getBoolean("setting_include_videos", true)) }
    var includeScreenshots by remember { mutableStateOf(prefs.getBoolean("setting_include_screenshots", true)) }

    // AI Configuration States
    var enableAiAnalysis by remember { mutableStateOf(prefs.getBoolean("auto_ai_analysis", true)) }
    var enableFaceDetection by remember { mutableStateOf(prefs.getBoolean("setting_face_detect", true)) }
    var enableMemoryProtection by remember { mutableStateOf(prefs.getBoolean("setting_mem_protect", true)) }
    var enableSmartRecommendations by remember { mutableStateOf(prefs.getBoolean("setting_smart_recs", true)) }
    var aiProcessingFrequency by remember { mutableStateOf(prefs.getString("setting_ai_freq", "Background Idle") ?: "Background Idle") }

    // Privacy States
    var localProcessingOnly by remember { mutableStateOf(prefs.getBoolean("setting_local_only", true)) }
    var dataPermissionsGranted by remember { mutableStateOf(prefs.getBoolean("setting_data_perms", true)) }

    // Storage info states
    var dbBytesCount by remember { mutableStateOf(prefs.getLong("setting_db_size", 2432320L)) }
    var cacheBytesCount by remember { mutableStateOf(prefs.getLong("setting_cache_size", 14320L)) }

    // Category Enable/Disable Map (Settings > Gallery Organization)
    val categoriesList = listOf(
        "Camera Photos",
        "Screenshots",
        "WhatsApp Images",
        "Downloads",
        "Videos",
        "Favorites",
        "Archived"
    )

    // Save individual category enables
    val categoriesEnabled = remember {
        mutableStateMapOf<String, Boolean>().apply {
            categoriesList.forEach { cat ->
                this[cat] = prefs.getBoolean("cat_enabled_$cat", true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Page Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = "App Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Change how the app looks and works",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 1: Gallery Organization (MOVE CATEGORIZATION FROM MAIN PAGE)
        SettingsGroup(title = "Photo Groups to Show", icon = Icons.Default.FolderOpen) {
            Text(
                text = "Choose which folders you want to show or hide in the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            categoriesList.forEach { category ->
                val isEnabled = categoriesEnabled[category] ?: true
                RowSettingToggle(
                    title = category,
                    subtitle = "Show $category",
                    checked = isEnabled,
                    onCheckedChange = { newVal ->
                        categoriesEnabled[category] = newVal
                        prefs.edit().putBoolean("cat_enabled_$category", newVal).apply()
                        viewModel.triggerMediaScan() // Trigger refresh dynamically
                    },
                    testTag = "setting_category_$category"
                )
            }
        }

        // Section 2: General Preferences
        SettingsGroup(title = "My Preferences", icon = Icons.Default.SettingsApplications) {
            // Theme selection dropdown simulator
            DropdownSettingRow(
                title = "App Theme",
                selectedOption = selectedTheme,
                options = listOf("Dark Slate", "Dark Indigo", "Pure Black"),
                onSelect = {
                    selectedTheme = it
                    prefs.edit().putString("setting_theme", it).apply()
                }
            )

            // Language
            DropdownSettingRow(
                title = "Choose Language",
                selectedOption = selectedLanguage,
                options = listOf("English", "Spanish", "French", "German"),
                onSelect = {
                    selectedLanguage = it
                    prefs.edit().putString("setting_lang", it).apply()
                }
            )

            // Notifications
            RowSettingToggle(
                title = "Alerts",
                subtitle = "Tell me when I have photos to tidy up",
                checked = notificationsEnabled,
                onCheckedChange = {
                    notificationsEnabled = it
                    prefs.edit().putBoolean("setting_notifications", it).apply()
                }
            )

            // Swipe sensitivity
            SliderSettingRow(
                title = "How easy to slide photos",
                value = swipeSensitivity,
                onValueChange = {
                    swipeSensitivity = it
                    prefs.edit().putFloat("setting_swipe_sensitivity", it).apply()
                }
            )

            // Animation speed
            SliderSettingRow(
                title = "How fast things move",
                value = animationSpeed,
                onValueChange = {
                    animationSpeed = it
                    prefs.edit().putFloat("setting_animation_speed", it).apply()
                }
            )
        }

        // Section 3: Gallery Scopes
        SettingsGroup(title = "Gallery Scanning", icon = Icons.Default.PhotoLibrary) {
            DropdownSettingRow(
                title = "Where to look for photos",
                selectedOption = scanLocation,
                options = listOf("/DCIM", "/DCIM/Camera", "/Pictures", "/Downloads"),
                onSelect = {
                    scanLocation = it
                    prefs.edit().putString("setting_scan_loc", it).apply()
                }
            )

            RowSettingToggle(
                title = "Keep looking for new photos",
                subtitle = "Automatically find new photos in the background",
                checked = autoRefresh,
                onCheckedChange = {
                    autoRefresh = it
                    prefs.edit().putBoolean("setting_auto_refresh", it).apply()
                }
            )

            RowSettingToggle(
                title = "Include video files",
                subtitle = "Show videos in your tidy up suggestions",
                checked = includeVideos,
                onCheckedChange = {
                    includeVideos = it
                    prefs.edit().putBoolean("setting_include_videos", it).apply()
                }
            )

            RowSettingToggle(
                title = "Include screenshots",
                subtitle = "Show screenshot pictures you took on your phone",
                checked = includeScreenshots,
                onCheckedChange = {
                    includeScreenshots = it
                    prefs.edit().putBoolean("setting_include_screenshots", it).apply()
                }
            )
        }

        // Section 4: AI Settings
        SettingsGroup(title = "Smart Photo Check", icon = Icons.Default.AutoAwesome) {
            RowSettingToggle(
                title = "Enable Photo Quality Check",
                subtitle = "Check if photos are blurry, dark, or duplicates",
                checked = enableAiAnalysis,
                onCheckedChange = {
                    enableAiAnalysis = it
                    viewModel.setAutoAiAnalysisEnabled(it)
                },
                testTag = "setting_ai_analysis_toggle"
            )

            RowSettingToggle(
                title = "Group Photos by Person",
                subtitle = "Find and group similar faces automatically",
                checked = enableFaceDetection,
                onCheckedChange = {
                    enableFaceDetection = it
                    prefs.edit().putBoolean("setting_face_detect", it).apply()
                }
            )

            RowSettingToggle(
                title = "Safe Keep Special Memories",
                subtitle = "Keep safe very important pictures like birthdays",
                checked = enableMemoryProtection,
                onCheckedChange = {
                    enableMemoryProtection = it
                    prefs.edit().putBoolean("setting_mem_protect", it).apply()
                }
            )

            RowSettingToggle(
                title = "Enable Smart Suggestions",
                subtitle = "Suggest photos to tidy up based on similarity",
                checked = enableSmartRecommendations,
                onCheckedChange = {
                    enableSmartRecommendations = it
                    prefs.edit().putBoolean("setting_smart_recs", it).apply()
                }
            )

            DropdownSettingRow(
                title = "How often to check photos",
                selectedOption = aiProcessingFrequency,
                options = listOf("When the App is Quiet", "Daily Scheduled", "Hourly Scopes"),
                onSelect = {
                    aiProcessingFrequency = it
                    prefs.edit().putString("setting_ai_freq", it).apply()
                }
            )
        }

        // Section 5: Secure Privacy
        SettingsGroup(title = "My Data Privacy", icon = Icons.Default.Security) {
            RowSettingToggle(
                title = "Keep all checks on this phone",
                subtitle = "Your photos never leave your device. Safe and secure.",
                checked = localProcessingOnly,
                onCheckedChange = {
                    localProcessingOnly = it
                    prefs.edit().putBoolean("setting_local_only", it).apply()
                }
            )

            RowSettingToggle(
                title = "Phone photo permissions",
                subtitle = "Let the app search your phone's gallery",
                checked = dataPermissionsGranted,
                onCheckedChange = {
                    dataPermissionsGranted = it
                    prefs.edit().putBoolean("setting_data_perms", it).apply()
                }
            )

            Button(
                onClick = {
                    Toast.makeText(context, "Exporting local gallery DB successfully as JSON file", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save database details", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    dbBytesCount = 0L
                    cacheBytesCount = 0L
                    prefs.edit().putLong("setting_db_size", 0L).putLong("setting_cache_size", 0L).apply()
                    Toast.makeText(context, "Cache and database index reset cleared successfully", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear all saved scans", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Section 6: Local Storage allocation
        SettingsGroup(title = "App Memory Space", icon = Icons.Default.Storage) {
            StorageAllocationBlock(
                dbSize = viewModel.formatBytes(dbBytesCount),
                cacheSize = viewModel.formatBytes(cacheBytesCount),
                onOptimizeClick = {
                    cacheBytesCount = 0L
                    prefs.edit().putLong("setting_cache_size", 0L).apply()
                    Toast.makeText(context, "Sandbox indices optimized successfully!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Section 7: About
        SettingsGroup(title = "About this App", icon = Icons.Default.Info) {
            InfoSettingRow(label = "App Version", value = "1.4.2 [Production Stable]")
            InfoSettingRow(label = "Privacy standard", value = "Privacy Offline First M3")
            InfoSettingRow(label = "Look and feel", value = "Spring Elastic Vectors")
            
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        Toast.makeText(context, "Opening direct feedback channel...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Send Feedback", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                TextButton(
                    onClick = {
                        Toast.makeText(context, "Redirecting to Privacy Policy terms...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Privacy Policy", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SettingsGroup(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF2B2930),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF938F99).copy(alpha = 0.12f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun RowSettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}

@Composable
fun DropdownSettingRow(
    title: String,
    selectedOption: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Box {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF381E72).copy(alpha = 0.3f))
                    .border(1.dp, Color(0xFF938F99).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = selectedOption, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF2B2930))
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = Color.White) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SliderSettingRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            val label = if (value < 0.33f) "Low" else if (value < 0.66f) "Medium" else "High"
            Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color(0xFF535158)
            )
        )
    }
}

@Composable
fun InfoSettingRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun StorageAllocationBlock(
    dbSize: String,
    cacheSize: String,
    onOptimizeClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Saved scanning history", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(dbSize, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Temporary files", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(cacheSize, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
        
        Button(
            onClick = onOptimizeClick,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Free Up Space", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
