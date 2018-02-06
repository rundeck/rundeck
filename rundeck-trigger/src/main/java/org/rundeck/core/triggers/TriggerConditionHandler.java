package org.rundeck.core.triggers;

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
     * @param triggerCondition
     * @param contextInfo
     * @return true if the given triggerCondition should be handled
     */
    boolean handlesConditionChecks(TriggerCondition triggerCondition, T contextInfo);

    /**
     * Register triggerCondition checks for the trigger, will be called when a trigger is created or modified, so
     * if the registration has already occurred for the given trigger, the triggerCondition data should be
     * checked for changes
     *
     * @param triggerId
     * @param contextInfo
     * @param triggerCondition
     * @param action
     * @param service
     */
    void registerConditionChecksForAction(String triggerId, T contextInfo, TriggerCondition triggerCondition, TriggerAction action, TriggerActionInvoker service);

    /**
     * Remove triggerCondition check registration for the action
     *
     * @param triggerId
     * @param contextInfo
     * @param triggerCondition
     * @param action
     * @param service
     */
    void deregisterConditionChecksForAction(String triggerId, T contextInfo, TriggerCondition triggerCondition, TriggerAction action, TriggerActionInvoker service);
}
