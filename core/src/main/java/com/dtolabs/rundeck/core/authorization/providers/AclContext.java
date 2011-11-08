package com.dtolabs.rundeck.core.authorization.providers;

import java.util.Map;


public interface AclContext {

    public ContextDecision includes(Map<String, String> resource, String action);

}