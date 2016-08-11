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

package rundeck.services.feature

import grails.test.mixin.TestFor
import rundeck.services.ConfigurationService
import spock.lang.Specification
import spock.lang.Unroll

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(FeatureService)
@Unroll
class FeatureServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "feature enabled via config"() {
        given:
        service.configurationService = Mock(ConfigurationService)

        when:
        def result = service.featurePresent('afeature')

        then:
        result == ispresent
        1 * service.configurationService.getBoolean('feature.*.enabled', false) >> false
        1 * service.configurationService.getBoolean('feature.afeature.enabled', false) >> ispresent

        where:
        ispresent | _
        true      | _
        false     | _
    }
    void "feature enabled via splat"() {
        given:
        service.configurationService = Mock(ConfigurationService)

        when:
        def result = service.featurePresent('afeature')

        then:
        result == ispresent
        1 * service.configurationService.getBoolean('feature.*.enabled', false) >> ispresent
        if(!ispresent) {
            1 * service.configurationService.getBoolean('feature.afeature.enabled', false) >> false
        }

        where:
        ispresent | _
        true      | _
        false     | _
    }
    void "set feature to #ispresent"() {
        given:
        service.configurationService = Mock(ConfigurationService)

        when:
        service.toggleFeature('afeature', ispresent)

        then:
        1 * service.configurationService.setBoolean('feature.afeature.enabled', ispresent)

        where:
        ispresent | _
        true      | _
        false     | _
    }
    void "get feature config"() {
        given:
        service.configurationService = Mock(ConfigurationService)

        when:
        service.getFeatureConfig('afeature')

        then:
        1 * service.configurationService.getConfig('feature.afeature.config')

    }
}
