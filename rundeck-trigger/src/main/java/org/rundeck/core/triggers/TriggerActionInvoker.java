package org.rundeck.core.triggers;

import java.util.Map;

/**
 * Provides a callback when a condition is met by the ConditionHandler
 */
public interface TriggerActionInvoker<T> {
    /**
     * The condition was met for a registered trigger
     *
     * @param triggerId
     * @param conditionMap any additional data from the condition
     */
    void triggerConditionMet(String triggerId, T contextInfo, Map conditionMap);
}
