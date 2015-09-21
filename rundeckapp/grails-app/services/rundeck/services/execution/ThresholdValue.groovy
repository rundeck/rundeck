package rundeck.services.execution

/**
 * Holds results of threshold checking
 */
interface ThresholdValue<T> {
    /**
     * @return true whenever the threshold is met
     */
    boolean thresholdWasMet()

    /**
     * @return value met
     */
    T getThresholdValue()

    /**
     * @return reason string
     */
    String getDescription()

    /**
     * @return action to perform on threshold
     */
    String getAction()
}
