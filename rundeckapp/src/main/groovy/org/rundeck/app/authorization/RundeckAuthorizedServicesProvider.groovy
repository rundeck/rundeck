/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.jobs.JobService
import groovy.transform.CompileStatic
import org.rundeck.app.services.AppService
import org.rundeck.app.services.AuthorizedServicesProvider
import org.rundeck.app.services.Services
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.JobStateService

@CompileStatic
class RundeckAuthorizedServicesProvider implements AuthorizedServicesProvider {
    @Autowired JobStateService jobStateService

    @Override
    Services getServicesWith(final AuthContext authContext) {
        return new AuthedServices(authContext)
    }

    class AuthedServices implements Services {
        final AuthContext authContext

        AuthedServices(final AuthContext authContext) {
            this.authContext = authContext
        }

        @Override
        boolean hasService(final Class<? extends AppService> type) {
            return type == JobService
        }

        @Override
        def <T extends AppService> T getService(final Class<T> type) {
            if (type == JobService) {
                return (T) jobStateService.jobServiceWithAuthContext(authContext)
            }
            throw new IllegalStateException("Required service " + type.getName() + " was not available");
        }
    }
}
