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
     * Reads a <code>dataSource.*</code> setting straight out of the deployment's rundeck-config file.
     *
     * Grails 8 builds the dataSource bean from the nested <code>dataSource { }</code> map in the
     * application config and does not consult the flat <code>dataSource.*</code> keys that Rundeck
     * publishes as a Spring property source, so whatever the packaged config declares wins. That
     * silently overrode the database an operator configured -- and rundeck-config is the only file a
     * deployment can edit, so there was no way around it. application.groovy asks for the values
     * here and places them in the nested map itself, the one place the framework is guaranteed to
     * read.
     *
     * All of url, driverClassName, username and password have to come through this way. Supplying
     * only the url leaves the rest on their H2 defaults, which fails for any other database with
     * "Access to DialectResolutionInfo cannot be null" -- a MySQL URL opened by the H2 driver.
     *
     * Deliberately reads the file rather than the Spring environment: this is called while the
     * application config is still being parsed, before any property source is queryable.
     *
     * @param key the setting name below <code>dataSource.</code>, e.g. <code>url</code>
     * @return the configured value, or null when unset or the file cannot be read
     */
    /** The driver Rundeck ships for MySQL and MariaDB connectivity. */
    private static final String MARIADB_DRIVER = "org.mariadb.jdbc.Driver"

    /** Connection parameter that makes the MariaDB 3.x driver accept jdbc:mysql: URLs again. */
    private static final String PERMIT_MYSQL_SCHEME = "permitMysqlScheme"

    /**
     * Reads the configured JDBC url, keeping <code>jdbc:mysql:</code> urls working.
     *
     * Rundeck's documented url for MySQL has always used the <code>jdbc:mysql:</code> scheme, and
     * mariadb-java-client 2.x accepted it. The Spring Boot 4 platform brings 3.x, which rejects that
     * scheme unless the connection carries <code>permitMysqlScheme</code>; without it the driver
     * simply returns no connection:
     *
     *   java.sql.SQLException: Driver:org.mariadb.jdbc.Driver@2a9b482d returned null for
     *   URL:jdbc:mysql://host/rundeck
     *
     * Appending the parameter here keeps every existing deployment working untouched, which matters
     * because rundeck-config is the only file they can edit. Only urls that are actually going to be
     * opened by the MariaDB driver are touched, so anyone supplying their own MySQL driver keeps the
     * url they wrote.
     *
     * @return the configured url, with permitMysqlScheme appended where required, or null
     */
    static String configuredDataSourceUrl() {
        String url = configuredDataSourceSetting("url")
        if (!url || !url.startsWith("jdbc:mysql:")) {
            return url
        }
        if (configuredDataSourceSetting("driverClassName") != MARIADB_DRIVER) {
            return url
        }
        if (url.contains(PERMIT_MYSQL_SCHEME)) {
            return url
        }
        String separator = url.contains("?") ? "&" : "?"
        String permitted = "${url}${separator}${PERMIT_MYSQL_SCHEME}"
        // Not LOG: this runs while the application config is being parsed, before logging is
        // initialised, so a logger call here is silently dropped (verified). stderr is what the
        // prebootstrap phase already uses, and it reaches service.log where operators look.
        System.err.println(
                "Added ${PERMIT_MYSQL_SCHEME} to dataSource.url: ${MARIADB_DRIVER} 3.x rejects the " +
                "jdbc:mysql: scheme without it. Configure a jdbc:mariadb: url to avoid this."
        )
        return permitted
    }

    static String configuredDataSourceSetting(String key) {
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
                def value = new ConfigSlurper().parse(configFile.toURI().toURL())?.flatten()?.get("dataSource.${key}".toString())
                return value ? value.toString() : null
            }
            Properties props = new Properties()
            configFile.withInputStream { props.load(it) }
            return props.getProperty("dataSource.${key}".toString()) ?: null
        } catch (Exception ex) {
            LOG.warn("Unable to read dataSource.${key} from ${location}", ex)
            return null
        }
    }

}
