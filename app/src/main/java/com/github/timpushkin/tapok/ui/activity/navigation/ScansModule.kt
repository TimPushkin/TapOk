package com.github.timpushkin.tapok.ui.activity.navigation

import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.ui.activity.scans.ScanInspectorScreen
import com.github.timpushkin.tapok.ui.activity.scans.ScanListPane
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.ElementsIntoSet
import kotlinx.serialization.Serializable

@Serializable
data object ScanListKey : Pane.Primary(
    order = 3,
    label = R.string.scans,
    icon = R.drawable.data_table_outlined,
    iconSelected = R.drawable.data_table_filled
) {
    @Composable
    override fun Content(directive: PaneScaffoldDirective, backStack: MutableList<NavKey>) {
        ScanListPane(
            selectedScanId =
                (backStack.findLast { it is ScanInspectorKey } as ScanInspectorKey?)?.scanId,
            onSelectScan = { scan -> backStack.addScreen(ScanInspectorKey(scan.id)) },
            onDeleteScan = { scan ->
                backStack.removeAll { it is ScanInspectorKey && it.scanId == scan.id }
            }
        )
    }
}

@Serializable
data class ScanInspectorKey(val scanId: Long) : Screen {
    @Composable
    override fun Content(backStack: MutableList<NavKey>) {
        ScanInspectorScreen(scanId, onClose = { backStack.remove(this) })
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
object ScansModule {
    @Provides
    @ElementsIntoSet
    fun provideNavDescriptors(): Set<NavDescriptor<*>> = setOf(
        NavDescriptor.OfInstance(ScanListKey),
        NavDescriptor.OfClass(ScanInspectorKey::class)
    )
}
