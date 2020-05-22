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

import com.dtolabs.rundeck.core.storage.AuthStorageTree
import grails.testing.services.ServiceUnitTest
import org.rundeck.storage.api.Resource
import spock.lang.Specification
/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
class StorageServiceTests extends Specification implements ServiceUnitTest<StorageService>{
/**
 * utility method to mock a class
 */

    def "testHasResource"() {
        when:
        service.authRundeckStorageTree=Mock(AuthStorageTree){
            1 * hasResource(_, { it.path == 'abc/123' }) >> true
        }
        then:
        service.hasResource(null,'abc/123')
    }
    def "testHasPath"() {
        when:
        service.authRundeckStorageTree=Mock(AuthStorageTree){
            1 * hasPath(_,{ it.path == 'abc/123' })>>true
        }
        then:
        service.hasPath(null,'abc/123')
    }
    def "testGetResource"() {
        when:
        service.authRundeckStorageTree=Mock(AuthStorageTree){
            1 * getPath(_,{it.path=='abc/123'})
        }
        then:
            service.getResource(null, 'abc/123')==null
    }
    def "testListDir"() {
        when:
        service.authRundeckStorageTree=Mock(AuthStorageTree){
            listDirectory(_,{it.path=='abc/123'})>>( [] as Set)

        }
        then:
        service.listDir(null, 'abc/123')==([] as Set)
    }
    def "testDelResource"() {
        when:
        service.authRundeckStorageTree=Mock(AuthStorageTree){
            1 * deleteResource(_, { it.path=='abc/123' })>>true
        }
        then:
        service.delResource(null, 'abc/123')
    }
    def "testCreateResource"() {
        when:
        service.authRundeckStorageTree=Mock(AuthStorageTree){
            createResource(_, { it.path == 'abc/123' }, { it.meta != null && it.getInputStream().text == 'data' }) >>
            Mock(Resource)
        }
        then:
        service.createResource(null, 'abc/123',[:],stream('data'))
    }
    def "testUpdateResource"() {
        when:
        service.authRundeckStorageTree=Mock(AuthStorageTree){
            updateResource(_,{ it.path == 'abc/123' }, { it.meta != null && it.getInputStream().text == 'data' })>>
                Mock(Resource)

        }
        then:
        service.updateResource(null, 'abc/123',[:],stream('data'))
    }

    InputStream stream(String s) {
        return new ByteArrayInputStream(s.bytes);
    }
}
