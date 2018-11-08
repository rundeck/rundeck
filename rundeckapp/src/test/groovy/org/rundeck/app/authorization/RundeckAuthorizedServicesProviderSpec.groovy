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

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.jobs.JobService
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree
import org.rundeck.app.spi.Services
import org.rundeck.app.spi.ServicesProvider
import rundeck.services.JobStateService
import rundeck.services.StorageService
import spock.lang.Specification
import spock.lang.Unroll

class RundeckAuthorizedServicesProviderSpec extends Specification {
    def "get services with auth context"() {
        given:
            def sut = new RundeckAuthorizedServicesProvider()
            sut.baseServices = Mock(ServicesProvider)
            sut.jobStateService = Mock(JobStateService)
            sut.storageService = Mock(StorageService)
            def auth = Mock(UserAndRolesAuthContext)

        when:
            def result = sut.getServicesWith(auth)
        then:
            result != null

    }

    @Unroll
    def "hasServices is true for #service"() {
        given:
            def sut = new RundeckAuthorizedServicesProvider()
            sut.baseServices = Mock(ServicesProvider) {
                getServices() >> Mock(Services)
            }
            sut.jobStateService = Mock(JobStateService)
            sut.storageService = Mock(StorageService)
            def auth = Mock(UserAndRolesAuthContext)
            def svcs = sut.getServicesWith(auth)

        expect:
            svcs != null
            svcs.hasService(service)
        where:
            service        | _
            KeyStorageTree | _
            JobService     | _
    }

    @Unroll
    def "getService for #service"() {
        given:
            def auth = Mock(UserAndRolesAuthContext)
            def jobServiceMock = Mock(JobService)
            def keyStorageMock = Mock(KeyStorageTree)
            def resultServices = [
                (KeyStorageTree): keyStorageMock,
                (JobService)    : jobServiceMock
            ]

            def sut = new RundeckAuthorizedServicesProvider()
            sut.baseServices = Mock(ServicesProvider) {
                getServices() >> Mock(Services)
            }
            sut.jobStateService = Mock(JobStateService) {
                jobServiceWithAuthContext(auth) >> jobServiceMock
            }
            sut.storageService = Mock(StorageService) {
                storageTreeWithContext(auth) >> keyStorageMock
            }
            def svcs = sut.getServicesWith(auth)

        when:
            def result = svcs.getService(service)

        then:
            result == resultServices[service]

        where:
            service        | _
            KeyStorageTree | _
            JobService     | _
    }
}
