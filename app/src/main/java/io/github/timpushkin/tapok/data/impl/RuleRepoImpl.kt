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

package io.github.timpushkin.tapok.data.impl

import io.github.timpushkin.tapok.data.Deletion
import io.github.timpushkin.tapok.data.impl.database.RuleDao
import io.github.timpushkin.tapok.data.rules.Rule
import io.github.timpushkin.tapok.data.rules.RuleRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.inject.Inject

class RuleRepoImpl @Inject constructor(private val dao: RuleDao) : RuleRepo {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val cleanupRequestChannel = Channel<Unit>(Channel.CONFLATED)

    init {
        scheduleCleanupOfDeletedRules() // Cleanup in case something was left because of a crash
        coroutineScope.launch {
            for (request in cleanupRequestChannel) {
                for ((id) in dao.loadSoftDeletedRules()) {
                    dao.deleteRuleIfSoftDeleted(id)
                }
            }
        }
    }

    override fun observeAllRules() = dao.observeAllRules()

    override suspend fun loadRule(id: Long) = dao.loadRule(id)

    override fun observeRuleOrNull(id: Long) = dao.observeRuleOrNull(id)

    override suspend fun insertOrUpdateRule(rule: Rule) = dao.insertOrUpdateRule(rule)

    override suspend fun deleteRule(id: Long, mode: Deletion) {
        when (mode) {
            Deletion.SOFT -> dao.softDeleteRule(id)
            Deletion.HARD_AFTER_SOFT -> dao.deleteRuleIfSoftDeleted(id)
            Deletion.HARD -> dao.deleteRule(id)
        }
    }

    override suspend fun undoDeleteRule(id: Long) = dao.undoSoftDeleteRule(id) != 0

    override fun scheduleCleanupOfDeletedRules() {
        cleanupRequestChannel.trySend(Unit)
    }
}
