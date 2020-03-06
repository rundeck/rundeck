package org.rundeck.app.components.schedule;

import java.util.Map;

public interface TriggerBuilderHelper {

    /**
     * Returns the trigger builder
     */
    Object getTriggerBuilder();

    /**
     * Returns data related to the trigger builder, like time zone for example
     */
    Map getParams();

    /**
     * Returns time zone applied to the trigger builder
     */
    Object getTimeZone();

}
