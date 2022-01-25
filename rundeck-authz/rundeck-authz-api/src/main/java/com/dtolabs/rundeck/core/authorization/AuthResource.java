package com.dtolabs.rundeck.core.authorization;

import java.util.Map;

public interface AuthResource {
    Map<String, String> getResourceMap();

    static enum Context {
        Project,
        System
    }

    Context getContext();
}
