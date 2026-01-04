package com.dtolabs.rundeck.server

import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Urn
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authorization.Attribute
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.Authorization
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.core.authorization.Explanation
import com.dtolabs.rundeck.core.authorization.SubjectAuthContext
import org.grails.testing.GrailsUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import javax.security.auth.Subject

class AuthContextEvaluatorCacheManagerSpec  extends Specification implements GrailsUnitTest {
    @Unroll
    def "key equality"(){
        given:
            def key1 = new AuthContextEvaluatorCacheManager.AuthContextEvaluatorCacheKey(
                ctx,
                resources.toSet(),
                actions.toSet(),
                project
            )
            def key2 = new AuthContextEvaluatorCacheManager.AuthContextEvaluatorCacheKey(
                ctx2,
                resources2.toSet(),
                actions2.toSet(),
                project2
            )
        expect:
            key1.equals(key2) == result
        where:
            ctx | ctx2 |resources | resources2 | actions |actions2 | project | project2 | result
            mkctx('auser','arole')|mkctx('auser','arole')|[[a:'b']]|[[a:'b']]|['run']|['run']|'aproj'|'aproj' | true
            mkctx('auser','arole')|mkctx('auser','arole')|[[a:'b']]|[[a:'b']]|['run']|['run']|'aproj'|'bproj' | false
            mkctx('auser','arole')|mkctx('auser','arole')|[[a:'b']]|[[a:'b']]|['run']|['read']|'aproj'|'aproj' | false
            mkctx('auser','arole')|mkctx('auser','arole')|[[a:'b']]|[[a:'c']]|['run']|['run']|'aproj'|'aproj' | false
            mkctx('auser','arole')|mkctx('auser','brole')|[[a:'b']]|[[a:'b']]|['run']|['run']|'aproj'|'aproj' | false

    }


    Subject mksubject(String user, String... roles){
        def sub = new Subject()
        sub.getPrincipals().add(new Username(user))
        roles.each {
            sub.getPrincipals().add(new Group(it))
        }
        sub
    }
    Subject mksubjecturn(String urn){
        def sub = new Subject()
        sub.getPrincipals().add(new Urn(urn))
        sub
    }
    AuthContext mkctx(String user, String... roles){
        new SubjectAuthContext(mksubject(user, roles), Mock(Authorization))
    }
    AuthContext mkctxUrn(String urn){
        new SubjectAuthContext(mksubjecturn(urn), Mock(Authorization))
    }

    def "acl evaluation should cache with same args"() {
        given:
        AuthContextEvaluatorCacheManager authContextEvaluatorCacheManager = new AuthContextEvaluatorCacheManager()
        authContextEvaluatorCacheManager.enabled = enabled
        authContextEvaluatorCacheManager.afterPropertiesSet()
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
        authContextEvaluatorCacheManager.afterPropertiesSet()
        SubjectAuthContext subjectAuthContext1 = Mock(SubjectAuthContext){
            getUsername() >>  "username1"
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
        authContextEvaluatorCacheManager.afterPropertiesSet()
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
        authContextEvaluatorCacheManager.afterPropertiesSet()
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

    @Unroll
    def "key equality using URN"(){
        given:
        def key1 = new AuthContextEvaluatorCacheManager.AuthContextEvaluatorCacheKey(
                ctx,
                resources.toSet(),
                actions.toSet(),
                project
        )
        def key2 = new AuthContextEvaluatorCacheManager.AuthContextEvaluatorCacheKey(
                ctx2,
                resources2.toSet(),
                actions2.toSet(),
                project2
        )
        expect:
        key1.equals(key2) == result
        where:
        ctx | ctx2 |resources | resources2 | actions |actions2 | project | project2 | result
        mkctxUrn('project:testProjectA')|mkctxUrn('project:testProjectA')|[[a:'b']]|[[a:'b']]|['read']|['read']|'testProjectA'|'testProjectA' | true
        mkctxUrn('project:testProjectA')|mkctxUrn('project:testProjectB')|[[a:'b']]|[[a:'b']]|['read']|['read']|'testProjectA'|'testProjectB' | false


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
