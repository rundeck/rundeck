package rundeck.controllers

import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authorization.Validation
import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.plugins.testing.GrailsMockMultipartFile
import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder
import rundeck.services.ApiService
import rundeck.services.ArchiveOptions
import rundeck.services.AuthorizationService
import rundeck.services.FrameworkService
import rundeck.services.ProjectService
import spock.lang.Specification
import spock.lang.Unroll

import javax.security.auth.Subject

import static com.dtolabs.rundeck.server.authorization.AuthConstants.ACTION_ADMIN
import static com.dtolabs.rundeck.server.authorization.AuthConstants.ACTION_CREATE
import static com.dtolabs.rundeck.server.authorization.AuthConstants.ACTION_DELETE
import static com.dtolabs.rundeck.server.authorization.AuthConstants.ACTION_IMPORT
import static com.dtolabs.rundeck.server.authorization.AuthConstants.ACTION_READ
import static com.dtolabs.rundeck.server.authorization.AuthConstants.ACTION_UPDATE

/**
 * Created by greg on 2/26/15.
 */
@TestFor(ProjectController)
class ProjectControllerSpec extends Specification{
    def setup(){

    }
    def cleanup(){

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
        })

        where:
        eidparam | expectedset
        '123' | ['123'] as Set
        '123,456' | ['123','456'] as Set
        ['123','456'] | ['123','456'] as Set
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


    def "project file delete"(String filename,_){
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
            1 * extractResponseFormat(*_) >> 'xml'
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
            1 * renderWrappedFileContents('blah','json',_) >> {args-> args[2].success=true}
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        response.format='json'
        def result=controller.apiProjectAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/json')
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
            1 * renderWrappedFileContents('blah','xml',_) >> {args-> args[2]}
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
            1 * jsonRenderDirlist('acls/',_,_,['acls/blah.aclpolicy'],_) >> {args->
                args[4].success=true
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
            1 * renderWrappedFileContents('blah','json',_)>>{args->args[2].contents=args[0]}
        }

        controller.authorizationService=Stub(AuthorizationService){
            validateYamlPolicy('test','test.aclpolicy',_)>>Stub(Validation){
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
            1 * renderJsonAclpolicyValidation(_,_)>>{args->args[1].contents='blah'}
        }

        controller.authorizationService=Stub(AuthorizationService){
            validateYamlPolicy('test','test.aclpolicy',_)>>Stub(Validation){
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
            validateYamlPolicy('test','test.aclpolicy',_)>>Stub(Validation){
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
            validateYamlPolicy('test','test.aclpolicy',_)>>Stub(Validation){
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
            1 * renderWrappedFileContents('blah','json',_)>>{args->args[2].contents=args[0]}
        }

        controller.authorizationService=Stub(AuthorizationService){
            validateYamlPolicy('test','test.aclpolicy',_)>>Stub(Validation){
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
        response.redirectedUrl=='/menu/admin?project=test'
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
        response.redirectedUrl=='/menu/admin?project=test'
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
        response.redirectedUrl=='/menu/admin?project=test'
        flash.error=='request.error.invalidtoken.message'

    }
}
