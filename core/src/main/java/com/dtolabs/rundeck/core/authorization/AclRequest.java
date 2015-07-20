package com.dtolabs.rundeck.core.authorization;

import java.util.Map;
import java.util.Set;

/**
 * Created by greg on 7/20/15.
 */
public interface AclRequest {
    Map<String, String> getResource();

    AclSubject getSubject();

    String getAction();

    Set<Attribute> getEnvironment();
}
