package rundeck.services

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.execution.ExecArgList
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import org.rundeck.app.authorization.AppAuthContextEvaluator
import org.rundeck.core.auth.AuthConstants
import spock.lang.Specification

class DirectNodeExecutionServiceSpec extends Specification {
    def "node exec with auth"() {
        given:
            def sut = new DirectNodeExecutionService()
            def authContext = Mock(UserAndRolesAuthContext)
            def ctx = Mock(ExecutionContext)
            def command = ExecArgList.fromStrings(false, ['asdf'], false)
            def node = Stub(INodeEntry) {
                getNodename() >> 'aNode'
            }
            def authorizedSet = new NodeSetImpl([aNode: node])

            sut.frameworkService = Mock(FrameworkService)
            sut.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator)

            def expectResult = Mock(NodeExecutorResult)
        when:
            def result = sut.nodeExecutionServiceWithAuth(authContext)
        then:
            result instanceof AuthorizingNodeExecutionService
        when:

            def result2 = result.executeCommand(ctx, command, node)

        then:
            result2 == expectResult
            1 * ctx.getFrameworkProject() >> 'aProject'
            1 * sut.rundeckAuthContextEvaluator.filterAuthorizedNodes(
                'aProject',
                [AuthConstants.ACTION_RUN].toSet(),
                { INodeSet it -> it.nodeNames.contains('aNode') },
                authContext
            ) >> authorizedSet
            0 * sut.rundeckAuthContextEvaluator._(*_)
            1 * sut.frameworkService.getRundeckFramework() >> Mock(IFramework) {
                1 * getExecutionService() >> Mock(com.dtolabs.rundeck.core.execution.ExecutionService) {
                    1 * executeCommand(ctx, command, node) >> expectResult
                }
            }

    }
}
