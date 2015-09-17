package rundeck.services.execution

/**
 * Created by greg on 9/17/15.
 */
interface ThresholdListener {
    void thresholdWasMet(Long value, String reason)
}

interface ThresholdValue {
    boolean thresholdWasMet()

    Long getThresholdValue()

    String getThresholdReason()

    String getAction()
}