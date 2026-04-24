package com.github.timpushkin.tapok.ui.activity.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
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
data object RuleListKey : PaneKey.Primary(
    order = 2,
    label = R.string.rules,
    icon = R.drawable.page_info_outlined,
    iconSelected = R.drawable.page_info_filled
) {
    @Composable
    override fun Content(directive: PaneScaffoldDirective, backStack: MutableList<PaneKey>) {
        RuleListPane(
            selectedRuleId = (backStack.findLast { it is RuleEditorKey } as RuleEditorKey?)?.ruleId,
            onEditRule = { backStack.addSecondary(RuleEditorKey(it?.id)) },
            onDeleteRule = { rule ->
                backStack.removeAll { it is RuleEditorKey && it.ruleId == rule.id }
            }
        )
    }
}

@Serializable
data class RuleEditorKey(val ruleId: Long?) : PaneKey.Secondary(parent = RuleListKey) {
    @Composable
    override fun Content(directive: PaneScaffoldDirective, backStack: MutableList<PaneKey>) {
        RuleEditorPane(
            ruleId,
            onCreated = { id ->
                backStack.replaceAll {
                    if (it is RuleEditorKey && it.ruleId == ruleId) RuleEditorKey(id) else it
                }
            },
            onBack = { backStack.remove(this) },
            showBackButton = directive.maxHorizontalPartitions == 1
        )
    }
}

@Module
@InstallIn(ActivityRetainedComponent::class)
object RulesModule {
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    @Provides
    @ElementsIntoSet
    fun provideNavDescriptors(): Set<NavDescriptor<PaneKey>> = setOf(
        NavDescriptor.OfInstance(
            RuleListKey,
            ListDetailSceneStrategy.listPane(sceneKey = toString()) { RuleEditorPlaceholder() }
        ),
        NavDescriptor.OfClass(
            RuleEditorKey::class,
            ListDetailSceneStrategy.detailPane(sceneKey = toString())
        )
    )
}
