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

package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authorization.Attribute
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthEvaluator
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.server.AuthContextEvaluatorCacheManager
import rundeck.ScheduledExecution
import spock.lang.Specification
import spock.lang.Unroll


class BaseAuthContextEvaluatorSpec extends Specification {
    @Unroll
    def "application resource type all"() {
        given:
            def sut = new BaseAuthContextEvaluator()
            sut.authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()
            def ctx = Mock(AuthContext)
            Set<Decision> decisionResults = new HashSet<>(
                    decisions.collect {
                        [
                                isAuthorized: { -> it }
                        ] as Decision
                    }
            )

        when:
            def result = sut.authorizeApplicationResourceTypeAll(ctx, 'aType', ['test', 'test2'])
        then:
            1 * ctx.evaluate(
                    [[type: 'resource', kind: 'aType']].toSet(), ['test', 'test2'].toSet(), {
                Attribute attr = it.iterator().next()
                "rundeck:auth:env:application" == attr.property.toString() && "rundeck" == attr.value
            }
            ) >> decisionResults
            result == expect

        where:
            decisions      | expect
            [true]         | true
            [false]        | false
            [true, true]   | true
            [false, true]  | false
            [false, false] | false
    }

    @Unroll
    def "application resource type "() {
        given:
            def sut = new BaseAuthContextEvaluator()
            sut.authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()
            def ctx = Mock(AuthContext)
            Decision decisionResult = [
                    isAuthorized: { -> decisionAuth }
            ] as Decision

        when:
            def result = sut.authorizeApplicationResourceType(ctx, 'aType', 'act1')
        then:
            1 * ctx.evaluate(
                    [type: 'resource', kind: 'aType'], 'act1', {
                Attribute attr = it.iterator().next()
                "rundeck:auth:env:application" == attr.property.toString() && "rundeck" == attr.value
            }
            ) >> decisionResult
            result == expect

        where:
            decisionAuth | expect
            true         | true
            false        | false
    }

    @Unroll
    def "application resource all  #decisions is #expect"() {
        given:
            def sut = new BaseAuthContextEvaluator()
            sut.authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()
            def ctx = Mock(AuthContext)
            Set<Decision> decisionResults = new HashSet<>(
                    decisions.collect {
                        [
                                isAuthorized: { -> it }
                        ] as Decision
                    }
            )
            def resource = [type: 'job', name: 'name1', group: 'blah/blee']

        when:
            def result = sut.authorizeApplicationResourceAll(ctx, resource, ['test', 'test2'])
        then:
            1 * ctx.evaluate(
                    [resource].toSet(), ['test', 'test2'].toSet(), {
                Attribute attr = it.iterator().next()
                "rundeck:auth:env:application" == attr.property.toString() && "rundeck" == attr.value
            }
            ) >> decisionResults
            result == expect

        where:
            decisions      | expect
            [true]         | true
            [false]        | false
            [true, true]   | true
            [false, true]  | false
            [false, false] | false
    }

    @Unroll
    def "application resource any #decisions is #expect"() {
        given:
            def sut = new BaseAuthContextEvaluator()
            sut.authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()
            def ctx = Mock(AuthContext)
            Set<Decision> decisionResults1 = new HashSet<>(
                    [
                            [
                                    isAuthorized: { -> decisions[0] }
                            ] as Decision
                    ]
            )
            Set<Decision> decisionResults2 = new HashSet<>(
                    [
                            [
                                    isAuthorized: { -> decisions[1] }
                            ] as Decision
                    ]
            )
            def resource = [type: 'job', name: 'name1', group: 'blah/blee']

        when:
            def result = sut.authorizeApplicationResourceAny(ctx, resource, ['test', 'test2'])
        then:
            ctx.evaluate(
                    [resource].toSet(), ['test'].toSet(), {
                Attribute attr = it.iterator().next()
                "rundeck:auth:env:application" == attr.property.toString() && "rundeck" == attr.value
            }
            ) >> decisionResults1
            ctx.evaluate(
                    [resource].toSet(), ['test2'].toSet(), {
                Attribute attr = it.iterator().next()
                "rundeck:auth:env:application" == attr.property.toString() && "rundeck" == attr.value
            }
            ) >> decisionResults2
            result == expect

        where:
            decisions      | expect
            [true, true]   | true
            [false, true]  | true
            [true, false]  | true
            [false, false] | false
    }


