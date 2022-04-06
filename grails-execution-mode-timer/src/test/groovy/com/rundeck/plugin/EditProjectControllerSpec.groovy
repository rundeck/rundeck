package com.rundeck.plugin

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.testing.web.controllers.ControllerUnitTest
import org.rundeck.core.auth.AuthConstants
import spock.lang.Specification
import spock.lang.Unroll

import javax.security.auth.Subject
import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authorization.SubjectAuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext

class EditProjectControllerSpec extends Specification implements ControllerUnitTest<EditProjectController> {

    def setup() {
    }

    def cleanup() {
    }

    def "test getExecutionLater auth"(){
        given:
        String projectName = "Test"

        controller.updateModeProjectService = Mock(UpdateModeProjectService){
            getScheduleExecutionLater(_,_)>>[executions:[active:false]]
        }
        controller.frameworkService = new MockFrameworkService()

        controller.rundeckAuthContextProcessor = Mock(AuthContextProcessor){
            1 * getAuthContextForSubjectAndProject(_, projectName)>>Mock(UserAndRolesAuthContext)
            1 * authResourceForProject(projectName)>>[:]
            1 * authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            0*_(*_)
        }

        when:
        controller.getExecutionLater(projectName)

        then:

        response.json  != null
        response.json.executions  != null
        response.json.executions  == [active:false]

    }

    def "test getNextExecutionChangeStatus auth"(){
        given:
        String projectName = "Test"

        controller.updateModeProjectService = Mock(UpdateModeProjectService){
            getProjectModeChangeStatus(_, _)>> [active:false]
        }
        controller.frameworkService = new MockFrameworkService()

        controller.rundeckAuthContextProcessor = Mock(AuthContextProcessor){
            1 * getAuthContextForSubjectAndProject(_, projectName)>>Mock(UserAndRolesAuthContext)
            1 * authResourceForProject(projectName)>>[:]
            1 * authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
            0*_(*_)
        }

        when:
        controller.getNextExecutionChangeStatus(projectName)

        then:

        response.json  != null
        response.json.execution  != null
        response.json.execution  == [active:false]
        response.json.schedule  != null
        response.json.schedule  == [active:false]

    }

    def "test api apiProjectEnableLater auth"(){
        given:
        String project = "TestProject"
        controller.frameworkService = new MockFrameworkService()

            controller.rundeckAuthContextProcessor = Mock(AuthContextProcessor){
                1 * getAuthContextForSubjectAndProject(_, project)>>Mock(UserAndRolesAuthContext)
                1 * authResourceForProject(project)>>[:]
                1 * authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> false
                0*_(*_)
            }
        controller.apiService = new MockApiService(requireVersion: true)
        controller.updateModeProjectService = Mock(UpdateModeProjectService)

        when:
        request.method = "POST"
        request.addHeader('accept', 'application/json')
        controller.apiProjectEnableLater(project)

        then:

        response.status == 403
    }

    def "test api apiProjectDisableLater auth"(){
        given:
        String project = "TestProject"
        controller.frameworkService = new MockFrameworkService()
            controller.rundeckAuthContextProcessor = Mock(AuthContextProcessor){
                1 * getAuthContextForSubjectAndProject(_, project)>>Mock(UserAndRolesAuthContext)
                1 * authResourceForProject(project)>>[:]
                1 * authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> false
                0*_(*_)
            }
        controller.apiService = new MockApiService(requireVersion: true)
        controller.updateModeProjectService = Mock(UpdateModeProjectService)

        when:
        request.method = "POST"
        request.addHeader('accept', 'application/json')
        controller.apiProjectDisableLater(project)

        then:

        response.status == 403
    }

