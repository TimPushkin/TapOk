package io.github.timpushkin.tapok.data.rules

import io.github.timpushkin.tapok.data.Deletion
import kotlinx.coroutines.flow.Flow

interface RuleRepo {
    fun observeAllRules(): Flow<List<Rule>>

    suspend fun loadRule(id: Long): Rule

    fun observeRuleOrNull(id: Long): Flow<Rule?>

    suspend fun insertOrUpdateRule(rule: Rule): Long

    suspend fun deleteRule(id: Long, mode: Deletion = Deletion.HARD)

    suspend fun undoDeleteRule(id: Long): Boolean

    fun scheduleCleanupOfDeletedRules()
}
