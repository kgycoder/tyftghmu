package com.iosstyle.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            val context = LocalContext.current
            val pm = context.packageManager
            val apps = remember { getInstalledApps(pm) }
            
            MaterialTheme(
                colorScheme = darkColorScheme(background = Color.Black)
            ) {
                LauncherScreen(apps = apps, onAppClick = { resolveInfo ->
                    val launchIntent = pm.getLaunchIntentForPackage(resolveInfo.activityInfo.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                    }
                })
            }
        }
    }

    private fun getInstalledApps(packageManager: PackageManager): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.category.LAUNCHER)
        }
        return packageManager.queryIntentActivities(intent, 0).sortedBy { 
            it.loadLabel(packageManager).toString() 
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherScreen(apps: List<ResolveInfo>, onAppClick: (ResolveInfo) -> Unit) {
    var showControlCenter by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E3A8A), Color(0xFF9333EA))
                )
            )
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    if (delta > 50 && !showControlCenter) showControlCenter = true
                }
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(50.dp))
            
            val pages = (apps.size / 24) + 1
            val pagerState = rememberPagerState(pageCount = { pages + 1 })
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                if (page < pages) {
                    val pageApps = apps.drop(page * 24).take(24)
                    AppGrid(apps = pageApps, onAppClick = onAppClick)
                } else {
                    AppLibrary(apps = apps, onAppClick = onAppClick)
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages + 1) { index ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.4f))
                    )
                }
            }
            
            val dockApps = apps.take(4)
            Dock(apps = dockApps, onAppClick = onAppClick)
            Spacer(modifier = Modifier.height(30.dp))
        }

        AnimatedVisibility(
            visible = showControlCenter,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ControlCenter(onClose = { showControlCenter = false })
        }
    }
}

@Composable
fun AppGrid(apps: List<ResolveInfo>, onAppClick: (ResolveInfo) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(apps) { app ->
            AppIcon(app = app, onClick = { onAppClick(app) })
        }
    }
}

@Composable
fun Dock(apps: List<ResolveInfo>, onAppClick: (ResolveInfo) -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.25f))
            .blur(radius = 30.dp, edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            apps.forEach { app ->
                AppIcon(app = app, onClick = { onAppClick(app) }, isDock = true)
            }
        }
    }
}

@Composable
fun AppIcon(app: ResolveInfo, onClick: () -> Unit, isDock: Boolean = false) {
    val pm = LocalContext.current.packageManager
    val label = app.loadLabel(pm).toString()
    val icon = remember { app.loadIcon(pm) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .width(64.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = icon,
                contentDescription = label,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (!isDock) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ControlCenter(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .blur(radius = 40.dp, edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)
            .clickable(onClick = onClose)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 16.dp)
                .width(320.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .padding(16.dp)
                .clickable(enabled = false) {}
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                CircleToggle(Icons.Rounded.Wifi, active = true, color = Color.Blue)
                                CircleToggle(Icons.Rounded.Bluetooth, active = true, color = Color.Blue)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                CircleToggle(Icons.Rounded.AirplanemodeActive, active = false, color = Color.Gray)
                                CircleToggle(Icons.Rounded.CellTower, active = true, color = Color.Green)
                            }
                        }
                    }
                }
                
                Box(modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Not Playing", color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.SkipPrevious, contentDescription = null, tint = Color.White)
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
                            Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VerticalSlider(icon = Icons.Rounded.LightMode, modifier = Modifier.weight(1f).height(120.dp))
                VerticalSlider(icon = Icons.Rounded.VolumeUp, modifier = Modifier.weight(1f).height(120.dp))
            }
        }
    }
}

@Composable
fun CircleToggle(icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, color: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(50))
            .background(if (active) color else Color.White.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
    }
}

@Composable
fun VerticalSlider(icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.3f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .background(Color.White)
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
fun AppLibrary(apps: List<ResolveInfo>, onAppClick: (ResolveInfo) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text("App Library", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search Bar
        BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            cursorBrush = SolidColor(Color.White),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.width(8.dp))
                    if (searchQuery.isEmpty()) {
                        Text("App Library", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
                    }
                    innerTextField()
                }
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val filteredApps = apps.filter { 
            it.loadLabel(LocalContext.current.packageManager).toString().contains(searchQuery, ignoreCase = true) 
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val chunks = filteredApps.chunked(4)
            items(chunks.size) { index ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(16.dp)
                ) {
                    AppGrid(apps = chunks[index], onAppClick = onAppClick)
                }
            }
        }
    }
}
