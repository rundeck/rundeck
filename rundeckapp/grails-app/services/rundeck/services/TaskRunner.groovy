package rundeck.services

import org.apache.log4j.Logger

import java.util.concurrent.BlockingQueue

/**
 * Consumes tasks from a blocking queue, and runs them with a specified runner until interrupted
 * @param < E >
 */
class TaskRunner<E> implements Runnable{
    static final Logger log = Logger.getLogger(TaskRunner.class)
    BlockingQueue<E> queue
    Closure runner

    public TaskRunner(BlockingQueue<E> queue, Closure runner) {
        this.queue = queue
        this.runner=runner
    }

    @Override
    void run() {
        log.debug("starting queue consumer")
        while (true) {
            try {
                E task = queue.take()
                log.debug("running a task...")
                runner.call(task)
            } catch (InterruptedException e) {
                break;
            } catch (Throwable t) {
                log.error("An error occured while processing a task: ${t}", t)
            }
        }
    }
}
