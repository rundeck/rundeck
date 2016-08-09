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

package com.dtolabs.rundeck.server.projects;

import com.dtolabs.rundeck.core.common.IRundeckProjectConfig;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * project config implementation using property lookup
 */
public class RundeckProjectConfig implements IRundeckProjectConfig {
    private String name;
    private Date lastModifiedTime;
    private IPropertyLookup lookup;
    private IPropertyLookup projectLookup;

    public RundeckProjectConfig(
            final String name,
            final IPropertyLookup lookup,
            final IPropertyLookup projectLookup,
            final Date lastModifiedTime
    )
    {
        this.name = name;
        this.setLookup(lookup);
        this.setProjectLookup(projectLookup);
        this.setLastModifiedTime(lastModifiedTime);

    }

    public String getName(){
        return name;
    }
    public String getProperty(final String property) {
        return getLookup().getProperty(property);
    }


    public boolean hasProperty(final String key) {
        return getProperties().containsKey(key);
    }

    public Map<String, String> getProperties() {
        HashMap<String, String> result = new HashMap<>();
        if(null!= getLookup()){
            for(Object key: getLookup().getPropertiesMap().keySet()) {
                result.put(key.toString(), getLookup().getProperty(key.toString()));
            }
        }
        return result;
    }
    public Map<String, String> getProjectProperties() {
        HashMap<String, String> result = new HashMap<>();
        if(null!= getProjectLookup()){
            for(Object key: getProjectLookup().getPropertiesMap().keySet()) {
                result.put(key.toString(), getProjectLookup().getProperty(key.toString()));
            }
        }
        return result;
    }


    public Date getConfigLastModifiedTime() {
        return getLastModifiedTime();
    }


    public String toString() {
        return "RundeckProjectConfig{" +
               "name='" + name + '\'' +
               ", lastModifiedTime=" + getLastModifiedTime() +
               '}';
    }

    public IPropertyLookup getLookup() {
        return lookup;
    }

    public void setLookup(final IPropertyLookup lookup) {
        this.lookup = lookup;
    }

    public IPropertyLookup getProjectLookup() {
        return projectLookup;
    }

    public void setProjectLookup(final IPropertyLookup projectLookup) {
        this.projectLookup = projectLookup;
    }

    public Date getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(final Date lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

}
