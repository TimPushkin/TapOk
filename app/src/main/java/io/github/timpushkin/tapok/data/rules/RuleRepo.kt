package io.github.timpushkin.tapok.data.rules

import kotlinx.coroutines.flow.Flow

interface RuleRepo {
    fun loadAllRules(): Flow<List<Rule>>

    suspend fun loadRule(ruleId: Long): Rule

    fun loadAllRulesByPackage(): Flow<Map<String, List<Rule>>>

    suspend fun insertOrUpdateRule(rule: Rule): Long

    suspend fun deleteRule(rule: Rule)
}
