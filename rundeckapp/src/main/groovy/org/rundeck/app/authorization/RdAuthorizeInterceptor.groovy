package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authorization.RdAuthorize
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation

class RdAuthorizeInterceptor implements MethodInterceptor {
    @Override
    Object invoke(MethodInvocation invocation) throws Throwable {
        def authorize = invocation.getMethod().getAnnotation(RdAuthorize)
        println "do authorization check : " + authorize.value()
        //check auth against Subject in session
        //if not authorized throw exception
        //otherwise proceed
        return invocation.proceed()
    }
}
