package rundeck.services

import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class TaskService {
    static transactional = false

    /**
     * Scheduled executor for retries
     */
    def ListeningScheduledExecutorService scheduledExecutor = MoreExecutors.listeningDecorator(
            Executors.newScheduledThreadPool(5)
    )

    def schedule(long delay, TimeUnit unit = TimeUnit.SECONDS, Closure clos) {
        scheduledExecutor.schedule(clos, delay, unit)
    }

    def runAt(Date time, Closure clos) {
        long delay = time.time - System.currentTimeMillis()
        scheduledExecutor.schedule(clos, delay, TimeUnit.MILLISECONDS)
    }

    def periodic(long delay, long period, TimeUnit unit = TimeUnit.SECONDS, Closure clos) {
        scheduledExecutor.scheduleAtFixedRate(clos, delay, period, unit)
    }
}
