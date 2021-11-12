package org.rundeck.core.auth.access;

import java.util.Arrays;
import java.util.List;

/**
 * Represents access request allowed by any actions (OR).
 */
public interface AuthActions {

    /**
     * @return list of actions any are allowed
     */
    List<String> getActions();

    /**
     * @return Description of access
     */
    String getDescription();


    /**
     * @param actions
     */
    default AuthActions or(AuthActions actions) {
        return AuthActionsUtil.or(this, actions);
    }

    /**
     * @param actions
     */
    default AuthActions or(String... actions) {
        return AuthActionsUtil.or(this, Arrays.asList(actions));
    }

    default AuthActions withDescription(String description) {
        if (description != null && !description.equals("")) {
            return AuthActionsUtil.withDescription(this, description);
        }
        return this;
    }
}
