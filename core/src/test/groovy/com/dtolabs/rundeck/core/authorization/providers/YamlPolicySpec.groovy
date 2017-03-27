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

package com.dtolabs.rundeck.core.authorization.providers

import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import spock.lang.Specification

/**
 * Created by greg on 7/24/15.
 */
class YamlPolicySpec extends Specification {


    def "forced context missing context"(){
        given:
        def ctx = AuthorizationUtil.projectContext("monkey")
        def policy = YamlPolicy.createYamlPolicy(
                ctx,
                [
                        description:'',
                        by: [username: 'bob'],
                        for: [resource: [[allow: ['action']]]],
                ],
                "test1",
                1,
                null
        )
        def rules=policy.getRuleSet().getRules()

        expect:
        rules.size()==1
        rules.first().environment.key=='project'
        rules.first().environment.value=='monkey'
    }
    def "forced context any context"(){
        given:
        def ctx = AuthorizationUtil.projectContext("monkey")

        when:
        def policy = YamlPolicy.createYamlPolicy(
                ctx,
                [
                        description:'',
                        by: [username: 'bob'],
                        for: [resource: [[allow: ['action']]]],
                        context: ctxMap
                ],
                "test1",
                1,
                null
        )

        then:
        AclPolicySyntaxException e = thrown()
        e.message.contains('Context section should not be specified, it is already set to: {project=monkey}')

        where:
        ctxMap                   | _
        [project: 'dolphin']     | _
        [project: 'test1']       | _
        [project: '.*']          | _
        [application: 'rundeck'] | _
    }
}
