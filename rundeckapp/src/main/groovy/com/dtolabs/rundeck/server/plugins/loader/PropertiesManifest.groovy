/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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

package com.dtolabs.rundeck.server.plugins.loader

/**
 * PropertiesManifest is ...
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-07-18
 */
class PropertiesManifest implements PluginFileManifest{
    public static final String PLUGIN_NAME_PROPERTY = 'plugin.name'
    public static final String PLUGIN_DESCRIPTION_PROPERTY = 'plugin.description'
    public static final String PLUGIN_FILENAME_PROPERTY = 'plugin.filename'
    public static final String PLUGIN_URL_PROPERTY = 'plugin.url'
    public static final String PLUGIN_AUTHOR_PROPERTY = 'plugin.author'
    public static final String PLUGIN_VERSION_PROPERTY = 'plugin.version'
    String prefix
    Properties properties

    PropertiesManifest(Properties properties) {
        this.properties = properties
    }

    PropertiesManifest(String prefix, Properties properties) {
        this.prefix = prefix
        this.properties = properties
    }

    @Override
    String getName() {
        return getProp(PLUGIN_NAME_PROPERTY)
    }

    @Override
    String getDescription() {
        return getProp(PLUGIN_DESCRIPTION_PROPERTY)
    }

    private String getProp(String property) {
        properties.getProperty((prefix ?: '') + property)
    }

    @Override
    String getFileName() {
        return getProp(PLUGIN_FILENAME_PROPERTY)
    }

    @Override
    String getUrl() {
        return getProp(PLUGIN_URL_PROPERTY)
    }

    @Override
    String getAuthor() {
        return getProp(PLUGIN_AUTHOR_PROPERTY)
    }

    @Override
    String getVersion() {
        return getProp(PLUGIN_VERSION_PROPERTY)
    }


    @Override
    public String toString() {
        return "Manifest{" +
                "name='" + name + '\'' +
                ", description=\'" + description + "'" +
                ", fileName=\'" + fileName + "'" +
                ", url=\'" + url + "'" +
                ", author=\'" + author + "'" +
                ", version=\'" + version + "'" +
                '}';
    }
}
