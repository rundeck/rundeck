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

import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.grails.plugins.testing.GrailsMockMultipartFile
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import rundeck.Project
import rundeck.services.ApiService
import rundeck.services.ArchiveOptions
import rundeck.services.AuthorizationService
import rundeck.services.FrameworkService
import rundeck.services.ImportResponse
import rundeck.services.ProgressSummary
import rundeck.services.ProjectService
import rundeck.services.authorization.PoliciesValidation
import spock.lang.Specification
import spock.lang.Unroll

import javax.security.auth.Subject

import static org.rundeck.core.auth.AuthConstants.ACTION_ADMIN
import static org.rundeck.core.auth.AuthConstants.ACTION_CREATE
import static org.rundeck.core.auth.AuthConstants.ACTION_DELETE
import static org.rundeck.core.auth.AuthConstants.ACTION_IMPORT
import static org.rundeck.core.auth.AuthConstants.ACTION_READ
import static org.rundeck.core.auth.AuthConstants.ACTION_UPDATE

/**
 * Created by greg on 2/26/15.
 */
@TestFor(ProjectController)
@Mock([Project])
class ProjectControllerSpec extends Specification{
    def setup(){

    }
    def cleanup(){

    }

    def "api project config PUT "() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.frameworkService = Mock(FrameworkService)
        controller.apiService = Mock(ApiService)
        params.project = 'aproject'
        request.contentType = 'text/plain'
        request.content = 'a=b\nz=d\n'.bytes
        request.method = 'PUT'
        when:
        controller.apiProjectConfigPut()
        then:
        response.status == 200
        response.contentType == 'text/plain'
        response.text.split(/[\n\r]/).contains 'x=y'
        1 * controller.apiService.requireVersion(_, _, 11) >> true
        1 * controller.apiService.extractResponseFormat(*_) >> 'text'
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.frameworkService.existsFrameworkProject('aproject') >> true
        1 * controller.frameworkService.authResourceForProject('aproject')
        1 * controller.frameworkService.authorizeApplicationResourceAny(_, _, ['configure', 'admin']) >> true
        1 * controller.frameworkService.getFrameworkProject(_) >> Mock(IRundeckProject) {
            getProjectProperties() >> [
                x: 'y'
            ]
        }
        1 * controller.frameworkService.setFrameworkProjectConfig(_, [a: 'b', z: 'd']) >> [success: true]
        0 * controller.frameworkService._(*_)
        0 * controller.apiService._(*_)
        0 * controller.projectService._(*_)

    }
    @Unroll
    def "api project create description #inputDesc"(){
        given:
        controller.projectService=Mock(ProjectService)
        controller.apiService=Mock(ApiService)
        controller.frameworkService=Mock(FrameworkService)
        params.project='aproject'

        request.method='POST'
        request.format='json'
        request.json=[name:'aproject',description:inputDesc]
        when:

        def result=controller.apiProjectCreate()

        then:
        1 * controller.apiService.requireVersion(_, _, 11) >> true
        1 * controller.apiService.extractResponseFormat(*_) >> 'json'
        1 * controller.apiService.parseJsonXmlWith(*_) >> { args ->
            args[2].json.call(args[0].JSON)
            true
        }
        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.frameworkService.authorizeApplicationResourceTypeAll(*_)>>true
        1 * controller.frameworkService.existsFrameworkProject('aproject')>>false
        1 * controller.frameworkService.createFrameworkProject('aproject',{
            it['project.description']==inputDesc
        })>>[Mock(IRundeckProject){
            getName()>>'aproject'
        },[]]
        1 * controller.frameworkService.loadProjectProperties(*_)>>([:] as Properties)
        0 * controller.frameworkService._(*_)

        where:
        inputDesc       | _
        'a description' | _
        null            | _
    }
    @Unroll
    def "api project create validate input json #inputJson"(){
        given:
        controller.projectService=Mock(ProjectService)
        controller.apiService=Mock(ApiService)
        controller.frameworkService=Mock(FrameworkService)
        params.project='aproject'

        request.method='POST'
        request.format='json'
        request.json=inputJson
        when:

        def result=controller.apiProjectCreate()

        then:
        1 * controller.apiService.requireVersion(_, _, 11) >> true
        1 * controller.apiService.extractResponseFormat(*_) >> 'json'
        1 * controller.apiService.parseJsonXmlWith(*_) >> { args ->
            args[2].json.call(args[0].JSON)
            true
        }
        1 * controller.apiService.renderErrorFormat(_, [status: 400, code:'api.error.invalid.request',args: [errMsg], format: 'json'])

        1 * controller.frameworkService.getAuthContextForSubject(_)
        1 * controller.frameworkService.authorizeApplicationResourceTypeAll(*_)>>true
        0 * controller.frameworkService._(*_)

        where:
        inputJson                                              | errMsg
        [name: 'aproject', description: 'xyz', config: 'blah'] | 'json: expected \'config\' to be a Map'
        [name: 'aproject', description: 12]                    | 'json: expected \'description\' to be a String'
        [name: [a: 'b'], description: null]                    | 'json: expected \'name\' to be a String'
        [description: 'monkey']                                | 'json: required \'name\' but it was not found'
    }

    def "api export execution ids string"(){
        given:
        controller.projectService=Mock(ProjectService)
        controller.apiService=Mock(ApiService)
        controller.frameworkService=Mock(FrameworkService)
        params.project='aproject'
        params.executionIds=eidparam

        when:
        def result=controller.apiProjectExport()

        then:
        1 * controller.apiService.requireVersion(_,_,_) >> true
        1 * controller.frameworkService.existsFrameworkProject('aproject') >> true
        1 * controller.frameworkService.authorizeApplicationResourceAny(_,_,['export','admin']) >> true
        1 * controller.frameworkService.getFrameworkProject(_) >> Mock(IRundeckProject)
        1 * controller.projectService.exportProjectToOutputStream(_,_,_,_,_,{ ArchiveOptions opts ->
            opts.executionsOnly==true && opts.executionIds==(expectedset)
        },_)

        where:
        eidparam       | expectedset
        '123'          | ['123'] as Set
        '123,456'      | ['123', '456'] as Set
        ['123', '456'] | ['123', '456'] as Set
    }

    def "api export params"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        params.project = 'aproject'
        params.exportAll = all
        params.exportJobs = jobs
        params.exportExecutions = execs
        params.exportConfigs = configs
        params.exportReadmes = readmes
        params.exportAcls = acls

        when:
        def result = controller.apiProjectExport()

        then:
        1 * controller.apiService.requireVersion(_, _, _) >> true
        1 * controller.frameworkService.existsFrameworkProject('aproject') >> true
        1 * controller.frameworkService.authorizeApplicationResourceAny(_, _, ['export', 'admin']) >> true
        1 * controller.frameworkService.getFrameworkProject(_) >> Mock(IRundeckProject)
        1 * controller.projectService.exportProjectToOutputStream(_, _, _, _, _, { ArchiveOptions opts ->
            opts.executionsOnly == false &&
                    opts.all == all &&
                    opts.jobs == jobs &&
                    opts.executions == execs &&
                    opts.configs == configs &&
                    opts.readmes == readmes &&
                    opts.acls == acls
        },_
        )

        where:
        all  | jobs  | execs | configs | readmes | acls
        true | false | false | false   | false   | false
    }

    def "api project delete error"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        params.project = 'aproject'

        when:
        request.method = 'DELETE'
        def result = controller.apiProjectDelete()

        then:
        1 * controller.apiService.requireVersion(_, _, _) >> true
        1 * controller.frameworkService.existsFrameworkProject('aproject') >> true
        1 * controller.frameworkService.authorizeApplicationResourceAny(_, _, ['delete', 'admin']) >> true
        1 * controller.frameworkService.getFrameworkProject(_) >> Mock(IRundeckProject)
        1 * controller.projectService.deleteProject(_, _, _, _) >> [success: false, error: 'message']
        1 * controller.apiService.renderErrorFormat(_, [
                status : 500,
                code   : 'api.error.unknown',
                message: 'message'
        ]
        )


    }

    def "export prepare"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        params.project = 'aproject'
        params.exportAll = all
        params.exportJobs = jobs
        params.exportExecutions = execs
        params.exportConfigs = configs
        params.exportReadmes = readmes
        params.exportAcls = acls

        when:
        def result = controller.exportPrepare()

        then:
        1 * controller.frameworkService.existsFrameworkProject('aproject') >> true
        1 * controller.frameworkService.authorizeApplicationResourceAny(_, _, ['admin', 'export']) >> true
        1 * controller.frameworkService.getFrameworkProject(_) >> Mock(IRundeckProject)
        1 * controller.projectService.exportProjectToFileAsync(_, _, _, _, { ArchiveOptions opts ->
            opts.executionsOnly == false &&
                    opts.all == (all ?: false) &&
                    opts.jobs == (jobs ?: false) &&
                    opts.executions == (execs ?: false) &&
                    opts.configs == (configs ?: false) &&
                    opts.readmes == (readmes ?: false) &&
                    opts.acls == (acls ?: false)
        }, _
        ) >> 'dummytoken'
        1 * controller.projectService.validateAllProjectComponentExportOptions(_) >> [:]
        response.redirectedUrl ==  '/project/aproject/exportWait/dummytoken'

        where:
        all  | jobs  | execs | configs | readmes | acls
        true | false | false | false   | false   | false
        true | false | false | false   | false   | null
    }

    def "export wait response format json"() {
        given:
        controller.projectService = Mock(ProjectService)
        when:
        params.token = 'abc'
        response.format = 'json'
        controller.exportWait()
        then:
        1 * controller.projectService.hasPromise(_, 'abc') >> true
        1 * controller.projectService.promiseError(_, 'abc') >> null
        1 * controller.projectService.promiseReady(_, 'abc')
        1 * controller.projectService.promiseSummary(_, 'abc') >> Mock(ProgressSummary) {
            percent() >> 50
        }

        response.status == 200
        response.json == [token: 'abc', ready: false, percentage: 50]
    }

    def "export wait no token param format json"() {
        given:
        controller.projectService = Mock(ProjectService)
        when:
        params.token = ptoken
        response.format = 'json'
        controller.exportWait()
        then:


        response.status == 200
        response.json == [token: ptoken, errorMessage: 'token is required']

        where:
        ptoken | _
        null   | _

    }

    def "export wait missing token format json"() {
        given:
        controller.projectService = Mock(ProjectService)
        when:
        params.token = ptoken
        response.format = 'json'
        controller.exportWait()
        then:

        controller.projectService.hasPromise(_, ptoken) >> false

        response.status == 200
        response.json == [token: ptoken, notFound: true]

        where:
        ptoken | _
        'xyz'  | _

    }

    def "export wait error message format json"() {
        given:
        controller.projectService = Mock(ProjectService)
        when:
        params.token = ptoken
        response.format = 'json'
        controller.exportWait()
        then:

        controller.projectService.hasPromise(_, ptoken) >> true
        controller.projectService.promiseError(_, ptoken) >> new Exception("expected exception")

        response.status == 200
        response.json == [token: ptoken, errorMessage: 'Project export request failed: expected exception']

        where:
        ptoken | _
        'xyz'  | _

    }

    def "export wait remote error message format json"() {
        given:
        controller.projectService = Mock(ProjectService)
        when:
        params.token = ptoken
        params.instance = 'true'
        response.format = 'json'
        controller.exportWait()
        then:

        controller.projectService.hasPromise(_, ptoken) >> true
        controller.projectService.promiseError(_, ptoken) >> null
        controller.projectService.promiseResult(_, ptoken) >> new ImportResponse(
            ok: false,
            errors: ['a', 'b'],
            executionErrors: ['c', 'd'],
            aclErrors: ['e', 'f']
        )

        response.status == 200
        response.json == [token: ptoken, errors: ['a', 'b', 'c', 'd', 'e', 'f']]

        where:
        ptoken | _
        'xyz'  | _

    }

    def "api export execution ids async"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        params.project = 'aproject'
        params.executionIds = eidparam
        params.async = true

        when:
        def result = controller.apiProjectExport()

        then:
        1 * controller.apiService.requireVersion(_, _, 11) >> true
        1 * controller.apiService.requireVersion(_, _, 19) >> true
        1 * controller.frameworkService.existsFrameworkProject('aproject') >> true
        1 * controller.frameworkService.authorizeApplicationResourceAny(_, _, ['export', 'admin']) >> true
        1 * controller.frameworkService.getFrameworkProject(_) >> Mock(IRundeckProject)
        1 * controller.projectService.exportProjectToFileAsync(_, _, _, _, { ArchiveOptions opts ->
            opts.executionsOnly == true && opts.executionIds == (expectedset)
        },_
        ) >> 'atoken'
        1 * controller.projectService.promiseReady(_, 'atoken') >> null
        1 * controller.projectService.promiseSummary(_, 'atoken') >> Mock(ProgressSummary)

        where:
        eidparam | expectedset
        '123' | ['123'] as Set
        '123,456' | ['123','456'] as Set
        ['123','456'] | ['123','456'] as Set
    }

    def "api export async status"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        params.project = 'aproject'
        params.token = 'atoken'
        params.async = true

        when:
        def result = controller.apiProjectExportAsyncStatus()

        then:
        1 * controller.apiService.requireVersion(_, _, 19) >> true
        1 * controller.apiService.requireParameters(_, _, ['token']) >> true
        1 * controller.apiService.requireExists(_, true, ['Export Request Token', 'atoken']) >> true
        1 * controller.projectService.hasPromise(_, 'atoken') >> true
        1 * controller.projectService.promiseError(_, 'atoken') >> null
        1 * controller.projectService.promiseReady(_, 'atoken') >> null
        1 * controller.projectService.promiseSummary(_, 'atoken') >> Mock(ProgressSummary)

    }

    def "api export async download"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        params.project = 'aproject'
        params.token = 'atoken'
        params.async = true
        def afile = File.createTempFile("project-async-export-test", "data")
        afile.text << 'test'


        when:
        def result = controller.apiProjectExportAsyncDownload()

        then:
        1 * controller.apiService.requireVersion(_, _, 19) >> true
        1 * controller.apiService.requireParameters(_, _, ['token']) >> true
        1 * controller.apiService.requireExists(_, true, ['Export Request Token', 'atoken']) >> true
        1 * controller.projectService.hasPromise(_, 'atoken') >> true
        1 * controller.projectService.promiseReady(_, 'atoken') >> afile
        1 * controller.apiService.requireExists(_, afile, ['Export File for Token', 'atoken']) >> true
        1 * controller.projectService.promiseRequestStarted(_, 'atoken') >> new Date()
        1 * controller.projectService.releasePromise(_, 'atoken')
        response.getHeader('content-disposition') != null
        response.getContentType() == 'application/zip'

    }
    def "project file readme get not project param"(){
        given:
        params.filename="readme.md"
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,13) >> true
            1 * requireVersion(_,_,11) >> true
            1 * renderErrorFormat(_,{it.code=='api.error.parameter.required' && it.args.contains('project')})
        }
        when:
        def result=controller.apiProjectFileGet()

        then:
        null==result
    }
    def "project file readme get project dne"(){
        given:
        params.filename="readme.md"
        params.project="test"
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> false
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,13) >> true
            1 * requireVersion(_,_,11) >> true
            1 * renderErrorFormat(_,{it.code=='api.error.item.doesnotexist' && it.args==['Project','test']})
        }
        when:
        def result=controller.apiProjectFileGet()

        then:
        null==result
    }
    def "project file readme get project not authorized"(){
        given:
        params.filename="readme.md"
        params.project="test"
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> false
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,13) >> true
            1 * requireVersion(_,_,11) >> true
            1 * renderErrorFormat(_,{it.code=='api.error.item.unauthorized' && it.args==['configure','Project','test']})
        }
        when:
        def result=controller.apiProjectFileGet()

        then:
        null==result
    }
    def "project file readme get project authorized wrong filename"(){
        given:
        params.filename="wrong.md"
        params.project="test"
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){

            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,13) >> true
            1 * requireVersion(_,_,11) >> true
            1 * renderErrorFormat(_,{it.code=='api.error.item.doesnotexist' && it.args==['resource','wrong.md']})
        }
        when:
        def result=controller.apiProjectFileGet()

        then:
        null==result
    }
    def "project file readme get not found"(){
        given:
        params.filename="readme.md"
        params.project="test"
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                1 * existsFileResource('readme.md') >> false
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,13) >> true
            1 * requireVersion(_,_,11) >> true
            1 * renderErrorFormat(_,{it.code=='api.error.item.doesnotexist' && it.args==['resource','readme.md']})
        }
        when:
        def result=controller.apiProjectFileGet()

        then:
        null==result
    }
    def "project file GET text format"(){
        given:
        params.filename="readme.md"
        params.project="test"
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                1 * existsFileResource('readme.md') >> true
                1 * loadFileResource('readme.md',!null)
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,13) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'text'
        }
        when:
        def result=controller.apiProjectFileGet()

        then:
        response.contentType=='text/plain'
    }
    def "project file GET xml format"(String filename,String text){
        given:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                1 * existsFileResource(filename) >> true
                1 * loadFileResource(filename,!null)
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,13) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'xml'
            1 * renderSuccessXml(_,_,_) >> text
        }
        when:
        params.filename=filename
        params.project="test"
        def result=controller.apiProjectFileGet()

        then:
        result==text

        where:
        filename    | text
        'readme.md' | 'test1'
        'motd.md'   | 'test2'
    }
    def "project file GET json format"(String filename,String text){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
            1 * getFrameworkProject('test') >> Stub(IRundeckProject){
                existsFileResource(filename) >> true
                loadFileResource(filename,_) >> {args->
                    args[1].write(text.bytes)
                    text.length()
                }
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,13) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
        }
        when:
        params.filename=filename
        params.project="test"
        def result=controller.apiProjectFileGet()

        then:
        response.contentType==~/^application\/json(;.+)?$/
        response.json==[contents:text]

        where:
        filename    | text
        'readme.md' | 'test1'
        'motd.md'   | 'test2'
    }


    def "project file delete"(String filename){
        given:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                1 * deleteFileResource(filename) >> true
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,13) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(_,_,_) >> 'xml'
        }
        when:
        params.filename=filename
        params.project="test"
        request.method='DELETE'
        def result=controller.apiProjectFileDelete()

        then:
        response.status==204

        where:
        filename    | _
        'readme.md' | _
        'motd.md'   | _
    }


    def "project file delete wrong method"(String filename,String method){

        when:
        params.filename=filename
        params.project="test"
        request.method=method
        def result=controller.apiProjectFileDelete()

        then:
        response.status==405

        where:
        filename    | method
        'readme.md' | 'GET'
        'readme.md' | 'PUT'
        'readme.md' | 'POST'
        'motd.md'   | 'GET'
        'motd.md'   | 'PUT'
        'motd.md'   | 'POST'
    }

    def "project file PUT json"(String filename,String text){
        given:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                1 * storeFileResource(filename,{args->
                    byte[] bar=new byte[1024]
                    def len=args.read(bar)
                    text == new String(bar,0,len)
                }) >> text.length()

                1 * loadFileResource(filename,_) >> {args->
                    args[1].write(text.bytes)
                    text.length()
                }
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,13) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(*_) >> 'xml'
            1 * parseJsonXmlWith(*_) >> {args->
                args[2].json.call(args[0].JSON)
                true
            }
        }
        when:
        params.filename=filename
        params.project="test"
        request.method='PUT'
        request.format='json'
        request.json=[contents:text]
        def result=controller.apiProjectFilePut()

        then:
        response.status==200

        where:
        filename    | text
        'readme.md' | 'test1'
        'motd.md'   | 'test2'
    }
    def "project file PUT xml"(String filename,String text){
        given:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                1 * storeFileResource(filename,{args->
                    byte[] bar=new byte[1024]
                    def len=args.read(bar)
                    text == new String(bar,0,len)
                }) >> text.length()

                1 * loadFileResource(filename,_) >> {args->
                    args[1].write(text.bytes)
                    text.length()
                }
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,13) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(*_) >> 'xml'
            1 * parseJsonXmlWith(_,_,_) >> {args->
                args[2].xml.call(args[0].XML)
                true
            }
        }
        when:
        params.filename=filename
        params.project="test"
        request.method='PUT'
        request.format='xml'
        request.content=('<contents>'+text+'</contents>').bytes

        def result=controller.apiProjectFilePut()

        then:
        response.status==200

        where:
        filename    | text
        'readme.md' | 'test1'
        'motd.md'   | 'test2'
    }
    def "project file PUT text"(String filename,String text){
        given:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                1 * storeFileResource(filename,{args->
                    byte[] bar=new byte[1024]
                    def len=args.read(bar)
                    text == new String(bar,0,len)
                }) >> text.length()

                1 * loadFileResource(filename,_) >> {args->
                    args[1].write(text.bytes)
                    text.length()
                }
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,13) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(*_) >> 'xml'
            1 * renderSuccessXml(*_)

        }
        when:
        params.filename=filename
        params.project="test"
        request.method='PUT'
        request.format='text'
        request.content=text.bytes
        def result=controller.apiProjectFilePut()

        then:
        response.status==200

        where:
        filename    | text
        'readme.md' | 'test1'
        'motd.md'   | 'test2'
    }


    def "project acls require api_version 14"(){
        setup:
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> {args->
                args[1].status=400
                false
            }
        }
        when:
        controller.apiProjectAcls()

        then:
        response.status==400
    }
    def "project acls require project parameter"(){
        setup:
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * requireVersion(_,_,11) >> true
            1 * renderErrorFormat(_,[status:400,code:'api.error.parameter.required',args:['project']]) >> {args->
                args[0].status=args[1].status
            }
        }
        when:
        controller.apiProjectAcls()

        then:
        response.status==400
    }
    def "project acls project not found"(){
        setup:
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * requireVersion(_,_,11) >> true
            1 * renderErrorFormat(_,[status:404,code:'api.error.item.doesnotexist',args:['Project','monkey']]) >> {args->
                args[0].status=args[1].status
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('monkey') >> false
        }
        when:
        params.project='monkey'
        controller.apiProjectAcls()

        then:
        response.status==404
    }
    @Unroll
    def "project acls not authorized"(){
        setup:
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * requireVersion(_,_,11) >> true
            1 * renderErrorFormat(_,[status:403,code:'api.error.item.unauthorized',args:[action,'ACL for Project', 'monkey']]) >> {args->
                args[0].status=args[1].status
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('monkey') >> true
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProjectAcl('monkey') >> [type:'project_acl',name:'monkey']
            1 * authorizeApplicationResourceAny(null,[type:'project_acl',name:'monkey'],[action,ACTION_ADMIN])>>false
        }
        when:
        params.project='monkey'
        request.method=method
        controller.apiProjectAcls()

        then:
        response.status==403

        where:
        method | action
        'GET' | ACTION_READ
        'POST' | ACTION_CREATE
        'PUT' | ACTION_UPDATE
        'DELETE' | ACTION_DELETE
    }
    def "project acls invalid path"(){
        setup:
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * requireVersion(_,_,11) >> true

            1 * extractResponseFormat(_,_,_,_) >> 'json'
            1 * renderErrorFormat(
                    _,
                    [
                            status: 400,
                            code: 'api.error.parameter.invalid',
                            args: ['elf', 'path', 'Must refer to a file ending in .aclpolicy'],
                            format: 'json'
                    ]
            ) >> { args ->
                args[0].status = args[1].status
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('monkey') >> true
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProjectAcl('monkey') >> null
            1 * authorizeApplicationResourceAny(null,null,[ACTION_READ,ACTION_ADMIN])>>true
            1 * getFrameworkProject('monkey') >> Stub(IRundeckProject)
        }
        when:
        params.path='elf'
        params.project='monkey'
        controller.apiProjectAcls()

        then:
        response.status==400
    }
    def "project acls GET 404"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[ACTION_READ,ACTION_ADMIN]) >> true
            1 * getFrameworkProject('test') >> Stub(IRundeckProject){
                existsFileResource(_) >> false
                existsDirResource(_) >> false

            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
            1 * renderErrorFormat(_,_) >> {args->
                args[0].status=args[1].status
                null
            }
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        def result=controller.apiProjectAcls()

        then:
        response.status==404
    }
    def "project acls GET json"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[ACTION_READ,ACTION_ADMIN]) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                1 * existsFileResource(_) >> true
                1 * loadFileResource('acls/blah.aclpolicy',_) >> {args->
                    args[1].write('blah'.bytes)
                    4
                }
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        response.format='json'
        def result=controller.apiProjectAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/json')
        response.json==[contents:"blah"]
    }
    def "project acls GET unsupported format"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[ACTION_READ,ACTION_ADMIN]) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                1 * existsFileResource(_) >> true
                0 * loadFileResource('acls/blah.aclpolicy',_) >> {args->
                    args[1].write('blah'.bytes)
                    4
                }
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(_,_,_,_) >> {it[3]}
            1 * renderErrorFormat(_,[status:406,code:'api.error.resource.format.unsupported',args:['jambajuice']])>>{it[0].status=it[1].status}
            0 * _(*_)
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        response.format='jambajuice'
        def result=controller.apiProjectAcls()

        then:
        response.status==406
    }
    def "project acls GET default format"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[ACTION_READ,ACTION_ADMIN]) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                1 * existsFileResource(_) >> true
                1 * loadFileResource('acls/blah.aclpolicy',_) >> {args->
                    args[1].write('blah'.bytes)
                    4
                }
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(_,_,_,_) >> {it[3]}
            0 * _(*_)
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        def result=controller.apiProjectAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/json')
        response.json==[contents:"blah"]
    }
    def "project acls GET xml"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[ACTION_READ,ACTION_ADMIN]) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                1 * existsFileResource(_) >> true
                1 * loadFileResource('acls/blah.aclpolicy',_) >> {args->
                    args[1].write('blah'.bytes)
                    4
                }
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'xml'
            1 * renderWrappedFileContentsXml('blah','xml',_) >> {args-> args[2]}
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        response.format='xml'
        controller.apiProjectAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/xml')
    }
    def "project acls GET text/yaml"(String respFormat, String contentType){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[ACTION_READ,ACTION_ADMIN]) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                1 * existsFileResource(_) >> true
                1 * loadFileResource('acls/blah.aclpolicy',_) >> {args->
                    args[1].write('blah'.bytes)
                    4
                }
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(_,_,_,_) >> respFormat
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        def result=controller.apiProjectAcls()

        then:
        response.status==200
        response.contentType.split(';').contains(contentType)
        response.contentAsString=='blah'

        where:
        respFormat | contentType
        'text'     | 'text/plain'
        'yaml'     | 'application/yaml'
    }
    def "project acls GET dir JSON"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[ACTION_READ,ACTION_ADMIN]) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                1 * existsFileResource(_) >> false
                1 * existsDirResource('acls/') >> true
                1 * listDirPaths('acls/') >> { args ->
                    ['acls/test','acls/blah.aclpolicy','acls/adir/']
                }
                getName()>>'test'
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
            1 * jsonRenderDirlist('acls/',_,_,['acls/blah.aclpolicy']) >> {args->
                [success: true]
            }
            pathRmPrefix(_,_)>>'x'
        }
        when:
        params.path=''
        params.project="test"
        response.format='json'
        def result=controller.apiProjectAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/json')

    }
    def "project acls GET dir XML"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[ACTION_READ,ACTION_ADMIN]) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                1 * existsFileResource(_) >> false
                1 * existsDirResource('acls/') >> true
                1 * listDirPaths('acls/') >> { args ->
                    ['acls/test','acls/blah.aclpolicy','acls/adir/']
                }
                getName()>>'test'
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'xml'
            1 * xmlRenderDirList('acls/',_,_,['acls/blah.aclpolicy'],_)
        }
        when:
        params.path=''
        params.project="test"
        response.format='xml'
        def result=controller.apiProjectAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/xml')
    }
    def "project acls POST text"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[ACTION_CREATE,ACTION_ADMIN]) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                1 * existsFileResource('acls/test.aclpolicy') >> false
                1 * storeFileResource('acls/test.aclpolicy',_) >> {args->
                    0
                }
                1 * loadFileResource('acls/test.aclpolicy',_) >> {args->
                    args[1].write('blah'.bytes)
                    4
                }

                getName()>>'test'
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
        }

        controller.authorizationService=Stub(AuthorizationService){
            validateYamlPolicy('test','test.aclpolicy',_)>>Stub(PoliciesValidation){
                isValid()>>true
            }
        }
        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='json'
        request.method='POST'
        request.contentType='application/yaml'
        request.content=('{ description: \'\', \n' +
                'context: { project: \'test\' }, \n' +
                'by: { username: \'test\' }, \n' +
                'for: { resource: [ { allow: \'x\' } ] } }').bytes
        def result=controller.apiProjectAcls()

        then:
        response.status==201
        response.contentType.split(';').contains('application/json')
        response.json==[contents:'blah']


    }
    def "project acls POST text, invalid policy, json response"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[ACTION_CREATE,ACTION_ADMIN]) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){

                getName()>>'test'
                1 * existsFileResource('acls/test.aclpolicy')>>false
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
            1 * renderJsonAclpolicyValidation(_)>>{args-> [contents: 'blah']}
        }

        controller.authorizationService=Stub(AuthorizationService){
            validateYamlPolicy('test','test.aclpolicy',_)>>Stub(PoliciesValidation){
                isValid()>>false
            }
        }
        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='json'
        request.method='POST'
        request.contentType='application/yaml'
        request.content=('{ description: \'\', \n' +
                'context: { project: \'test\' }, \n' +
                'by: { username: \'test\' }, \n' +
                'for: { resource: [ { allow: \'x\' } ] } }').bytes
        def result=controller.apiProjectAcls()

        then:
        response.status==400
        response.contentType.split(';').contains('application/json')
        response.json==[contents:'blah']


    }
    def "project acls POST text, invalid policy, xml response"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[ACTION_CREATE,ACTION_ADMIN]) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){

                getName()>>'test'
                1 * existsFileResource('acls/test.aclpolicy') >> false
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'xml'
            1 * renderXmlAclpolicyValidation(_,_)>>{args->args[1].contents('data')}
        }

        controller.authorizationService=Stub(AuthorizationService){
            validateYamlPolicy('test','test.aclpolicy',_)>>Stub(PoliciesValidation){
                isValid()>>false
            }
        }
        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='xml'
        request.method='POST'
        request.contentType='application/yaml'
        request.content=('{ description: \'\', \n' +
                'context: { project: \'test\' }, \n' +
                'by: { username: \'test\' }, \n' +
                'for: { resource: [ { allow: \'x\' } ] } }').bytes
        def result=controller.apiProjectAcls()

        then:
        response.status==400
        response.contentType.split(';').contains('application/xml')
        response.xml!=null
        response.xml.text()=='data'


    }
    def "project acls PUT not found"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[ACTION_UPDATE,ACTION_ADMIN]) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                1 * existsFileResource('acls/test.aclpolicy') >> false

                getName()>>'test'
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
            1 * renderErrorFormat(
                    _,
                    [status: 404,
                     code: 'api.error.item.doesnotexist',
                     args: ['Project ACL Policy File', 'test.aclpolicy for project test'],
                     format: 'json'] ) >> { args ->
                args[0].status = args[1].status
            }
        }

        controller.authorizationService=Stub(AuthorizationService){
            validateYamlPolicy('test','test.aclpolicy',_)>>Stub(PoliciesValidation){
                isValid()>>true
            }
        }
        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='json'
        request.method='PUT'
        request.contentType='application/yaml'
        request.content=('{ description: \'\', \n' +
                'context: { project: \'test\' }, \n' +
                'by: { username: \'test\' }, \n' +
                'for: { resource: [ { allow: \'x\' } ] } }').bytes
        def result=controller.apiProjectAcls()

        then:
        response.status==404

    }

    def "project acls PUT text ok"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[ACTION_UPDATE,ACTION_ADMIN]) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                1 * existsFileResource('acls/test.aclpolicy') >> true
                1 * storeFileResource('acls/test.aclpolicy',_) >> {args->
                    0
                }
                1 * loadFileResource('acls/test.aclpolicy',_) >> {args->
                    args[1].write('blah'.bytes)
                    4
                }

                getName()>>'test'
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
        }

        controller.authorizationService=Stub(AuthorizationService){
            validateYamlPolicy('test','test.aclpolicy',_)>>Stub(PoliciesValidation){
                isValid()>>true
            }
        }
        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='json'
        request.method='PUT'
        request.contentType='application/yaml'
        request.content=('{ description: \'\', \n' +
                'context: { project: \'test\' }, \n' +
                'by: { username: \'test\' }, \n' +
                'for: { resource: [ { allow: \'x\' } ] } }').bytes
        def result=controller.apiProjectAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/json')
        response.json==[contents:'blah']


    }
    def "project acls DELETE not found"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[ACTION_DELETE,ACTION_ADMIN]) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                1 * existsFileResource('acls/test.aclpolicy') >> false

                getName()>>'test'
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
            1 * renderErrorFormat(
                    _,
                    [status: 404,
                     code: 'api.error.item.doesnotexist',
                     args: ['Project ACL Policy File', 'test.aclpolicy for project test'],
                     format: 'json'] ) >> { args ->
                args[0].status = args[1].status
            }
        }

        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='json'
        request.method='DELETE'
        def result=controller.apiProjectAcls()

        then:
        response.status==404

    }
    def "project acls DELETE ok"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[ACTION_DELETE,ACTION_ADMIN]) >> true
            1 * getFrameworkProject('test') >> Mock(IRundeckProject){
                1 * existsFileResource('acls/test.aclpolicy') >> true
                1 * deleteFileResource('acls/test.aclpolicy') >> true

                getName()>>'test'
            }
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * requireVersion(_,_,11) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'

        }

        when:
        params.path='test.aclpolicy'
        params.project="test"
        response.format='json'
        request.method='DELETE'
        def result=controller.apiProjectAcls()

        then:
        response.status==204

    }

    def "import archive importACL"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * getAuthContextForSubjectAndProject(_,'test') >> null
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProject('test') >> null
            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[ACTION_ADMIN,ACTION_IMPORT]) >> true
            1 * authorizeApplicationResourceAny(_,_,[ACTION_CREATE,ACTION_ADMIN]) >> true

            1 * getFrameworkProject('test') >> null
            1 * getRundeckFramework() >> null

            0 * _(*_)
        }
        controller.projectService=Mock(ProjectService){
            1*importToProject(null,null,null,!null, {
                it.jobUuidOption== 'preserve'
                it.importExecutions== true
                it.importConfig== false
                it.importACL== true
            })>>[success:true]
            1 * validateAllProjectComponentImportOptions(_) >> [:]
            0 * _(*_)
        }
        request.subject=new Subject(true,[
                Mock(Group){
                    getName()>>'test1'
                }
        ] as Set,[] as Set,[] as Set)


        when:

        def tokenHolder = SynchronizerTokensHolder.store(session)

        params[SynchronizerTokensHolder.TOKEN_URI] = '/controller/handleForm'
        params[SynchronizerTokensHolder.TOKEN_KEY] = tokenHolder.generateToken(params[SynchronizerTokensHolder.TOKEN_URI])

        params.project="test"
        params.importACL='true'
        response.format='json'
        request.method='POST'
        def file = new GrailsMockMultipartFile('zipFile', 'data'.bytes)
        request.addFile file
        def result=controller.importArchive()

        then:
        response.redirectedUrl=='/project/test/import'
        flash.message=='archive.successfully.imported'
        response.status==302
    }
    def "import archive no importACL"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * getAuthContextForSubjectAndProject(_,'test') >> null
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProject('test') >> null
            0 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[ACTION_ADMIN,ACTION_IMPORT]) >> true
            0 * authorizeApplicationResourceAny(_,_,[ACTION_CREATE,ACTION_ADMIN]) >> true

            1 * getFrameworkProject('test') >> null
            1 * getRundeckFramework() >> null

            0 * _(*_)
        }
        controller.projectService=Mock(ProjectService){
            1*importToProject(null,null,null,!null,{
                it.jobUuidOption== 'preserve'
                it.importExecutions== true
                it.importConfig== false
                it.importACL== false
            })>>[success:true]
            1 * validateAllProjectComponentImportOptions(_) >> [:]

            0 * _(*_)
        }
        request.subject=new Subject(true,[
                Mock(Group){
                    getName()>>'test1'
                }
        ] as Set,[] as Set,[] as Set)


        when:

        def tokenHolder = SynchronizerTokensHolder.store(session)

        params[SynchronizerTokensHolder.TOKEN_URI] = '/controller/handleForm'
        params[SynchronizerTokensHolder.TOKEN_KEY] = tokenHolder.generateToken(params[SynchronizerTokensHolder.TOKEN_URI])

        params.project="test"
        params.importACL='false'
        response.format='json'
        request.method='POST'
        def file = new GrailsMockMultipartFile('zipFile', 'data'.bytes)
        request.addFile file
        def result=controller.importArchive()

        then:
        response.redirectedUrl=='/project/test/import'
        flash.message=='archive.successfully.imported'
        response.status==302
    }
    def "import archive importACL unauthorized"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            1 * existsFrameworkProject('test') >> true
            1 * getAuthContextForSubjectAndProject(_,'test') >> null
            1 * getAuthContextForSubject(_) >> null
            1 * authResourceForProject('test') >> null
            1 * authResourceForProjectAcl('test') >> null
            1 * authorizeApplicationResourceAny(_,_,[ACTION_ADMIN,ACTION_IMPORT]) >> true
            1 * authorizeApplicationResourceAny(_,_,[ACTION_CREATE,ACTION_ADMIN]) >> false

