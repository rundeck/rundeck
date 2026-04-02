package rundeck.controllers

import com.dtolabs.rundeck.app.api.ApiVersions
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.config.FeatureService
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.data.providers.v1.execution.ReferencedExecutionDataProvider
import org.rundeck.app.data.workflow.ConditionalDefinitionImpl
import org.rundeck.app.data.workflow.ConditionalSetImpl
import org.rundeck.app.data.workflow.ConditionalStep
import org.rundeck.app.data.workflow.WorkflowDataImpl
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.ApiService
import rundeck.services.ConfigurationService
import rundeck.services.FrameworkService
import rundeck.services.NotificationService
import rundeck.services.OrchestratorPluginService
import rundeck.services.PluginService
import rundeck.services.ScheduledExecutionService
import spock.lang.Specification

import javax.servlet.http.HttpServletResponse

/**
 * Tests for ScheduledExecutionController conditional step export validation
 */
class ScheduledExecutionControllerConditionalSpec extends Specification implements ControllerUnitTest<ScheduledExecutionController>, DataTest {

    def setupSpec() {
        mockDomains ScheduledExecution, Execution, Workflow, CommandExec
    }

    def setup() {
        controller.rundeckJobDefinitionManager = Mock(RundeckJobDefinitionManager)
        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
        controller.scheduledExecutionService = Mock(ScheduledExecutionService)
        controller.notificationService = Mock(NotificationService)
        controller.orchestratorPluginService = Mock(OrchestratorPluginService)
        controller.pluginService = Mock(PluginService)
        controller.featureService = Mock(FeatureService)
        controller.apiService = Mock(ApiService)
    }

