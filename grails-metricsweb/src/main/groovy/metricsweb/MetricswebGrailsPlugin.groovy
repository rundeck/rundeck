package metricsweb
/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

import com.codahale.metrics.jmx.JmxReporter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.health.HealthCheckRegistry
import grails.core.GrailsClass
import org.grails.plugins.metricsweb.DisablingAdminServlet
import org.springframework.boot.web.servlet.ServletRegistrationBean

import java.util.concurrent.Callable

class MetricswebGrailsPlugin {
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "4.0.3 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views",
        "web-app/**"
    ]

    // TODO Fill in these fields
    def title = "Metricsweb Plugin" // Headline display name of the plugin
    def author = "Greg Schueler"
    def authorEmail = ""
    def description = '''\
Adds the Metrics 3.x AdminServlet at a configurable path, adds the InstrumentedFilter to add metrics around
each HTTP reqest, and provides some utility methods to Controllers and Services for using metrics.
'''

    // URL to the plugin's documentation
    def documentation = "http://rundeck.org"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
    def organization = [ name: "Rundeck", url: "http://rundeck.com/" ]

    // Any additional developers beyond the author specified above.
    def developers = [ [ name: "Greg Schueler", email: "greg@rundeck.com" ]]

    // Location of the plugin's issue tracker.
    def issueManagement = [ system: "Github", url: "http://github.com/dtolabs/rundeck/issues" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "http://github.com/dtolabs/rundeck" ]

    def doWithWebDescriptor = { xml ->
        boolean metricsEnabled = grailsApplication.config.getProperty("rundeck.metrics.enabled", Boolean.class, false)
        boolean requestFilterEnabled = grailsApplication.config.getProperty("rundeck.metrics.requestFilterEnabled", Boolean.class , false)
        String servletUrlPattern = grailsApplication.config.getProperty("rundeck.metrics.servletUrlPattern", String.class, false)

        if(!metricsEnabled){
            return
        }
        if(servletUrlPattern){
            addServlets(grailsApplication.config,xml)
        }
        if (requestFilterEnabled) {
            addFilters(grailsApplication.config,xml)
        }
    }
    def addFilters(ConfigObject config,def xml) {
        def filterNodes = xml.'context-param'
        if (filterNodes.size() > 0) {

            def filterElement = filterNodes[filterNodes.size() - 1]
            filterElement + {
                'filter' {
                    'filter-name'("instrumentedFilter")
                    'filter-class'("com.codahale.metrics.servlet.InstrumentedFilter")
                }
            }


            def mappingNodes = xml.'filter'
            if (mappingNodes.size() > 0) {

                def mappingElement = mappingNodes[mappingNodes.size() - 1]
                mappingElement + {
                    'filter-mapping' {
                        'filter-name'("instrumentedFilter")
                        'url-pattern'("/*")
                    }
                }
            }
        }
    }

    def addServlets(ConfigObject config,def xml) {
        def servletNodes = xml.'servlet'
        if (servletNodes.size() > 0) {

            def servletElement = servletNodes[servletNodes.size() - 1]
            servletElement + {
                'servlet' {
                    'servlet-name'("metrics-admin-servlet")
                    'servlet-class'("org.grails.plugins.metricsweb.DisablingAdminServlet")
                }
            }
        }
        def mappingNodes = xml.'servlet-mapping'

        if (mappingNodes.size() > 0) {

            def lastMapping = mappingNodes[mappingNodes.size() - 1]
            lastMapping + {
                'servlet-mapping' {
                    'servlet-name'("metrics-admin-servlet")
                    'url-pattern'(config.rundeck.metrics.servletUrlPattern.toString())
                }
            }
        }
    }

    def doWithSpring = {

        rundeckMetricsDisablingAdminServlet(DisablingAdminServlet)
        disablingAdminServletRegistrationBean(ServletRegistrationBean, ref('rundeckMetricsDisablingAdminServlet'), grailsApplication.config.rundeck.metrics.servletUrlPattern.toString()) {
            loadOnStartup = -1
        }

        metricRegistry(MetricRegistry)
        healthCheckRegistry(HealthCheckRegistry)

    }

    def doWithDynamicMethods = { applicationContext ->
        def metricRegistry = applicationContext.getBean(MetricRegistry)
        boolean metricsEnabled = grailsApplication.config.getProperty("rundeck.metrics.enabled", Boolean.class, false)
        def disabledClasses = grailsApplication.config.getProperty("rundeck.metrics.disabledClasses", String.class, "")?:[]

        if(disabledClasses instanceof String){
            disabledClasses= disabledClasses.split(/,\s*/) as List
        }
        for (domainClass in grailsApplication.domainClasses) {
            addDynamicMetricMethods(domainClass, metricRegistry, metricsEnabled && !disabledClasses.contains(domainClass.clazz.name))
        }
        for (serviceClass in grailsApplication.serviceClasses) {
            addDynamicMetricMethods(serviceClass, metricRegistry, metricsEnabled && !disabledClasses.contains(serviceClass.clazz.name))
        }
    }

    /**
     * Adds methods to controllers and services for using metrics library
     * @param grailsClass
     * @param metricRegistry
     * @param enabled if false, all added methods are noop or passthru
     */
    private static void addDynamicMetricMethods(GrailsClass grailsClass, MetricRegistry metricRegistry, Boolean enabled) {
        def cname = grailsClass.clazz.name

        def noopString = { String classname = cname, String metricName -> }

        def mrMeter = enabled ? { String classname = cname, String metricName ->
            metricRegistry.meter(MetricRegistry.name(classname, metricName))
        } : noopString


        def mrCounter = enabled ? { String classname = cname, String metricName ->
            metricRegistry.counter(MetricRegistry.name(classname, metricName))
        } : noopString


        def mrTimer = enabled ? {String classname = cname, String metricName ->
            metricRegistry.timer(MetricRegistry.name(classname, metricName))
        } : noopString

        grailsClass.metaClass.static.metricMeter = mrMeter
        grailsClass.metaClass.static.metricMeterMark = { String metricName -> mrMeter(metricName)?.mark() }
        grailsClass.metaClass.static.metricMeterMark = { String className, String metricName -> mrMeter(className,metricName)?.mark() }
        grailsClass.metaClass.static.metricCounter =  mrCounter
        grailsClass.metaClass.static.metricCounterInc =  { String metricName -> mrCounter(metricName)?.inc() }
        grailsClass.metaClass.static.metricCounterInc =  { String className, String metricName -> mrCounter(className, metricName)?.inc() }
        grailsClass.metaClass.static.metricCounterDec =  { String metricName -> mrCounter(metricName)?.dec() }
        grailsClass.metaClass.static.metricCounterDec =  { String className, String metricName -> mrCounter(className, metricName)?.dec() }
        grailsClass.metaClass.static.metricTimer =  mrTimer

        grailsClass.metaClass.static.withTimer = enabled ? { Closure clos ->
            metricRegistry.timer(MetricRegistry.name(cname, 'Timer')).time((Callable)clos)
        } : { Closure clos -> clos.call() }

        grailsClass.metaClass.static.withTimer = enabled ? { String name, Closure clos ->
            metricRegistry.timer(MetricRegistry.name(cname, name)).time((Callable)clos)
        } : { String name, Closure clos -> clos.call() }

        grailsClass.metaClass.static.withTimer = enabled ? { String className, String name, Closure clos ->
            metricRegistry.timer(MetricRegistry.name(className, name)).time((Callable)clos)
        } : { String className, String name, Closure clos -> clos.call() }

        grailsClass.metaClass.static.getMetricRegistry = {->
            metricRegistry
        }
    }

    def doWithApplicationContext = { applicationContext ->
        //define necessary servletContext attributes for the metrics filters and servlet
        applicationContext.servletContext.setAttribute('com.codahale.metrics.servlet.InstrumentedFilter.registry',
                applicationContext.getBean(MetricRegistry))
        applicationContext.servletContext.setAttribute('com.codahale.metrics.servlets.MetricsServlet.registry',
                applicationContext.getBean(MetricRegistry))
        applicationContext.servletContext.setAttribute('com.codahale.metrics.servlets.HealthCheckServlet.registry',
                applicationContext.getBean(HealthCheckRegistry))

        boolean metricsEnabled = grailsApplication.config.getProperty("rundeck.metrics.enabled", Boolean.class, false)

        if (!metricsEnabled) {
            return
        }
        boolean metricsJmx = grailsApplication.config.getProperty("rundeck.metrics.jmxEnabled", Boolean.class, false)

        if (metricsJmx && applicationContext.getBean(MetricRegistry)) {
            final JmxReporter reporter = JmxReporter.forRegistry(applicationContext.getBean(MetricRegistry)).build();
            reporter.start();
        }
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    def onShutdown = { event ->
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
