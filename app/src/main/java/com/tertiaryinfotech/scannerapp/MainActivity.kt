package com.tertiaryinfotech.scannerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tertiaryinfotech.scannerapp.ui.AppNav
import com.tertiaryinfotech.scannerapp.ui.theme.TertiaryScannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TertiaryScannerTheme {
                AppNav()
            }
        }
    }
}