    @Unroll
    def "test api apiProjectEnableLater method"(){
        given:
        String project = "TestProject"
        controller.frameworkService = new MockFrameworkService()

            controller.rundeckAuthContextProcessor = Mock(AuthContextProcessor){
                invocations * getAuthContextForSubjectAndProject(_, project)>>Mock(UserAndRolesAuthContext)
                invocations * authResourceForProject(project)>>[:]
                invocations * authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
                0*_(*_)
            }
        controller.apiService = new MockApiService(requireVersion: true)
        controller.updateModeProjectService = Mock(UpdateModeProjectService)


        when:
        request.method = method
        request.addHeader('accept', 'application/json')
        controller.apiProjectEnableLater(project)

        then:

        response.status == statusCode

        where:
            method   | statusCode | invocations
            'POST'   | 400        | 1
            'GET'    | 405        | 0
            'PUT'    | 405        | 0
            'DELETE' | 405        | 0

    }

    @Unroll
    def "test api apiProjectDisableLater method"(){
        given:
        String project = "TestProject"
        controller.frameworkService = new MockFrameworkService()

            controller.rundeckAuthContextProcessor = Mock(AuthContextProcessor){
                invocations * getAuthContextForSubjectAndProject(_, project)>>Mock(UserAndRolesAuthContext)
                invocations * authResourceForProject(project)>>[:]
                invocations * authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
                0*_(*_)
            }
        controller.apiService = new MockApiService(requireVersion: true)
        controller.updateModeProjectService = Mock(UpdateModeProjectService)


        when:
        request.method = method
        request.addHeader('accept', 'application/json')
        controller.apiProjectDisableLater(project)

        then:

        response.status == statusCode

        where:
            method   | statusCode | invocations
            'POST'   | 400        | 1
            'GET'    | 405        | 0
            'PUT'    | 405        | 0
            'DELETE' | 405        | 0

    }

    def "test api apiProjectDisableLater test"(){
        given:
        String project = "TestProject"
        Properties properties = new Properties()
        properties.put("project.disable.executions","false")
        properties.put("project.disable.schedule","false")

        def rundeckProject = Mock(IRundeckProject){
            getProjectProperties() >> properties
        }

        MockFrameworkService mockFrameworkService = new MockFrameworkService()

            controller.rundeckAuthContextProcessor = Mock(AuthContextProcessor){
                1 * getAuthContextForSubjectAndProject(_, project)>>Mock(UserAndRolesAuthContext)
                1 * authResourceForProject(project)>>[:]
                1 * authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
                0*_(*_)
            }
        mockFrameworkService.setRundeckProject(rundeckProject)

        controller.frameworkService = mockFrameworkService
        controller.apiService = new MockApiService(requireVersion: true)
        controller.updateModeProjectService = Mock(UpdateModeProjectService)

        when:
        request.method = 'POST'
        request.content = body
        request.addHeader('accept', 'application/json')
        controller.apiProjectDisableLater(project)

        then:

        saveCall*controller.updateModeProjectService.saveExecutionLaterSettings(project, _)>>saved
        response.json  != null
        response.json  == [msg:msg, saved:saved]
        response.status == responseStatus

        where:
        body                                                | saved     | responseStatus | msg                                   | saveCall
        '{"type":"executions","value": "3m"}'.bytes         | true      | 200            | "Project Execution Mode Later saved"  | 1
        '{"type":"schedule","value": "30m"}'.bytes          | true      | 200            | "Project Execution Mode Later saved"  | 1
        '{"type":"executions","value": "3m"}'.bytes         | false     | 200            | "No changed found"                    | 1
        '{"type":"schedule","value": "30m"}'.bytes          | false     | 200            | "No changed found"                    | 1
        '{"type":"sdadasdsa","value": "3m"}'.bytes          | false     | 400            | "Format was not valid, the attribute type must be set with the proper value(executions or schedule)."  | 0
        '{"value": "3m"}'.bytes                             | false     | 400            | "Format was not valid, the attribute type must be set (executions or schedule)."  | 0
        '{"type":"executions","value": "badvalue"}'.bytes   | false     | 400            | "Format was not valid, the attribute value is not set properly. Use something like: 3m, 1h, 3d"  | 0
        '{"type":"schedule"}'.bytes                         | false     | 400            | "Format was not valid, the attribute value must be set."  | 0
        'badvalue'.bytes                                    | false     | 400            | "Format was not valid, the request must be a json object with the format: {\"type\":\"<executions|schedule>\",\"value\":\"<timeExpression>\"}"  | 0
        null                                                | false     | 400            | "Format was not valid, the request must be a json object with the format: {\"type\":\"<executions|schedule>\",\"value\":\"<timeExpression>\"}"  | 0

    }