    @Unroll
    def "application resource"() {
        given:
            def sut = new BaseAuthContextEvaluator()
            sut.authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()
            def ctx = Mock(AuthContext)
            Decision decisionResult = [
                    isAuthorized: { -> decisionAuth }
            ] as Decision

            def resource = [type: 'job', name: 'name1', group: 'blah/blee']
            def action = 'act1'
        when:
            def result = sut.authorizeApplicationResource(ctx, resource, action)
        then:
            1 * ctx.evaluate(
                    resource, action, {
                Attribute attr = it.iterator().next()
                "rundeck:auth:env:application" == attr.property.toString() && "rundeck" == attr.value
            }
            ) >> decisionResult
            result == expect

        where:
            decisionAuth | expect
            true         | true
            false        | false
    }


    @Unroll
    def "project resource all  #decisions is #expect"() {
        given:
            def sut = new BaseAuthContextEvaluator()
            sut.authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()
            def ctx = Mock(AuthContext)
            Set<Decision> decisionResults = new HashSet<>(
                    decisions.collect {
                        [
                                isAuthorized: { -> it }
                        ] as Decision
                    }
            )
            def resource = [type: 'job', name: 'name1', group: 'blah/blee']
            def project = 'AProject'

        when:
            def result = sut.authorizeProjectResourceAll(ctx, resource, ['test', 'test2'], project)
        then:
            1 * ctx.evaluate(
                    [resource].toSet(), ['test', 'test2'].toSet(), {
                Attribute attr = it.iterator().next()
                "rundeck:auth:env:project" == attr.property.toString() && project == attr.value
            }
            ) >> decisionResults
            result == expect

        where:
            decisions      | expect
            [true]         | true
            [false]        | false
            [true, true]   | true
            [false, true]  | false
            [false, false] | false
    }

    @Unroll
    def "project resource"() {
        given:
            def sut = new BaseAuthContextEvaluator()
            sut.authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()
            def ctx = Mock(AuthContext)
            Decision decisionResult = [
                    isAuthorized: { -> decisionAuth }
            ] as Decision

            def resource = [type: 'job', name: 'name1', group: 'blah/blee']
            def action = 'act1'
            def project = 'AProject'
        when:
            def result = sut.authorizeProjectResource(ctx, resource, action, project)
        then:
            1 * ctx.evaluate(
                    resource, action, {
                Attribute attr = it.iterator().next()
                "rundeck:auth:env:project" == attr.property.toString() && project == attr.value
            }
            ) >> decisionResult
            result == expect

        where:
            decisionAuth | expect
            true         | true
            false        | false
    }

    @Unroll
    def "project resources"() {
        given:
            def sut = new BaseAuthContextEvaluator()
            sut.authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()
            def ctx = Mock(AuthContext)
            Set<Decision> decisionResults = new HashSet<>(
                    decisions.collect {
                        [
                                isAuthorized: { -> it }
                        ] as Decision
                    }
            )

            Map<String, String> resource = [type: 'job', name: 'name1', group: 'blah/blee']
            Map<String, String> resource2 = [type: 'job', name: 'name2', group: 'blah2/blee']
            def resources = new HashSet<Map<String, String>>([resource, resource2])
            def action = 'act1'
            def project = 'AProject'
        when:
            def result = sut.authorizeProjectResources(ctx, resources, [action].toSet(), project)
        then:
            1 * ctx.evaluate(
                    resources, [action].toSet(), {
                Attribute attr = it.iterator().next()
                "rundeck:auth:env:project" == attr.property.toString() && project == attr.value
            }
            ) >> decisionResults
            result == decisionResults

        where:
            decisions      | _
            [true, true]   | _
            [false, true]  | _
            [false, false] | _
    }


