package com.github.timpushkin.tapok.data.impl.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Upsert
import com.github.timpushkin.tapok.data.rules.Rule
import com.github.timpushkin.tapok.data.rules.RuleRepo
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao : RuleRepo {
    @Query("SELECT * FROM rule")
    override fun loadAllRules(): Flow<List<Rule>>

    @Query("SELECT * FROM rule WHERE rule.id = :ruleId")
    override suspend fun loadRule(ruleId: Long): Rule

    @Query("SELECT * FROM rule")
    override fun loadAllRulesByPackage(): Flow<Map<@MapColumn("package_name") String, List<Rule>>>

    @Upsert
    override suspend fun insertOrUpdateRule(rule: Rule): Long

    @Delete
    override suspend fun deleteRule(rule: Rule)
}