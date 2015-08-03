package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.Validation
import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.test.mixin.TestFor
import rundeck.services.ApiService
import rundeck.services.AuthorizationService
import rundeck.services.FrameworkService
import rundeck.services.StorageManager
import spock.lang.Specification

import static com.dtolabs.rundeck.server.authorization.AuthConstants.ACTION_ADMIN
import static com.dtolabs.rundeck.server.authorization.AuthConstants.ACTION_ADMIN
import static com.dtolabs.rundeck.server.authorization.AuthConstants.ACTION_CONFIGURE
import static com.dtolabs.rundeck.server.authorization.AuthConstants.ACTION_CONFIGURE
import static com.dtolabs.rundeck.server.authorization.AuthConstants.ACTION_CONFIGURE

/**
 * Created by greg on 7/28/15.
 */
@TestFor(FrameworkController)
class FrameworkControllerSpec extends Specification {
    def "system acls require api_version 14"(){
        setup:
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> {args->
                args[1].status=400
                false
            }
        }
        when:
        controller.apiSystemAcls()

        then:
        response.status==400
    }
    def "system acls not authorized"(){
        setup:
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * renderErrorFormat(_,[status:403,code:'api.error.item.unauthorized',args:['Manage ACLs','Rundeck','']]) >> {args->
                args[0].status=args[1].status
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(null,!null,[ACTION_CONFIGURE,ACTION_ADMIN])>>false
        }
        when:
        params.project='monkey'
        controller.apiSystemAcls()

        then:
        response.status==403
    }
    def "system acls invalid path"(){
        setup:
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true

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
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(null,!null,[ACTION_CONFIGURE,ACTION_ADMIN])>>true
        }
        when:
        params.path='elf'
        params.project='monkey'
        controller.apiSystemAcls()

        then:
        response.status==400
    }
    def "system acls GET 404"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
            existsFileResource(_) >> false
            existsDirResource(_) >> false
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
            1 * renderErrorFormat(_,_) >> {args->
                args[0].status=args[1].status
                null
            }
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        def result=controller.apiSystemAcls()

        then:
        response.status==404
    }
    def "system acls GET json"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
            1 * existsFileResource(_) >> true
            1 * loadFileResource('acls/blah.aclpolicy',_) >> {args->
                args[1].write('blah'.bytes)
                4
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
            1 * renderWrappedFileContents(_,_,_) >> { args ->
                args[2].success=true
            }
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        response.format='json'
        def result=controller.apiSystemAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/json')
    }
    def "system acls GET xml"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
            1 * existsFileResource(_) >> true
            1 * loadFileResource('acls/blah.aclpolicy',_) >> {args->
                args[1].write('blah'.bytes)
                4
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'xml'
            1 * renderWrappedFileContents('blah','xml',_)
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        response.format='xml'
        controller.apiSystemAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/xml')
    }
    def "system acls GET text/yaml"(String respFormat, String contentType){
        setup:
        controller.configStorageService=Mock(StorageManager){
            1 * existsFileResource(_) >> true
            1 * loadFileResource('acls/blah.aclpolicy',_) >> {args->
                args[1].write('blah'.bytes)
                4
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> respFormat
        }
        when:
        params.path='blah.aclpolicy'
        params.project="test"
        def result=controller.apiSystemAcls()

        then:
        response.status==200
        response.contentType.split(';').contains(contentType)
        response.contentAsString=='blah'

        where:
        respFormat | contentType
        'text'     | 'text/plain'
        'yaml'     | 'application/yaml'
    }
    def "system acls GET dir JSON"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
            1 * existsFileResource(_) >> false
            1 * existsDirResource('acls/') >> true
            1 * listDirPaths('acls/') >> { args ->
                ['acls/test','acls/blah.aclpolicy','acls/adir/']
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'
            1 * jsonRenderDirlist('acls/',_,_,['acls/blah.aclpolicy'],_)>>{args-> args[4].success=true}
        }
        when:
        params.path=''
        params.project="test"
        def result=controller.apiSystemAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/json')
        response.json==[success:true]

    }
    def "system acls GET dir XML"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
            1 * existsFileResource(_) >> false
            1 * existsDirResource('acls/') >> true
            1 * listDirPaths('acls/') >> { args ->
                ['acls/test','acls/blah.aclpolicy','acls/adir/']
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'xml'
            1 * xmlRenderDirList('acls/',_,_,['acls/blah.aclpolicy'],_)>>{args-> args[4].success(ok:true)}
        }
        when:
        params.path=''
        params.project="test"
        response.format='xml'
        def result=controller.apiSystemAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/xml')
//        response.contentAsString==''
        response.xml.'@ok'.text()=='true'

    }
    def "system acls POST text"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
            1 * writeFileResource('acls/test.aclpolicy',_,_) >> null
            1 * loadFileResource('acls/test.aclpolicy',_) >> {args->
                args[1].write('blah'.bytes)
                4
            }
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'

            1 * renderWrappedFileContents(_,_,_) >> { args ->
                args[2].contents=args[0]
            }
        }
        controller.authorizationService=Stub(AuthorizationService){
            validateYamlPolicy('test.aclpolicy',_)>>Stub(Validation){
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
        def result=controller.apiSystemAcls()

        then:
        response.status==200
        response.contentType.split(';').contains('application/json')
        response.json==[contents:'blah']


    }
    /**
     * Policy validation failure
     * @return
     */
    def "system acls POST text invalid (json response)"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'json'

            1 * renderJsonAclpolicyValidation(_,_)>>{args->
                args[1].contents='blahz'
            }
        }
        controller.authorizationService=Stub(AuthorizationService){
            validateYamlPolicy('test.aclpolicy',_)>>Stub(Validation){
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
        def result=controller.apiSystemAcls()

        then:
        response.status==400
        response.contentType.split(';').contains('application/json')
        response.json==[contents:'blahz']
    }
    /**
     * Policy validation failure
     * @return
     */
    def "system acls POST text invalid (xml response)"(){
        setup:
        controller.configStorageService=Mock(StorageManager){
        }
        controller.frameworkService=Mock(FrameworkService){
            1 * getAuthContextForSubject(_) >> null
            1 * authorizeApplicationResourceAny(_,_,['configure','admin']) >> true
        }
        controller.apiService=Mock(ApiService){
            1 * requireVersion(_,_,14) >> true
            1 * extractResponseFormat(_,_,_,_) >> 'xml'

            1 * renderXmlAclpolicyValidation(_,_)>>{args->
                args[1].contents {
                    'data'('value')
                }
            }
        }
        controller.authorizationService=Stub(AuthorizationService){
            validateYamlPolicy('test.aclpolicy',_)>>Stub(Validation){
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
        def result=controller.apiSystemAcls()

        then:
        response.status==400
        response.contentType.split(';').contains('application/xml')
        response.xml!=null
        response.xml.data.text()=='value'

    }
}
