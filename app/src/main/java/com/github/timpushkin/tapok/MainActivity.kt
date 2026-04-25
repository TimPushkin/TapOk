package com.github.timpushkin.tapok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.timpushkin.tapok.ui.activity.ActivityUi
import com.github.timpushkin.tapok.ui.activity.EntryProviderInstaller
import com.github.timpushkin.tapok.ui.activity.PrimaryKey
import com.github.timpushkin.tapok.ui.activity.home.HomeKey
import com.github.timpushkin.tapok.ui.theme.TapOkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    /* private */ lateinit var primaries: Set<@JvmSuppressWildcards PrimaryKey>

    @Inject
    /* private */ lateinit var entryProviderInstallers:
            Set<@JvmSuppressWildcards EntryProviderInstaller>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TapOkTheme { ActivityUi(HomeKey, primaries, entryProviderInstallers) } }
    }
}
