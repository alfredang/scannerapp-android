package com.tertiaryinfotech.scannerapp.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.tertiaryinfotech.scannerapp.BuildConfig
import com.tertiaryinfotech.scannerapp.model.FilterType
import com.tertiaryinfotech.scannerapp.settings.PdfPageSize
import com.tertiaryinfotech.scannerapp.ui.components.SectionCard
import com.tertiaryinfotech.scannerapp.vm.SettingsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val settings by vm.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Done")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard {
                Column {
                    Text("PDF Export", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Text("Page Size", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    PickerMenu(
                        label = settings.pdfPageSize.displayName,
                        options = PdfPageSize.entries.map { it.displayName },
                        onSelected = { idx -> vm.setPageSize(PdfPageSize.entries[idx]) }
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Quality: ${(settings.pdfQuality * 100).roundToInt()}%", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = settings.pdfQuality,
                        onValueChange = { vm.setQuality(it) },
                        valueRange = 0.30f..1.0f,
                        steps = 13
                    )
                }
            }

            SectionCard {
                Column {
                    Text("Scanning", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Text("Default Filter", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    PickerMenu(
                        label = settings.defaultFilter.displayName,
                        options = FilterType.entries.map { it.displayName },
                        onSelected = { idx -> vm.setDefaultFilter(FilterType.entries[idx]) }
                    )
                }
            }

            SectionCard {
                Column {
                    Text("About", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Tertiary Scanner", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Scan, enhance, and share documents — fully offline. Nothing leaves your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Powered by Tertiary Infotech Academy Pte Ltd",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerMenu(label: String, options: List<String>, onSelected: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(label)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    open = false
                    onSelected(index)
                })
            }
        }
    }
}
