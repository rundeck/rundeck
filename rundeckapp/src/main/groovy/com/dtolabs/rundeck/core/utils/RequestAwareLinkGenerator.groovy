package com.dtolabs.rundeck.core.utils

import org.grails.web.mapping.DefaultLinkGenerator
import grails.web.mapping.LinkGenerator
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder

class RequestAwareLinkGenerator extends DefaultLinkGenerator implements LinkGenerator {

    RequestAwareLinkGenerator(String serverBaseURL, String contextPath) {
        super(serverBaseURL, contextPath)
    }

    RequestAwareLinkGenerator(String serverBaseURL) {
        super(serverBaseURL)
    }

    /**
     * @return serverURL based on the baseUrl of the incoming request.
     */
    String makeServerURL() {
        GrailsWebRequest webRequest = liveWebRequest()
        if (webRequest) {
            String baseUrl = webRequest.baseUrl
            return baseUrl
        } else {
            String serverUrl = super.makeServerURL()
            return serverUrl
        }
    }

    /** Attribute name used only to ask the bound request whether it is still alive. */
    private static final String LIVENESS_PROBE = 'rundeck.linkGenerator.livenessProbe'

    /**
     * The bound request, or null when there is none or the container has already recycled it.
     *
     * Jetty 12 recycles the request once its response completes, and work that outlives it -- a
     * project deletion runs in the background long after the DELETE has been answered -- still has
     * it bound by the async task decorator. Reading anything from it then throws, so report it as
     * absent, which is the case this class already answers: fall back to the configured serverURL.
     */
    private static GrailsWebRequest liveWebRequest() {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup()
        if (webRequest == null) {
            return null
        }
        try {
            webRequest.currentRequest.getAttribute(LIVENESS_PROBE)
            return webRequest
        } catch (Exception ignored) {
            return null
        }
    }

    /**
     * Unbinds a recycled request for the duration of the call, because DefaultLinkGenerator reads
     * the HTTP method off whatever is bound and throws on a recycled one, which aborted the
     * deletion of an execution and left its project undeletable.
     */
    String link(Map attrs, String encoding = 'UTF-8') {
        if (GrailsWebRequest.lookup() != null && liveWebRequest() == null) {
            RequestAttributes recycled = RequestContextHolder.getRequestAttributes()
            RequestContextHolder.resetRequestAttributes()
            try {
                return linkUsingBoundRequest(attrs, encoding)
            } finally {
                RequestContextHolder.setRequestAttributes(recycled)
            }
        }
        return linkUsingBoundRequest(attrs, encoding)
    }

    private String linkUsingBoundRequest(Map attrs, String encoding) {
        String cp = super.getContextPath()
        String url = super.link(attrs, encoding)
        if (cp) {
            String serverUrl = makeServerURL()

            // this will strip out the context path from url
            // e.g.: cp=/rdk url=/rdk/menu/executionMode => menu/executionMode
            if (cp != null && url.indexOf(cp) == 0) url = url.substring(cp.length())

            // in some cases, the url is resolved as "context"/"fullurl", the previous step removes the context part so
            // its safe to return the url
            // e.g.: cp=/rdk url=/rdk/http://rundeck.local:4440/rdk/tour/listAll => http://rundeck.local:4440/rdk/tour/listAll
            if (serverUrl != null && url.indexOf(serverUrl) == 0) return url

            // e.g.: cp=/rdk url=/menu/executionMode => menu/executionMode
            if (url[0] == '/') url = url.substring(1)

            return serverUrl + "/" + url
        }
        return url
    }
}
