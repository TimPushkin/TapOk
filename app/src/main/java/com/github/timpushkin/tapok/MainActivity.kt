package com.github.timpushkin.tapok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.rememberCoroutineScope
import com.github.timpushkin.tapok.data.preferences.PreferencesRepo
import com.github.timpushkin.tapok.ui.activity.ActivityUi
import com.github.timpushkin.tapok.ui.activity.EntryProviderInstaller
import com.github.timpushkin.tapok.ui.activity.PrimaryKey
import com.github.timpushkin.tapok.ui.activity.home.HomeKey
import com.github.timpushkin.tapok.ui.activity.home.TutorialKey
import com.github.timpushkin.tapok.ui.activity.rememberNavigator
import com.github.timpushkin.tapok.ui.theme.TapOkTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var preferencesRepo: PreferencesRepo

    @Inject
    lateinit var primaryRoutes: Set<@JvmSuppressWildcards PrimaryKey>

    @Inject
    lateinit var entryProviderInstallers: Set<@JvmSuppressWildcards EntryProviderInstaller>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Preferences IO is fast so just blocking actually looks better than a loading animation
        val isTutorialFinished = runBlocking { preferencesRepo.isTutorialFinishedFlow().first() }
        setContent {
            TapOkTheme {
                val coroutineScope = rememberCoroutineScope()
                val navigator = rememberNavigator(
                    primaryRoutes,
                    HomeKey,
                    if (isTutorialFinished) emptyList() else listOf(TutorialKey),
                    onNavigateBack = { from ->
                        if (from == TutorialKey) {
                            coroutineScope.launch { preferencesRepo.markTutorialFinished() }
                        }
                    }
                )
                ActivityUi(navigator, entryProviderInstallers)
            }
        }
    }
}
