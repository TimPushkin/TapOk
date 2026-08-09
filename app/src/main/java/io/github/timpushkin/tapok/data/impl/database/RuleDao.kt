package io.github.timpushkin.tapok.data.impl.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.timpushkin.tapok.data.rules.Rule
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RuleDao {
    @Query("SELECT * FROM rule WHERE is_pending_delete = 0")
    abstract fun observeAllRules(): Flow<List<Rule>>

    @Query("SELECT * FROM rule WHERE is_pending_delete = 1")
    abstract suspend fun loadSoftDeletedRules(): List<Rule>

    @Query("SELECT * FROM rule WHERE rule.id = :id")
    abstract suspend fun loadRule(id: Long): Rule

    @Query("SELECT * FROM rule WHERE rule.id = :id AND is_pending_delete = 0")
    abstract fun observeRuleOrNull(id: Long): Flow<Rule?>

    @Upsert
    abstract suspend fun insertOrUpdateRule(rule: Rule): Long

    @Query("UPDATE rule SET is_pending_delete = 1 WHERE id = :id")
    abstract suspend fun softDeleteRule(id: Long)

    @Query("UPDATE rule SET is_pending_delete = 0 WHERE id = :id")
    abstract suspend fun undoSoftDeleteRule(id: Long): Int

    @Query("DELETE FROM rule WHERE id = :id")
    abstract suspend fun deleteRule(id: Long)

    @Query("DELETE FROM rule WHERE id = :id AND is_pending_delete = 1")
    abstract suspend fun deleteRuleIfSoftDeleted(id: Long): Int
}
