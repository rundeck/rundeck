package org.rundeck.core.triggers;

import java.util.Map;

/**
 * Handles a trigger action
 */
public interface TriggerActionHandler<T> {
    /**
     * @return true if this handler needs registration info at system startup
     */
    default boolean onStartup(T contextInfo) {
        return false;
    }

    /**
     * The action for a trigger should be performed
     *
     * @param triggerId    ID
     * @param conditionMap data from the condition
     * @param condition    condition
     * @param action       action
     */
    void performTriggerAction(String triggerId, T contextInfo, Map conditionMap, Condition condition, Action action);

    /**
     * @param action
     * @param contextInfo
     * @return true if the given action will be handled internally, meaning the registration of the condition check for a trigger will also handle invoking the action
     */
    boolean handlesAction(Action action, T contextInfo);
}
