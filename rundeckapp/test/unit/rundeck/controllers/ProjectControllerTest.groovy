package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.test.mixin.TestFor
import org.junit.Test
import rundeck.filters.ApiRequestFilters
import rundeck.services.ApiService
import rundeck.services.FrameworkService

import javax.servlet.http.HttpServletResponse

/**
 * ProjectControllerTest is ...
 * @author greg
 * @since 2014-03-04
 */
@TestFor(ProjectController)
class ProjectControllerTest {
    @Test
    void apiProjectList_xml(){
        def fwct = mockFor(FrameworkService)
        fwct.demand.getAuthContextForSubject(1..1){subj->
            null
        }
        fwct.demand.projects(1..1){auth->
            ["a", "b"]
        }
        controller.frameworkService=fwct.createMock()

        controller.apiService = mockFor(ApiService).with {
            demand.renderSuccessXml(1..1) { req, resp, clos ->

            }
            createMock()
        }

        response.format='xml'
        controller.apiProjectList()
        assert response.status == HttpServletResponse.SC_OK

    }
    @Test
    void apiProjectList_json(){
        def fwct = mockFor(FrameworkService)
        fwct.demand.getAuthContextForSubject(1..1){subj->
            null
        }
        fwct.demand.projects(1..1){auth->
            [
                    [name:'testproject'],
                    [name:'testproject2'],
            ]
        }
        controller.frameworkService=fwct.createMock()



        response.format='json'
        controller.apiProjectList()
        assert response.status == HttpServletResponse.SC_OK
        assert response.json.size()==2
        assert response.json[0].name=='testproject'
        assert response.json[0].description==''
        assert response.json[0].url=='http://localhost:8080/api/11/project/testproject'
        assert response.json[1].name=='testproject2'
        assert response.json[1].description==''
        assert response.json[1].url=='http://localhost:8080/api/11/project/testproject2'
    }
    @Test
    void apiProjectList_unacceptableReceivesXml(){
        def fwct = mockFor(FrameworkService)
        fwct.demand.getAuthContextForSubject(1..1){subj->
            null
        }
        fwct.demand.projects(1..1){auth->
            [
                    [name:'testproject'],
                    [name:'testproject2'],
            ]
        }
        controller.frameworkService=fwct.createMock()

        controller.apiService = mockFor(ApiService).with {
            demand.renderSuccessXml(1..1) { req, resp, clos ->

            }
            createMock()
        }

        response.format='text'
        controller.apiProjectList()
        assert response.status==HttpServletResponse.SC_OK

    }

    @Test
    void apiProjectGet_missingProjectParam(){
        controller.frameworkService = mockFor(FrameworkService).with{
            demand.getAuthContextForSubject(1..1){subj->
                null
            }
            createMock()
        }

        controller.apiService = mockFor(ApiService).with {
            demand.renderErrorFormat(1..1) { resp, map ->
                assertEquals(HttpServletResponse.SC_BAD_REQUEST,map.status)
                assertEquals('api.error.parameter.required',map.code)
                resp.status=map.status
            }
            createMock()
        }

        response.format='xml'
        controller.apiProjectGet()
        assert response.status==HttpServletResponse.SC_BAD_REQUEST
    }
    @Test
    void apiProjectGet_unauthorized(){
        controller.frameworkService = mockFor(FrameworkService).with{
            demand.getAuthContextForSubject(1..1){subj->
                null
            }
            demand.authorizeApplicationResourceAll(1..1){ AuthContext authContext, Map resource, Collection actions->
                assertEquals('project',resource.type)
                assertEquals('test1',resource.name)
                assertEquals([AuthConstants.ACTION_READ],actions)
                false
            }
            createMock()
        }

        controller.apiService = mockFor(ApiService).with {
            demand.renderErrorFormat(1..1) { resp, map ->
                assertEquals(HttpServletResponse.SC_FORBIDDEN,map.status)
                assertEquals('api.error.item.unauthorized',map.code)
                resp.status=map.status
            }
            createMock()
        }

        response.format='xml'
        params.project='test1'
        controller.apiProjectGet()
        assert response.status==HttpServletResponse.SC_FORBIDDEN
    }
    @Test
    void apiProjectGet_notfound(){
        controller.frameworkService = mockFor(FrameworkService).with{
            demand.getAuthContextForSubject(1..1){subj->
                null
            }
            demand.authorizeApplicationResourceAll(1..1){ AuthContext authContext, Map resource, Collection actions->
                assertEquals('project',resource.type)
                assertEquals('test1',resource.name)
                assertEquals([AuthConstants.ACTION_READ],actions)
                true
            }
            demand.existsFrameworkProject(1..1) { proj ->
                assertEquals('test1',proj)
                false
            }
            createMock()
        }

        controller.apiService = mockFor(ApiService).with {
            demand.renderErrorFormat(1..1) { resp, map ->
                assertEquals(HttpServletResponse.SC_NOT_FOUND,map.status)
                assertEquals('api.error.item.doesnotexist',map.code)
                resp.status=map.status
            }
            createMock()
        }

        response.format='xml'
        params.project='test1'
        controller.apiProjectGet()
        assert response.status==HttpServletResponse.SC_NOT_FOUND
    }

