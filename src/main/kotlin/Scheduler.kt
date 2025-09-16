package gay.spiders

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.ZonedDateTime

fun CoroutineScope.scheduleDailyTask(
    hour: Int,
    minute: Int,
    timeZone: String = "America/New_York",
    action: suspend () -> Unit
) {
    val logger = LoggerFactory.getLogger("DailyScheduler")

    launch {
        val zoneId = ZonedDateTime.now().zone.rules.getOffset(ZonedDateTime.now().toInstant())
        val now = ZonedDateTime.now(zoneId)
        var nextRunTime = now.withHour(hour).withMinute(minute).withSecond(0)

        if (now.isAfter(nextRunTime)) {
            nextRunTime = nextRunTime.plusDays(1)
        }

        val initialDelay = Duration.between(now, nextRunTime).toMillis()
        logger.info("Task scheduled. Next run at: $nextRunTime. Initial delay: ${initialDelay / 1000} seconds.")

        delay(initialDelay)

        while (true) {
            try {
                logger.info("Executing daily task...")
                action()
                logger.info("Daily task finished.")
            } catch (e: Exception) {
                logger.error("An error occurred in the daily scheduled task", e)
            }
            delay(Duration.ofDays(1).toMillis())
        }
    }
}