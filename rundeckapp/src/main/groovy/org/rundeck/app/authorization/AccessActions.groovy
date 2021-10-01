package org.rundeck.app.authorization

interface AccessActions {
    /**
     *
     * @return list of actions all must be allowed
     */
    List<String> getRequiredActions();

    /**
     *
     * @return list of actions any are allowed
     */
    List<String> getAnyActions();

}