    def "test api apiProjectEnableLater test"(){
        given:
        String project = "TestProject"
        Properties properties = new Properties()
        properties.put("project.disable.executions","true")
        properties.put("project.disable.schedule","true")

        def rundeckProject = Mock(IRundeckProject){
            getProjectProperties() >> properties
        }

        MockFrameworkService mockFrameworkService = new MockFrameworkService()

            controller.rundeckAuthContextProcessor = Mock(AuthContextProcessor){
                1 * getAuthContextForSubjectAndProject(_, project)>>Mock(UserAndRolesAuthContext)
                1 * authResourceForProject(project)>>[:]
                1 * authorizeApplicationResourceAny(_, _, [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]) >> true
                0*_(*_)
            }
        mockFrameworkService.setRundeckProject(rundeckProject)

        controller.frameworkService = mockFrameworkService
        controller.apiService = new MockApiService(requireVersion: true)
        controller.updateModeProjectService = Mock(UpdateModeProjectService)

        when:
        request.method = 'POST'
        request.content = body
        request.addHeader('accept', 'application/json')
        controller.apiProjectEnableLater(project)

        then:

        saveCall*controller.updateModeProjectService.saveExecutionLaterSettings(project, _)>>saved
        response.json  != null
        response.json  == [msg:msg, saved:saved]
        response.status == responseStatus

        where:
        body                                                | saved     | responseStatus | msg                                   | saveCall
        '{"type":"executions","value": "3m"}'.bytes         | true      | 200            | "Project Execution Mode Later saved"  | 1
        '{"type":"schedule","value": "30m"}'.bytes          | true      | 200            | "Project Execution Mode Later saved"  | 1
        '{"type":"executions","value": "3m"}'.bytes         | false     | 200            | "No changed found"                    | 1
        '{"type":"schedule","value": "30m"}'.bytes          | false     | 200            | "No changed found"                    | 1
        '{"type":"sdadasdsa","value": "3m"}'.bytes          | false     | 400            | "Format was not valid, the attribute type must be set with the proper value(executions or schedule)."  | 0
        '{"value": "3m"}'.bytes                             | false     | 400            | "Format was not valid, the attribute type must be set (executions or schedule)."  | 0
        '{"type":"executions","value": "badvalue"}'.bytes   | false     | 400            | "Format was not valid, the attribute value is not set properly. Use something like: 3m, 1h, 3d"  | 0
        '{"type":"schedule"}'.bytes                         | false     | 400            | "Format was not valid, the attribute value must be set."  | 0
        'badvalue'.bytes                                    | false     | 400            | "Format was not valid, the request must be a json object with the format: {\"type\":\"<executions|schedule>\",\"value\":\"<timeExpression>\"}"  | 0
        null                                                | false     | 400            | "Format was not valid, the request must be a json object with the format: {\"type\":\"<executions|schedule>\",\"value\":\"<timeExpression>\"}"  | 0

    }

}

class MockFrameworkService{

    String serverUUID
    IRundeckProject rundeckProject

    def projectList
    String frameworkNodeName
    Map frameworkPropertiesMap = [:]

    Map frameworkProjectsTestData = [:]

    def getFrameworkProject(String name) {
        if(rundeckProject){
            return rundeckProject
        }
        frameworkProjectsTestData[name]
    }

    IRundeckProject getRundeckProject() {
        return rundeckProject
    }

    void setRundeckProject(IRundeckProject rundeckProject) {
        this.rundeckProject = rundeckProject
    }

    def updateFrameworkProjectConfig(String project,Properties properties, Set<String> removePrefixes){
        [success:true]
    }

    boolean isClusterModeEnabled() {
        serverUUID==null?false:true
    }

    def projectNames(){
        projectList
    }

}

