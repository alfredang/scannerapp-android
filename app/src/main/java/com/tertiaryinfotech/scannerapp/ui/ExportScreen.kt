package com.tertiaryinfotech.scannerapp.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.tertiaryinfotech.scannerapp.ScannerApplication
import com.tertiaryinfotech.scannerapp.service.ExportService
import com.tertiaryinfotech.scannerapp.service.PdfService
import com.tertiaryinfotech.scannerapp.ui.components.SectionCard
import com.tertiaryinfotech.scannerapp.vm.ScannerViewModel
import com.tertiaryinfotech.scannerapp.vm.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(navController: NavHostController, scannerVm: ScannerViewModel) {
    val context = LocalContext.current
    val container = (context.applicationContext as ScannerApplication).container
    val settingsVm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val settings by settingsVm.settings.collectAsStateWithLifecycle()
    val working by scannerVm.working.collectAsStateWithLifecycle()
    val isRunningOcr by scannerVm.isRunningOcr.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val doc = working ?: return
    val baseName = doc.name.ifBlank { "Document" }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    // SAF: save PDF to Files. We build the bytes first, then write them to the chosen location.
    val pendingPdf = androidx.compose.runtime.remember { arrayOfNulls<ByteArray>(1) }
    val savePdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val bytes = pendingPdf[0]
        if (uri != null && bytes != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                }
                toast("Saved PDF")
            }
        }
        pendingPdf[0] = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Save & Share") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard {
                Column {
                    Text("Document", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = doc.name,
                        onValueChange = { scannerVm.setName(it) },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${doc.pages.size} ${if (doc.pages.size == 1) "page" else "pages"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionCard {
                Column {
                    Text("Text Recognition", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { scannerVm.runOcr { toast("Text recognition complete") } },
                        enabled = !isRunningOcr,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isRunningOcr) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.size(8.dp))
                        } else {
                            Icon(Icons.Filled.TextSnippet, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                        }
                        Text(if (isRunningOcr) "Recognizing…" else "Run OCR")
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Recognized text is saved with the document and is searchable in your library.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Export", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                    ExportAction("Share PDF", Icons.Filled.Share) {
                        scope.launch {
                            val bytes = withContext(Dispatchers.Default) {
                                PdfService.makePdf(doc.pages.map { it.processed }, settings.pdfPageSize, settings.pdfQuality)
                            }
                            val file = withContext(Dispatchers.IO) { ExportService.writePdf(context, bytes, baseName) }
                            context.startActivity(
                                android.content.Intent.createChooser(
                                    ExportService.shareFiles(context, listOf(file), "application/pdf"), "Share PDF"
                                )
                            )
                        }
                    }

                    ExportAction("Save PDF to Files", Icons.Filled.PictureAsPdf) {
                        scope.launch {
                            pendingPdf[0] = withContext(Dispatchers.Default) {
                                PdfService.makePdf(doc.pages.map { it.processed }, settings.pdfPageSize, settings.pdfQuality)
                            }
                            savePdfLauncher.launch("$baseName.pdf")
                        }
                    }

                    ExportAction("Share Images", Icons.Filled.Share) {
                        scope.launch {
                            val files = withContext(Dispatchers.IO) {
                                ExportService.writeJpgs(context, doc.pages.map { it.processed }, baseName)
                            }
                            context.startActivity(
                                android.content.Intent.createChooser(
                                    ExportService.shareFiles(context, files, "image/jpeg"), "Share Images"
                                )
                            )
                        }
                    }

                    ExportAction("Save Images to Photos", Icons.Filled.Photo) {
                        scope.launch {
                            val n = withContext(Dispatchers.IO) {
                                ExportService.saveToGallery(context, doc.pages.map { it.processed }, baseName)
                            }
                            toast(if (n > 0) "Saved $n image(s) to Photos" else "Could not save images")
                        }
                    }
                }
            }

            Button(
                onClick = { scannerVm.save { navController.backToHome() } },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Save to Library", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ExportAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.size(10.dp))
        Text(label)
        Spacer(Modifier.weight(1f))
    }
}
