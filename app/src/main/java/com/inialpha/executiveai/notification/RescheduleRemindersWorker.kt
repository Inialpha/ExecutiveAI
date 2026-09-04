package com.inialpha.executiveai.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inialpha.executiveai.ExecutiveAIApplication
import com.inialpha.executiveai.domain.model.ExecutiveItemState
import com.inialpha.executiveai.domain.model.ExecutiveItemType
import kotlinx.coroutines.flow.first

/** Re-schedules every still-future, ACCEPTED reminder item after a device reboot. */
class RescheduleRemindersWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as ExecutiveAIApplication).container
        // Flow -> take first snapshot: a one-shot reschedule pass, not an ongoing collection.
        val snapshot = container.executiveItemRepository
            .observeByType(ExecutiveItemType.REMINDER)
            .first()
        snapshot
            .filter { it.state == ExecutiveItemState.ACCEPTED }
            .filter { (it.dueAtMillis ?: 0L) > System.currentTimeMillis() }
            .forEach { item ->
                ReminderScheduler.schedule(
                    context = applicationContext,
                    itemId = item.id,
                    title = item.title,
                    body = item.description ?: "",
                    triggerAtMillis = item.dueAtMillis!!,
                )
            }
        return Result.success()
    }
}
