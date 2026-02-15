package com.github.timpushkin.tapok.ui.activity.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.ui.activity.scans.ScanInspectorPane
import com.github.timpushkin.tapok.ui.activity.scans.ScanInspectorPlaceholder
import com.github.timpushkin.tapok.ui.activity.scans.ScanListPane
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.ElementsIntoSet
import kotlinx.serialization.Serializable

@Serializable
data object ScanListKey : TapOkNavKey.Primary(
    order = 3,
    label = R.string.scans,
    icon = R.drawable.data_table_outlined,
    iconSelected = R.drawable.data_table_filled
) {
    @Composable
    override fun Content(directive: PaneScaffoldDirective, backStack: NavBackStack<NavKey>) {
        ScanListPane(
            selectedScanId =
                (backStack.findLast { it is ScanInspectorKey } as ScanInspectorKey?)?.scanId,
            onSelectScan = { scan -> backStack.addSecondary(ScanInspectorKey(scan.id)) },
            onDeleteScan = { scan ->
                backStack.removeAll { it is ScanInspectorKey && it.scanId == scan.id }
            },
            directive
        )
    }
}

@Serializable
data class ScanInspectorKey(val scanId: Long) : TapOkNavKey.Secondary(parent = ScanListKey) {
    @Composable
    override fun Content(directive: PaneScaffoldDirective, backStack: NavBackStack<NavKey>) {
        ScanInspectorPane(
            scanId,
            onClose = { backStack.remove(this) },
            directive
        )
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
object ScansModule {
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    @Provides
    @ElementsIntoSet
    fun provideNavKeyDescriptors(): Set<TapOkNavKeyDescriptor> = setOf(
        TapOkNavKeyDescriptor.OfInstance(
            ScanListKey,
            ListDetailSceneStrategy.listPane(sceneKey = toString()) { ScanInspectorPlaceholder() }
        ),
        TapOkNavKeyDescriptor.OfClass(
            ScanInspectorKey::class,
            ListDetailSceneStrategy.detailPane(sceneKey = toString())
        )
    )
}
