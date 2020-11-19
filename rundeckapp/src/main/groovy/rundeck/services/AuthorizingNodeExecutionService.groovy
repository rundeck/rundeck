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

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import org.rundeck.app.authorization.AppAuthContextEvaluator
import org.rundeck.core.auth.AuthConstants
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.execution.ExecArgList
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionException
import com.dtolabs.rundeck.core.execution.NodeExecutionService
import com.dtolabs.rundeck.core.execution.UnauthorizedException
import com.dtolabs.rundeck.core.execution.service.FileCopierException
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import groovy.transform.CompileStatic

/**
 * Applies authorization checks before running command execution
 */
@CompileStatic
class AuthorizingNodeExecutionService implements NodeExecutionService {
    public static final Set<String> RUN_ACTION_SET =
            Collections.unmodifiableSet(new HashSet<String>([AuthConstants.ACTION_RUN]))
    FrameworkService frameworkService
    AppAuthContextEvaluator rundeckAuthContextEvaluator
    NodeExecutionService nodeExecutionService
    UserAndRolesAuthContext authContext


    private boolean authCheckNode(String project, AuthContext auth, INodeEntry node) {
        def nodes = new NodeSetImpl()
        nodes.putNode(node)
        def result = rundeckAuthContextEvaluator.filterAuthorizedNodes(project, RUN_ACTION_SET, nodes, auth)
        return result.nodeNames.contains(node.getNodename())
    }


    @Override
    NodeExecutorResult executeCommand(
            final ExecutionContext context,
            final ExecArgList command,
            final INodeEntry node
    ) throws ExecutionException {
        if (!authCheckNode(context.getFrameworkProject(), authContext, node)) {
            throw new UnauthorizedException("Unauthorized: cannot execute on node: " + node.getNodename())
        }
        return nodeExecutionService.executeCommand(context, command, node)
    }

    @Override
    String fileCopyFileStream(
            final ExecutionContext context,
            final InputStream input,
            final INodeEntry node,
            final String destinationPath
    ) throws FileCopierException,ExecutionException {
        if (!authCheckNode(context.getFrameworkProject(), authContext, node)) {
            throw new UnauthorizedException("Unauthorized: cannot copy file to node: " + node.getNodename())
        }
        return nodeExecutionService.fileCopyFileStream(context, input, node, destinationPath)
    }

    @Override
    String fileCopyFile(
            final ExecutionContext context,
            final File file,
            final INodeEntry node,
            final String destinationPath
    ) throws FileCopierException,ExecutionException {
        if (!authCheckNode(context.getFrameworkProject(), authContext, node)) {
            throw new UnauthorizedException("Unauthorized: cannot copy file to node: " + node.getNodename())
        }
        return nodeExecutionService.fileCopyFile(context, file, node, destinationPath)
    }
}
