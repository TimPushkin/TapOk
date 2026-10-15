// Copyright (C) 2026  Timofei Pushkin
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

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
