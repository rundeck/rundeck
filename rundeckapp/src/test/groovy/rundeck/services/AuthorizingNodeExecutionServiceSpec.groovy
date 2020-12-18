/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package rundeck.services

import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.execution.ExecArgList
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.NodeExecutionService
import com.dtolabs.rundeck.core.execution.UnauthorizedException
import org.rundeck.app.authorization.AppAuthContextEvaluator
import spock.lang.Specification
import spock.lang.Unroll

class AuthorizingNodeExecutionServiceSpec extends Specification {
    def "execute without auth"() {
        given:
            def sut = new AuthorizingNodeExecutionService()
            def node = new NodeEntryImpl('testnode1')
            def cmd = ExecArgList.fromStrings(false, 'cmd1')
            def ctx = Mock(ExecutionContext)
            sut.frameworkService = Mock(FrameworkService)
            sut.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator)
            sut.nodeExecutionService = Mock(NodeExecutionService)

            def emptyset = new NodeSetImpl()


        when:
            sut.executeCommand(ctx, cmd, node)
        then:
            1 * ctx.getFrameworkProject() >> 'aProject'
            1 * sut.rundeckAuthContextEvaluator.filterAuthorizedNodes(
                    'aProject',
                    AuthorizingNodeExecutionService.RUN_ACTION_SET,
                    _,
                    _
            ) >> emptyset
            0 * sut.nodeExecutionService.executeCommand(*_)
            UnauthorizedException e = thrown()
            e.failureReason.toString() == 'Unauthorized'
            e.message.contains 'cannot execute on node'
    }

    def "execute with auth"() {
        given:
            def sut = new AuthorizingNodeExecutionService()
            def node = new NodeEntryImpl('testnode1')
            def cmd = ExecArgList.fromStrings(false, 'cmd1')
            def ctx = Mock(ExecutionContext)
            sut.frameworkService = Mock(FrameworkService)
            sut.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator)
            sut.nodeExecutionService = Mock(NodeExecutionService)

            def resultSet = new NodeSetImpl()
            resultSet.putNode(node)


        when:
            sut.executeCommand(ctx, cmd, node)
        then:
            1 * ctx.getFrameworkProject() >> 'aProject'
            1 * sut.rundeckAuthContextEvaluator.filterAuthorizedNodes(
                    'aProject',
                    AuthorizingNodeExecutionService.RUN_ACTION_SET,
                    _,
                    _
            ) >> resultSet
            1 * sut.nodeExecutionService.executeCommand(ctx, cmd, node)
    }


    @Unroll
    def "file copy #action without auth"() {
        given:
            def sut = new AuthorizingNodeExecutionService()
            def node = new NodeEntryImpl('testnode1')
            def cmd = ExecArgList.fromStrings(false, 'cmd1')
            def ctx = Mock(ExecutionContext)
            sut.frameworkService = Mock(FrameworkService)
            sut.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator)
            sut.nodeExecutionService = Mock(NodeExecutionService)

            def emptyset = new NodeSetImpl()


        when:
            sut."$action"(ctx, obj, node, '/a/path')
        then:
            1 * ctx.getFrameworkProject() >> 'aProject'
            1 * sut.rundeckAuthContextEvaluator.filterAuthorizedNodes(
                    'aProject',
                    AuthorizingNodeExecutionService.RUN_ACTION_SET,
                    _,
                    _
            ) >> emptyset
            0 * sut.nodeExecutionService."$action"(*_)
            UnauthorizedException e = thrown()
            e.failureReason.toString() == 'Unauthorized'
            e.message.contains 'cannot copy file to node'

        where:
            obj  | action
            null | 'fileCopyFileStream'
            null | 'fileCopyFile'
    }
    @Unroll
    def "file copy #action with auth"() {
        given:
            def sut = new AuthorizingNodeExecutionService()
            def node = new NodeEntryImpl('testnode1')
            def cmd = ExecArgList.fromStrings(false, 'cmd1')
            def ctx = Mock(ExecutionContext)
            sut.frameworkService = Mock(FrameworkService)
            sut.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator)
            sut.nodeExecutionService = Mock(NodeExecutionService)

            def resultSet = new NodeSetImpl()
            resultSet.putNode(node)


        when:
            sut."$action"(ctx, obj, node, '/a/path')
        then:
            1 * ctx.getFrameworkProject() >> 'aProject'
            1 * sut.rundeckAuthContextEvaluator.filterAuthorizedNodes(
                    'aProject',
                    AuthorizingNodeExecutionService.RUN_ACTION_SET,
                    _,
                    _
            ) >> resultSet
            1 * sut.nodeExecutionService."$action"(*_)

        where:
            obj  | action
            null | 'fileCopyFileStream'
            null | 'fileCopyFile'
    }
}
