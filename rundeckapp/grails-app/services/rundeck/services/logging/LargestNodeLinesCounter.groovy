package rundeck.services.logging

import rundeck.services.execution.ValueHolder

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Collates the number of output lines per-node, and returns the largest value
 */
class LargestNodeLinesCounter implements ValueHolder<Long> {
    private AtomicLong largest = new AtomicLong(0)
    final ConcurrentMap<String, AtomicLong> nodes = new ConcurrentHashMap<>()


    @Override
    Long getValue() {
        return largest.get()
    }

    /**
     * compareAndSet if the new value is greater than the current value
     * @param atomic atomic long
     * @param update the new value
     * @return
     */
    static boolean greaterAndSet(AtomicLong atomic, long update) {
        while (true) {
            long cur = atomic.get();
            if (update <= cur) {
                return false
            }
            //should set if it hasn't changed
            if (atomic.compareAndSet(cur, update)) {
                return true;
            }
            //otherwise try again
        }
    }

    /**
     * Add a number of lines logged for a node
     * @param name node name
     * @param count number of lines
     */
    public void nodeLogged(String name, long count) {
        long test = count
        AtomicLong previous = nodes.putIfAbsent(name, new AtomicLong(count))
        if (null != previous) {
            test = previous.addAndGet(count)
        }
        greaterAndSet(largest, test)
    }
}
