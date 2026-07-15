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
import groovy.util.logging.Slf4j
import org.rundeck.app.authorization.AppAuthContextEvaluator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy

@Slf4j
@CompileStatic
class DirectNodeExecutionService implements NodeExecutionService {
    @Autowired FrameworkService frameworkService
    @Autowired AppAuthContextEvaluator rundeckAuthContextEvaluator
    
    /**
     * Injected Spring-managed NodeExecutionService bean.
     * This will be the bean-replaced RunnerExecutionService when runners are enabled.
     * @Lazy creates a proxy that resolves the bean at method call time, ensuring we get
     * the bean AFTER BeanReplacementPostProcessor has replaced it with RunnerExecutionService.
     * This fixes the Grails 7/Spring Boot 3 initialization order issue where bean replacement
     * happens after dependency injection.
     */
    @Lazy
    @Autowired 
    @Qualifier('rundeckExecutionPluginService') 
    NodeExecutionService nodeExecutionService

    @Override
    NodeExecutorResult executeCommand(final ExecutionContext context, final ExecArgList command, final INodeEntry node)
            throws ExecutionException {
        // Use injected Spring bean instead of framework registry
        // This ensures we get the bean-replaced RunnerExecutionService
        log.info("[DIAG-DIRECT] DirectNodeExecutionService.executeCommand() called for node=${node.nodename}, " +
                 "injected service class=${nodeExecutionService?.getClass()?.name}, " +
                 "service toString=${nodeExecutionService?.toString()}")
        return nodeExecutionService.executeCommand(context, command, node)
    }

    @Override
    String fileCopyFileStream(
            final ExecutionContext context,
            final InputStream input,
            final INodeEntry node,
            final String destinationPath
    ) throws FileCopierException {
        // Use injected Spring bean instead of framework registry
        return nodeExecutionService.fileCopyFileStream(context, input, node, destinationPath)
    }

    @Override
    String fileCopyFile(
            final ExecutionContext context,
            final File file,
            final INodeEntry node,
            final String destinationPath
    ) throws FileCopierException {
        // Use injected Spring bean instead of framework registry
        return nodeExecutionService.fileCopyFile(context, file, node, destinationPath)
    }

    NodeExecutionService nodeExecutionServiceWithAuth(final UserAndRolesAuthContext authContext) {
        return new AuthorizingNodeExecutionService(rundeckAuthContextEvaluator, this, authContext)
    }
}
