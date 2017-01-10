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

package rundeck.controllers

import com.dtolabs.rundeck.app.support.StorageParams
import com.dtolabs.rundeck.core.storage.ResourceMeta
import grails.converters.JSON
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.web.GroovyPageUnitTestMixin
import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.Resource
import rundeck.UtilityTagLib
import rundeck.services.ApiService
import rundeck.services.FrameworkService
import rundeck.services.StorageService
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 11/28/16
 */
@TestFor(StorageController)
@TestMixin(GroovyPageUnitTestMixin)
class StorageControllerSpec extends Specification {
    protected void setupFormTokens(def sec) {
        def token = SynchronizerTokensHolder.store(session)
        sec.params[SynchronizerTokensHolder.TOKEN_KEY] = token.generateToken('/test')
        sec.params[SynchronizerTokensHolder.TOKEN_URI] = '/test'
    }

    def "key storage access no params"() {
        given:

        controller.storageService = Mock(StorageService)
        controller.frameworkService = Mock(FrameworkService)
        when:

        def result = controller.keyStorageAccess()

        then:
        response.status == 200
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.storageService.hasPath(_, '/keys') >> true
        1 * controller.storageService.getResource(_, '/keys') >> Mock(Resource) {
            isDirectory() >> true
            getPath() >> Mock(Path)
        }
        1 * controller.storageService.listDir(_, '/keys') >> ([] as Set)
    }

    def "key storage access with params"() {
        given:

        controller.storageService = Mock(StorageService)
        controller.frameworkService = Mock(FrameworkService)
        when:
        params.relativePath = 'donuts/forgood'

        def result = controller.keyStorageAccess()

        then:
        response.status == 200
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.storageService.hasPath(_, '/keys/donuts/forgood') >> true
        1 * controller.storageService.getResource(_, '/keys/donuts/forgood') >> Mock(Resource) {
            isDirectory() >> true
            getPath() >> Mock(Path)
        }
        1 * controller.storageService.listDir(_, '/keys/donuts/forgood') >> ([] as Set)
    }

    def "key storage download with params"() {
        given:

        controller.storageService = Mock(StorageService)
        controller.frameworkService = Mock(FrameworkService)
        when:
        params.relativePath = 'donuts/forgood'

        def result = controller.keyStorageDownload()

        then:
        response.status == 200
        response.text == 'abc'
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.storageService.hasPath(_, '/keys/donuts/forgood') >> true
        1 * controller.storageService.getResource(_, '/keys/donuts/forgood') >> Mock(Resource) {
            isDirectory() >> false
            getPath() >> Mock(Path)
            getContents() >> Mock(ResourceMeta) {
                writeContent(_) >> { args ->
                    args[0].write('abc'.bytes)
                    3L
                }
            }
        }
        0 * controller.storageService._(*_)
        0 * controller.apiService._(*_)
    }

    def "key storage delete with relativePath"() {
        given:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.security.useHMacRequestTokens = 'false'
        def assetTaglib = mockTagLib(UtilityTagLib)
        controller.storageService = Mock(StorageService)
        controller.frameworkService = Mock(FrameworkService)
        controller.apiService = Mock(ApiService)
        when:
        params.relativePath = 'monkey'
        setupFormTokens(controller)
        request.method = 'POST'
        def result = controller.keyStorageDelete()

        then:
        response.status == 204
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.storageService.hasResource(_, '/keys/monkey') >> true
        1 * controller.storageService.delResource(_, '/keys/monkey') >> true
        0 * controller.storageService._(*_)
        0 * controller.apiService._(*_)
    }

    def "key storage upload without relativePath"() {
        given:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.security.useHMacRequestTokens = 'false'
        def assetTaglib = mockTagLib(UtilityTagLib)
        controller.storageService = Mock(StorageService)
        controller.frameworkService = Mock(FrameworkService)
        controller.apiService = Mock(ApiService)
        when:
        params.uploadKeyType = 'public'
        params.inputType = 'text'
        params.fileName = 'monkey'
        params.uploadText = 'abc'
        setupFormTokens(controller)
        request.method = 'POST'
        def result = controller.keyStorageUpload()

        then:
        response.status == 302
        response.redirectedUrl=='/menu/storage?project=&resourcePath=keys%2Fmonkey'
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.storageService.hasResource(_, 'keys/monkey') >> false
        1 * controller.storageService.hasPath(_, 'keys/monkey') >> false
        1 * controller.storageService.createResource(_, 'keys/monkey',_,_) >> true
        0 * controller.storageService._(*_)
        0 * controller.apiService._(*_)
    }
    @Unroll
    def "require sub path param for #method request"() {
        given:
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        when:
        request.method = method
        def result = controller.apiKeys()


        then:
        response.status == 400
        1 * controller.apiService.requireApi(*_) >> true
        1 * controller.apiService.requireVersion(*_) >> true
        1 * controller.apiService.renderErrorFormat(
                _,
                { arg -> arg.status == 400 && arg.code == 'api.error.invalid.request' }
        ) >> { args ->
            args[0].status = 400
        }

        where:
        method   | _
        'POST'   | _
        'PUT'    | _
        'DELETE' | _
    }

    def "don't require sub path param for get"() {
        given:
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.storageService = Mock(StorageService)
        when:
        request.method = 'GET'
        def result = controller.apiKeys()


        then:
        response.status == 200
        1 * controller.apiService.requireApi(*_) >> true
        1 * controller.apiService.requireVersion(*_) >> true
        1 * controller.storageService.hasPath(_, '/keys/') >> true
        1 * controller.storageService.getResource(_, '/keys/') >> Mock(Resource) {
            isDirectory()>>true
            getPath()>>Mock(Path)
        }
        1 * controller.storageService.listDir(_, '/keys/') >> ([] as Set)

    }

    def "validate storage params"() {
        given:
        def req = new StorageParams()

        when:
        req.validate()

        then:
        !req.hasErrors()
    }

    def "validate storage params base keypath"() {
        given:
        def req = new StorageParams(resourcePath: resourcePath)

        when:
        req.validate()
        req.requireRoot('/keys/')


        then:
        req.hasErrors()
        req.errors.hasFieldErrors('resourcePath')

        where:
        resourcePath | _
        '/keys/'     | _
        null         | _
        '/bogus/'    | _
    }

    def "validate storage params valid base keypath"() {
        given:
        def req = new StorageParams(resourcePath: resourcePath)

        when:
        req.validate()
        req.requireRoot('/keys/')


        then:
        !req.hasErrors()
        !req.errors.hasFieldErrors('resourcePath')

        where:
        resourcePath      | _
        '/keys/xyz'       | _
        '/keys/asdf/asdf' | _
    }
}
