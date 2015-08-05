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
