/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.env.PropertySource

//@CompileStatic - This can break depending on what is loaded by the ServiceLoader
class ReloadableRundeckPropertySource {

    private static final Properties rundeckProps = new Properties()
    private static final PropertiesPropertySource propertySource = new PropertiesPropertySource(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION,rundeckProps)

    static {
        refreshRundeckPropertyFile()
    }

    static PropertySource getRundeckPropertySourceInstance() {
        return propertySource;
    }

    private static void refreshRundeckPropertyFile() {
        String configLocation = System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION)
        
        if (configLocation && !configLocation.endsWith(".groovy")) {
            CoreConfigurationPropertiesLoader rundeckConfigPropertyFileLoader = new DefaultRundeckConfigPropertyLoader()
            ServiceLoader<CoreConfigurationPropertiesLoader> rundeckPropertyLoaders = ServiceLoader.load(
                    CoreConfigurationPropertiesLoader
            )
            rundeckPropertyLoaders.each { loader ->
                rundeckConfigPropertyFileLoader = loader
            }
            Properties tmp = rundeckConfigPropertyFileLoader.loadProperties()
            rundeckProps.clear()
            tmp.each {key, value ->
                rundeckProps[key] = tmp.get(key)
            }
            permitMysqlSchemeOnDataSourceUrl()
        }
    }

    /**
     * Rewrites this source's dataSource.url so the MariaDB 3.x driver accepts a jdbc:mysql: scheme.
     *
     * application.groovy already asks DefaultRundeckConfigPropertyLoader for a permitted url, but it
     * does not win: Application.loadRundeckPropertySources adds this source with addFirst, so the
     * flat dataSource.url straight out of rundeck-config.properties outranks the nested map and is
     * what the connection pool is built from. The symptom is a startup that logs the fix being
     * applied and then dies anyway on
     *
     *   java.sql.SQLException: Driver:org.mariadb.jdbc.Driver returned null for URL:jdbc:mysql://...
     *
     * with the url in the message identical to the configured one. Patching the winning source is
     * the only placement that covers every consumer, reload() included.
     */
    private static void permitMysqlSchemeOnDataSourceUrl() {
        String url = rundeckProps["dataSource.url"]
        String permitted = DefaultRundeckConfigPropertyLoader.permitMysqlScheme(
                url, rundeckProps["dataSource.driverClassName"] as String
        )
        if (permitted != url) {
            rundeckProps["dataSource.url"] = permitted
        }
    }

    static void reload() {
        refreshRundeckPropertyFile()
    }
}
