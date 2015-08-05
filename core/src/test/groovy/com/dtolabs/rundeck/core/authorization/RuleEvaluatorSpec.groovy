package com.dtolabs.rundeck.core.authorization

import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext
import spock.lang.Specification

import javax.security.auth.Subject

/**
 * Created by greg on 7/17/15.
 */
class RuleEvaluatorSpec extends Specification {

    def "test allow project context regex"(){
        given:
        Authorization eval = new RuleEvaluator(basicProjectRegexRules())
        when:
        def projectContext = [new Attribute(EnvironmentalContext.PROJECT_BASE_URI,"aproject")] as Set
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
        Authorization eval = new RuleEvaluator(basicRules())
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("bob","admin","user"),
                "EXECUTE",
                EnvironmentalContext.RUNDECK_APP_ENV
        )
        then:
        result!=null
        result.isAuthorized()
        "EXECUTE"==result.action
    }
    def "test reject wrong type"(){
        given:
        Authorization eval = new RuleEvaluator(basicRules())
        when:
        def result = eval.evaluate(
                [
                        type   : 'boj',
                        jobName: 'bob'
                ],
                basicSubject("bob", "admin", "user"),
                "EXECUTE",
                EnvironmentalContext.RUNDECK_APP_ENV
        )
        then:
        result!=null
        !result.isAuthorized()
        "EXECUTE"==result.action
    }
    def "test reject wrong name"(){
        given:
        Authorization eval = new RuleEvaluator(basicRules())
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'sam'
                ],
                basicSubject("bob", "admin", "user"),
                "EXECUTE",
                EnvironmentalContext.RUNDECK_APP_ENV
        )
        then:
        result!=null
        !result.isAuthorized()
        "EXECUTE"==result.action
    }
    def "test reject wrong action"(){
        given:
        Authorization eval = new RuleEvaluator(basicRules())
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("bob","admin","user"),
                "TWIST",
                EnvironmentalContext.RUNDECK_APP_ENV
        )
        then:
        result!=null
        !result.isAuthorized()
        "TWIST"==result.action
    }
    def "test reject wrong group"(){
        given:
        Authorization eval = new RuleEvaluator(basicRules())
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("bob","zadmin","user"),
                "EXECUTE",
                EnvironmentalContext.RUNDECK_APP_ENV
        )
        then:
        result!=null
        !result.isAuthorized()
        "EXECUTE"==result.action
    }
    def "test deny"(){
        given:
        Authorization eval = new RuleEvaluator(basicRules())
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("bob","admin","user"),
                "DELETE",
                EnvironmentalContext.RUNDECK_APP_ENV
        )
        then:
        result!=null
        !result.isAuthorized()
        "DELETE"==result.action
    }
    def "test allow with deny"(){
        given:
        Authorization eval = new RuleEvaluator(basicRules2())
        when:
        def result = eval.evaluate(
                [
                        type   : 'job',
                        jobName: 'bob'
                ],
                basicSubject("bob","admin","user"),
                "DELETE",
                EnvironmentalContext.RUNDECK_APP_ENV
        )
        then:
        result!=null
        !result.isAuthorized()
        "DELETE"==result.action
    }

    Subject basicSubject(final String user, final String... groups) {
        def subject = new Subject()
        subject.principals<< new Username(user)
        subject.principals.addAll(groups.collect{new Group(it)})
        subject
    }

    AclRuleSet basicRules() {
        def rules = [
                new Rule(
                        sourceIdentity: "test1",
                        description   : "bob job allow exec, deny delete for admin group",
                        resource      : [
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
                        environment   : BasicEnvironmentalContext.staticContextFor("application","rundeck")
                ),
        ] as Set
        new AclRuleSetImpl(rules)
    }
    AclRuleSet basicProjectRegexRules() {
        def rules = [
                new Rule(
                        sourceIdentity: "test1",
                        description   : "bob job allow exec, deny delete for admin group",
                        resource      : [
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
                        environment   : BasicEnvironmentalContext.patternContextFor("project",".*")
                ),
        ] as Set
        new AclRuleSetImpl(rules)
    }
    AclRuleSet basicRules2() {
        def rules = [new Rule(
                        sourceIdentity: "test2",
                        description   : "bob job allow delete for admin group",
                        resource      : [
                                jobName: 'bob'
                        ],
                        resourceType  : 'job',
                        regexMatch    : false,
                        containsMatch : false,
                        equalsMatch : true,
                        username      : null,
                        group         : 'admin',
                        allowActions  : ['READ'] as Set,
                        environment   : BasicEnvironmentalContext.staticContextFor("application","rundeck")
        ),
        ] as Set
        new AclRuleSetImpl(basicRules().rules + rules)
    }
    static class Rule implements AclRule{
        String sourceIdentity;
        String description
        Map<String, Object> resource;

        String resourceType;

        String resourceKind;

        boolean regexMatch;

        boolean containsMatch;
        boolean equalsMatch;

        String username;

        String group;

        Set<String> allowActions

        EnvironmentalContext environment;

        Set<String> denyActions;
    }
}
