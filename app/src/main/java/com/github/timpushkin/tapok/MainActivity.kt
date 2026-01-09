package com.github.timpushkin.tapok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.timpushkin.tapok.ui.activity.ActivityUi
import com.github.timpushkin.tapok.ui.theme.TapOkTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TapOkTheme { ActivityUi() } }
    }
}
