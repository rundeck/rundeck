package com.rundeck.plugin

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class FrameworkControllerInterceptorSpec extends Specification implements InterceptorUnitTest<FrameworkControllerInterceptor> {

    def setup() {
    }

    def cleanup() {

    }


    void "Test frameworkController interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(controller:"framework", action:"saveProject")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }

}
