package com.tertiaryinfotech.scannerapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.tertiaryinfotech.scannerapp.imaging.ImageProcessor
import com.tertiaryinfotech.scannerapp.model.FilterType
import com.tertiaryinfotech.scannerapp.ui.components.FilterThumbnail
import com.tertiaryinfotech.scannerapp.vm.ScannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(navController: NavHostController, scannerVm: ScannerViewModel, pageId: String) {
    val working by scannerVm.working.collectAsStateWithLifecycle()
    val doc = working
    val page = doc?.pages?.firstOrNull { it.id == pageId }
    if (page == null) {
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }

    val previews = remember { mutableStateMapOf<FilterType, androidx.compose.ui.graphics.ImageBitmap>() }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(page.id, page.rotationDegrees) {
        previews.clear()
        val small = withContext(Dispatchers.Default) {
            ImageProcessor.makeThumbnail(page.original, 220)
        }
        FilterType.entries.forEach { filter ->
            val bmp = withContext(Dispatchers.Default) {
                ImageProcessor.process(small, filter, page.rotationDegrees)
            }
            previews[filter] = bmp.asImageBitmap()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Filters") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Apply to All Pages") },
                            onClick = {
                                menuOpen = false
                                scannerVm.applyFilterToAll(page.filter)
                            }
                        )
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = page.processed.asImageBitmap(),
                    contentDescription = "Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Row(Modifier.padding(vertical = 8.dp)) {
                LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) {
                    items(FilterType.entries.toList()) { filter ->
                        FilterThumbnail(
                            filter = filter,
                            preview = previews[filter],
                            selected = filter == page.filter,
                            onClick = { scannerVm.setFilter(page.id, filter) }
                        )
                    }
                }
            }
        }
    }
}
