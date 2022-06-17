package org.rundeck.app.data

import grails.util.Holders
import groovy.transform.CompileStatic
import org.rundeck.spi.data.AccessContextProvider
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder

@CompileStatic
class RundeckDataAccessContext implements AccessContextProvider<BaseAppContext> {
    static class AppContext implements BaseAppContext {

    }

    @Override
    BaseAppContext getContext() {
//        RequestContextHolder.getRequestAttributes().getAttribute("tenantId", RequestAttributes.SCOPE_REQUEST)
        return new AppContext()
    }
}
