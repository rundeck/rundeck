package org.rundeck.core.triggers;

import java.util.Map;

/**
 * Trigger condition handler
 */
public interface TriggerConditionHandler<T> {
    /**
     * @return true if this handler needs registration info at system startup
     */
    default boolean onStartup() {
        return false;
    }

    /**
     * @param condition
     * @param contextInfo
     * @return true if the given condition should be handled
     */
    boolean handlesConditionChecks(Condition condition, T contextInfo);

    /**
     * Register condition checks for the trigger, will be called when a trigger is created or modified, so
     * if the registration has already occurred for the given trigger, the condition data should be
     * checked for changes
     *
     * @param triggerId
     * @param contextInfo
     * @param condition
     * @param action
     * @param service
     */
    void registerConditionChecksForAction(String triggerId, T contextInfo, Condition condition, Action action, TriggerActionInvoker service);

    /**
     * Remove condition check registration for the action
     *
     * @param triggerId
     * @param contextInfo
     * @param condition
     * @param action
     * @param service
     */
    void deregisterConditionChecksForAction(String triggerId, T contextInfo, Condition condition, Action action, TriggerActionInvoker service);
}
