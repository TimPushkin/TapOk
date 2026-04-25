package com.github.timpushkin.tapok.ui.activity.scans

import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.ui.activity.EntryProviderInstaller
import com.github.timpushkin.tapok.ui.activity.PrimaryKey
import com.github.timpushkin.tapok.ui.activity.SecondaryKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
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
    @Provides
    @IntoSet
    fun providePrimaryKey(): PrimaryKey = ScanListKey

    @Provides
    @IntoSet
    fun provideEntryProviderInstaller(): EntryProviderInstaller = { navigator ->
        entry<ScanListKey> {
            ScanListPane(
                selectedScanId = (navigator.currentKey as? ScanInspectorKey)?.scanId,
                onSelectScan = { scan ->
                    if (navigator.currentKey is ScanInspectorKey) {
                        navigator.navigateBack()
                    }
                    navigator.navigateTo(ScanInspectorKey(scan.id))
                },
                onDeleteScan = {
                    // Scan inspector is full-screen so it cannot be open now
                    check(navigator.currentKey is ScanListKey)
                }
            )
        }
        entry<ScanInspectorKey> { key ->
            ScanInspectorScreen(key.scanId, onClose = navigator::navigateBack)
        }
    }
}
