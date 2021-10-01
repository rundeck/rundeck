package org.rundeck.core.auth.access;

import java.util.List;

/**
 * Represents access request actions, either a list of all required actions (AND), or a list of any required actions
 * (OR).
 */
public interface AccessActions {
    /**
     * @return list of actions all must be allowed
     */
    List<String> getRequiredActions();

    /**
     * @return list of actions any are allowed
     */
    List<String> getAnyActions();
}
