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
import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageUtil
import grails.testing.web.controllers.ControllerUnitTest
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.rundeck.app.authorization.AppAuthContextEvaluator
import org.rundeck.storage.api.*
import rundeck.UtilityTagLib
import rundeck.services.ApiService
import rundeck.services.ConfigurationService
import rundeck.services.FrameworkService
import rundeck.services.StorageService
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import static org.junit.Assert.*
/**
 * @author greg
 * @since 11/28/16
 */
class StorageControllerSpec extends Specification implements ControllerUnitTest<StorageController> {
    protected void setupFormTokens(def sec) {
        def token = SynchronizerTokensHolder.store(session)
        sec.params[SynchronizerTokensHolder.TOKEN_KEY] = token.generateToken('/test')
        sec.params[SynchronizerTokensHolder.TOKEN_URI] = '/test'
    }

    def "key storage access no params"() {
        given:

        controller.storageService = Mock(StorageService)
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
        when:

        def result = controller.keyStorageAccess()

        then:
        response.status == 200
        1 * controller.rundeckAuthContextProvider.getAuthContextForSubject(_)
        1 * controller.storageService.hasPath(_, '/keys') >> true
        1 * controller.storageService.getResource(_, '/keys') >> Mock(Resource) {
            isDirectory() >> true
            getPath() >> Mock(Path)
        }
        1 * controller.storageService.listDir(_, '/keys') >> ([] as Set)
    }

