package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext;
import com.dtolabs.rundeck.core.common.Framework;

import javax.security.auth.Subject;
import java.util.Map;
import java.util.Set;

/**
 * Created by greg on 7/17/15.
 */
public interface AclRule {
    public String getSourceIdentity();

    public String getDescription();

    public Map<String, Object> getResource();

    public String getResourceType();

    public boolean isRegexMatch();

    public boolean isContainsMatch();
    public boolean isEqualsMatch();

    //    public Subject getSubject();
    public String getUsername();

    public String getGroup();

    public Set<String> getAllowActions();

    public EnvironmentalContext getEnvironment();

//    boolean isAppContext() {
//        return environment.equals(Framework.RUNDECK_APP_ENV);
//    }

    public Set<String> getDenyActions();
}
