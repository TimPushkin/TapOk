package com.github.timpushkin.tapok.ui.activity.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.github.timpushkin.tapok.R
import com.github.timpushkin.tapok.ui.activity.rules.RuleEditorPane
import com.github.timpushkin.tapok.ui.activity.rules.RuleEditorPlaceholder
import com.github.timpushkin.tapok.ui.activity.rules.RuleListPane
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.ElementsIntoSet
import kotlinx.serialization.Serializable

@Serializable
data object RuleListKey : TapOkNavKey.Primary(
    order = 2,
    label = R.string.rules,
    icon = R.drawable.settings_outlined,
    iconSelected = R.drawable.settings_filled
) {
    @Composable
    override fun Content(directive: PaneScaffoldDirective, backStack: NavBackStack<NavKey>) {
        RuleListPane(
            selectedRuleId = (backStack.findLast { it is RuleEditorKey } as RuleEditorKey?)?.ruleId,
            onEditRule = { backStack.addSecondary(RuleEditorKey(it?.id)) },
            onDeleteRule = { rule ->
                backStack.removeAll { it is RuleEditorKey && it.ruleId == rule.id }
            },
            directive
        )
    }
}

@Serializable
data class RuleEditorKey(val ruleId: Long?) : TapOkNavKey.Secondary(parent = RuleListKey) {
    @Composable
    override fun Content(directive: PaneScaffoldDirective, backStack: NavBackStack<NavKey>) {
        RuleEditorPane(
            ruleId,
            onClose = { backStack.remove(this) },
            directive
        )
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
object RulesModule {
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    @Provides
    @ElementsIntoSet
    fun provideNavKeyDescriptors(): Set<TapOkNavKeyDescriptor> = setOf(
        TapOkNavKeyDescriptor.OfInstance(
            RuleListKey,
            ListDetailSceneStrategy.listPane(sceneKey = toString()) { RuleEditorPlaceholder() }
        ),
        TapOkNavKeyDescriptor.OfClass(
            RuleEditorKey::class,
            ListDetailSceneStrategy.detailPane(sceneKey = toString())
        )
    )
}
