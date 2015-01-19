/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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

package org.grails.plugins.metricsweb

import com.codahale.metrics.servlets.AdminServlet
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.support.WebApplicationContextUtils

import javax.servlet.ServletConfig
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * <p>Wraps the {@link AdminServlet} class, to provide selective disabling of any of the metrics servlets.
 * Configuration in grails config file:</p>
 * <p>
 *     <code>rundeck.web.metrics.servlets.[name].enabled=true/false</code>
 *     </p>
 *     <p>
 *         Where <code>[name]</code> is one of the servlet names:
 *         </p>
 *         <ul>
 *             <li><code>metrics</code></li>
 *             <li><code>ping</code></li>
 *             <li><code>threads</code></li>
 *             <li><code>healthcheck</code></li>
 *
 *             </ul>
 *             <p>
 *                 Default configuration is that all servlets are enabled.
 *                 </p>
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-08-25
 */
class DisablingAdminServlet extends AdminServlet {
    Map<String, Boolean> enabledMap

    @Override
    void init(ServletConfig config) throws ServletException {
        super.init(config)

        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(config.servletContext);
        def gapp = context.getBean(GrailsApplication.class)
        if (gapp == null) {
            throw new IllegalStateException("grailsApplication not found in context")
        }
        def map = new HashMap<String, Boolean>()

        def metricsUri = notNullParam(config.getInitParameter("metrics-uri"), DEFAULT_METRICS_URI);
        def pingUri = notNullParam(config.getInitParameter("ping-uri"), DEFAULT_PING_URI);
        def threadsUri = notNullParam(config.getInitParameter("threads-uri"), DEFAULT_THREADS_URI);
        def healthcheckUri = notNullParam(config.getInitParameter("healthcheck-uri"), DEFAULT_HEALTHCHECK_URI);

        map[metricsUri] = gapp.config.rundeck?.web?.metrics?.servlets?.metrics?.enabled in [true, 'true']
        map[pingUri] = gapp.config.rundeck?.web?.metrics?.servlets?.ping?.enabled in [true, 'true']
        map[threadsUri] = gapp.config.rundeck?.web?.metrics?.servlets?.threads?.enabled in [true, 'true']
        map[healthcheckUri] = gapp.config.rundeck?.web?.metrics?.servlets?.healthcheck?.enabled in [true, 'true']
        this.enabledMap = Collections.unmodifiableMap(map)
    }

    static String notNullParam(String s, String s1) {
        return s == null ? s1 : s
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Boolean isenabled = enabledMap.get(req.getPathInfo())
        if (null != isenabled && !isenabled) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND)
        } else {
            super.service(req, resp)
        }
    }
}