//            1 * getFrameworkProject('test') >> null
//            1 * getRundeckFramework() >> null

            0 * _(*_)
        }
        controller.projectService=Mock(ProjectService){

            0 * _(*_)
        }
        request.subject=new Subject(true,[
                Mock(Group){
                    getName()>>'test1'
                }
        ] as Set,[] as Set,[] as Set)


        when:

        def tokenHolder = SynchronizerTokensHolder.store(session)

        params[SynchronizerTokensHolder.TOKEN_URI] = '/controller/handleForm'
        params[SynchronizerTokensHolder.TOKEN_KEY] = tokenHolder.generateToken(params[SynchronizerTokensHolder.TOKEN_URI])

        params.project="test"
        params.importACL='true'
        response.format='json'
        request.method='POST'
        def file = new GrailsMockMultipartFile('zipFile', 'data'.bytes)
        request.addFile file
        def result=controller.importArchive()

        then:
        view == '/common/error'
        request.errorCode == 'request.error.unauthorized.message'
        request.errorArgs == [ACTION_CREATE, 'ACL for Project', 'test']

    }
    def "import archive token failure"(){
        setup:
        controller.frameworkService=Mock(FrameworkService){
            0 * _(*_)
        }
        controller.projectService=Mock(ProjectService){

            0 * _(*_)
        }

        when:

        def tokenHolder = SynchronizerTokensHolder.store(session)

        params[SynchronizerTokensHolder.TOKEN_URI] = '/controller/handleForm'
        params[SynchronizerTokensHolder.TOKEN_KEY] = 'xxx'//tokenHolder.generateToken(params[SynchronizerTokensHolder.TOKEN_URI])

        params.project="test"
        params.importACL='true'
        response.format='json'
        request.method='POST'
        def file = new GrailsMockMultipartFile('zipFile', 'data'.bytes)
        request.addFile file
        def result=controller.importArchive()

        then:
        response.redirectedUrl=='/project/test/import'
        flash.error=='request.error.invalidtoken.message'

    }

    def "export Instance Prepare"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        params.project = 'aproject'
        params.exportAll = true
        params.exportJobs = true
        params.exportExecutions = true
        params.exportConfigs = true
        params.exportReadmes = true
        params.exportAcls = true
        params.url = url
        params.apitoken = token
        params.targetproject = target
        params.preserveuuid = preserveuuid


        when:
        def result = controller.exportInstancePrepare()

        then:
        1 * controller.frameworkService.existsFrameworkProject('aproject') >> true
        1 * controller.frameworkService.authorizeApplicationResourceAny(_, _, ['admin', 'promote']) >> true
        1 * controller.frameworkService.getFrameworkProject(_) >> Mock(IRundeckProject)
        1 * controller.projectService.exportProjectToInstanceAsync(_, _, _, _, { ArchiveOptions opts ->
            opts.executionsOnly == false &&
                    opts.all == true &&
                    opts.jobs == true &&
                    opts.executions == true &&
                    opts.configs == true &&
                    opts.readmes == true &&
                    opts.acls == true
        },_,_,_,preserveuuid?:false,_
        ) >> 'dummytoken'
        1 * controller.projectService.validateAllProjectComponentExportOptions(_) >> [:]
        response.redirectedUrl == '/project/aproject/exportWait/dummytoken?instance=' + url + '&iproject=' + target

        where:
        url      | token  | target      | preserveuuid
        'url1'   | '123'  | 'proj1'     | null
        'url2'   | '456'  | 'proj2'     | true

    }

    def "export Instance Prepare With missing properties"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        params.project = 'aproject'
        params.exportAll = true
        params.exportJobs = true
        params.exportExecutions = true
        params.exportConfigs = true
        params.exportReadmes = true
        params.exportAcls = true
        params.url = url
        params.apitoken = token
        params.targetproject = target
        params.preserveuuid = preserveuuid


        when:
        def result = controller.exportInstancePrepare()

        then:
        0 * controller.frameworkService.existsFrameworkProject('aproject') >> true
        0 * controller.projectService.exportProjectToInstanceAsync(_, _, _, _, { ArchiveOptions opts ->
            opts.executionsOnly == false &&
                    opts.all == true &&
                    opts.jobs == true &&
                    opts.executions == true &&
                    opts.configs == true &&
                    opts.readmes == true &&
                    opts.acls == true
        },_,_,_,preserveuuid?:false
        ) >> 'dummytoken'
        flash.error
        response.redirectedUrl == '/project/aproject/export'

        where:
        url      | token  | target      | preserveuuid
        null     | '123'  | 'proj1'     | null
        'url1'   | null   | 'proj2'     | true
        'url2'   | '456'  | null        | true

    }


    def "export Instance with scm config"() {
        given:
        controller.projectService = Mock(ProjectService)
        controller.apiService = Mock(ApiService)
        controller.frameworkService = Mock(FrameworkService)
        params.project = 'aproject'
        params.exportAll = true
        params.exportJobs = true
        params.exportExecutions = true
        params.exportConfigs = true
        params.exportReadmes = true
        params.exportAcls = true
        params.exportScm = true
        params.url = url
        params.apitoken = token
        params.targetproject = target
        params.preserveuuid = preserveuuid


        when:
        def result = controller.exportInstancePrepare()

        then:
        1 * controller.frameworkService.existsFrameworkProject('aproject') >> true
        1 * controller.frameworkService.authorizeApplicationResourceAny(_, _, ['admin', 'promote']) >> true
        1 * controller.frameworkService.getFrameworkProject(_) >> Mock(IRundeckProject)
        1 * controller.projectService.exportProjectToInstanceAsync(_, _, _, _, { ArchiveOptions opts ->
            opts.executionsOnly == false &&
                    opts.all == true &&
                    opts.jobs == true &&
                    opts.executions == true &&
                    opts.configs == true &&
                    opts.readmes == true &&
                    opts.acls == true &&
                    opts.scm == true
        },_,_,_,preserveuuid?:false,_
        ) >> 'dummytoken'
        1 * controller.projectService.validateAllProjectComponentExportOptions(_) >> [:]
        response.redirectedUrl == '/project/aproject/exportWait/dummytoken?instance=' + url + '&iproject=' + target

        where:
        url      | token  | target      | preserveuuid
        'url1'   | '123'  | 'proj1'     | null
        'url2'   | '456'  | 'proj2'     | true

    }
}
