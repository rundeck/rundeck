package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.testing.web.controllers.ControllerUnitTest
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.web.WebExceptionHandler
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.access.MissingParameter
import rundeck.ScheduledExecution
import rundeck.error_handling.InvalidParameterException
import rundeck.services.ScheduledExecutionService
import spock.lang.Specification

class AuthorizationsControllerSpec extends Specification implements ControllerUnitTest<AuthorizationsController>  {

    def "verify authorizations/application/{kind} successful response"() {
        given:
        final def kind = 'project'
        final def authorizedAction = 'read'
        final def unAuthorizedAction = 'write'

        final def expectedResource = [type: 'resource', kind: kind]
        final Set<String> expectedActionNames = [authorizedAction, unAuthorizedAction]
        final def expectedContext = AuthorizationUtil.RUNDECK_APP_ENV

        UserAndRolesAuthContext authContext = Mock(UserAndRolesAuthContext) {
            1 * evaluate(Set.of(expectedResource),
                    expectedActionNames,
                    expectedContext) >> Set.of(
                        decisionFor(authorizedAction, true),
                        decisionFor(unAuthorizedAction, false))
        }

        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
            1 * getAuthContextForSubject(_) >> authContext
        }

        request.addParameter('kind', kind)
        request.addParameter('actions', authorizedAction)
        request.addParameter('actions', unAuthorizedAction)
        request.method='GET'

        when:
        controller.appContextAuthorizationsForResourceKind()

