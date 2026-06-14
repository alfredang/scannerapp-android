package com.tertiaryinfotech.scannerapp.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tertiaryinfotech.scannerapp.ScannerApplication
import com.tertiaryinfotech.scannerapp.ui.components.DocumentRow
import com.tertiaryinfotech.scannerapp.vm.LibraryViewModel
import com.tertiaryinfotech.scannerapp.vm.ScannerViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, scannerVm: ScannerViewModel) {
    val container = (LocalContext.current.applicationContext as ScannerApplication).container
    val libraryVm: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
    val documents by libraryVm.documents.collectAsStateWithLifecycle()

    val launchCapture = rememberDocumentCapture { bitmaps ->
        scannerVm.beginNewDocument(bitmaps)
        navController.navigate(Routes.PREVIEW)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tertiary Scanner", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Routes.LIBRARY) }) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Library")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            ScanButton(onClick = launchCapture)
            Spacer(Modifier.height(28.dp))

            if (documents.isEmpty()) {
                EmptyHome()
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { navController.navigate(Routes.LIBRARY) }) { Text("See All") }
                }
                documents.take(5).forEachIndexed { index, doc ->
                    DocumentRow(
                        doc = doc,
                        storage = container.storage,
                        onClick = { navController.navigate(Routes.detail(doc.document.id)) }
                    )
                    if (index < documents.take(5).lastIndex) HorizontalDivider()
                }
            }

            Spacer(Modifier.height(40.dp))
            Text(
                "Powered by Tertiary Infotech Academy Pte Ltd",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
                Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ScanButton(onClick: () -> Unit) {
    val gradient = Brush.horizontalGradient(listOf(Color(0xFF2E7CF6), Color(0xFF2E66D9)))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DocumentScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            Spacer(Modifier.size(12.dp))
            Text("Scan Document", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyHome() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Inbox, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        Text("No scans yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Tap Scan Document to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