    private Object createFrameworkService(boolean configAuth, String projectName, LinkedHashMap<String,
    String> projectProperties=[:]) {
        mockFor(FrameworkService).with {
            demand.getAuthContextForSubject(1..1) { subj ->
                null
            }
            def count = 0
            demand.authorizeApplicationResourceAll(1..1) { AuthContext authContext, Map resource, Collection actions ->
                assertEquals('project', resource.type)
                assertEquals(projectName, resource.name)
                count++
                if (count == 1) {
                    assertEquals([AuthConstants.ACTION_READ], actions)
                    return true
                } else {
                    assertEquals([AuthConstants.ACTION_CONFIGURE], actions)
                    return configAuth
                }
            }
            demand.existsFrameworkProject(1..1) { proj ->
                assertEquals(projectName, proj)
                true
            }
            demand.authorizeApplicationResourceAll(1..1) { AuthContext authContext, Map resource, Collection actions ->
                assertEquals('project', resource.type)
                assertEquals(projectName, resource.name)
                count++
                if (count == 1) {
                    assertEquals([AuthConstants.ACTION_READ], actions)
                    return true
                } else {
                    assertEquals([AuthConstants.ACTION_CONFIGURE], actions)
                    return configAuth
                }
            }
            demand.getFrameworkProject(1..1) { String name ->
                assertEquals(projectName, name)
                [name: projectName]
            }
            if(configAuth){
                demand.loadProjectProperties(1..1){pject->
                    assertEquals([name:projectName],pject)
                    projectProperties
                }
            }
            createMock()
        }
    }

