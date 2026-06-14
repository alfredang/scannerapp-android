package com.tertiaryinfotech.scannerapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.tertiaryinfotech.scannerapp.vm.ScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(navController: NavHostController, scannerVm: ScannerViewModel) {
    val working by scannerVm.working.collectAsStateWithLifecycle()

    LaunchedEffect(working) {
        if (working == null) navController.backToHome()
    }
    val doc = working ?: return
    val pages = doc.pages
    if (pages.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val currentIndex = pagerState.currentPage.coerceIn(0, pages.lastIndex)
    val currentPage = pages[currentIndex]

    val addPages = rememberDocumentCapture { bitmaps -> scannerVm.addPages(bitmaps) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Page ${currentIndex + 1} of ${pages.size}") },
                navigationIcon = {
                    TextButton(onClick = {
                        scannerVm.discard()
                        navController.backToHome()
                    }) { Text("Cancel") }
                },
                actions = {
                    TextButton(onClick = { navController.navigate(Routes.EXPORT) }) {
                        Text("Next", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) { page ->
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = pages[page].processed.asImageBitmap(),
                        contentDescription = "Page ${page + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            ControlBar(
                onRotate = { scannerVm.rotate(currentPage.id) },
                onFilters = { navController.navigate(Routes.filter(currentPage.id)) },
                onAddPage = addPages,
                onDelete = { scannerVm.deletePage(currentPage.id) }
            )
        }
    }
}

@Composable
private fun ControlBar(
    onRotate: () -> Unit,
    onFilters: () -> Unit,
    onAddPage: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ControlButton("Rotate", Icons.Filled.RotateRight, onRotate)
        ControlButton("Filters", Icons.Filled.Tune, onFilters)
        ControlButton("Add Page", Icons.Filled.Add, onAddPage)
        ControlButton("Delete", Icons.Filled.Delete, onDelete, destructive = true)
    }
}

@Composable
private fun ControlButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(width = 76.dp, height = 64.dp)
    ) {
        androidx.compose.material3.IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(26.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}
