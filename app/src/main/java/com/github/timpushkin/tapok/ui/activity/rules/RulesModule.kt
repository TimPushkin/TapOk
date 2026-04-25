package com.github.timpushkin.tapok.ui.activity.rules

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
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
data object RuleListKey : PrimaryKey(
    index = 2,
    label = R.string.rules,
    icon = R.drawable.page_info_outlined,
    iconSelected = R.drawable.page_info_filled
)

@Serializable
data class RuleEditorKey(val ruleId: Long?) : SecondaryKey()

@Module
@InstallIn(ActivityRetainedComponent::class)
object RulesModule {
    @Provides
    @IntoSet
    fun providePrimaryKey(): PrimaryKey = RuleListKey

    @Provides
    @IntoSet
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    fun provideEntryProviderInstaller(): EntryProviderInstaller = { navigator ->
        entry<RuleListKey>(
            metadata = ListDetailSceneStrategy.listPane { RuleEditorPlaceholder() }
        ) {
            RuleListPane(
                selectedRuleId = (navigator.currentKey as? RuleEditorKey)?.ruleId,
                onEditRule = {
                    if (navigator.currentKey is RuleEditorKey) {
                        navigator.navigateBack()
                    }
                    navigator.navigateTo(RuleEditorKey(it?.id))
                },
                onDeleteRule = { rule ->
                    val deletingCurrent = navigator.currentKey.let {
                        it is RuleEditorKey && it.ruleId == rule.id
                    }
                    if (deletingCurrent) {
                        navigator.navigateBack()
                    }
                }
            )
        }
        entry<RuleEditorKey>(
            metadata = ListDetailSceneStrategy.detailPane()
        ) { key ->
            RuleEditorPane(
                key.ruleId,
                onCreated = { id ->
                    // Need to update this key with the new ID
                    check(navigator.currentKey == key)
                    navigator.navigateBack()
                    navigator.navigateTo(RuleEditorKey(id))
                },
                onBack = navigator::navigateBack
            )
        }
    }
}
