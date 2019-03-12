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

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.ExecArgList
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionException
import com.dtolabs.rundeck.core.execution.NodeExecutionService
import com.dtolabs.rundeck.core.execution.service.FileCopierException
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
class DirectNodeExecutionService implements NodeExecutionService {
    @Autowired FrameworkService frameworkService

    @Override
    NodeExecutorResult executeCommand(final ExecutionContext context, final ExecArgList command, final INodeEntry node)
            throws ExecutionException {
        def service = frameworkService.getRundeckFramework().getExecutionService()
        return service.executeCommand(context, command, node)
    }

    @Override
    String fileCopyFileStream(
            final ExecutionContext context,
            final InputStream input,
            final INodeEntry node,
            final String destinationPath
    ) throws FileCopierException {
        def service = frameworkService.getRundeckFramework().getExecutionService()
        return service.fileCopyFileStream(context, input, node, destinationPath)
    }

    @Override
    String fileCopyFile(
            final ExecutionContext context,
            final File file,
            final INodeEntry node,
            final String destinationPath
    ) throws FileCopierException {
        def service = frameworkService.getRundeckFramework().getExecutionService()
        return service.fileCopyFile(context, file, node, destinationPath)
    }

    NodeExecutionService nodeExecutionServiceWithAuth(final UserAndRolesAuthContext authContext) {
        return new AuthorizingNodeExecutionService(
                authContext: authContext,
                frameworkService: frameworkService,
                nodeExecutionService: this
        )
    }
}
