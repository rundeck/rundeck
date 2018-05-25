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

import spock.lang.Specification

/**
 * Created by greg on 7/22/15.
 */
class BasicEnvironmentalContextSpec extends Specification {
    def "static test"(){
        given:
        def ctx=BasicEnvironmentalContext.staticContextFor("a", "b+")
        expect:
        ctx.matches(AuthorizationUtil.context("a", "b+"))
        !ctx.matches(AuthorizationUtil.context("z", "b+"))
        !ctx.matches(AuthorizationUtil.context("a", "bb"))
        !ctx.matches(AuthorizationUtil.context("z", "bb"))
    }
    def "pattern"(){
        given:
        def ctx=BasicEnvironmentalContext.patternContextFor("a", "b+")
        expect:
        ctx.matches(AuthorizationUtil.context("a", "b+"))
        !ctx.matches(AuthorizationUtil.context("z", "b+"))
        ctx.matches(AuthorizationUtil.context("a", "bb"))
        !ctx.matches(AuthorizationUtil.context("a", "abbc"))
        !ctx.matches(AuthorizationUtil.context("z", "bb"))
    }
    def "not null pattern value"(){
        when:
        def ctx=BasicEnvironmentalContext.patternContextFor("a",null)
        then:
        IllegalArgumentException e = thrown()
    }
    def "not null static value"(){
        when:
        def ctx=BasicEnvironmentalContext.staticContextFor("a",null)
        then:
        IllegalArgumentException e = thrown()
    }
    def "not null static key"(){
        when:
        def ctx=BasicEnvironmentalContext.staticContextFor(null,"b")
        then:
        IllegalArgumentException e = thrown()
    }
    def "not null pattern key"(){
        when:
        def ctx=BasicEnvironmentalContext.patternContextFor(null,"b")
        then:
        IllegalArgumentException e = thrown()
    }
}
