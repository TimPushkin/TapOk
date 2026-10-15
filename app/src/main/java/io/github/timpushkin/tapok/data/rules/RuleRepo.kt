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