    @Unroll
    def "testAuthorizeProjectJobAll success"() {
        given:
            def test = new BaseAuthContextEvaluator()
            def auth = Mock(AuthContext)
            Set<Decision> decisionResults = makeDecisions(decisions)
            test.authContextEvaluatorCacheManager=Mock(AuthEvaluator){
                1 * evaluate(
                    auth,
                    [['name':'name1', 'type':'job', 'uuid':'8b411ae8-8931-4db9-b12e-8d29a6fec43b', 'group':'blah/blee']].toSet(),
                    ['test'].toSet(),
                    'testProject'
                ) >> decisionResults
                0 * _(*_)
            }

            ScheduledExecution job = new ScheduledExecution(
                jobName: 'name1',
                groupPath: 'blah/blee',
                uuid: '8b411ae8-8931-4db9-b12e-8d29a6fec43b'
            )
        when:
            def result = test.authorizeProjectJobAll(auth, job, ['test'], 'testProject')
        then:
            result == authorized
        where:
            decisions      | authorized
            [true]         | true
            [false]        | false
            [true, false]  | false
            [false, false] | false
            [true, true]   | true
    }

    public HashSet<Decision> makeDecisions(List<Boolean> decisions) {
        new HashSet<>(
            decisions.collect { val ->
                Stub(Decision) {
                    isAuthorized() >> val
                }
            }
        )
    }


    def testAuthResourceForJob(){
        given:
            def test = new BaseAuthContextEvaluator()
        expect:

            test.authResourceForJob(new ScheduledExecution(jobName: 'name1', groupPath: 'blah/blee', uuid: '8b411ae8-8931-4db9-b12e-8d29a6fec43b'))==
            [type: 'job', name: 'name1', group: 'blah/blee', uuid: '8b411ae8-8931-4db9-b12e-8d29a6fec43b']

            test.authResourceForJob(new ScheduledExecution(jobName: 'name1', groupPath: 'blah', uuid: '8b411ae8-8931-4db9-b12e-8d29a6fec43b'))==

            [type: 'job', name: 'name1', group: 'blah', uuid: '8b411ae8-8931-4db9-b12e-8d29a6fec43b']

            test.authResourceForJob(new ScheduledExecution(jobName: 'name1', groupPath: '', uuid: '8b411ae8-8931-4db9-b12e-8d29a6fec43b'))==

            [type: 'job', name: 'name1', group: '', uuid: '8b411ae8-8931-4db9-b12e-8d29a6fec43b']

            test.authResourceForJob(new ScheduledExecution(jobName: 'name1', groupPath: null, uuid: '8b411ae8-8931-4db9-b12e-8d29a6fec43b'))==

            [type: 'job', name: 'name1', group: '', uuid: '8b411ae8-8931-4db9-b12e-8d29a6fec43b']

    }

    def testAuthResourceForJobParams() {

        given:
            def test = new BaseAuthContextEvaluator()
        expect:
            test.authResourceForJob(name, group, id) == expected
        where:
            group       | expected
            'blah/blee' | [type: 'job', name: 'name1', group: 'blah/blee', uuid: '8b411ae8-8931-4db9-b12e-8d29a6fec43b']
            'blah'      | [type: 'job', name: 'name1', group: 'blah', uuid: '8b411ae8-8931-4db9-b12e-8d29a6fec43b']
            ''          | [type: 'job', name: 'name1', group: '', uuid: '8b411ae8-8931-4db9-b12e-8d29a6fec43b']
            null        | [type: 'job', name: 'name1', group: '', uuid: '8b411ae8-8931-4db9-b12e-8d29a6fec43b']
            name = 'name1'
            id = '8b411ae8-8931-4db9-b12e-8d29a6fec43b'
    }

}
