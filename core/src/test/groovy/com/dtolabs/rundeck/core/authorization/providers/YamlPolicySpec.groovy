package com.dtolabs.rundeck.core.authorization.providers

import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import spock.lang.Specification

/**
 * Created by greg on 7/24/15.
 */
class YamlPolicySpec extends Specification {

    def "forced context equivalent rule"(){
        given:
        def ctx = AuthorizationUtil.projectContext("monkey")
        def policy = YamlPolicy.createYamlPolicy(
                ctx,
                [
                        description:'',
                        by: [username: 'bob'],
                        for: [resource: [[allow: ['action']]]],
                        context: [project: 'monkey']
                ],
                "test1",
                1
        )
        def rules=policy.getRuleSet().getRules()

        expect:
        rules.size()==1
        rules.first().environment.key=='project'
        rules.first().environment.value=='monkey'
    }

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
                1
        )
        def rules=policy.getRuleSet().getRules()

        expect:
        rules.size()==1
        rules.first().environment.key=='project'
        rules.first().environment.value=='monkey'
    }
    def "forced context mismatched context"(){
        given:
        def ctx = AuthorizationUtil.projectContext("monkey")

        when:
        def policy = YamlPolicy.createYamlPolicy(
                ctx,
                [
                        description:'',
                        by: [username: 'bob'],
                        for: [resource: [[allow: ['action']]]],
                        context: [project: 'dolphin']
                ],
                "test1",
                1
        )

        then:
        YamlPolicy.AclPolicySyntaxException e = thrown()
        e.message.contains('Context section is not valid: {project=dolphin}, it should be empty or match the expected context: [http://dtolabs.com/rundeck/env/project:monkey]')
    }
}
