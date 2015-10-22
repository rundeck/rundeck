package rundeck.services.execution

/**
 * can receive a ValueHolder to watch
 */
interface ValueWatcher<T> {
    /**
     *
     * @param holder
     */
    void watch(ValueHolder<T> holder)
}


