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
    private String url;
    private List<String> resourcesList;
    private String resourcesDir;
    private List<Map<String, Object>> providers;
    private List<ProviderDef> pluginDefs;

    //2.0
    private String id; //This is the first 12 characters of the name digested with sha256
    private String description;
    private String rundeckCompatibilityVersion;
    private String license;
    private List<String> tags;
    private String thirdPartyDependencies;
    private String sourceLink;
    private String targetHostCompatibility;

    public List<ProviderDef> getPluginDefs() {
        if (null == pluginDefs && null!=providers) {
            pluginDefs = new ArrayList<ProviderDef>();
            for (final Map<String, Object> provider : providers) {
                pluginDefs.add(new ProviderDef(provider));
            }
        }
        return pluginDefs;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
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

    public List<String> getResourcesList() {
        return resourcesList;
    }

    public void setResourcesList(List<String> resourcesList) {
        this.resourcesList = resourcesList;
    }

    public String getResourcesDir() {
        return resourcesDir;
    }

    public void setResourcesDir(String resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getRundeckCompatibilityVersion() {
        return rundeckCompatibilityVersion;
    }

    public void setRundeckCompatibilityVersion(final String rundeckCompatibilityVersion) {
        this.rundeckCompatibilityVersion = rundeckCompatibilityVersion;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(final String license) {
        this.license = license;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(final List<String> tags) {
        this.tags = tags;
    }

    public String getThirdPartyDependencies() {
        return thirdPartyDependencies;
    }

    public void setThirdPartyDependencies(final String thirdPartyDependencies) {
        this.thirdPartyDependencies = thirdPartyDependencies;
    }

    public String getSourceLink() {
        return sourceLink;
    }

    public void setSourceLink(final String sourceLink) {
        this.sourceLink = sourceLink;
    }

    public String getTargetHostCompatibility() {
        return targetHostCompatibility;
    }

    public void setTargetHostCompatibility(final String targetHostCompatibility) {
        this.targetHostCompatibility = targetHostCompatibility;
    }
}
