/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* RepList.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/4/11 6:16 PM
* 
*/
package com.dtolabs.rundeck.core.plugins.metadata;

import java.util.*;

/**
 * PluginMeta defines plugin metadata
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PluginMeta {
    private String name;
    private String author;
    private String date;
    private String version;
    private String rundeckPluginVersion;
    private List<Map<String, Object>> providers;
    private List<ProviderDef> pluginDefs;

    public List<ProviderDef> getPluginDefs() {
        if (null == pluginDefs && null!=providers) {
            pluginDefs = new ArrayList<ProviderDef>();
            for (final Map<String, Object> provider : providers) {
                pluginDefs.add(new ProviderDef(provider));
            }
        }
        return pluginDefs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<Map<String, Object>> getProviders() {
        return providers;
    }

    public void setProviders(List<Map<String, Object>> providers) {
        this.providers = providers;
    }

    @Override
    public String toString() {
        return "PluginMeta{" +
               "name='" + name + '\'' +
               ", author='" + author + '\'' +
               ", date='" + date + '\'' +
               ", version='" + version + '\'' +
               ", providers=" + providers +
               ", pluginDefs=" + pluginDefs +
               '}';
    }

    public String getRundeckPluginVersion() {
        return rundeckPluginVersion;
    }

    public void setRundeckPluginVersion(final String rundeckPluginVersion) {
        this.rundeckPluginVersion = rundeckPluginVersion;
    }
}
