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

package io.github.timpushkin.tapok.ui.activity.scans

import androidx.navigation3.runtime.result.LocalResultEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.ui.activity.EntryProviderInstaller
import io.github.timpushkin.tapok.ui.activity.PrimaryKey
import io.github.timpushkin.tapok.ui.activity.SecondaryKey
import io.github.timpushkin.tapok.ui.activity.rules.RuleEditorKey
import kotlinx.serialization.Serializable

@Serializable
data object ScanListKey : PrimaryKey(
    index = 3,
    label = R.string.scans,
    icon = R.drawable.home_storage_outlined,
    iconSelected = R.drawable.home_storage_filled
)

@Serializable
data class ScanInspectorKey(val scanId: Long) : SecondaryKey(fullScreen = true)

@Module
@InstallIn(ActivityRetainedComponent::class)
object ScansModule {
    const val DELETED_SCAN_EVENT_KEY = "deleted_scan"

    @Provides
    @IntoSet
    fun providePrimaryKey(): PrimaryKey = ScanListKey

    @Provides
    @IntoSet
    fun provideEntryProviderInstaller(): EntryProviderInstaller = { _, navigator ->
        entry<ScanListKey> {
            ScanListPane(
                selectedScanId = (navigator.currentKey as? ScanInspectorKey)?.scanId,
                onSelectScan = { scan ->
                    navigator.navigateTo(ScanInspectorKey(scan.id))
                }
            )
        }
        entry<ScanInspectorKey> { key ->
            val resultBus = LocalResultEventBus.current
            ScanInspectorScreen(
                key.scanId,
                onDelete = { resultBus.sendResult(DELETED_SCAN_EVENT_KEY, key.scanId) },
                onCreateRule = { prefilling ->
                    navigator.navigateTo(RuleEditorKey(null, prefilling))
                },
                onClose = navigator::navigateBack
            )
        }
    }
}
