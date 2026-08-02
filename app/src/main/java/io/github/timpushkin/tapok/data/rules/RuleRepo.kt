package io.github.timpushkin.tapok.data.rules

import kotlinx.coroutines.flow.Flow

interface RuleRepo {
    fun observeAllRules(): Flow<List<Rule>>

    suspend fun loadRule(ruleId: Long): Rule

    fun observeRuleOrNull(ruleId: Long): Flow<Rule?>

    suspend fun insertOrUpdateRule(rule: Rule): Long

    suspend fun deleteRule(rule: Rule)
}
