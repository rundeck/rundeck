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

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.health.HealthCheckRegistry
import com.codahale.metrics.servlets.AdminServlet
import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import grails.core.GrailsApplication
import org.rundeck.core.auth.AuthConstants
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.support.WebApplicationContextUtils

import javax.security.auth.Subject
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
 *             <li><code>cpuProfile</code></li>
 *
 *             </ul>
 *             <p>
 *                 Default configuration is that all servlets are enabled.
 *                 </p>
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-08-25
 */
class DisablingAdminServlet extends AdminServlet implements ApplicationContextAware {
    public static final String APPLICATION_JSON = "application/json"
    Map<String, Boolean> enabledMap
    ApplicationContext applicationContext
    private AuthContextProcessor rundeckAuthContextProcessor

    void load() throws Exception {
        def beans = applicationContext.getBeansOfType(AuthContextProcessor)
        if (beans && beans['rundeckAuthContextProcessor']) {
            this.rundeckAuthContextProcessor = beans['rundeckAuthContextProcessor']
        } else {
            throw new IllegalStateException("Could not resolve bean: rundeckAuthContextProcessor")
        }
    }

    @Override
    void init(ServletConfig config) throws ServletException {

        config.getServletContext().setAttribute('com.codahale.metrics.servlet.InstrumentedFilter.registry',
                applicationContext.getBean(MetricRegistry))
        config.getServletContext().setAttribute('com.codahale.metrics.servlets.MetricsServlet.registry',
                applicationContext.getBean(MetricRegistry))
        config.getServletContext().setAttribute('com.codahale.metrics.servlets.HealthCheckServlet.registry',
                applicationContext.getBean(HealthCheckRegistry))
        
        super.init(config)

        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(config.servletContext);
        def gapp = context.getBean(GrailsApplication.class)
        if (gapp == null) {
            throw new IllegalStateException("grailsApplication not found in context")
        }
        def map = new HashMap<String, Boolean>()

        def metricsUri = notNullParam(config.getInitParameter(METRICS_URI_PARAM_KEY), DEFAULT_METRICS_URI);
        def pingUri = notNullParam(config.getInitParameter(PING_URI_PARAM_KEY), DEFAULT_PING_URI);
        def threadsUri = notNullParam(config.getInitParameter(THREADS_URI_PARAM_KEY), DEFAULT_THREADS_URI);
        def healthcheckUri = notNullParam(config.getInitParameter(HEALTHCHECK_URI_PARAM_KEY), DEFAULT_HEALTHCHECK_URI);
        def cpuProfileUri = notNullParam(config.getInitParameter(CPU_PROFILE_URI_PARAM_KEY), DEFAULT_CPU_PROFILE_URI);

        def apiEnabled = (gapp.config.rundeck?.metrics?.enabled in [true, 'true']) &&
                         (gapp.config.rundeck?.metrics?.api?.enabled in [true, 'true'])
        map[metricsUri] = apiEnabled && gapp.config.rundeck?.metrics?.api?.metrics?.enabled in [true, 'true']
        map[pingUri] = apiEnabled && gapp.config.rundeck?.metrics?.api?.ping?.enabled in [true, 'true']
        map[threadsUri] = apiEnabled && gapp.config.rundeck?.metrics?.api?.threads?.enabled in [true, 'true']
        map[healthcheckUri] = apiEnabled && gapp.config.rundeck?.metrics?.api?.healthcheck?.enabled in [true, 'true']
        map[cpuProfileUri] = apiEnabled && gapp.config.rundeck?.metrics?.api?.cpuProfile?.enabled in [true, 'true']
        this.enabledMap = Collections.unmodifiableMap(map)
    }

    static String notNullParam(String s, String s1) {
        return s == null ? s1 : s
    }

    private void respondError(HttpServletResponse resp, int status, String message) {
        resp.setStatus(status)
        resp.setContentType(APPLICATION_JSON)
        PrintWriter writer = resp.getWriter()
        try {
            writer.println('{"error":true,"message":"' + message + '"}');
        } finally {
            writer.close()
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        def info = req.getPathInfo()

        if (info == null || info == '/') {
            //disable AdminServlet's builtin html response
            respondError(resp, HttpServletResponse.SC_NOT_FOUND, 'Not Found')
            return
        }

        Boolean isenabled = enabledMap.get(info)
        if (null == isenabled && !isenabled) {
            respondError(resp, HttpServletResponse.SC_NOT_FOUND, 'Not Found')
            return
        }

        if (!authorize(req)) {
            respondError(resp, HttpServletResponse.SC_UNAUTHORIZED, 'Unauthorized')
            return
        }
        super.service(req, resp)

    }

    private boolean authorize(HttpServletRequest req) {
        def subj = req.getSession(false).getAttribute("subject")
        if (!(subj instanceof Subject)) {
            return false
        }
        Subject authSubject = (Subject) subj
        load()
        def authContext = rundeckAuthContextProcessor.getAuthContextForSubject(authSubject)
        //check access
        return rundeckAuthContextProcessor.authorizeApplicationResourceAny(
            authContext,
            AuthConstants.RESOURCE_TYPE_SYSTEM,
            [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN]
        )

    }
}
