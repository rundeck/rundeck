/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
package rundeckapp.init

import com.dtolabs.rundeck.core.properties.CoreConfigurationPropertiesLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class DefaultRundeckConfigPropertyLoader implements CoreConfigurationPropertiesLoader {
    private static final transient Logger LOG = LoggerFactory.getLogger(DefaultRundeckConfigPropertyLoader.class)

    @Override
    Properties loadProperties() {
        Properties rundeckConfigs = new Properties()
        try {
            rundeckConfigs.load(
                    new File(System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION)).newInputStream()
            )
        } catch (Exception ex) {
            LOG.error("Unable to load rundeck-config.properties.", ex)
        }

        return rundeckConfigs
    }

    /**
     * Reads <code>dataSource.url</code> straight out of the deployment's rundeck-config file.
     *
     * Grails 8 builds the dataSource bean from the nested <code>dataSource { }</code> map in the
     * application config and does not consult the flat <code>dataSource.url</code> key that Rundeck
     * publishes as a Spring property source, so the URL an operator sets was ignored in favour of
     * the one packaged inside the WAR. rundeck-config is the only file a deployment can edit, so
     * application.groovy asks for the value here and places it in the nested map itself -- the one
     * place the framework is guaranteed to read.
     *
     * Deliberately reads the file rather than the Spring environment: this is called while the
     * application config is still being parsed, before any property source is queryable.
     *
     * @return the configured JDBC URL, or null when none is set or the file cannot be read
     */
    static String configuredDataSourceUrl() {
        String location = System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION)
        if (!location) {
            return null
        }
        File configFile = new File(location)
        if (!configFile.isFile()) {
            return null
        }
        try {
            if (location.endsWith(".groovy")) {
                def url = new ConfigSlurper().parse(configFile.toURI().toURL())?.flatten()?.get("dataSource.url")
                return url ? url.toString() : null
            }
            Properties props = new Properties()
            configFile.withInputStream { props.load(it) }
            return props.getProperty("dataSource.url") ?: null
        } catch (Exception ex) {
            LOG.warn("Unable to read dataSource.url from ${location}", ex)
            return null
        }
    }

}
