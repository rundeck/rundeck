import com.codahale.metrics.JmxReporter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.health.HealthCheckRegistry

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
Adds the Metrics 3.x AdminServlet at a configurable path
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
        if(application.config.rundeck.metrics.servletUrlPattern){
            addServlets(application.config,xml)
        }
        addFilters(application.config,xml)
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
                    'servlet-class'("com.codahale.metrics.servlets.AdminServlet")
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

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
        //define necessary servletContext attributes for the metrics filters and servlet
        applicationContext.servletContext.setAttribute('com.codahale.metrics.servlet.InstrumentedFilter.registry',
                applicationContext.getBean(MetricRegistry))
        applicationContext.servletContext.setAttribute('com.codahale.metrics.servlets.MetricsServlet.registry',
                applicationContext.getBean(MetricRegistry))
        applicationContext.servletContext.setAttribute('com.codahale.metrics.servlets.HealthCheckServlet.registry',
                applicationContext.getBean(HealthCheckRegistry))

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
