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

package rundeck.services.execution.logstorage

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.execution.ExecutionNotFound
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileLoader
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileLoaderService
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileManagerService
import groovy.transform.CompileStatic

@CompileStatic
class ResolvedExecutionFileLoaderService
        implements ExecutionFileLoaderService {
    UserAndRolesAuthContext authContext
    AuthorizingExecutionFileLoaderService authorizingExecutionFileLoaderService
    @Delegate ExecutionFileManagerService delegate


    @Override
    ExecutionFileLoader requestFileLoad(final ExecutionReference e, final String filetype, final boolean performLoad)
            throws ExecutionNotFound {
        return authorizingExecutionFileLoaderService.requestFileLoad(authContext, e, filetype, performLoad)
    }
}
