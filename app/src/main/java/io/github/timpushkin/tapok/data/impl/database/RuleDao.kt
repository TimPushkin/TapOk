package io.github.timpushkin.tapok.data.impl.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import io.github.timpushkin.tapok.data.rules.Rule
import io.github.timpushkin.tapok.data.rules.RuleRepo
import kotlinx.coroutines.flow.Flow

private const val QUERY_LOAD_RULE = "SELECT * FROM rule WHERE rule.id = :ruleId"

@Dao
interface RuleDao : RuleRepo {
    @Query("SELECT * FROM rule")
    override fun observeAllRules(): Flow<List<Rule>>

    @Query(QUERY_LOAD_RULE)
    override suspend fun loadRule(ruleId: Long): Rule

    @Query(QUERY_LOAD_RULE)
    override fun observeRuleOrNull(ruleId: Long): Flow<Rule?>

    @Upsert
    override suspend fun insertOrUpdateRule(rule: Rule): Long

    @Delete
    override suspend fun deleteRule(rule: Rule)
}