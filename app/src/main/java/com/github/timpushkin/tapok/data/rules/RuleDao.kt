package com.github.timpushkin.tapok.data.rules

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao : RuleRepo {
    @Query("SELECT * FROM rule")
    override suspend fun loadAllRules(): List<Rule>

    @Query("SELECT * FROM rule")
    override fun loadAllRulesByPackage(): Flow<Map<@MapColumn("package_name") String, List<Rule>>>

    @Upsert
    override suspend fun insertOrUpdateRule(rule: Rule)

    @Delete
    override suspend fun deleteRule(rule: Rule)
}
