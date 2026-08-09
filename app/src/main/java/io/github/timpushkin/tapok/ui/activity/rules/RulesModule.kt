package io.github.timpushkin.tapok.ui.activity.rules

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.ui.activity.EntryProviderInstaller
import io.github.timpushkin.tapok.ui.activity.PrimaryKey
import io.github.timpushkin.tapok.ui.activity.SecondaryKey
import kotlinx.serialization.Serializable

@Serializable
data object RuleListKey : PrimaryKey(
    index = 2,
    label = R.string.rules,
    icon = R.drawable.page_info_outlined,
    iconSelected = R.drawable.page_info_filled
)

@Serializable
data class RuleViewerKey(val ruleId: Long) : SecondaryKey()

@Serializable
data class RuleEditorKey(val ruleId: Long?, val basis: RuleBasis?) :
    SecondaryKey(fullScreen = true) // Full-screen to give fewer ways to leave without saving

@Module
@InstallIn(ActivityRetainedComponent::class)
object RulesModule {
    @Provides
    @IntoSet
    fun providePrimaryKey(): PrimaryKey = RuleListKey

    @Provides
    @IntoSet
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    fun provideEntryProviderInstaller(): EntryProviderInstaller = { directive, navigator ->
        entry<RuleListKey>(
            metadata = ListDetailSceneStrategy.listPane { RuleViewerPlaceholder() }
        ) {
            RuleListPane(
                selectedRuleId = (navigator.currentKey as? RuleViewerKey)?.ruleId,
                onCreateRule = { navigator.navigateTo(RuleEditorKey(null, null)) },
                onSelectRule = { rule ->
                    if (navigator.currentKey is RuleViewerKey) {
                        navigator.navigateBack()
                    }
                    navigator.navigateTo(RuleViewerKey(rule.id))
                }
            )
        }
        entry<RuleViewerKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
            RuleViewerPane(
                key.ruleId,
                onEdit = { navigator.navigateTo(RuleEditorKey(key.ruleId, basis = null)) },
                onBack = navigator::navigateBack,
                directive = directive
            )
        }
        entry<RuleEditorKey> { key ->
            RuleEditorScreen(
                key.ruleId,
                key.basis,
                onSave = { rule ->
                    navigator.navigateBack()
                    // If viewing rules, display the saved rule
                    if (navigator.currentKey is RuleViewerKey) {
                        navigator.navigateBack()
                    }
                    if (navigator.currentKey is RuleListKey) {
                        navigator.navigateTo(RuleViewerKey(rule.id))
                    }
                },
                onClose = navigator::navigateBack
            )
        }
    }
}
