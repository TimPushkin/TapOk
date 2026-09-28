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
                for (rule in dao.loadSoftDeletedRules()) {
                    dao.deleteRuleIfSoftDeleted(rule.id)
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
