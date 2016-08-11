/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