    def "show action returns error when XML export requested for job with conditional steps"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: '${option.env}', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo test')
        ]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [conditionalStep]

        def se = new ScheduledExecution(
            jobName: 'testJob',
            project: 'testProject',
            workflow: new Workflow().save()
        )
        se.setWorkflowData(workflow)
        se.save()

        controller.rundeckJobDefinitionManager.validateJobForExport(se, 'xml') >> Mock(Validator.Report) {
            isValid() >> false
        }
        controller.scheduledExecutionService.getByIDorUUID(se.id.toString()) >> se
        controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, _) >> Mock(UserAndRolesAuthContext)
        controller.rundeckAuthContextProcessor.authorizeProjectJobAny(_, _, _, _) >> true

        when:
        request.parameters = [id: se.id.toString(), project: 'testProject']
        response.format = 'xml'
        controller.show()

        then:
        response.status == 400
    }

    def "show action allows XML export for job without conditional steps"() {
        given:
        def workflow = new WorkflowDataImpl()
        workflow.steps = [
            new CommandExec(adhocRemoteString: 'echo test1'),
            new CommandExec(adhocRemoteString: 'echo test2')
        ]

        def se = new ScheduledExecution(
            jobName: 'testJob',
            project: 'testProject',
            workflow: new Workflow().save()
        )
        se.setWorkflowData(workflow)
        se.save()

        controller.rundeckJobDefinitionManager.validateJobForExport(se, 'xml') >> Mock(Validator.Report) {
            isValid() >> true
        }
        controller.rundeckJobDefinitionManager.exportAs('xml', [se], _) >> { args ->
            args[2] << '<?xml version="1.0"?><joblist></joblist>'
        }
        controller.scheduledExecutionService.getByIDorUUID(se.id.toString()) >> se
        controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, _) >> Mock(UserAndRolesAuthContext)
        controller.rundeckAuthContextProcessor.authorizeProjectJobAny(_, _, _, _) >> true
        controller.featureService.featurePresent(_) >> false
        controller.frameworkService.getRundeckFramework() >> Mock(IFramework) {
            getFrameworkNodeName() >> "localhost"
        }
        controller.referencedExecutionDataProvider = Mock(ReferencedExecutionDataProvider) {
            parentJobSummaries(_, _) >> []
        }
        controller.configurationService = Mock(ConfigurationService) {
            getString("min.isolation.level") >> "UNCOMMITTED"
        }

        when:
        request.parameters = [id: se.id.toString(), project: 'testProject']
        response.format = 'xml'
        controller.show()

        then:
        response.status == 200
    }

    def "show action allows JSON export for job with conditional steps"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: '${option.env}', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo test')
        ]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [conditionalStep]

        def se = new ScheduledExecution(
            jobName: 'testJob',
            project: 'testProject',
            workflow: new Workflow().save()
        )
        se.setWorkflowData(workflow)
        se.save()

        controller.rundeckJobDefinitionManager.validateJobForExport(se, 'json') >> Mock(Validator.Report) {
            isValid() >> true
        }
        controller.rundeckJobDefinitionManager.exportAs('json', [se], _) >> { args ->
            args[2] << '{"jobs":[]}'
        }
        controller.configurationService = Mock(ConfigurationService) {
            getString("min.isolation.level") >> "UNCOMMITTED"
        }
        controller.referencedExecutionDataProvider = Mock(ReferencedExecutionDataProvider) {
            parentJobSummaries(_, _) >> []
        }
        controller.frameworkService.getRundeckFramework() >> Mock(IFramework) {
            getFrameworkNodeName() >> "localhost"
        }
        controller.scheduledExecutionService.getByIDorUUID(se.id.toString()) >> se
        controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, _) >> Mock(UserAndRolesAuthContext)
        controller.rundeckAuthContextProcessor.authorizeProjectJobAny(_, _, _, _) >> true

        when:
        request.parameters = [id: se.id.toString(), project: 'testProject']
        response.format = 'json'
        controller.show()

        then:
        response.status == 200
    }

    def "apiJobGet returns error when XML format requested for job with conditional steps"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: '${option.env}', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo test')
        ]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [conditionalStep]

        def se = new ScheduledExecution(
            jobName: 'testJob',
            project: 'testProject',
            workflow: new Workflow().save()
        )
        se.setWorkflowData(workflow)
        se.save()

        controller.rundeckJobDefinitionManager.validateJobForExport(se, 'xml') >> Mock(Validator.Report) {
            isValid() >> false
        }
        controller.scheduledExecutionService.getByIDorUUID(se.id.toString()) >> se
        controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, _) >> Mock(UserAndRolesAuthContext)
        controller.rundeckAuthContextProcessor.authorizeProjectJobAll(_, _, _, _) >> true
        controller.frameworkService.isFrameworkProjectDisabled(_) >> false
        controller.apiService.requireApi(_,_) >> true
        controller.apiService.requireExists(_,_,_) >> true

        when:
        request.parameters = [id: se.id.toString()]
        request.api_version = ApiVersions.API_CURRENT_VERSION
        response.format = 'xml'
        controller.apiJobExport()

        then:
        1 * controller.apiService.renderErrorFormat(_, _) >> { args ->
            assert args[1].status == HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE
            assert args[1].code == 'api.error.item.unsupported-format'
        }
    }

    def "apiJobGet allows JSON format for job with conditional steps"() {
        given:
        def condDef = ConditionalDefinitionImpl.fromMap([key: '${option.env}', operator: '==', value: 'prod'])
        def condSet = new ConditionalSetImpl()
        condSet.conditionGroups = [[condDef]]

        def conditionalStep = new ConditionalStep()
        conditionalStep.conditionSet = condSet
        conditionalStep.subSteps = [
            new CommandExec(adhocRemoteString: 'echo test')
        ]

        def workflow = new WorkflowDataImpl()
        workflow.steps = [conditionalStep]

        def se = new ScheduledExecution(
            jobName: 'testJob',
            project: 'testProject',
            workflow: new Workflow().save()
        )
        se.setWorkflowData(workflow)
        se.save()

        controller.rundeckJobDefinitionManager.validateJobForExport(se, 'json') >> Mock(Validator.Report) {
            isValid() >> true
        }
        controller.rundeckJobDefinitionManager.exportAs('json', [se], _) >> { args ->
            args[2] << '{"jobs":[]}'
        }
        controller.scheduledExecutionService.getByIDorUUID(se.id.toString()) >> se
        controller.rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(_, _) >> Mock(UserAndRolesAuthContext)
        controller.rundeckAuthContextProcessor.authorizeProjectJobAll(_, _, _, _) >> true

        when:
        request.parameters = [id: se.id.toString()]
        response.format = 'json'
        controller.apiJobExport()

        then:
        response.status == 200
    }
}

