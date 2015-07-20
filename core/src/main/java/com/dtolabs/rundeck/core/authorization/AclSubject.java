package com.dtolabs.rundeck.core.authorization;

import java.util.Set;

/**
 * Created by greg on 7/17/15.
 */
public interface AclSubject {
    public String getUsername();
    public Set<String> getGroups();
}
