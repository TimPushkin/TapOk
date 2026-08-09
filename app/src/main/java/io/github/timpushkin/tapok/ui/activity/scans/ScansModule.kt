package io.github.timpushkin.tapok.ui.activity.scans

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
            ScanInspectorScreen(
                key.scanId,
                onCreateRule = { prefilling ->
                    navigator.navigateTo(RuleEditorKey(null, prefilling))
                },
                onClose = navigator::navigateBack
            )
        }
    }
}
