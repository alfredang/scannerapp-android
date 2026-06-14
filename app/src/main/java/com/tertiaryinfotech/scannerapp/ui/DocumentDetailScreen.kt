package com.tertiaryinfotech.scannerapp.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.tertiaryinfotech.scannerapp.ScannerApplication
import com.tertiaryinfotech.scannerapp.service.ExportService
import com.tertiaryinfotech.scannerapp.service.PdfService
import com.tertiaryinfotech.scannerapp.ui.components.SectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(navController: NavHostController, docId: String) {
    val context = LocalContext.current
    val container = (context.applicationContext as ScannerApplication).container
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val doc by container.dao.observeDocument(docId).collectAsStateWithLifecycle(initialValue = null)
    var menuOpen by remember { mutableStateOf(false) }
    var isRunningOcr by remember { mutableStateOf(false) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    val savePdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val d = doc ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            scope.launch {
                val bitmaps = container.storage.loadProcessedBitmaps(d)
                val s = container.settings.current()
                val bytes = withContext(Dispatchers.Default) { PdfService.makePdf(bitmaps, s.pdfPageSize, s.pdfQuality) }
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } }
                toast("Saved PDF")
            }
        }
    }

    val current = doc
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current?.document?.name ?: "Document", maxLines = 1) },
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
                        DropdownMenuItem(text = { Text("Share PDF") }, onClick = {
                            menuOpen = false
                            val d = current ?: return@DropdownMenuItem
                            scope.launch {
                                val bitmaps = container.storage.loadProcessedBitmaps(d)
                                val s = container.settings.current()
                                val bytes = withContext(Dispatchers.Default) { PdfService.makePdf(bitmaps, s.pdfPageSize, s.pdfQuality) }
                                val file = withContext(Dispatchers.IO) { ExportService.writePdf(context, bytes, d.document.name) }
                                context.startActivity(
                                    android.content.Intent.createChooser(
                                        ExportService.shareFiles(context, listOf(file), "application/pdf"), "Share PDF"
                                    )
                                )
                            }
                        })
                        DropdownMenuItem(text = { Text("Export PDF to Files") }, onClick = {
                            menuOpen = false
                            current?.let { savePdfLauncher.launch("${it.document.name}.pdf") }
                        })
                        DropdownMenuItem(text = { Text("Save Images to Photos") }, onClick = {
                            menuOpen = false
                            val d = current ?: return@DropdownMenuItem
                            scope.launch {
                                val bitmaps = container.storage.loadProcessedBitmaps(d)
                                val n = withContext(Dispatchers.IO) {
                                    ExportService.saveToGallery(context, bitmaps, d.document.name)
                                }
                                toast(if (n > 0) "Saved $n image(s)" else "Could not save")
                            }
                        })
                        DropdownMenuItem(text = { Text("Run OCR") }, onClick = {
                            menuOpen = false
                            val d = current ?: return@DropdownMenuItem
                            scope.launch {
                                isRunningOcr = true
                                container.storage.runOcr(d.document.id)
                                isRunningOcr = false
                                toast("Text recognition complete")
                            }
                        })
                    }
                }
            )
        }
    ) { padding ->
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val pages = current.orderedPages
        val pagerState = rememberPagerState(pageCount = { pages.size })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) { index ->
                val file = container.storage.processedFile(current.document.id, pages[index])
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = file,
                        contentDescription = "Page ${index + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            if (pages.isNotEmpty()) {
                Text(
                    "Page ${pagerState.currentPage + 1} of ${pages.size}",
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Box(Modifier.padding(16.dp)) {
                SectionCard {
                    Column {
                        Text("Recognized Text", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        val text = current.document.combinedOcrText
                        when {
                            isRunningOcr -> Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.size(8.dp))
                                Text("Recognizing…")
                            }
                            text.isBlank() -> Text(
                                "No text yet. Use Run OCR from the menu.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            else -> {
                                Text(text, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        clipboard.setText(AnnotatedString(text))
                                        toast("Copied")
                                    }) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = null)
                                        Spacer(Modifier.size(6.dp)); Text("Copy")
                                    }
                                    OutlinedButton(onClick = {
                                        context.startActivity(
                                            android.content.Intent.createChooser(ExportService.shareText(text), "Share Text")
                                        )
                                    }) {
                                        Icon(Icons.Filled.Share, contentDescription = null)
                                        Spacer(Modifier.size(6.dp)); Text("Share Text")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
