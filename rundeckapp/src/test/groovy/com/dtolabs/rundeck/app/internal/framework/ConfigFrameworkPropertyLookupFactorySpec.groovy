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
package com.dtolabs.rundeck.app.internal.framework

import com.dtolabs.rundeck.core.utils.IPropertyLookup
import org.grails.config.NavigableMap
import org.grails.testing.GrailsUnitTest
import spock.lang.Specification


class ConfigFrameworkPropertyLookupFactorySpec extends Specification implements GrailsUnitTest {

    def "create property lookup"() {
        setup:
        ConfigFrameworkPropertyLookupFactory.metaClass.getFrameworkProperties = {
            NavigableMap m = new NavigableMap()
            m.merge(fwkProps)
            return m
        }

        when:
        IPropertyLookup lkp =  new ConfigFrameworkPropertyLookupFactory().create()

        then:
        lkp.propertiesMap.keySet().size() == count

        where:
        count | fwkProps
        0     | [:]
        2     | ['fwk.framework.server.name':"test_server",'fwk.framework.server.port':4440]
    }
}
