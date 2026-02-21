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

package rundeck.services

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import org.rundeck.app.data.providers.GormPluginMetaDataProvider
import rundeck.LogFileStorageRequest
import rundeck.PluginMeta

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
// Grails 7: Use DataTest + ServiceUnitTest instead of RundeckHibernateSpec
class JobMetadataServiceSpec extends Specification implements ServiceUnitTest<JobMetadataService>, DataTest {

    GormPluginMetaDataProvider pluginMetaProvider = new GormPluginMetaDataProvider()
    
    void setupSpec() {
        mockDomains(PluginMeta)
    }

    def setup() {
        service.pluginMetaDataProvider = pluginMetaProvider
    }

    def cleanup() {
    }

    void removeAllPluginMetaForProject() {
        given:
            PluginMeta a = new PluginMeta(key: 'abc', project: project).save(flush: true)
            PluginMeta a2 = new PluginMeta(key: 'bcd', project: project).save(flush: true)
            PluginMeta b = new PluginMeta(key: 'def', project: 'otherproject').save(flush: true)
        when:
            def result = service.removeAllPluginMetaForProject(project)
        then:
            result == 2
            PluginMeta.findAllByProject(project) == []
            PluginMeta.list() == [b]

        where:
            project = 'aproj'
    }
}
