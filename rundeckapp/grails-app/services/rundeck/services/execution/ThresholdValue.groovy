package rundeck.services.execution

/**
 * Holds results of threshold checking
 */
interface ThresholdValue<T> {
    /**
     * @return true whenever the threshold is exceeded
     */
    boolean isThresholdExceeded()

    /**
     * @return current value
     */
    T getValue()

    /**
     * @return reason string
     */
    String getDescription()

    /**
     * @return action to perform on threshold
     */
    String getAction()
}