    def "key storage access no params xml"() {
        given:

        controller.storageService = Mock(StorageService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
        response.format='xml'
        when:

        def result = controller.keyStorageAccess()

        then:
        response.status == 200
        1 * controller.rundeckAuthContextProvider.getAuthContextForSubject(_)
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
            controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
        when:
        params.relativePath = 'donuts/forgood'

        def result = controller.keyStorageAccess()

        then:
        response.status == 200
        1 * controller.rundeckAuthContextProvider.getAuthContextForSubject(_)
        1 * controller.storageService.hasPath(_, '/keys/donuts/forgood') >> true
        1 * controller.storageService.getResource(_, '/keys/donuts/forgood') >> Mock(Resource) {
            isDirectory() >> true
            getPath() >> Mock(Path)
        }
        1 * controller.storageService.listDir(_, '/keys/donuts/forgood') >> ([] as Set)
    }

    def "key storage access with exception"(){
        given:
        controller.apiService = Mock(ApiService)
        controller.storageService = Mock(StorageService)
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
        controller.storageService = Mock(StorageService){
            it.hasPath(_, '/keys') >> true
            it.hasPath(_, '/keys/donuts/forgood') >> true
            it.getResource(_,'/keys/donuts/forgood') >> { throw new Exception(expectedException) }
        }

        response.format='JSON'

        when:
        params.relativePath = 'donuts/forgood'
        def result = controller.keyStorageAccess()

        then:
        1 * controller.rundeckAuthContextProvider.getAuthContextForSubject(_)
        1 * controller.apiService.renderErrorFormat(
                _,
                { arg -> arg.status == 500 && arg.message == expectedGuiOutput }
        ) >> { args ->
            args[0].status = 500
        }

        where:
        expectedException | expectedGuiOutput
        "I"               | "Error: ${expectedException}"
        "Believe"         | "Error: ${expectedException}"
        "Them Bones"      | "Error: ${expectedException}"
        "Are Me"          | "Error: ${expectedException}"
    }

    def "key storage download with params"() {
        given:

        controller.configurationService = Mock(ConfigurationService){
            getBoolean("gui.keystorage.downloadenabled", true)>>true
        }
        controller.storageService = Mock(StorageService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
        when:
        params.relativePath = 'donuts/forgood'

        def result = controller.keyStorageDownload()

        then:
        response.status == 200
        response.text == 'abc'
        1 * controller.rundeckAuthContextProvider.getAuthContextForSubject(_)
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
        1 * controller.storageService._(*_)
        0 * controller.apiService._(*_)
    }

    @Unroll
    @Ignore('TODO: download request of dir path should respond 400 ')
    def "key storage download with params directory format #format"() {
        given:

        controller.configurationService = Mock(ConfigurationService){
            getBoolean("gui.keystorage.downloadenabled", true)>>true
        }
        controller.storageService = Mock(StorageService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
        response.format=format
        when:
        params.relativePath = 'donuts/'

        def result = controller.keyStorageDownload()

        then:
        response.status == 400
        1 * controller.rundeckAuthContextProvider.getAuthContextForSubject(_)
        1 * controller.storageService.hasPath(_, '/keys/donuts/') >> true
        1 * controller.storageService.listDir(_, '/keys/donuts/') >> ([] as Set)
        1 * controller.storageService.getResource(_, '/keys/donuts/') >> Mock(Resource) {
            isDirectory() >> true
        }
        0 * controller.storageService._(*_)
        0 * controller.apiService._(*_)
        where:
            format<<['xml','json']
    }

    @Unroll
    def "key storage download disabled"() {
        given:

        controller.storageService = Mock(StorageService)
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
        controller.configurationService = Mock(ConfigurationService)
        response.format="json"
        when:
        params.relativePath = 'donuts/'

        def result = controller.keyStorageDownload()

        then:
        1 * controller.configurationService.getBoolean("gui.keystorage.downloadenabled", true)>>false
        response.status == 403

    }

    def "key storage delete with relativePath"() {
        given:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.security.useHMacRequestTokens = 'false'

        defineBeans {
            configurationService(ConfigurationService) {
                grailsApplication = grailsApplication
            }
        }

        def assetTaglib = mockTagLib(UtilityTagLib)
        controller.storageService = Mock(StorageService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
        controller.apiService = Mock(ApiService)
        when:
        params.relativePath = 'monkey'
        setupFormTokens(controller)
        request.method = 'POST'
        def result = controller.keyStorageDelete()

        then:
        response.status == 204
        1 * controller.rundeckAuthContextProvider.getAuthContextForSubject(_)
        1 * controller.storageService.hasResource(_, '/keys/monkey') >> true
        1 * controller.storageService.delResource(_, '/keys/monkey') >> true
        1 * controller.storageService._(*_)
        0 * controller.apiService._(*_)
    }

    def "add new key storage without relativePath"() {
        given:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.security.useHMacRequestTokens = 'false'
        controller.storageService = Mock(StorageService)
        controller.frameworkService = Mock(FrameworkService)
            controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
        controller.apiService = Mock(ApiService)
        when:
        params.uploadKeyType = uploadKeyType
        params.inputType = inputType
        params.fileName = 'monkey'
        params.uploadText = 'abc'
        params.project = project
        setupFormTokens(controller)
        request.method = 'POST'
        def result = controller.keyStorageUpload()

        then:
        response.status == 302
        response.redirectedUrl==expect
        1 * controller.rundeckAuthContextProvider.getAuthContextForSubject(_)
        1 * controller.storageService.hasResource(_, 'keys/monkey') >> false
        1 * controller.storageService.hasPath(_, 'keys/monkey') >> false
        1 * controller.storageService.createResource(_, 'keys/monkey',_,_)
        1 * controller.storageService._(*_)
        0 * controller.apiService._(*_)
        where:
            project | uploadKeyType | inputType     | expect
            null    | 'public'      | 'text'        | '/menu/storage/keys/monkey'
            null    | 'public'      | 'file'        | '/menu/storage/keys/monkey'
            'disco' | 'public'      | 'text'        | '/menu/storage/keys/monkey?project=disco'
            'disco' | 'public'      | 'file'        |'/menu/storage/keys/monkey?project=disco'
            null    | 'private'     | 'text'        | '/menu/storage/keys/monkey'
            null    | 'private'     | 'file'        | '/menu/storage/keys/monkey'
            'disco' | 'private'     | 'text'        | '/menu/storage/keys/monkey?project=disco'
            'disco' | 'private'     | 'file'        |'/menu/storage/keys/monkey?project=disco'
    }

    def "override existing key storage without relativePath"() {
        given:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.security.useHMacRequestTokens = 'false'
        controller.storageService = Mock(StorageService)
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
        controller.apiService = Mock(ApiService)
        when:
        params.dontOverwrite = false
        params.uploadKeyType = uploadKeyType
        params.inputType = inputType
        params.fileName = 'monkey'
        params.uploadText = 'abc'
        params.project = project
        setupFormTokens(controller)
        request.method = 'POST'
        def result = controller.keyStorageUpload()

        then:
        response.status == 302
        response.redirectedUrl==expect
        1 * controller.rundeckAuthContextProvider.getAuthContextForSubject(_)
        1 * controller.storageService.hasResource(_, 'keys/monkey') >> true
        1 * controller.storageService.updateResource(_, 'keys/monkey',_,_)
        1 * controller.storageService._(*_)
        0 * controller.apiService._(*_)
        where:
        project | uploadKeyType | inputType     | expect
        null    | 'public'      | 'text'        | '/menu/storage/keys/monkey'
        null    | 'public'      | 'file'        | '/menu/storage/keys/monkey'
        'disco' | 'public'      | 'text'        | '/menu/storage/keys/monkey?project=disco'
        'disco' | 'public'      | 'file'        |'/menu/storage/keys/monkey?project=disco'
        null    | 'private'     | 'text'        | '/menu/storage/keys/monkey'
        null    | 'private'     | 'file'        | '/menu/storage/keys/monkey'
        'disco' | 'private'     | 'text'        | '/menu/storage/keys/monkey?project=disco'
        'disco' | 'private'     | 'file'        |'/menu/storage/keys/monkey?project=disco'
    }

    @Unroll
    def "require sub path param for #method request"() {
        given:
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        controller.storageService = Mock(StorageService)
        controller.rundeckAuthContextProvider=Mock(AuthContextProvider)

        when:
        request.method = method
        def result = controller.apiKeys()


        then:
        response.status == 400
        1 * controller.apiService.requireApi(*_) >> true
        1 * controller.apiService.requireApi(*_) >> true
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
        controller.rundeckAuthContextProvider=Mock(AuthContextProvider)
        controller.storageService = Mock(StorageService)
        when:
        request.method = 'GET'
        def result = controller.apiKeys()


        then:
        response.status == 200
        1 * controller.apiService.requireApi(*_) >> true
        1 * controller.apiService.requireApi(*_) >> true
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


    def apiGetResource_notfound() {
        given:
            params.resourcePath = 'abc'

            controller.rundeckAuthContextProvider=Mock(AuthContextProvider) {
                1 * getAuthContextForSubject(_)
            }

            controller.storageService = Mock(StorageService) {
                1 *hasPath(_, _) >> false
            }
            controller.apiService = Mock(ApiService) {
                1 *requireApi(_, _) >> true
            }

        when:
            def result = controller.apiGetResource()
        then:
            assertEquals(404, response.status)
    }

    def apiGetResource_foundContent() {
        given:
            params.resourcePath = 'abc'


            controller.rundeckAuthContextProvider=Mock(AuthContextProvider) {
                1 * getAuthContextForSubject(_)
            }
            def mContent = Mock(ContentMeta) {
                2 * getMeta() >> ['Rundeck-content-type': 'test/data']

                1 * writeContent(_) >> {
                    def bytes = "data1".bytes
                    it[0].write(bytes)
                    return (long) bytes.length
                }
            }
            def mRes = Mock(Resource) {
                1 * isDirectory() >> false
                2 * getContents() >> mContent
            }
            controller.storageService = Mock(StorageService) {
                1 * hasPath(_, _) >> true
                1 * getResource(_, _) >> mRes
            }
            controller.apiService = Mock(ApiService) {
                1 * requireApi(_, _) >> true
            }

        when:
            def result = controller.apiGetResource()
        then:
            assertEquals(200, response.status)
            assertEquals('test/data', response.contentType)
            assertEquals('data1', response.text)
    }

    class TestRes implements Resource<ResourceMeta> {
        ResourceMeta contents
        boolean directory
        Path path
    }

    @Unroll
    def "apiGetResource foundDirectory format #format"() {
        given:
            params.resourcePath = 'abc'


            controller.rundeckAuthContextProvider=Mock(AuthContextProvider) {
                1 * getAuthContextForSubject(_)
            }

            def mRes1 = new TestRes(
                contents: StorageUtil.withStream(
                    new ByteArrayInputStream("data1".bytes),
                    ['Rundeck-content-type': 'test/data']
                ), directory: false, path:
                    PathUtil.asPath("abc/test1")
            )
            def mRes2 = new TestRes(
                contents: StorageUtil.withStream(
                    new ByteArrayInputStream("data2".bytes),
                    ['Rundeck-content-type': 'test/data']
                ), directory: false, path: PathUtil.asPath("abc/test2")
            )
            def mRes = new TestRes(directory: true, path: PathUtil.asPath("abc"))

            controller.storageService = Mock(StorageService) {
                1 * hasPath(_,_)>>true
                1 * getResource(_,_)>>mRes
                1 * listDir(_, _) >> ([mRes1, mRes2] as Set)

            }
            controller.apiService = Mock(ApiService) {
                1 * requireApi(_,_) >> true
            }

            response.format = format
        request.addHeader('Accept','application/xml')

        when:
            def result = controller.apiGetResource()
        then:
            assertEquals(200, response.status)
            assertEquals(ctype, response.contentType)
            if(format=='json'){
                assertEquals('abc', response.json.path)
                assertEquals('directory', response.json.type)
                assertNotNull(response.json.url)
                assertEquals(2, response.json.resources.size())
                assertEquals('abc/test1', response.json.resources[0].path)
                assertEquals('file', response.json.resources[0].type)
                assertEquals('test1', response.json.resources[0].name)
            }else{

                assertEquals('abc', response.xml.@path.text())
                assertEquals('directory', response.xml.@type.text())
                assertNotNull(response.xml.@url.text())
                assertNotNull(response.xml.contents)
                assertEquals('2', response.xml.contents.@count.text())
                assertEquals('abc/test1', response.xml.contents.resource[0].@path.text())
                assertEquals('file', response.xml.contents.resource[0].@type.text())
                assertEquals('test1', response.xml.contents.resource[0].@name.text())
            }


        where:
            format | ctype
            'json' | 'application/json;charset=UTF-8'
            'xml' | 'application/xml;charset=utf-8'

    }

    def apiPostResource_conflictFile() {
        given:

            controller.rundeckAuthContextProvider=Mock(AuthContextProvider) {
                1 * getAuthContextForSubject(_)
            }
            controller.storageService = Mock(StorageService) {
                1 * hasResource(_,_) >> true
            }
            controller.apiService = Mock(ApiService) {
                1 * requireApi(_,_) >> true
            }
            params.resourcePath = '/keys/abc'
        when:
            def result = controller.apiPostResource()
        then:
            assertEquals(409, response.status)
    }

    def apiPostResource_conflictDir() {
        given:

            controller.rundeckAuthContextProvider=Mock(AuthContextProvider) {
                1 * getAuthContextForSubject(_)
            }
            controller.storageService = Mock(StorageService) {
                1 * hasResource(_,_) >> false
                1 * hasPath(_,_)>>true
            }
            controller.apiService = Mock(ApiService) {
                1 * requireApi(_,_) >> true
            }
            params.resourcePath = '/keys/abc'
        when:
            def result = controller.apiPostResource()
        then:
            assertEquals(409, response.status)
    }

    def apiPostResource_ok() {
        given:

            controller.rundeckAuthContextProvider=Mock(AuthContextProvider){
                1 * getAuthContextForSubject(_)
            }
            def mRes2 = new TestRes(
                contents: StorageUtil.withStream(
                    new ByteArrayInputStream("data2".bytes),
                    ['Rundeck-content-type': 'test/data']
                ), directory: false, path: PathUtil.asPath("abc/test2")
            )
            controller.storageService = Mock(StorageService) {
                1 * hasResource(_,_) >> false
                1 * hasPath(_,_) >> false
                1 * createResource (_,_,_,_) >> mRes2
            }
            controller.apiService = Mock(ApiService) {
                1 * requireApi(_,_) >> true
            }
            params.resourcePath = '/keys/abc'
            response.format = 'json'
        when:
            def result = controller.apiPostResource()
        then:
            assertEquals(201, response.status)
            assertEquals('application/json;charset=UTF-8', response.contentType)
            assertEquals('file', response.json.type)
            assertEquals('abc/test2', response.json.path)
            assertEquals('test2', response.json.name)
            assertEquals('test/data', response.json.meta['Rundeck-content-type'])
    }

    def apiPostResource_exception() {
        given:

            controller.rundeckAuthContextProvider=Mock(AuthContextProvider){
                1 *  getAuthContextForSubject(_)
            }
            def mRes2 = new TestRes(
                contents: StorageUtil.withStream(
                    new ByteArrayInputStream("data2".bytes),
                    ['Rundeck-content-type': 'test/data']
                ), directory: false, path: PathUtil.asPath("abc/test2")
            )
            controller.storageService = Mock(StorageService) {
                1 * hasResource(_,_) >> false
                1 * hasPath(_,_) >> false
                1 * createResource (_,_,_,_) >> {
                    throw StorageException.createException(PathUtil.asPath("abc/test2"), "failed")
                }
            }
            params.resourcePath = '/keys/abc'
            response.format = 'json'
        when:
            def result = controller.apiPostResource()
        then:
            controller.apiService = Mock(ApiService) {
                1 * requireApi(_,_) >> true
                1 * renderErrorFormat (_,_)>>{
                    assertEquals(500, it[1].status)
                    assertEquals("failed", it[1].message)
                }
            }
    }

    def apiPutResource_notfound() {
        given:

            controller.rundeckAuthContextProvider=Mock(AuthContextProvider){
                1 *   getAuthContextForSubject(_)
            }
            controller.storageService = Mock(StorageService) {
                1 * hasResource(_,_) >> false
            }
            controller.apiService = Mock(ApiService) {
                1 *  requireApi(_,_) >> true
            }
            params.resourcePath = '/keys/abc'
        when:
            def result = controller.apiPutResource()
        then:
            assertEquals(404, response.status)
    }

    def apiPutResource_ok() {
        given:

            controller.rundeckAuthContextProvider=Mock(AuthContextProvider){
                1 *   getAuthContextForSubject(_)
            }
            def mRes2 = new TestRes(
                contents: StorageUtil.withStream(
                    new ByteArrayInputStream("data2".bytes),
                    ['Rundeck-content-type': 'test/data']
                ), directory: false, path: PathUtil.asPath("abc/test2")
            )
            controller.storageService = Mock(StorageService) {
               1 *  hasResource(_,_) >> true
               1 *  updateResource(_,_,_,_)>> mRes2

            }
            controller.apiService = Mock(ApiService) {
                1 * requireApi(_,_) >> true
            }
            params.resourcePath = '/keys/abc'
            response.format = 'json'
        when:
            def result = controller.apiPutResource()
        then:
            assertEquals(200, response.status)
            assertEquals('application/json;charset=UTF-8', response.contentType)
            assertEquals('file', response.json.type)
            assertEquals('abc/test2', response.json.path)
            assertEquals('test2', response.json.name)
            assertEquals('test/data', response.json.meta['Rundeck-content-type'])
    }

    def apiPutResource_exception() {
        given:

            controller.rundeckAuthContextProvider=Mock(AuthContextProvider){
                1 * getAuthContextForSubject(_)
            }
            def mRes2 = new TestRes(
                contents: StorageUtil.withStream(
                    new ByteArrayInputStream("data2".bytes),
                    ['Rundeck-content-type': 'test/data']
                ), directory: false, path: PathUtil.asPath("abc/test2")
            )
            controller.storageService = Mock(StorageService) {
                1 * hasResource(_,_) >> true
                1 * updateResource(_,_,_,_)>>{
                    throw StorageException.createException(PathUtil.asPath("abc/test2"), "failed")
                }
            }
            params.resourcePath = '/keys/abc'
            response.format = 'json'
        when:
            def result = controller.apiPutResource()
        then:
            controller.apiService = Mock(ApiService) {
                1 * requireApi(_,_) >> true
                1 * renderErrorFormat(_,_)>>{
                    assertEquals(500, it[1].status)
                    assertEquals("failed", it[1].message)
                }
            }
    }

    def apiDeleteResource_notfound() {
        given:

            controller.rundeckAuthContextProvider=Mock(AuthContextProvider){
                1 *  getAuthContextForSubject(_)
            }
            controller.storageService = Mock(StorageService) {
                1 *  hasResource(_,_) >> false
            }
            params.resourcePath = '/keys/abc'
            response.format = 'json'
        when:
            def result = controller.apiDeleteResource()
        then:
            controller.apiService = Mock(ApiService) {
                1 * requireApi(_,_) >> true

                1 *  renderErrorFormat(_,_)>>{
                    assertEquals(404, it[1].status)
                    assertEquals("api.error.item.doesnotexist", it[1].code)
                }
            }
    }

    def apiDeleteResource_success() {
        given:

            controller.rundeckAuthContextProvider=Mock(AuthContextProvider){
                1 * getAuthContextForSubject(_)
            }
            controller.storageService = Mock(StorageService) {
               1 *  hasResource(_,_) >> true
               1 *  delResource(_,_)>>{ true }
            }
            controller.apiService = Mock(ApiService) {
                1 * requireApi(_,_) >> true
            }
            params.resourcePath = '/keys/abc'
        when:
            def result = controller.apiDeleteResource()
        then:
            assertEquals(204, response.status)
    }

    def apiDeleteResource_failure() {
        given:

            controller.rundeckAuthContextProvider=Mock(AuthContextProvider){
                1 *  getAuthContextForSubject(_)
            }
            controller.storageService = Mock(StorageService) {
               1 *  hasResource(_,_) >> true
               1 *  delResource(_,_)>>{ false }
            }
            params.resourcePath = '/keys/abc/test1'
        when:
            def result = controller.apiDeleteResource()
        then:
            controller.apiService = Mock(ApiService) {
                1 * requireApi(_,_) >> true


                1 *  renderErrorFormat(_,_)>>{
                    assertEquals(500, it[1].status)
                    assertEquals("Resource was not deleted: /keys/abc/test1", it[1].message?.toString())
                }
            }
    }

    def apiDeleteResource_exception() {
        given:

            controller.rundeckAuthContextProvider=Mock(AuthContextProvider){
                1 *  getAuthContextForSubject(_)
            }
            controller.storageService = Mock(StorageService) {
                1 * hasResource(_,_) >> true
                1 * delResource(_,_)>>{
                    throw StorageException.deleteException(PathUtil.asPath("abc/test2"), "failed")
                }
            }
            params.resourcePath = '/keys/abc/test1'
        when:
            def result = controller.apiDeleteResource()
        then:
            controller.apiService = Mock(ApiService) {
                1 *  requireApi(_,_) >> true

                1 *  renderErrorFormat(_,_)>>{
                    assertEquals(500, it[1].status)
                    assertEquals("failed", it[1].message)
                }
            }
    }

    def storageParamsValidationBasic() {
        given:
            def params = new StorageParams()
            params.resourcePath = 'keys/monkey/bonanza'
        expect:
            assertTrue(params.validate())
    }

    def storageParamsValidation_allowdotdot() {
        given:
            def params = new StorageParams()
            params.resourcePath = 'keys/monkey/bonanza..double'
        expect:
            assertTrue(params.validate())
    }

    def storageParamsValidation_allowdotdot2() {
        given:
            def params = new StorageParams()
            params.resourcePath = 'keys/monkey/bonanza/..double'
        expect:
            assertTrue(params.validate())
    }

    def storageParamsValidation_allowdotdot3() {
        given:
            def params = new StorageParams()
            params.resourcePath = 'keys/monkey/bonanza../double'
        expect:
            assertTrue(params.validate())
    }

    def storageParamsValidation_invaliddotdot() {
        given:
            def params = new StorageParams()
            params.resourcePath = 'keys/monkey/../bonanza'
        expect:
            assertFalse("should not allow /../", params.validate())
    }

    def storageParamsValidation_invalidspace() {
        given:
            def params = new StorageParams()
            params.resourcePath = 'keys/monkey/ bonanza'
        expect:
            assertFalse(params.validate())
    }
}
