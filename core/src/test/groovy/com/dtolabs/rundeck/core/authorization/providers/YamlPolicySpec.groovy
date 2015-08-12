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
        YamlPolicy.AclPolicySyntaxException e = thrown()
        e.message.contains('Context section should not be specified, it is already set to: {project=monkey}')

        where:
        ctxMap                   | _
        [project: 'dolphin']     | _
        [project: 'test1']       | _
        [project: '.*']          | _
        [application: 'rundeck'] | _
    }
}
