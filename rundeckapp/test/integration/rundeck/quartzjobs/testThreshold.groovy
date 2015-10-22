package rundeck.quartzjobs

import rundeck.services.execution.ThresholdValue

/**
 * Created by greg on 9/22/15.
 */
class testThreshold implements ThresholdValue<Long> {
    Long myValue
    String description
    String action
    boolean wasMet

    testThreshold() {
    }

    boolean isThresholdExceeded() {
        return wasMet
    }

    @Override
    Long getValue() {
        return myValue
    }

}
