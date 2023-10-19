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

package com.dtolabs.rundeck.core.authorization

import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext
import spock.lang.Specification
import spock.lang.Unroll

import javax.security.auth.Subject
import java.security.Principal

/**
 * Created by greg on 7/17/15.
 */
class RuleEvaluatorSpec extends Specification {

    static class Username implements Principal{
        String name

        Username(final String name) {
            this.name = name
        }
    }
    static class Group implements Principal{
        String name

        Group(final String name) {
            this.name = name
        }
    }

    static class Urn implements Principal{
        String name

        Urn(final String name) {
            this.name = name
        }
    }

    Subject basicSubject(final String user, final String... groups) {
        def subject = new Subject()
        subject.principals<< new Username(user)
        subject.principals.addAll(groups.collect{new Group(it)})
        subject
    }
    Subject urnSubject(final String project) {
        def subject = new Subject()
        subject.principals<< new Urn("project:" + project)
        subject
    }
    def Authorization newRuleEvaluator(AclRuleSet ruleSet) {
        RuleEvaluator.createRuleEvaluator(ruleSet, TypedSubject.aclSubjectCreator(Username, Group, Urn))
    }

    def "test allow project context regex"(){
        given:
        Authorization eval = newRuleEvaluator(basicProjectRegexRules())
        when:
        def projectContext = [new Attribute(AuthorizationUtil.PROJECT_BASE_URI,"aproject")] as Set
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("bob","admin","user"),
                "EXECUTE",
                projectContext
        )
        then:
        result!=null
        result.isAuthorized()
        "EXECUTE"==result.action
    }
    def "test allow"(){
        given:
        Authorization eval = newRuleEvaluator(basicRules())
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("bob","admin","user"),
                "EXECUTE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result!=null
        result.isAuthorized()
        "EXECUTE"==result.action
    }
    def "test reject wrong type"(){
        given:
        Authorization eval = newRuleEvaluator(basicRules())
        when:
        def result = eval.evaluate(
                [
                        type   : 'boj',
                        jobName: 'bob'
                ],
                basicSubject("bob", "admin", "user"),
                "EXECUTE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result!=null
        !result.isAuthorized()
        "EXECUTE"==result.action
    }
    def "test reject wrong name"(){
        given:
        Authorization eval = newRuleEvaluator(basicRules())
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'sam'
                ],
                basicSubject("bob", "admin", "user"),
                "EXECUTE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result!=null
        !result.isAuthorized()
        "EXECUTE"==result.action
    }
    def "test reject wrong action"(){
        given:
        Authorization eval = newRuleEvaluator(basicRules())
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("bob","admin","user"),
                "TWIST",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result!=null
        !result.isAuthorized()
        "TWIST"==result.action
    }
    def "test reject wrong group"(){
        given:
        Authorization eval = newRuleEvaluator(basicRules())
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("bob","zadmin","user"),
                "EXECUTE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result!=null
        !result.isAuthorized()
        "EXECUTE"==result.action
    }
    def "test reject null rule"(){
        given:
        def rulelist = [
                new Rule(
                        sourceIdentity: "test1",
                        description: "bob job allow exec, deny delete for admin group",
                        equalsResource: [
                                jobName: null
                        ],
                        resourceType: 'job',
                        regexMatch: false,
                        containsMatch: false,
                        equalsMatch: true,
                        username: null,
                        group: 'admin',
                        allowActions: ['EXECUTE'] as Set,
                        denyActions: ['DELETE'] as Set,
                        by: true,
                        environment: BasicEnvironmentalContext.staticContextFor("application", "rundeck")
                ),
        ] as Set
        def rules=new AclRuleSetImpl(rulelist)
        Authorization eval = newRuleEvaluator(rules)
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("bob","admin","user"),
                "EXECUTE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result!=null
        !result.isAuthorized()
        "EXECUTE"==result.action
    }
    def "test deny"(){
        given:
        Authorization eval = newRuleEvaluator(basicRules())
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("bob","admin","user"),
                "DELETE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result!=null
        !result.isAuthorized()
        "DELETE"==result.action
    }

    def "test deny with other rules"() {
        given:
        Authorization eval = newRuleEvaluator(basicRules2())
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("bob","admin","user"),
                "DELETE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result!=null
        !result.isAuthorized()
        "DELETE"==result.action
    }

    def "test allow with deny"() {
        given:
        Authorization eval = newRuleEvaluator(basicRules3())
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("bob", "admin", "user"),
                "DELETE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result != null
        !result.isAuthorized()
        result.action == "DELETE"
        result.explain().code == Explanation.Code.REJECTED_DENIED
    }

    def "test multiple match clauses"() {
        given:
        Authorization eval = newRuleEvaluator(basicRules3())
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("bob", "admin", "user"),
                "DELETE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result != null
        !result.isAuthorized()
        "DELETE" == result.action
    }

    @Unroll
    def "evaluate actions"() {
        when:
        def rules = allowed.collect {
            [
                    equalsResource: [
                            jobName: 'bob'
                    ],
                    resourceType  : 'job',
                    allowActions  : it as Set,
                    denyActions   : [] as Set,
            ]
        }
        def rules2 = denied.collect {
            [
                    equalsResource: [
                            jobName: 'bob'
                    ],
                    resourceType  : 'job',
                    allowActions  : [] as Set,
                    denyActions   : it as Set,
            ]
        }
        def ruleset = rules + rules2
        Authorization eval = newRuleEvaluator(basicRulesFromList(ruleset))
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("bob", "admin", "user"),
                tested,
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result != null
        result.explain().code == code
        result.isAuthorized() == isauthorized
        result.action == tested

        where:
        allowed                 | denied                  | tested    | isauthorized | code
        []                      | []                      |
                'EXECUTE'                                             |
                false                                                                |
                Explanation.Code.REJECTED_NO_SUBJECT_OR_ENV_FOUND
        [['*']]                 | []                      | 'EXECUTE' | true         | Explanation.Code.GRANTED
        [['*']]                 | []                      | 'HALLO'   | true         | Explanation.Code.GRANTED
        [['EXECUTE']]           | []                      | 'EXECUTE' | true         | Explanation.Code.GRANTED
        [['EXECUTE', 'MINGLE']] | []                      | 'EXECUTE' | true         | Explanation.Code.GRANTED
        [['*', 'MINGLE']]       | []                      | 'EXECUTE' | true         | Explanation.Code.GRANTED
        [['BREXECUTE']]         | []                      | 'EXECUTE' | false        | Explanation.Code.REJECTED
        [['EXECUTE']]           | [['EXECUTE']]           | 'EXECUTE' | false        | Explanation.Code.REJECTED_DENIED
        [['EXECUTE']]           | [['*']]                 | 'EXECUTE' | false        | Explanation.Code.REJECTED_DENIED
        [['EXECUTE', 'FLIM']]   | [['*']]                 | 'FLIM'    | false        | Explanation.Code.REJECTED_DENIED
        [['EXECUTE', 'FLIM']]   | [['*']]                 | 'FLAM'    | false        | Explanation.Code.REJECTED_DENIED
        [['MINGLE']]            | [['BREXCUTE']]          | 'EXECUTE' | false        | Explanation.Code.REJECTED
        [['MINGLE']]            | [['EXECUTE', 'MINGLE']] | 'EXECUTE' | false        | Explanation.Code.REJECTED_DENIED
        [['MINGLE']]            | [['EXECUTE', 'MINGLE']] | 'MINGLE'  | false        | Explanation.Code.REJECTED_DENIED
        [['MINGLE']]            | [['EXECUTE', '*']]      | 'MINGLE'  | false        | Explanation.Code.REJECTED_DENIED
        [['MINGLE']]            | [['*', 'MINGLE']]       | 'MINGLE'  | false        | Explanation.Code.REJECTED_DENIED
        [['*', 'MINGLE']]       | [['*', 'MINGLE']]       | 'MINGLE'  | false        | Explanation.Code.REJECTED_DENIED
        [['*', 'MINGLE']]       | [['*', 'MINGLE']]       | 'BLABLA'  | false        | Explanation.Code.REJECTED_DENIED
        [['*']]                 | [['*']]                 | 'MINGLE'  | false        | Explanation.Code.REJECTED_DENIED
        [['*']]                 | [['EXECUTE']]           | 'MINGLE'  | true         | Explanation.Code.GRANTED
        [['*']]                 | [['EXECUTE']]           | 'ZINGLE'  | true         | Explanation.Code.GRANTED
        [['*']]                 | [['EXECUTE']]           | 'EXECUTE' | false        | Explanation.Code.REJECTED_DENIED
        [['MINGLE']]            | [['EXECUTE']]           | 'MINGLE'  | true         | Explanation.Code.GRANTED
        [['MINGLE']]            | [['EXECUTE']]           | 'SHINGLE' | false        | Explanation.Code.REJECTED
        [['MINGLE']]            | [['EXECUTE']]           | 'EXECUTE' | false        | Explanation.Code.REJECTED_DENIED

    }

    @Unroll
    def "evaluate rule#testRule - #testType - #testKey : #testValue"() {
        when:
        def matchResourceName = [match: 'regexResource']
        Authorization eval = newRuleEvaluator(basicRules([
                regexMatch                                              : testType == 'match',
                containsMatch                                           : testType == 'contains',
                subsetMatch                                             : testType == 'subset',
                equalsMatch                                             : testType == 'equals',
                (matchResourceName[testType] ?: (testType + 'Resource')): [
                        (testKey): testRule
                ],
                resourceType                                            : 'job',
                allowActions                                            : ['EXECUTE'] as Set,
                denyActions                                             : [] as Set,
        ]
        )
        )
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: testValue
                ],
                basicSubject("bob", "admin", "user"),
                "EXECUTE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result != null
        result.isAuthorized() == isauthorized
        result.action == "EXECUTE"

        where:
        testKey  | testValue       | testRule          | testType   | isauthorized
        'jobName'| 'bob'           | 'bob'             | 'match'    | true
        'jobName'| 'boblinkious'   | 'bob.*'           | 'match'    | true
        'jobName'| 'abboblinkious' | 'bob.*'           | 'match'    | false

        'jobName'| 'bob'           | 'bob'             | 'equals'   | true
        'jobName'| 'bob'           | 'bobasdf'         | 'equals'   | false
        'jobName'| 'asdfbob'       | 'bob'             | 'equals'   | false

        'jobName'| 'val1'          | 'val1'            | 'contains' | true
        'jobName'| 'val1'          | ['val1']          | 'contains' | true
        'jobName'| 'val1,val2'     | 'val1'            | 'contains' | true
        'jobName'| 'val1,val2'     | 'val2'            | 'contains' | true
        'jobName'| 'val1,val2'     | ['val1', 'val2']  | 'contains' | true
        'jobName'| 'val1,val2'     | ['val2', 'val1']  | 'contains' | true
        'jobName'| 'val1,val2'     | ['val3', 'val1']  | 'contains' | false
        'jobName'| 'val1,val3'     | ['val12', 'val1'] | 'contains' | false
        'tag1'   | 'val1'          | 'val1'            | 'contains' | false

        'jobName'| 'val1'          | 'val1'            | 'subset'   | true
        'jobName'| 'val1'          | ['val1']          | 'subset'   | true
        'jobName'| 'val1,val2'     | 'val1'            | 'subset'   | false
        'jobName'| 'val1,val2'     | 'val2'            | 'subset'   | false
        'jobName'| 'val1,val2'     | ['val1', 'val2']  | 'subset'   | true
        'jobName'| 'val1,val2'     | ['val2', 'val1']  | 'subset'   | true
        'jobName'| 'val1,val2'     | ['val3', 'val1']  | 'subset'   | false
        'jobName'| 'val1,val3'     | ['val12', 'val1'] | 'subset'   | false

    }

    @Unroll
    def "evaluate multi match"() {
        when:
        def matchResourceName = [match: 'regexResource']
        Authorization eval = newRuleEvaluator(basicRules([
                regexMatch    : true,
                containsMatch : false,
                subsetMatch   : true,
                equalsMatch   : false,
                regexResource : [
                        jobName: regexValue
                ],
                subsetResource: [
                        blahval: subsetValue
                ],
                resourceType  : 'job',
                allowActions  : ['EXECUTE'] as Set,
                denyActions   : [] as Set,
        ]
        )
        )
        def testRes = [
                type   : 'job',
                jobName: testJobName,
        ]
        if (null != testBlahval) {
            testRes['blahval'] = testBlahval
        }
        def result = eval.evaluate(
                testRes,
                basicSubject("bob", "admin", "user"),
                "EXECUTE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result != null
        result.isAuthorized() == isauthorized
        result.action == "EXECUTE"

        where:
        testJobName   | testBlahval        | regexValue | subsetValue      | isauthorized
        'bob'         | null               | 'bob'      | ['asdf', 'ghij'] | true
        'boblinkious' | 'asdf'             | 'bob.*'    | ['asdf', 'ghij'] | true
        'boblinkious' | 'ghij'             | 'bob.*'    | ['asdf', 'ghij'] | true
        'boblinkious' | 'asdf,ghij'        | 'bob.*'    | ['asdf', 'ghij'] | true
        //subset fail
        'boblinkious' | 'asdf,ghij,bunkle' | 'bob.*'    | ['asdf', 'ghij'] | false
        'boblinkious' | 'asdf,bunkle'      | 'bob.*'    | ['asdf', 'ghij'] | false
        'boblinkious' | 'ghij,bunkle'      | 'bob.*'    | ['asdf', 'ghij'] | false
        'boblinkious' | 'fwinny'           | 'bob.*'    | ['asdf', 'ghij'] | false
        //regex fail
        'zingle'      | 'asdf'             | 'bob.*'    | ['asdf', 'ghij'] | false
    }

    @Unroll
    def "evaluate match missing attribute"() {
        when:
        Authorization eval = newRuleEvaluator(basicRules([
                regexMatch   : true,
                containsMatch: false,
                subsetMatch  : false,
                equalsMatch  : false,
                regexResource: [
                        otherattr: 'bob.*'
                ],
                resourceType : 'restype',
                allowActions : ['EXECUTE'] as Set,
                denyActions  : [] as Set,
        ]
        )
        )
        def testRes = [
                type    : 'restype',
                someattr: 'attrval',
        ]
        def result = eval.evaluate(
                testRes,
                basicSubject("bob", "admin", "user"),
                "EXECUTE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result != null
        result.isAuthorized() == false
        result.action == "EXECUTE"
    }
    @Unroll
    def "evaluate equals missing attribute"() {
        when:
        Authorization eval = newRuleEvaluator(basicRules([

                resourceType : 'restype',
                allowActions : ['EXECUTE'] as Set,
                denyActions  : [] as Set,
        ]
        )
        )
        def testRes = [
                type    : 'restype',
                someattr: 'attrval',
        ]
        def result = eval.evaluate(
                testRes,
                basicSubject("bob", "admin", "user"),
                "EXECUTE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result != null
        result.isAuthorized() == false
        result.action == "EXECUTE"
    }

    def "matches any pattern"() {
        expect:
        RuleEvaluator.matchesAnyPatterns(["abc"], "abc")
        RuleEvaluator.matchesAnyPatterns(["abc", "def", "ghi"], "abc")
        RuleEvaluator.matchesAnyPatterns(["abc"], "a.*")
        RuleEvaluator.matchesAnyPatterns(["abc", "def", "ghi"], "a.*")
        RuleEvaluator.matchesAnyPatterns(["abc", "def", "ghi"], "d.*")
        RuleEvaluator.matchesAnyPatterns(["abc", "def", "ghi"], "g.*")
        RuleEvaluator.matchesAnyPatterns(["abc"], ".*c")
        RuleEvaluator.matchesAnyPatterns(["abc", "def", "ghi"], ".*c")
        RuleEvaluator.matchesAnyPatterns(["abc"], ".*")
        RuleEvaluator.matchesAnyPatterns(["abc", "def", "ghi"], ".*")

        !RuleEvaluator.matchesAnyPatterns(["abc"], "d.*")
        !RuleEvaluator.matchesAnyPatterns(["abc"], "g.*")
        RuleEvaluator.matchesAnyPatterns(["abc", "def", "ghi"], "d.*")
        RuleEvaluator.matchesAnyPatterns(["abc", "def", "ghi"], "g.*")
        !RuleEvaluator.matchesAnyPatterns(["abc"], "d")

    }


    def "matches any pattern skips null group"() {
        expect:
        expect == RuleEvaluator.matchesAnyPatterns(input, pattern)
        where:
        input          | pattern | expect
        [null]         | 'd'     | false
        [null, 'asdf'] | '..d.'  | true
        [null, 'asdf'] | '..x.'  | false
        ['asdf', null] | '..d.'  | true
        ['asdf', null] | '..x.'  | false
    }

    def "narrow contexts"() {
        given:

        AclRuleSet ruleset = (basicRulesFromList([
                [
                        sourceIdentity: 'a',
                        username      : null,
                        group         : 'dev',
                        environment   : BasicEnvironmentalContext.staticContextFor("application", "rundeck")
                ],
                [
                        sourceIdentity: 'b',
                        username      : 'bob',
                        group         : null,
                        environment   : BasicEnvironmentalContext.staticContextFor("application", "rundeck")
                ],
                [
                        sourceIdentity: 'c',
                        username      : null,
                        group         : 'qa.*',
                        environment   : BasicEnvironmentalContext.staticContextFor("application", "rundeck")
                ],
                [
                        sourceIdentity: 'd',
                        username      : null,
                        group         : 'blah',
                        environment   : BasicEnvironmentalContext.staticContextFor("project", "testproj1")
                ],
                [
                        sourceIdentity: 'e',
                        username      : 'bob',
                        group         : null,
                        environment   : BasicEnvironmentalContext.staticContextFor("project", "testproj1")
                ],
                [
                        sourceIdentity: 'f',
                        username      : 'bob.*',
                        group         : null,
                        environment   : BasicEnvironmentalContext.staticContextFor("project", "testproj1")
                ],
                [
                        sourceIdentity: 'g',
                        username      : null,
                        group         : 'bloo.*',
                        environment   : BasicEnvironmentalContext.staticContextFor("project", "testproj1")
                ],
                [
                        sourceIdentity: 'h',
                        username      : null,
                        group         : null,
                        urn           : 'project:testproj1',
                        environment   : BasicEnvironmentalContext.staticContextFor("application", "rundeck")
                ],
                [
                        sourceIdentity: 'i',
                        username      : null,
                        group         : null,
                        urn           : 'group:urnrole1',
                        environment   : BasicEnvironmentalContext.staticContextFor("application", "rundeck")
                ],
                [
                        sourceIdentity: 'j',
                        username      : null,
                        group         : null,
                        urn           : 'user:urnuserA',
                        environment   : BasicEnvironmentalContext.staticContextFor("application", "rundeck")
                ],
                [
                        sourceIdentity: 'k',
                        username      : null,
                        group         : null,
                        urn           : 'group:urnrole2',
                        environment   : BasicEnvironmentalContext.staticContextFor("project", "testproj1")
                ],
                [
                        sourceIdentity: 'l',
                        username      : null,
                        group         : null,
                        urn           : 'user:urnuserB',
                        environment   : BasicEnvironmentalContext.staticContextFor("project", "testproj1")
                ]
        ]
        )
        )
        AclSubject subject = Mock(AclSubject) {
            getUsername() >> testuser
            getGroups() >> testgroups
            getUrn()    >> testurn
        }
        def env = projenv as String ? [new Attribute(AuthorizationUtil.PROJECT_BASE_URI, projenv)] as Set :
                AuthorizationUtil.RUNDECK_APP_ENV
        when:
        def result = RuleEvaluator.narrowContext(ruleset, subject, env)

        then:
        result*.sourceIdentity == expectrules

        where:
        testuser  | testgroups             | testurn | projenv     | expectrules
        'bob'     | ['dev']                | null    | null        | ['a', 'b']
        'bob'     | ['qa']                 | null    | null        | ['b', 'c']
        'bob'     | ['other']              | null    | null        | ['b']
        'bob'     | ['dev', 'qa']          | null    | null        | ['a', 'b', 'c']
        'zob'     | ['zoop']               | null    | null        | []
        'zob'     | ['qa_regex']           | null    | null        | ['c']

        //project
        'bob'     | ['dev']                | null    | 'testproj1' | ['e', 'f']
        'zob'     | ['blah']               | null    | 'testproj1' | ['d']
        'bobbert' | ['other']              | null    | 'testproj1' | ['f']
        'bob'     | ['blah', 'blee', 'qa'] | null    | 'testproj1' | ['d', 'e', 'f']
        'zob'     | ['blig']               | null    | 'testproj1' | []
        'zob'     | ['bloo_regex']         | null    | 'testproj1' | ['g']

        //urn
        null      | null                   | 'project:testproj1'    | null        | ['h']

        //urn group: and user: specifier in acl
        'auser'   | ['urnrole1']           | null    | null        | ['i']
        'auser'   | ['urnrole1','dev']     | null    | null        | ['a', 'i']
        'urnuserA'| ['asdf']               | null    | null        | ['j']
        'auser'   | ['urnrole2']           | null    | 'testproj1' | ['k']
        'auser'   | ['urnrole2','blah']    | null    | 'testproj1' | ['d','k']
        'urnuserB'| ['asdf']               | null    | 'testproj1' | ['l']


    }

    @Unroll
    def "matchesContexts"() {
        given:

            AclSubject subject = Mock(AclSubject) {
                getUsername() >> testuser
                getGroups() >> testgroups
                getUrn() >> testurn
            }
            def rule = basicRule([username: null, group: null, urn: null, by:isby] + detail)
            def env = projenv as String ? [new Attribute(AuthorizationUtil.PROJECT_BASE_URI, projenv)] as Set :
                      AuthorizationUtil.RUNDECK_APP_ENV

        expect:
            RuleEvaluator.matchesContexts(rule, subject, env) == expect
        where:
            detail                  | isby  | projenv | testuser | testgroups | testurn        | expect
            [group: 'dev']          | true  | null    | 'bob'    | ['dev']    | null           | true
            [group: 'qa']           | true  | null    | 'bob'    | ['dev']    | null           | false
            [group: '(dev|qa)']     | true  | null    | 'bob'    | ['dev']    | null           | true
            [group: '(dev|qa)']     | true  | null    | 'bob'    | ['qa']     | null           | true
            [group: '(dev|qa)']     | true  | null    | 'bob'    | ['prod']   | null           | false
            [group: 'dev']          | false | null    | 'bob'    | ['dev']    | null           | false
            [group: 'qa']           | false | null    | 'bob'    | ['dev']    | null           | true
            [group: '(dev|qa)']     | false | null    | 'bob'    | ['dev']    | null           | false
            [group: '(dev|qa)']     | false | null    | 'bob'    | ['qa']     | null           | false
            [group: '(dev|qa)']     | false | null    | 'bob'    | ['prod']   | null           | true

            [username: 'bob']       | true  | null    | 'bob'    | ['dev']    | null           | true
            [username: 'sam']       | true  | null    | 'bob'    | ['dev']    | null           | false
            [username: '(bob|sam)'] | true  | null    | 'bob'    | ['dev']    | null           | true
            [username: '(bob|sam)'] | true  | null    | 'sam'    | ['dev']    | null           | true
            [username: '(bob|sam)'] | true  | null    | 'ann'    | ['dev']    | null           | false
            [username: 'bob']       | false | null    | 'bob'    | ['dev']    | null           | false
            [username: 'sam']       | false | null    | 'bob'    | ['dev']    | null           | true
            [username: '(bob|sam)'] | false | null    | 'bob'    | ['dev']    | null           | false
            [username: '(bob|sam)'] | false | null    | 'sam'    | ['dev']    | null           | false
            [username: '(bob|sam)'] | false | null    | 'ann'    | ['dev']    | null           | true

            [urn: 'group:dev']      | true  | null    | 'bob'    | ['dev']    | null           | true
            [urn: 'group:qa']       | true  | null    | 'bob'    | ['dev']    | null           | false

            [urn: 'group:dev']      | false | null    | 'bob'    | ['dev']    | null           | false
            [urn: 'group:qa']       | false | null    | 'bob'    | ['dev']    | null           | true

            [urn: 'user:bob']       | true  | null    | 'bob'    | ['dev']    | null           | true
            [urn: 'user:sam']       | true  | null    | 'bob'    | ['dev']    | null           | false
            [urn: 'user:bob']       | false | null    | 'bob'    | ['dev']    | null           | false
            [urn: 'user:sam']       | false | null    | 'bob'    | ['dev']    | null           | true

            [urn: 'project:blah']   | true  | null    | null     | null       | 'project:blah' | true
            [urn: 'project:blee']   | true  | null    | null     | null       | 'project:blah' | false
            [urn: 'project:blah']   | false | null    | null     | null       | 'project:blah' | false
            [urn: 'project:blee']   | false | null    | null     | null       | 'project:blah' | true
    }


    def "test allow using notBy group"(){
        given:
        Authorization eval = newRuleEvaluator(basicRulesNotBy())
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("bob","test","user"),
                "EXECUTE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result!=null
        result.isAuthorized()
        "EXECUTE"==result.action
    }

    def "test allow using notBy username"(){
        given:
        Authorization eval = newRuleEvaluator(basicRulesNotByUsername('bob'))
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("admin","admin","user"),
                "EXECUTE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result!=null
        result.isAuthorized()
        "EXECUTE"==result.action
    }

    def "test reject using notBy username"(){
        given:
        Authorization eval = newRuleEvaluator(basicRulesNotByUsername('bob'))
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("bob","admin","user"),
                "EXECUTE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result!=null
        !result.isAuthorized()
        "EXECUTE"==result.action
    }

    def "test reject using notBy username regex"(){
        given:
        Authorization eval = newRuleEvaluator(basicRulesNotByUsername('bob|dan'))
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("bob","admin","user"),
                "EXECUTE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        def result2 = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("dan","admin","user"),
                "EXECUTE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )

        def result3 = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("jhon","admin","user"),
                "EXECUTE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result!=null
        result2!= null
        result3!= null
        !result.isAuthorized()
        !result2.isAuthorized()
        result3.isAuthorized()
        "EXECUTE"==result.action
    }
    def "test allow urn"(){
        given:
        Authorization eval = newRuleEvaluator(basicRulesUrn("project:demo"))
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                urnSubject("demo"),
                "EXECUTE",
                AuthorizationUtil.RUNDECK_APP_ENV
        )
        then:
        result!=null
        result.isAuthorized()
        "EXECUTE"==result.action
    }


    AclRule basicRule(Map detail) {
        new Rule(
            [
                sourceIdentity: "test1",
                description   : "bob job allow exec, deny delete for admin group",
                equalsResource: [
                    jobName: 'bob'
                ],
                resourceType  : 'job',
                regexMatch    : false,
                containsMatch : false,
                equalsMatch   : true,
                username      : null,
                group         : 'admin',
                allowActions  : ['EXECUTE'] as Set,
                denyActions   : ['DELETE'] as Set,
                by: true,
                environment   : BasicEnvironmentalContext.staticContextFor("application", "rundeck")
            ] + detail
        )
    }
    AclRuleSet basicRulesFromList(List input) {
        def rules = input.collect {Map detail ->
            basicRule(detail)
        } as Set
        new AclRuleSetImpl(rules)
    }
    AclRuleSet basicRules(Map detail) {
        def rules = [
                new Rule(
                        [
                                sourceIdentity: "test1",
                                description   : "bob job allow exec, deny delete for admin group",
                                equalsResource: [
                                        jobName: 'bob'
                                ],
                                resourceType  : 'job',
                                regexMatch    : false,
                                containsMatch : false,
                                equalsMatch   : true,
                                username      : null,
                                group         : 'admin',
                                allowActions  : ['EXECUTE'] as Set,
                                denyActions   : ['DELETE'] as Set,
                                by: true,
                                environment   : BasicEnvironmentalContext.staticContextFor("application", "rundeck")
                        ] + detail
                ),
        ] as Set
        new AclRuleSetImpl(rules)
    }

    AclRuleSet basicRules() {
        def rules = [
                new Rule(
                        sourceIdentity: "test1",
                        description: "bob job allow exec, deny delete for admin group",
                        equalsResource: [
                                jobName: 'bob'
                        ],
                        resourceType: 'job',
                        regexMatch: false,
                        containsMatch: false,
                        equalsMatch: true,
                        username: null,
                        group: 'admin',
                        allowActions: ['EXECUTE'] as Set,
                        denyActions: ['DELETE'] as Set,
                        by: true,
                        environment: BasicEnvironmentalContext.staticContextFor("application", "rundeck")
                ),
        ] as Set
        new AclRuleSetImpl(rules)
    }
    AclRuleSet basicProjectRegexRules() {
        def rules = [
                new Rule(
                        sourceIdentity: "test1",
                        description   : "bob job allow exec, deny delete for admin group",
                        equalsResource: [
                                jobName: 'bob'
                        ],
                        resourceType  : 'job',
                        regexMatch    : false,
                        containsMatch : false,
                        equalsMatch : true,
                        username      : null,
                        group         : 'admin',
                        allowActions  : ['EXECUTE'] as Set,
                        denyActions   : ['DELETE'] as Set,
                        by: true,
                        environment   : BasicEnvironmentalContext.patternContextFor("project",".*")
                ),
        ] as Set
        new AclRuleSetImpl(rules)
    }
    AclRuleSet basicRules2() {
        def rules = [new Rule(
                sourceIdentity: "test2",
                description: "bob job allow delete for admin group",
                equalsResource: [
                                jobName: 'bob'
                        ],
                resourceType: 'job',
                regexMatch: false,
                containsMatch: false,
                equalsMatch: true,
                username: null,
                group: 'admin',
                allowActions: ['READ'] as Set,
                by: true,
                environment: BasicEnvironmentalContext.staticContextFor("application", "rundeck")
        ),
        ] as Set
        new AclRuleSetImpl(basicRules().rules + rules)
    }

    AclRuleSet basicRules3() {
        def rules = [new Rule(
                sourceIdentity: "test2",
                description: "bob job allow delete for admin group",
                equalsResource: [
                        jobName: 'bob'
                ],
                resourceType: 'job',
                regexMatch: false,
                containsMatch: false,
                equalsMatch: true,
                username: null,
                group: 'admin',
                allowActions: ['DELETE'] as Set,
                by: true,
                environment: BasicEnvironmentalContext.staticContextFor("application", "rundeck")
        ),
        ] as Set
        new AclRuleSetImpl(basicRules().rules + rules)
    }

    AclRuleSet basicRulesNotBy() {
        def rules = [
                new Rule(
                        sourceIdentity: "test1",
                        description: "bob job allow exec, deny delete for admin group",
                        equalsResource: [
                                jobName: 'bob'
                        ],
                        resourceType: 'job',
                        regexMatch: false,
                        containsMatch: false,
                        equalsMatch: true,
                        username: null,
                        group: 'admin',
                        allowActions: ['EXECUTE'] as Set,
                        denyActions: ['DELETE'] as Set,
                        by: false,
                        environment: BasicEnvironmentalContext.staticContextFor("application", "rundeck")
                ),
        ] as Set
        new AclRuleSetImpl(rules)
    }
    AclRuleSet basicRulesNotByUsername(String username) {
        def rules = [
                new Rule(
                        sourceIdentity: "test1",
                        description: "bob job allow exec, deny delete for admin group",
                        equalsResource: [
                                jobName: 'bob'
                        ],
                        resourceType: 'job',
                        regexMatch: false,
                        containsMatch: false,
                        equalsMatch: true,
                        username: username,
                        group: null,
                        allowActions: ['EXECUTE'] as Set,
                        denyActions: ['DELETE'] as Set,
                        by: false,
                        environment: BasicEnvironmentalContext.staticContextFor("application", "rundeck")
                ),
        ] as Set
        new AclRuleSetImpl(rules)
    }
    AclRuleSet basicRulesUrn(String urn) {
        def rules = [
                new Rule(
                        sourceIdentity: "test1",
                        description: "bob job allow exec, deny delete for admin group",
                        equalsResource: [
                                jobName: 'bob'
                        ],
                        resourceType: 'job',
                        regexMatch: false,
                        containsMatch: false,
                        equalsMatch: true,
                        username: null,
                        group: null,
                        urn: urn,
                        allowActions: ['EXECUTE'] as Set,
                        denyActions: ['DELETE'] as Set,
                        by: true,
                        environment: BasicEnvironmentalContext.staticContextFor("application", "rundeck")
                ),
        ] as Set
        new AclRuleSetImpl(rules)
    }
    static class Rule implements AclRule{
        String sourceIdentity;
        String description
        Map<String, Object> resource;

        String resourceType;

        boolean regexMatch;
        Map<String, Object> regexResource;

        boolean containsMatch;
        Map<String, Object> containsResource;
        boolean subsetMatch;
        Map<String, Object> subsetResource
        boolean equalsMatch;
        Map<String, Object> equalsResource;

        String username;

        String group;

        String urn;

        Set<String> allowActions

        EnvironmentalContext environment;

        Set<String> denyActions;
        boolean by;


    }
}
