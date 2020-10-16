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

import org.springframework.core.env.PropertySource
import rundeckapp.Application
import spock.lang.Specification
import spock.lang.Stepwise

@Stepwise
class ReloadableRundeckPropertySourceSpec extends Specification {
    def "do not try to load rundeck-config.groovy if set in system property RDECK_CONFIG_LOCATION"() {

        when:
        File tmpGroovy = File.createTempFile("app-test",".groovy")
        tmpGroovy << "grails { mail {} } "
        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION,tmpGroovy.absolutePath)
        PropertySource props = ReloadableRundeckPropertySource.getRundeckPropertySourceInstance()

        then:
        props.propertyNames.toList().isEmpty()

        cleanup:
        System.clearProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION)
    }


    def "load rundeck-config.properties if set in system property RDECK_CONFIG_LOCATION"() {

        when:
        File tmpProp = File.createTempFile("app-test",".properties")
        tmpProp << "myprop=avalue"
        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION,tmpProp.absolutePath)
        ReloadableRundeckPropertySource.reload()
        PropertySource props = ReloadableRundeckPropertySource.getRundeckPropertySourceInstance()

        then:
        props.getProperty("myprop") == "avalue"

    }


}
