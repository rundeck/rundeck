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
        }
    }

    static void reload() {
        refreshRundeckPropertyFile()
    }
}
