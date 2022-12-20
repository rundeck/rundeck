package org.rundeck.app.authorization

import grails.web.api.ServletAttributes
import groovy.transform.CompileStatic
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.rundeck.core.auth.app.*
import org.rundeck.core.auth.web.IdParameter
import org.rundeck.core.auth.web.RdAuthorize
import org.rundeck.core.auth.web.WebDefaultParameterNamesMapper
import org.rundeck.core.auth.web.WebParamsIdResolver
import org.rundeck.web.ExceptionHandler
import org.springframework.beans.factory.annotation.Autowired

import javax.security.auth.Subject

/**
 * AOP method interceptor that can handles various  annotations applied to grails Controller actions to
 * enforce authorization.
 * it resolves an Authorization request based on annotations and parameters from the input GrailsParameterMap (if
 * necessary),
 * and uses the Subject defined in the Servlet HttpSession.
 * If the authorization check passes, the method call proceeds. If it fails due to throwing an exception handled by the
 * ExceptionHandler, then the invocation is prevented.
 * @see RdAuthorize, RdAuthorizeExecution, RdAuthorizeProject, RdAuthorizeSystem, IdParameter
 */
@CompileStatic
class RdAuthorizeInterceptor implements MethodInterceptor, ServletAttributes {
    @Autowired
    BaseTypedRequestAuthorizer rundeckAppAuthorizer
    @Autowired
    ExceptionHandler rundeckExceptionHandler
    @Autowired
    WebDefaultParameterNamesMapper rundeckWebDefaultParameterNamesMapper
    private static ServiceLoader<RequestMethodAuthorizer> requestMethodAuthorizerLoader;

    private static ServiceLoader<RequestMethodAuthorizer> getLoader() {
        if (null == requestMethodAuthorizerLoader) {
            requestMethodAuthorizerLoader = ServiceLoader.load(RequestMethodAuthorizer.class);
        }
        return requestMethodAuthorizerLoader;
    }

    protected Subject getSubject() {
        def subject = session.getAttribute('subject')
        if (subject instanceof Subject) {
            return subject
        }
        throw new IllegalStateException("no subject found in session")
    }


    @Override
    Object invoke(MethodInvocation invocation) throws Throwable {
        List<TypedNamedAuthRequest> reqs = getMethodAuthRequests(invocation)
        def nameResolver = paramNameMap(
            invocation.getMethod().getAnnotationsByType(IdParameter),
            rundeckWebDefaultParameterNamesMapper.getWebDefaultParameterNames()
        )
        def idResolver = new WebParamsIdResolver(nameResolver, getParams())
        for (TypedNamedAuthRequest req : reqs) {
            try {
                rundeckAppAuthorizer.authorize(subject, idResolver, req)
            } catch (Throwable e) {
                if (rundeckExceptionHandler.handleException(request, response, e)) {
                    return null
                }
                throw e;
            }
        }

        return invocation.proceed()
    }

    private static List<TypedNamedAuthRequest> getMethodAuthRequests(MethodInvocation invocation) {
        List<TypedNamedAuthRequest> result = new ArrayList<>()
        for (RequestMethodAuthorizer authorizer : getLoader()) {
            result.addAll(authorizer.requestsFromAnnotations(invocation.getMethod()))
        }
        return result
    }


    /**
     *
     * @param idParameters
     * @return
     */
    public static Map<String, String> paramNameMap(IdParameter[] idParameters, Map<String, String> defvals) {
        Map<String, String> map = new HashMap<>(defvals)
        for (IdParameter idParameter : idParameters) {
            map.put(idParameter.type(), idParameter.value())
        }
        map
    }
}