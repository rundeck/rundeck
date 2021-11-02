package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authorization.RdProjectAuthorize
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation

class RdProjectAuthorizeInterceptor implements MethodInterceptor {
    @Override
    Object invoke(MethodInvocation invocation) throws Throwable {
        def authorize = invocation.getMethod().getAnnotation(RdProjectAuthorize)
        println "do authorization check with project : " + authorize.value()

        //lookup the access definition for the value in the annotation
        //get the project from request attributes
        //check auth against Subject in session and acls
        //if not authorized throw exception
        //otherwise proceed
        return invocation.proceed()
    }
}