    @Test
    void apiProjectGet_xml_noconfig_withwrapper() {
        defineBeans {
            apiService(ApiService)
        }
        controller.frameworkService = createFrameworkService(false, 'test1')

        response.format = 'xml'
        params.project = 'test1'
        request.setAttribute('api_version', 10) // trigger xml <result> wrapper
        controller.apiProjectGet()
        assert response.status == HttpServletResponse.SC_OK

        //XML result has wrapper
        assertEquals 'result', response.xml.name()
        assertEquals 'true', response.xml.'@success'.text()
        assertEquals ApiRequestFilters.API_CURRENT_VERSION.toString(), response.xml.'@apiversion'.text()

        assertEquals 1, response.xml.projects.size()
        assertEquals 0, response.xml.project.size()
        assertEquals 1, response.xml.projects.size()
        assertEquals 1, response.xml.projects.project.size()
        def project = response.xml.projects.project[0]

        //test project element
        assertEquals 'test1', project.name.text()
        assertEquals '', project.description.text()
        assertEquals 0, project.config.size()
    }
    @Test
    void apiProjectGet_xml_noconfig_v11(){
        defineBeans {
            apiService(ApiService)
        }
        controller.frameworkService = createFrameworkService(false, 'test1')

        response.format='xml'
        params.project='test1'
        request.setAttribute('api_version', 11) //do not include <result> wrapper
        controller.apiProjectGet()
        assert response.status==HttpServletResponse.SC_OK

        //XML result has no wrapper
        assertEquals 'project', response.xml.name()
        assertEquals 0, response.xml.result.size()
        assertEquals 0, response.xml.projects.size()
        def project=response.xml

        //test project element
        assertEquals 'test1', project.name.text()
        assertEquals '', project.description.text()
        assertEquals 0, project.config.size()
    }
    /**
     * apiversion {@literal <} 11 will result in no {@literal <config>} element, even if authorized
     */
    @Test
    void apiProjectGet_xml_withconfig_withwrapper(){
        defineBeans {
            apiService(ApiService)
        }
        controller.frameworkService = createFrameworkService(true, 'test1', ["test.property": "value1", "test.property2": "value2"])

        response.format='xml'
        params.project='test1'
        request.setAttribute('api_version', 10) //do include <result> wrapper
        controller.apiProjectGet()
        assert response.status==HttpServletResponse.SC_OK

        //XML result has wrapper
        assertEquals 'result', response.xml.name()
        assertEquals 'true', response.xml.'@success'.text()
        assertEquals ApiRequestFilters.API_CURRENT_VERSION.toString(), response.xml.'@apiversion'.text()

        assertEquals 1, response.xml.projects.size()
        assertEquals 0, response.xml.project.size()
        assertEquals 1, response.xml.projects.size()
        assertEquals 1, response.xml.projects.project.size()
        def project = response.xml.projects.project[0]

        //test project element
        assertEquals 'test1', project.name.text()
        assertEquals '', project.description.text()
        assertEquals 0, project.config.size()
    }
    /**
     * apiversion {@literal >=} 11 will result in {@literal <config>} element, if authorized
     */
    @Test
    void apiProjectGet_xml_withconfig_v11(){
        defineBeans {
            apiService(ApiService)
        }
        controller.frameworkService = createFrameworkService(true, 'test1', ["test.property": "value1", "test.property2": "value2"])

        response.format='xml'
        params.project='test1'
        request.setAttribute('api_version', 11) //do not include <result> wrapper
        controller.apiProjectGet()
        assert response.status==HttpServletResponse.SC_OK

        //XML result has wrapper
        assertEquals 'project', response.xml.name()

        assertEquals 0, response.xml.projects.size()
        assertEquals 0, response.xml.project.size()
        def project = response.xml

        //test project element
        assertEquals 'test1', project.name.text()
        assertEquals '', project.description.text()
        assertEquals 1, project.config.size()
        assertEquals 2, project.config.property.size()
        assertEquals 'test.property', project.config.property[0].'@key'.text()
        assertEquals 'value1', project.config.property[0].'@value'.text()
        assertEquals 'test.property2', project.config.property[1].'@key'.text()
        assertEquals 'value2', project.config.property[1].'@value'.text()
    }

    @Test
    void apiProjectGet_json_noconfig() {
        defineBeans {
            apiService(ApiService)
        }
        controller.frameworkService = createFrameworkService(false, 'test1')

        response.format = 'json'
        params.project = 'test1'
        request.setAttribute('api_version', 11) // trigger xml <result> wrapper
        controller.apiProjectGet()
        assert response.status == HttpServletResponse.SC_OK

        def project = response.json

        //test project element
        assertEquals 'test1', project.name
        assertEquals '', project.description
        assertEquals null, project.config
    }
    @Test
    void apiProjectGet_json_withconfig() {
        defineBeans {
            apiService(ApiService)
        }
        controller.frameworkService = createFrameworkService(true, 'test1',['test.property':'value1',
                'test.property2':'value2'])

        response.format = 'json'
        params.project = 'test1'
        request.setAttribute('api_version', 11) // trigger xml <result> wrapper
        controller.apiProjectGet()
        assert response.status == HttpServletResponse.SC_OK

        def project = response.json

        //test project element
        assertEquals 'test1', project.name
        assertEquals '', project.description
        assertEquals(['test.property': 'value1', 'test.property2': 'value2'], project.config)
    }
}
