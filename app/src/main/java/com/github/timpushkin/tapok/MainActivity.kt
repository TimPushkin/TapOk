package com.github.timpushkin.tapok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.timpushkin.tapok.ui.activity.ActivityUi
import com.github.timpushkin.tapok.ui.activity.navigation.NavDescriptor
import com.github.timpushkin.tapok.ui.theme.TapOkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    /* private */ lateinit var navKeyDescriptors: Set<@JvmSuppressWildcards NavDescriptor<*>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TapOkTheme { ActivityUi(navKeyDescriptors) } }
    }
}
