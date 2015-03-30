package rundeck.controllers

import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.test.mixin.TestFor
import rundeck.services.ApiService
import rundeck.services.FrameworkService
import spock.lang.Specification

/**
 * Created by greg on 2/26/15.
 */
@TestFor(ProjectController)
class ProjectControllerSpec extends Specification{
    def setup(){

    }
    def cleanup(){

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

}
