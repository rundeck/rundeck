/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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
package rundeckapp.init.servlet

import jakarta.servlet.DispatcherType
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.FilterConfig
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import rundeck.services.UploadSizeLimitService

import java.util.regex.Pattern

/**
 * Rejects Key Storage and Access Control uploads whose declared size is already over the
 * configured maximum, before the body is parsed.
 */
@Configuration
class UploadSizeLimitFilterConfig {

    /**
     * @param uploadSizeLimitService configured limits
     * @return early upload-size filter
     */
    @Bean
    FilterRegistrationBean<UploadSizeLimitFilter> uploadSizeLimitFilter(UploadSizeLimitService uploadSizeLimitService) {
        FilterRegistrationBean<UploadSizeLimitFilter> registration = new FilterRegistrationBean<>()
        registration.setFilter(new UploadSizeLimitFilter(uploadSizeLimitService))
        registration.addUrlPatterns('/*')
        registration.setOrder(2)
        registration.setDispatcherTypes(DispatcherType.REQUEST)
        return registration
    }
}

/**
 * Servlet filter that enforces the Key Storage and Access Control upload ceilings.
 */
class UploadSizeLimitFilter implements Filter {

    static final long MULTIPART_ALLOWANCE = 64L * 1024L

    enum Target {
        KEY_STORAGE,
        ACL
    }

    private static final Pattern KEY_STORAGE_API = Pattern.compile('^/api/\\d+/storage/keys/.+')
    private static final Pattern PROJECT_ACL_UI = Pattern.compile('^/project/[^/]+/admin/acls/save$')
    private static final Pattern SYSTEM_ACL_API = Pattern.compile('^/api/\\d+/system/acl/.+')
    private static final Pattern PROJECT_ACL_API = Pattern.compile('^/api/\\d+/project/[^/]+/acl/.+')

    private final UploadSizeLimitService uploadSizeLimitService

    /**
     * @param uploadSizeLimitService configured limits
     */
    UploadSizeLimitFilter(UploadSizeLimitService uploadSizeLimitService) {
        this.uploadSizeLimitService = uploadSizeLimitService
    }

    @Override
    void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request
            Target target = targetFor(httpRequest.getMethod(), requestPath(httpRequest))
            if (target != null && declaredSizeExceeds(httpRequest, target)) {
                reject((HttpServletResponse) response, target)
                return
            }
        }
        chain.doFilter(request, response)
    }

    @Override
    void destroy() {
    }

    /**
     * @param method HTTP method
     * @param path request path without the context path
     * @return which upload limit applies, or null when the request is not limited
     */
    static Target targetFor(String method, String path) {
        boolean write = 'POST' == method || 'PUT' == method
        if (!write || path == null) {
            return null
        }
        if ('POST' == method && '/storage/upload/keys' == path) {
            return Target.KEY_STORAGE
        }
        if (KEY_STORAGE_API.matcher(path).matches()) {
            return Target.KEY_STORAGE
        }
        if ('POST' == method && ('/menu/acls/save' == path || PROJECT_ACL_UI.matcher(path).matches())) {
            return Target.ACL
        }
        if (SYSTEM_ACL_API.matcher(path).matches() || PROJECT_ACL_API.matcher(path).matches()) {
            return Target.ACL
        }
        return null
    }

    /**
     * @param request current request
     * @return path relative to the context, without a session id
     */
    static String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI() ?: ''
        String contextPath = request.getContextPath()
        if (contextPath && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length())
        }
        int semicolon = uri.indexOf(';')
        if (semicolon >= 0) {
            uri = uri.substring(0, semicolon)
        }
        return uri
    }

    private boolean declaredSizeExceeds(HttpServletRequest request, Target target) {
        long maxBytes = target == Target.KEY_STORAGE ?
                uploadSizeLimitService.keyStorageMaxBytes() :
                uploadSizeLimitService.aclMaxBytes()
        String contentType = request.getContentType()
        boolean multipart = contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith('multipart/')
        long ceiling = multipart ? maxBytes + MULTIPART_ALLOWANCE : maxBytes
        return UploadSizeLimitService.declaredLengthExceeds(request.getContentLengthLong(), ceiling)
    }

    private void reject(HttpServletResponse response, Target target) {
        String label = target == Target.KEY_STORAGE ?
                uploadSizeLimitService.keyStorageMaxSizeLabel() :
                uploadSizeLimitService.aclMaxSizeLabel()
        String safeLabel = label.replace('\\', '\\\\').replace('"', '\\"')
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE)
        response.setContentType('application/json')
        response.setCharacterEncoding('UTF-8')
        response.writer.write(
                '{"error":true,"errorCode":"' + UploadSizeLimitService.ERROR_CODE +
                        '","message":"Uploaded content is larger than the configured maximum size: ' +
                        safeLabel + '"}'
        )
    }
}
