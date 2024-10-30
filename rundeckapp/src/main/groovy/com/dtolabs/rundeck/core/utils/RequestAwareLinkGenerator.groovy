package com.dtolabs.rundeck.core.utils

import org.grails.web.mapping.DefaultLinkGenerator
import grails.web.mapping.LinkGenerator
import org.grails.web.servlet.mvc.GrailsWebRequest

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
        GrailsWebRequest webRequest = GrailsWebRequest.lookup()
        if (webRequest) {
            String baseUrl = webRequest.baseUrl
            return baseUrl
        } else {
            String serverUrl = super.makeServerURL()
            return serverUrl
        }
    }

    String link(Map attrs, String encoding = 'UTF-8') {
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
