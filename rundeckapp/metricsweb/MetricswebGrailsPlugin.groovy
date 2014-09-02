import com.codahale.metrics.JmxReporter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.health.HealthCheckRegistry
import org.codehaus.groovy.grails.commons.GrailsClass

class MetricswebGrailsPlugin {
    // the plugin version
    def version = "2.0.0-SNAPSHOT"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.2 > *"
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
    def organization = [ name: "SimplifyOps", url: "http://www.simplifyops.com/" ]

    // Any additional developers beyond the author specified above.
    def developers = [ [ name: "Greg Schueler", email: "greg@simplifyops.com" ]]

    // Location of the plugin's issue tracker.
    def issueManagement = [ system: "Github", url: "http://github.com/dtolabs/rundeck/issues" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "http://github.com/dtolabs/rundeck" ]

    def doWithWebDescriptor = { xml ->
        if(!(application.config.rundeck.metrics.enabled in [true,'true'])){
            return
        }
        if(application.config.rundeck.metrics.servletUrlPattern){
            addServlets(application.config,xml)
        }
        if (application.config.rundeck.metrics.requestFilterEnabled in [true,'true']) {
            addFilters(application.config,xml)
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
        metricRegistry(MetricRegistry)
        healthCheckRegistry(HealthCheckRegistry)
    }

    def doWithDynamicMethods = { applicationContext ->
        def metricRegistry = applicationContext.getBean(MetricRegistry)
        def metricsEnabled = application.config.rundeck.metrics.enabled in [true, 'true']
        def disabledClasses = application.config.rundeck.metrics.disabledClasses?:[]
        if(disabledClasses instanceof String){
            disabledClasses= disabledClasses.split(/,\s*/) as List
        }
        for (domainClass in application.domainClasses) {
            addDynamicMetricMethods(domainClass, metricRegistry, metricsEnabled && !disabledClasses.contains(domainClass.clazz.name))
        }
        for (serviceClass in application.serviceClasses) {
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
            metricRegistry.timer(MetricRegistry.name(cname, 'Timer')).time(clos)
        } : { Closure clos -> clos.call() }

        grailsClass.metaClass.static.withTimer = enabled ? { String name, Closure clos ->
            metricRegistry.timer(MetricRegistry.name(cname, name)).time(clos)
        } : { String name, Closure clos -> clos.call() }

        grailsClass.metaClass.static.withTimer = enabled ? { String className, String name, Closure clos ->
            metricRegistry.timer(MetricRegistry.name(className, name)).time(clos)
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

        if (!(application.config.rundeck.metrics.enabled in [true, 'true'])) {
            return
        }
        def metricsJmx = application.config.rundeck.metrics.jmxEnabled in ['true', true]
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
