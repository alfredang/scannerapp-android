package com.tertiaryinfotech.scannerapp.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tertiaryinfotech.scannerapp.vm.ScannerViewModel

object Routes {
    const val HOME = "home"
    const val PREVIEW = "preview"
    const val FILTER = "filter/{pageId}"
    const val EXPORT = "export"
    const val LIBRARY = "library"
    const val DETAIL = "detail/{docId}"
    const val SETTINGS = "settings"

    fun filter(pageId: String) = "filter/$pageId"
    fun detail(docId: String) = "detail/$docId"
}

@Composable
fun AppNav() {
    val navController = rememberNavController()
    // Share one ScannerViewModel across the whole capture/edit flow by scoping it to the Activity.
    val activity = LocalContext.current as ComponentActivity
    val scannerVm: ScannerViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = ScannerViewModel.Factory
    )

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(navController, scannerVm) }
        composable(Routes.PREVIEW) { PreviewScreen(navController, scannerVm) }
        composable(Routes.FILTER) { backStack ->
            val pageId = backStack.arguments?.getString("pageId").orEmpty()
            FilterScreen(navController, scannerVm, pageId)
        }
        composable(Routes.EXPORT) { ExportScreen(navController, scannerVm) }
        composable(Routes.LIBRARY) { LibraryScreen(navController) }
        composable(Routes.DETAIL) { backStack ->
            val docId = backStack.arguments?.getString("docId").orEmpty()
            DocumentDetailScreen(navController, docId)
        }
        composable(Routes.SETTINGS) { SettingsScreen(navController) }
    }
}

/** Pop the whole editor flow back to Home. */
fun NavHostController.backToHome() {
    popBackStack(Routes.HOME, inclusive = false)
}
