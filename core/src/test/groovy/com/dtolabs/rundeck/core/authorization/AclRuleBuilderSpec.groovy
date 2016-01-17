package com.dtolabs.rundeck.core.authorization

import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext
import spock.lang.Specification

/**
 * Created by greg on 7/22/15.
 */
class AclRuleBuilderSpec extends Specification {

    def "empty"(){
        given:
        def rule=AclRuleBuilder.builder().build()
        expect:
        rule!=null
        rule.resource==null
        rule.allowActions==null
        rule.denyActions==null
        rule.description==null
        rule.environment==null
        rule.group==null
        rule.resourceType==null
        rule.sourceIdentity==null
        rule.username==null
        !rule.containsMatch
        !rule.equalsMatch
        !rule.regexMatch
    }
    def "full"(){
        def builder = AclRuleBuilder.builder()
        given:
        builder.with {
            description "blah"
            sourceIdentity "sblah"
            resourceType "rblah"
            resource( [a: 'b'])
            group 'gblah'
            allowActions(['ablah', 'ablah2'] as Set)
            denyActions(['dblah', 'dblah2'] as Set)
            containsMatch true
            regexMatch true
            equalsMatch true
            username "ublah"
            environment Mock(EnvironmentalContext){
                isValid()>>true
            }
        }
        def rule=builder.build()
        expect:
        rule!=null
        rule.description=='blah'
        rule.sourceIdentity=='sblah'
        rule.resourceType=='rblah'
        rule.resource==[a:'b']
        rule.group=='gblah'
        rule.allowActions==['ablah','ablah2'] as Set
        rule.denyActions==['dblah','dblah2'] as Set
        rule.containsMatch
        rule.regexMatch
        rule.equalsMatch
        rule.username=='ublah'
        rule.environment!=null
        rule.environment.isValid()
    }
}
