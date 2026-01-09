package com.github.timpushkin.tapok.data.rules

import kotlinx.coroutines.flow.Flow

interface RuleRepo {
    suspend fun loadAllRules(): List<Rule>

    fun loadAllRulesByPackage(): Flow<Map<String, List<Rule>>>

    suspend fun insertOrUpdateRule(rule: Rule)

    suspend fun deleteRule(rule: Rule)
}
