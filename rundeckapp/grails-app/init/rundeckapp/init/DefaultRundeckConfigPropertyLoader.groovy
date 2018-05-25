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

}
