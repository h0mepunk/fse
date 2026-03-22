package com.example.fse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.fse.di.AppContainer
import com.example.fse.ui.FSEApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as FSEApplication
        setContent {
            FSEApp(container = AppContainer.from(app))
        }
    }
}
