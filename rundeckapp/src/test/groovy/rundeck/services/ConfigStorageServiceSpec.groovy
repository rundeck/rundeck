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

import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageManager
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.storage.TreeStorageManager
import grails.test.hibernate.HibernateSpec
import grails.test.mixin.TestFor
import grails.testing.services.ServiceUnitTest
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.StorageException
import spock.lang.Specification
import testhelper.RundeckHibernateSpec

import java.util.concurrent.atomic.AtomicInteger

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */

class ConfigStorageServiceSpec extends RundeckHibernateSpec implements ServiceUnitTest<ConfigStorageService> {

    def setup() {
    }

    def cleanup() {
    }

    def "has fix indicator"() {
        given:
            service.rundeckConfigStorageManager = Mock(TreeStorageManager)
        when:
            def result = service.hasFixIndicator('asdf')
        then:
            1 * service.rundeckConfigStorageManager.existsFileResource('sys/fix/asdf') >> respond
            result == respond
        where:
            respond << [true, false]
    }

    def "get fix indicator for path"() {
        expect:
            service.getSystemFixIndicatorPath(input) == expected
        where:
            input  | expected
            'asdf' | 'sys/fix/asdf'
    }
}
