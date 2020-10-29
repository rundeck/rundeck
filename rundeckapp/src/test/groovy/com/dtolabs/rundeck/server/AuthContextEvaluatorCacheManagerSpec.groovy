package com.dtolabs.rundeck.server

import com.dtolabs.rundeck.core.authorization.Attribute
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.core.authorization.Explanation
import com.dtolabs.rundeck.core.authorization.SubjectAuthContext
import org.grails.testing.GrailsUnitTest
import spock.lang.Specification

import javax.security.auth.Subject

class AuthContextEvaluatorCacheManagerSpec  extends Specification implements GrailsUnitTest {
    def "acl evaluation should cache with same args"() {
        given:
        AuthContextEvaluatorCacheManager authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()
        authContextEvaluatorCacheManager.enabled = enabled
        SubjectAuthContext subjectAuthContext1 = Mock(SubjectAuthContext){
            getUsername() >> { "username1" }
            getRoles() >> { ["role1"] as Set }
            calls * evaluate(_,_,_) >> { createDecision("action", true) }
        }
        Map resource1 = [akey: "value"]
        String action1 = "action1"
        String project1 = "project1"
        when:
        def evaluation1 = authContextEvaluatorCacheManager.evaluate(subjectAuthContext1, resource1, action1, project1)
        def evaluation2 = authContextEvaluatorCacheManager.evaluate(subjectAuthContext1, resource, action, project)

        then:
        evaluation1
        evaluation2
        evaluation1.isAuthorized()
        evaluation2.isAuthorized()

        where:
        calls | enabled  | resource                 | action    | project
        1     | true     | [akey: "value"]          | "action1" | "project1"
        2     | false    | [akey: "value"]          | "action1" | "project1"
        2     | true     | [otherkey: "othervalue"] | "action1" | "project1"
        2     | true     | [akey: "value"]          | "action2" | "project1"
        2     | true     | [akey: "value"]          | "action1" | "project2"
    }

    def "acl evaluation with multiple actions and resources should cache with same args"() {
        given:
        AuthContextEvaluatorCacheManager authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()
        authContextEvaluatorCacheManager.enabled = enabled
        SubjectAuthContext subjectAuthContext1 = Mock(SubjectAuthContext){
            getUsername() >> { "username1" }
            getRoles() >> { ["role1"] as Set }
            calls * evaluate(_,_,_) >> { [createDecision("action", true)] as Set }
        }
        List resources1 = [[akey1: "value1"],[akey2: "value2"]]
        List actions1 = ["action1", "action2"]
        String project1 = "project1"
        when:
        Set<Decision> evaluation1 = authContextEvaluatorCacheManager.evaluate(subjectAuthContext1, resources1 as Set, actions1 as Set, project1)
        Set<Decision> evaluation2 = authContextEvaluatorCacheManager.evaluate(subjectAuthContext1, resource as Set, action as Set, project)

        then:
        evaluation1
        evaluation2
        evaluation1.first()?.isAuthorized()
        evaluation2.first()?.isAuthorized()

        where:
        calls | enabled | resource                               | action                 | project
        1     | true    | [[akey1: "value1"], [akey2: "value2"]] | ["action1", "action2"] | "project1"
        2     | false   | [[akey1: "value1"], [akey2: "value2"]] | ["action1"]            | "project1"
        2     | true    | [[otherkey: "othervalue"]]             | ["action1"]            | "project1"
        2     | true    | [[akey: "value"]]                      | ["action2"]            | "project1"
        2     | true    | [[akey: "value"]]                      | ["action1"]            | "project2"
    }

    def "invalidate all entries from cache"() {
        given:
        AuthContextEvaluatorCacheManager authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()
        authContextEvaluatorCacheManager.enabled = true
        SubjectAuthContext subjectAuthContext1 = Mock(SubjectAuthContext){
            getUsername() >> { "username1" }
            getRoles() >> { ["role1"] as Set }
            2 * evaluate(_,_,_) >> { [createDecision("action", true)] as Set }
        }
        List resources1 = [[akey1: "value1"],[akey2: "value2"]]
        List actions1 = ["action1", "action2"]
        String project1 = "project1"
        when:
        Set<Decision> evaluation1 = authContextEvaluatorCacheManager.evaluate(subjectAuthContext1, resources1 as Set, actions1 as Set, project1)
        authContextEvaluatorCacheManager.invalidateAllCacheEntries()
        Set<Decision> evaluation2 = authContextEvaluatorCacheManager.evaluate(subjectAuthContext1, resources1 as Set, actions1 as Set, project1)

        then:
        evaluation1
        evaluation2
        evaluation1.first()?.isAuthorized()
        evaluation2.first()?.isAuthorized()
    }

    def "acl cache should be refreshed if roles or username is changed"() {
        given:
        AuthContextEvaluatorCacheManager authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()
        authContextEvaluatorCacheManager.enabled = true
        SubjectAuthContext subjectAuthContext1 = Mock(SubjectAuthContext){
            getUsername() >> { "username1" }
            getRoles() >> { ["role1"] as Set }
            evaluate(_,_,_) >> { createDecision("action", true) }
        }

        SubjectAuthContext subjectAuthContext2 = Mock(SubjectAuthContext){
            getUsername() >> { username }
            getRoles() >> { roles as Set }
            evaluate(_,_,_) >> { createDecision("action", false) }
        }
        Map resource = [akey: "value"]
        String action = "action1"
        String project = "project1"
        when:
        def evaluation1 = authContextEvaluatorCacheManager.evaluate(subjectAuthContext1, resource, action, project)
        def evaluation2 = authContextEvaluatorCacheManager.evaluate(subjectAuthContext2, resource, action, project)

        then:
        evaluation1
        evaluation2
        evaluation1.isAuthorized()
        !evaluation2.isAuthorized()

        where:
        username    | roles
        "username2" | ["role1"]
        "username1" | ["role2"]
        "username1" | ["role1", "role2"]
    }

    Decision createDecision(String action, boolean isAuthorized) {
        return new Decision() {
            @Override
            boolean isAuthorized() {
                return isAuthorized
            }

            @Override
            Explanation explain() {
                return null
            }

            @Override
            long evaluationDuration() {
                return 0
            }

            @Override
            Map<String, String> getResource() {
                return null
            }

            @Override
            String getAction() {
                return action
            }

            @Override
            Set<Attribute> getEnvironment() {
                return null
            }

            @Override
            Subject getSubject() {
                return null
            }
        }
    }
}
