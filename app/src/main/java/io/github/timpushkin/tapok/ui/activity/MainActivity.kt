// Copyright (C) 2026  Timofei Pushkin
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package io.github.timpushkin.tapok.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.timpushkin.tapok.data.preferences.PreferencesRepo
import io.github.timpushkin.tapok.ui.activity.settings.SettingsKey
import io.github.timpushkin.tapok.ui.activity.settings.TutorialKey
import io.github.timpushkin.tapok.ui.activity.scans.ScanListKey
import io.github.timpushkin.tapok.ui.theme.TapOkTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainActivity"

private val DefaultNavRoute = NavRoute(SettingsKey, emptyList())

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var preferencesRepo: PreferencesRepo

    @Inject
    lateinit var primaryRoutes: Set<@JvmSuppressWildcards PrimaryKey>

    @Inject
    lateinit var entryProviderInstallers: Set<@JvmSuppressWildcards EntryProviderInstaller>

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val newIntentChannel = Channel<Intent>(Channel.CONFLATED)
        addOnNewIntentListener(newIntentChannel::trySend)

        // TODO: use deep link API
        var initialRoute by mutableStateOf(
            // If there is saved state the initial route will be ignored and the saved one will be
            // restored. The launching intent should be already reflected in that state.
            if (savedInstanceState != null) DefaultNavRoute else parseNavRoute(intent)
        )
        if (initialRoute == null) {
            splashScreen.setKeepOnScreenCondition { initialRoute == null }
            lifecycleScope.launch {
                initialRoute = if (!preferencesRepo.isTutorialFinishedFlow().first()) {
                    NavRoute(SettingsKey, listOf(TutorialKey))
                } else {
                    DefaultNavRoute
                }
            }
        }

        setContent {
            TapOkTheme {
                val navigator = rememberNavigator(
                    primaries = primaryRoutes,
                    mainPrimary = DefaultNavRoute.primary,
                    initialRoute = initialRoute ?: return@TapOkTheme,
                    onNavigateBack = { from ->
                        if (from == TutorialKey) {
                            lifecycleScope.launch { preferencesRepo.markTutorialFinished() }
                        }
                    }
                )
                MainUi(navigator, entryProviderInstallers)
                LaunchedEffect(Unit) {
                    for (intent in newIntentChannel) {
                        val (primary, secondaries) = parseNavRoute(intent) ?: continue
                        navigator.navigateTo(primary)
                        secondaries.forEach(navigator::navigateTo)
                    }
                }
            }
        }
    }

    private fun parseNavRoute(intent: Intent?): NavRoute? {
        if (intent == null || intent.action != Intent.ACTION_VIEW) {
            return null
        }

        val data = intent.data
        if (data == null) {
            Log.w(TAG, "Received view intent without data: $intent")
            return null
        }
        if (data.scheme != DATA_SCHEME) {
            Log.w(TAG, "Received view intent with unrecognized data scheme: $intent")
            return null
        }

        val pathSegments = data.pathSegments
        if (pathSegments.size == 1 && pathSegments.first() == DATA_PATH_SCANS) {
            return NavRoute(ScanListKey, emptyList())
        }

        Log.w(TAG, "Received view intent with unrecognized data path: $intent")
        return null
    }

    companion object {
        const val DATA_SCHEME = "tapok"
        const val DATA_PATH_SCANS = "scans"
    }
}