        then:
        response.status == 200
        def jsonResponse = response.json
        jsonResponse.authorizationContext.type == 'application'
        jsonResponse.resource.kind == kind
        jsonResponse.actionAuthorizations.size() == 2
        jsonResponse.actionAuthorizations.findAll {it.isAuthorized && it.actionName == authorizedAction}
        jsonResponse.actionAuthorizations.findAll {!it.isAuthorized && it.actionName == unAuthorizedAction}
    }

    def "verify authorizations/application/{kind} throws MissingParameter when no `actions` supplied in query"() {
        given:
        final def kind = 'project'

        request.addParameter('kind', kind)
        request.method='GET'

        controller.rundeckExceptionHandler = Mock(WebExceptionHandler){
            1 * handleException(_, _, _ as MissingParameter) >> true
        }

        expect:
        controller.appContextAuthorizationsForResourceKind()
    }

    def "verify authorizations/application/{type}/{specifier} successful response"() {
        given:
        final def type = 'project_acl'
        final def specifier = 'proj1'
        final def authorizedAction = 'read'
        final def authorizedAction2 = 'write'

        final def expectedResource = [type: 'project_acl', 'name': specifier]
        final Set<String> expectedActionNames = [authorizedAction, authorizedAction2]
        final def expectedContext = AuthorizationUtil.RUNDECK_APP_ENV

        UserAndRolesAuthContext authContext = Mock(UserAndRolesAuthContext) {
            1 * evaluate(Set.of(expectedResource),
                    expectedActionNames,
                    expectedContext) >> Set.of(
                    decisionFor(authorizedAction, true),
                    decisionFor(authorizedAction2, true))
        }

        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
            1 * getAuthContextForSubject(_) >> authContext
        }

        request.addParameter('type', type)
        request.addParameter('specifier', specifier)
        request.addParameter('actions', authorizedAction)
        request.addParameter('actions', authorizedAction2)
        request.method='GET'

        when:
        controller.appContextAuthorizationsForTypeWithSpecifier()

        then:
        response.status == 200
        def jsonResponse = response.json
        jsonResponse.authorizationContext.type == 'application'
        jsonResponse.resource.type == type
        jsonResponse.resource.specifier == specifier
        jsonResponse.actionAuthorizations.size() == 2
        jsonResponse.actionAuthorizations.findAll {it.isAuthorized && it.actionName == authorizedAction}
        jsonResponse.actionAuthorizations.findAll {it.isAuthorized && it.actionName == authorizedAction2}
    }

    def "verify authorizations/application/{type}/{specifier} throws InvalidParameterException when invalid `type` is supplied in path"() {
        given:
        final def type = 'invalid-type'

        request.method='GET'
        request.addParameter('actions', 'read')
        request.addParameter('type', type)
        request.addParameter('specifier', "specifier")

        controller.rundeckExceptionHandler = Mock(WebExceptionHandler){
            1 * handleException(_,_, _ as InvalidParameterException) >> true
        }

        expect:
        controller.appContextAuthorizationsForTypeWithSpecifier()
    }

    def "verify authorizations/projects/{project}/{kind} successful response"() {
        given:
        final def kind = 'job'
        final def authorizedAction = 'read'
        final def unAuthorizedAction = 'write'
        final def projectName = 'proj1'
        final def expectedResource = [type: 'resource', kind: kind, project: projectName]
        final Set<String> expectedActionNames = [authorizedAction, unAuthorizedAction]
        final def expectedContext = AuthorizationUtil.projectContext(projectName)

        UserAndRolesAuthContext authContext = Mock(UserAndRolesAuthContext) {
            1 * evaluate(Set.of(expectedResource),
                    expectedActionNames,
                    expectedContext) >> Set.of(
                    decisionFor(authorizedAction, true),
                    decisionFor(unAuthorizedAction, false))
        }

        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
            1 * getAuthContextForSubjectAndProject(_, projectName) >> authContext
        }

        request.addParameter('project', projectName)
        request.addParameter('kind', kind)
        request.addParameter('actions', authorizedAction)
        request.addParameter('actions', unAuthorizedAction)
        request.method='GET'

        when:
        controller.projectContextAuthorizationsForResourceKind()

        then:
        response.status == 200
        def jsonResponse = response.json
        jsonResponse.authorizationContext.type == 'project'
        jsonResponse.authorizationContext.name == projectName
        jsonResponse.resource.kind == kind
        jsonResponse.actionAuthorizations.size() == 2
        jsonResponse.actionAuthorizations.findAll {it.isAuthorized && it.actionName == authorizedAction}
        jsonResponse.actionAuthorizations.findAll {!it.isAuthorized && it.actionName == unAuthorizedAction}
    }

    def "verify authorizations/project/{project}/{type}/{specifier} successful response"() {
        given:
        final def authorizedAction = 'read'
        final def unAuthorizedAction = 'write'
        final def projectName = 'proj1'
        final def resourceType = 'node'
        final def nodeName = 'node123'
        final def expectedResource = [type: resourceType, name: nodeName, project: projectName]
        final Set<String> expectedActionNames = [authorizedAction, unAuthorizedAction]
        final def expectedContext = AuthorizationUtil.projectContext(projectName)

        UserAndRolesAuthContext authContext = Mock(UserAndRolesAuthContext) {
            1 * evaluate(Set.of(expectedResource),
                    expectedActionNames,
                    expectedContext) >> Set.of(
                    decisionFor(authorizedAction, true),
                    decisionFor(unAuthorizedAction, false))
        }

        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
            1 * getAuthContextForSubjectAndProject(_, projectName) >> authContext
        }

        request.addParameter('project', projectName)
        request.addParameter('type', resourceType)
        request.addParameter('specifier', nodeName)
        request.addParameter('actions', authorizedAction)
        request.addParameter('actions', unAuthorizedAction)
        request.method='GET'

        when:
        controller.projectContextAuthorizationsForTypeWithSpecifier()

        then:
        response.status == 200
        def jsonResponse = response.json
        jsonResponse.authorizationContext.type == 'project'
        jsonResponse.authorizationContext.name == projectName
        jsonResponse.resource.type == resourceType
        jsonResponse.resource.specifier ==nodeName
        jsonResponse.actionAuthorizations.size() == 2
        jsonResponse.actionAuthorizations.findAll {it.isAuthorized && it.actionName == authorizedAction}
        jsonResponse.actionAuthorizations.findAll {!it.isAuthorized && it.actionName == unAuthorizedAction}
    }

    def "verify authorizations/project/{project}/job/{specifier} successful response"() {
        given:
        final def authorizedAction = 'read'
        final def unAuthorizedAction = 'write'
        final def jobId = 'job123'
        final def jobName = 'Name of job123'
        final def groupPath = 'groupPath123'
        final def projectName = 'proj1'
        final def expectedResource = [type: 'job', name: jobName, group: groupPath, uuid: jobId]
        final Set<String> expectedActionNames = [authorizedAction, unAuthorizedAction]
        final def expectedContext = AuthorizationUtil.projectContext(projectName)

        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            1 * getByIDorUUID(jobId) >> Mock(ScheduledExecution) {
                1 * getProject() >> projectName
            }
        }

        UserAndRolesAuthContext authContext = Mock(UserAndRolesAuthContext) {
            1 * evaluate(Set.of(expectedResource),
                    expectedActionNames,
                    expectedContext) >> Set.of(
                    decisionFor(authorizedAction, true),
                    decisionFor(unAuthorizedAction, false))
        }

        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor){
            1 * getAuthContextForSubjectAndProject(_, projectName) >> authContext
            1 * authResourceForJob(_) >> AuthorizationUtil.resource(AuthConstants.TYPE_JOB, [name: jobName, group: groupPath ?: '', uuid: jobId])
        }

        request.addParameter('project', projectName)
        request.addParameter('specifier', jobId)
        request.addParameter('actions', authorizedAction)
        request.addParameter('actions', unAuthorizedAction)
        request.method='GET'

        when:
        controller.projectContextAuthorizationsForJob()

        then:
        response.status == 200
        def jsonResponse = response.json
        jsonResponse.authorizationContext.type == 'project'
        jsonResponse.authorizationContext.name == projectName
        jsonResponse.resource.type == 'job'
        jsonResponse.resource.specifier == jobId
        jsonResponse.actionAuthorizations.size() == 2
        jsonResponse.actionAuthorizations.findAll {it.isAuthorized && it.actionName == authorizedAction}
        jsonResponse.actionAuthorizations.findAll {!it.isAuthorized && it.actionName == unAuthorizedAction}
    }

    def "verify authorizations/project/{project}/job/{specifier} throws InvalidParameterException when job is non-existent"() {
        given:
        final def authorizedAction = 'read'
        final def unAuthorizedAction = 'write'
        final def jobId = 'job123'
        final def projectName = 'proj1'
        final Set<String> expectedActionNames = [authorizedAction, unAuthorizedAction]

        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            1 * getByIDorUUID(jobId) >> null
        }

        request.addParameter('project', projectName)
        request.addParameter('specifier', jobId)
        request.addParameter('actions', authorizedAction)
        request.addParameter('actions', unAuthorizedAction)
        request.method = 'GET'

        controller.rundeckExceptionHandler = Mock(WebExceptionHandler) {
            1 * handleException(_, _, _ as InvalidParameterException) >> true
        }

        expect:
        controller.projectContextAuthorizationsForJob()
    }

    def "verify authorizations/project/{project}/job/{specifier} throws InvalidParameterException when job does not belong to the project specified in path param"() {
        given:
        final def authorizedAction = 'read'
        final def unAuthorizedAction = 'write'
        final def jobId = 'job123'
        final def projectName = 'proj1'
        final Set<String> expectedActionNames = [authorizedAction, unAuthorizedAction]

        controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
            1 * getByIDorUUID(jobId) >> Mock(ScheduledExecution) {
                1 * getProject() >> UUID.randomUUID().toString()
            }
        }

        request.addParameter('project', projectName)
        request.addParameter('specifier', jobId)
        request.addParameter('actions', authorizedAction)
        request.addParameter('actions', unAuthorizedAction)
        request.method = 'GET'

        controller.rundeckExceptionHandler = Mock(WebExceptionHandler) {
            1 * handleException(_, _, _ as InvalidParameterException) >> true
        }

        expect:
        controller.projectContextAuthorizationsForJob()
    }

    private Decision decisionFor(String action, boolean authorized) {
        return Mock(Decision){
            isAuthorized() >> authorized
            getAction() >> action
        }
    }
}
