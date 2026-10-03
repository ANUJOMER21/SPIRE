package com.example.spire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.spire.ui.SpireAppRoot
import com.example.spire.ui.theme.SPIRETheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SPIRETheme {
                SpireAppRoot()
            }
        }
    }
}
