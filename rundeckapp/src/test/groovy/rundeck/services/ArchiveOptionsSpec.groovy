/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

import spock.lang.Specification

/**
 * Created by greg on 1/11/16.
 */
class ArchiveOptionsSpec extends Specification {
    def "parseExecutionsIds single string"() {
        given:
        def opts = new ArchiveOptions()
        when:
        opts.parseExecutionsIds('123,456')
        then:
        opts.executionIds == ['123', '456'] as Set
    }
    def "parseExecutionsIds string list"() {
        given:
        def opts = new ArchiveOptions()
        when:
        opts.parseExecutionsIds(['123','456'])
        then:
        opts.executionIds == ['123', '456'] as Set
    }
}
