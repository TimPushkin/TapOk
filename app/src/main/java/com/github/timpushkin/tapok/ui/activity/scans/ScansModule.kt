package com.github.timpushkin.tapok.ui.activity.scans

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.ui.activity.TapOkNavKey
import com.github.timpushkin.tapok.ui.activity.TapOkNavKeyDescriptor
import com.github.timpushkin.tapok.ui.activity.addSecondary
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.ElementsIntoSet
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data object ScanListKey : TapOkNavKey.Primary() {
    @Transient
    override val order = 3

    @Composable
    override fun Icon(selected: Boolean) {
        Icon(
            painterResource(
                if (selected) R.drawable.data_table_filled else R.drawable.data_table_outlined
            ),
            contentDescription = stringResource(R.string.scans)
        )
    }

    @Composable
    override fun Label() {
        Text(stringResource(R.string.scans))
    }

    @Composable
    override fun Fab(backStack: NavBackStack<NavKey>) {
        ScannerEnablingSuggestionFab()
    }

    @Composable
    override fun Content(backStack: NavBackStack<NavKey>) {
        ScanList(
            onSelectScan = { scan ->
                backStack.addSecondary(ScanInspectorKey(scan.id))
            },
            onDeleteScan = { scan ->
                backStack.removeAll { it is ScanInspectorKey && it.scanId == scan.id }
            }
        )
    }
}

@Serializable
data class ScanInspectorKey(val scanId: Long) : TapOkNavKey.Secondary() {
    @Transient
    override val parent = ScanListKey

    @Transient
    override val hasLabel = true

    @Composable
    override fun Label() {
        // TODO: obtain and show app label
        Text(scanId.toString())
    }

    @Composable
    override fun Content(backStack: NavBackStack<NavKey>) {
        ScanInspector(scanId)
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
            ListDetailSceneStrategy.listPane(detailPlaceholder = { ScanInspectorPlaceholder() })
        ),
        TapOkNavKeyDescriptor.OfClass(
            ScanInspectorKey::class,
            ListDetailSceneStrategy.detailPane()
        )
    )
}
