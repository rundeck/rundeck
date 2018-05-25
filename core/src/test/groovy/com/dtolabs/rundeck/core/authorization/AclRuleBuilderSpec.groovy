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

/**
 * Created by greg on 7/22/15.
 */
class AclRuleBuilderSpec extends Specification {

    def "empty"(){
        given:
        def rule=AclRuleBuilder.builder().build()
        expect:
        rule!=null
        rule.regexResource==null
        rule.containsResource==null
        rule.equalsResource==null
        rule.subsetResource==null
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
            equalsResource( [a: 'b'])
            containsResource( [c: 'd'])
            subsetResource( [e: 'f'])
            regexResource( [g: 'h'])
            group 'gblah'
            allowActions(['ablah', 'ablah2'] as Set)
            denyActions(['dblah', 'dblah2'] as Set)
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
        rule.equalsResource==[a:'b']
        rule.containsResource==[c:'d']
        rule.subsetResource==[e:'f']
        rule.regexResource==[g:'h']
        rule.group=='gblah'
        rule.allowActions==['ablah','ablah2'] as Set
        rule.denyActions==['dblah','dblah2'] as Set
        rule.containsMatch
        rule.regexMatch
        rule.equalsMatch
        rule.subsetMatch
        rule.username=='ublah'
        rule.environment!=null
        rule.environment.isValid()
    }

    def "null resources"() {
        def builder = AclRuleBuilder.builder()
        given:
        builder.with {
            description "blah"
            sourceIdentity "sblah"
            resourceType "rblah"
            equalsResource(null)
            containsResource(null)
            subsetResource(null)
            regexResource(null)
            group 'gblah'
            allowActions(['ablah', 'ablah2'] as Set)
            denyActions(['dblah', 'dblah2'] as Set)
            username "ublah"
            environment Mock(EnvironmentalContext) {
                isValid() >> true
            }
        }
        def rule = builder.build()
        expect:
        rule != null
        rule.description == 'blah'
        rule.sourceIdentity == 'sblah'
        rule.resourceType == 'rblah'
        rule.equalsResource == null
        rule.containsResource == null
        rule.subsetResource == null
        rule.regexResource == null
        rule.group == 'gblah'
        rule.allowActions == ['ablah', 'ablah2'] as Set
        rule.denyActions == ['dblah', 'dblah2'] as Set
        !rule.containsMatch
        !rule.regexMatch
        !rule.equalsMatch
        !rule.subsetMatch
        rule.username == 'ublah'
        rule.environment != null
        rule.environment.isValid()
    }
}
