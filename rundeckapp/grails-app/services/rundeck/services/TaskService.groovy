package rundeck.services

import com.google.common.util.concurrent.ListenableScheduledFuture
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class TaskService {
    static transactional = false
    private Map<String, ListenableScheduledFuture> timers = [:]
    /**
     * Scheduled executor for retries
     */
    def ListeningScheduledExecutorService scheduledExecutor = MoreExecutors.listeningDecorator(
            Executors.newScheduledThreadPool(5)
    )

    def schedule(long delay, String id = null, TimeUnit unit = TimeUnit.SECONDS, Closure clos) {
        if (!id) {
            id = UUID.randomUUID().toString()
        }
        timers[id] = scheduledExecutor.schedule({
                                                    timers.remove(id)
                                                    clos()
                                                }, delay, unit
        )
        id
    }

    def runAt(Date time, String id = null, Closure clos) {
        long delay = time.time - System.currentTimeMillis()
        if (!id) {
            id = UUID.randomUUID().toString()
        }
        timers[id] = scheduledExecutor.schedule({
                                                    timers.remove(id)
                                                    clos()
                                                },
                                                delay,
                                                TimeUnit.MILLISECONDS
        )

        id
    }

    ListenableScheduledFuture<?> task(String id) {
        timers[id]
    }

    boolean cancel(String id, boolean interrupt = false) {
        timers.remove(id)?.cancel(interrupt)
    }

    def listen(ListenableScheduledFuture future, Closure clos) {
        future.addListener(clos, scheduledExecutor)
        future
    }

    def periodic(long delay, long period, String id, TimeUnit unit = TimeUnit.SECONDS, Closure clos) {
        if (!id) {
            id = UUID.randomUUID().toString()
        }
        timers[id] = scheduledExecutor.scheduleAtFixedRate(clos, delay, period, unit)
        id
    }
}
