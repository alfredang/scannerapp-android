package com.tertiaryinfotech.scannerapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.tertiaryinfotech.scannerapp.ScannerApplication
import com.tertiaryinfotech.scannerapp.data.DocumentWithPages
import com.tertiaryinfotech.scannerapp.service.ExportService
import com.tertiaryinfotech.scannerapp.service.PdfService
import com.tertiaryinfotech.scannerapp.ui.components.DocumentRow
import com.tertiaryinfotech.scannerapp.vm.LibraryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(navController: NavHostController) {
    val context = LocalContext.current
    val container = (context.applicationContext as ScannerApplication).container
    val vm: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
    val documents by vm.documents.collectAsStateWithLifecycle()
    val query by vm.searchText.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var renameTarget by remember { mutableStateOf<DocumentWithPages?>(null) }
    var renameText by remember { mutableStateOf("") }

    val filtered = vm.filter(documents, query)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.setSearch(it) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search by name or text") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (documents.isEmpty()) "No Documents" else "No matches",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)) {
                    items(filtered, key = { it.document.id }) { doc ->
                        LibraryRow(
                            doc = doc,
                            container = container,
                            onOpen = { navController.navigate(Routes.detail(doc.document.id)) },
                            onRename = { renameTarget = doc; renameText = doc.document.name },
                            onDuplicate = { vm.duplicate(doc) },
                            onDelete = { vm.delete(doc.document.id) },
                            onSharePdf = {
                                scope.launch {
                                    val bitmaps = container.storage.loadProcessedBitmaps(doc)
                                    val s = container.settings.current()
                                    val bytes = withContext(Dispatchers.Default) {
                                        PdfService.makePdf(bitmaps, s.pdfPageSize, s.pdfQuality)
                                    }
                                    val file = withContext(Dispatchers.IO) {
                                        ExportService.writePdf(context, bytes, doc.document.name)
                                    }
                                    context.startActivity(
                                        android.content.Intent.createChooser(
                                            ExportService.shareFiles(context, listOf(file), "application/pdf"),
                                            "Share PDF"
                                        )
                                    )
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    val target = renameTarget
    if (target != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.rename(target.document.id, renameText)
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun LibraryRow(
    doc: DocumentWithPages,
    container: com.tertiaryinfotech.scannerapp.AppContainer,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onSharePdf: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            DocumentRow(doc = doc, storage = container.storage, onClick = onOpen, modifier = Modifier.weight(1f))
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text("Rename") }, onClick = { menuOpen = false; onRename() })
            DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menuOpen = false; onDuplicate() })
            DropdownMenuItem(text = { Text("Share PDF") }, onClick = { menuOpen = false; onSharePdf() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
        }
    }
}
