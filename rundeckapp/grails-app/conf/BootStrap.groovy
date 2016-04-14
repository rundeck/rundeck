import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.health.HealthCheck
import com.codahale.metrics.health.HealthCheckRegistry
import com.dtolabs.launcher.Setup
import com.dtolabs.rundeck.core.Constants
import com.dtolabs.rundeck.core.VersionConstants
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream
import com.dtolabs.rundeck.util.quartz.MetricsSchedulerListener
import com.dtolabs.utils.Streams
import grails.util.Environment
import org.codehaus.groovy.grails.plugins.web.filters.FilterConfig
import org.codehaus.groovy.grails.plugins.web.filters.FilterToHandlerAdapter
import org.grails.plugins.metricsweb.CallableGauge
import org.quartz.Scheduler
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.support.WebApplicationContextUtils

import javax.servlet.ServletContext

class BootStrap {

    def grailsApplication
    def scheduledExecutionService
    def executionService
    def executionUtilService
    def frameworkService
    def workflowService
    def logFileStorageService
    def projectManagerService
    def filesystemProjectManager
    def reportService
    def configurationService
    def filterInterceptor
    Scheduler quartzScheduler
    MetricRegistry metricRegistry
    def messageSource
    def scmService
    HealthCheckRegistry healthCheckRegistry
    def dataSource

    def timer(String name,Closure clos){
        long bstart=System.currentTimeMillis()
        log.debug("BEGIN: ${name}")
        def res=clos()
        log.debug("${name} in ${System.currentTimeMillis()-bstart}ms")
        return res
    }

     def init = { ServletContext servletContext ->
         //setup profiler logging
         if(!(grailsApplication.config?.grails?.profiler?.disable)) {
             //re-enable log output for profiler info, which is disabled by miniprofiler
             grailsApplication.mainContext.profilerLog.appenderNames = ["loggingAppender", 'miniProfilerAppender']
         }
         long bstart=System.currentTimeMillis()
         def appname=messageSource.getMessage('main.app.name',null,'',null) ?: messageSource.getMessage('main.app.default.name',null,'',null) ?: 'Rundeck'
         log.info("Starting ${appname} ${grailsApplication.metadata['build.ident']}...")
         /*filterInterceptor.handlers.sort { FilterToHandlerAdapter handler1,
                                           FilterToHandlerAdapter handler2 ->
             FilterConfig filter1 = handler1.filterConfig
             FilterConfig filter2 = handler2.filterConfig
             filter1.name <=> filter2.name
         }*/

         def String rdeckBase
         if(!grailsApplication.config.rdeck.base){
             //look for system property
             rdeckBase=System.getProperty('rdeck.base')
             log.info("using rdeck.base system property: ${rdeckBase}");
             def newconf= new ConfigObject()
             newconf.rdeck.base = rdeckBase
             grailsApplication.config.merge(newconf)
         }else{
             rdeckBase=grailsApplication.config.rdeck.base
             log.info("using rdeck.base config property: ${rdeckBase}");
         }
         def serverLibextDir = grailsApplication.config.rundeck?.server?.plugins?.dir ?: "${rdeckBase}/libext"
         File pluginsDir = new File(serverLibextDir)
         def clusterMode = false
         def serverNodeUUID = null
         if (Environment.getCurrent()!=Environment.TEST) {
             if (!rdeckBase) {
                 throw new RuntimeException("config file did not contain property: rdeck.base")
             }
             servletContext.setAttribute("RDECK_BASE", rdeckBase)

             File basedir = new File(rdeckBase)
             if (!basedir.isDirectory()) {
                 basedir.mkdir()
             }
             //see if initialization system property is set
             def configDir = Constants.getFrameworkConfigDir(rdeckBase)
             File fprops = new File(configDir, "framework.properties")
             boolean isFirstRun=false
             if (!fprops.exists()) {
                 isFirstRun=true
                 log.info("Performing rundeck first-run initialization...")
                 //setup the base dir
                 Setup setup = new Setup()
                 setup.getParameters().setBaseDir(basedir.getAbsolutePath())
                 //determine hostname and port from grails config if available
                 String hostname
                 String port
                 if (grailsApplication.config.grails.serverURL) {
                     URL url = new URL(grailsApplication.config.grails.serverURL)
                     hostname = url.getHost()
                     port = url.getPort().toString()
                     def urlstr = url.toExternalForm().replaceAll('/+$', '')
                     setup.getParameters().properties["framework.server.url"] = urlstr
                     setup.getParameters().properties["framework.rundeck.url"] = urlstr
                 } else {
                     //determine hostname
                     hostname = InetAddress.getLocalHost().getHostName()
                     if (hostname =~ /^[\d\.]+$/) {
                         hostname = "localhost"
                     }
                     port = "4440"
                 }
                 setup.getParameters().properties["rundeck.server.uuid"] = UUID.randomUUID().toString()
                 setup.getParameters().setServerName(hostname)
                 setup.getParameters().properties["framework.server.port"] = port

                 setup.performSetup()
                 log.info("Rundeck initialization complete.")
             }
             if (!fprops.exists()) {
                 throw new RuntimeException("framework configuration file not found: " + fprops.getAbsolutePath())
             }
             InputStream is = new FileInputStream(fprops);
             Properties properties = new Properties()
             try {
                 properties.load(is)
             } finally {
                 if (null != is) {
                     is.close()
                 }
             }

             Properties props2 = com.dtolabs.rundeck.core.utils.PropertyUtil.expand(properties)
             servletContext.setAttribute("FRAMEWORK_PROPERTIES", props2)
             log.info("loaded configuration: " + fprops.getAbsolutePath())

             String nodeName = properties.getProperty("framework.server.name")
             if (!nodeName) {
                 throw new RuntimeException("Expected 'framework.server.name' in framework.properties: Not found")
             }
             servletContext.setAttribute("FRAMEWORK_NODE", nodeName)
             //check cluster mode
             clusterMode = grailsApplication.config.rundeck.clusterMode.enabled in [true, 'true']
             servletContext.setAttribute("CLUSTER_MODE_ENABLED", Boolean.toString(clusterMode))
             if (clusterMode) {
                 serverNodeUUID = properties.getProperty("rundeck.server.uuid")
                 if (!serverNodeUUID) {
                     throw new RuntimeException("Cluster mode: rundeck.clusterMode.enabled is set to 'true', but " +
                             "'rundeck.server.uuid' not found in framework.properties")
                 }
                 try {
                     UUID.fromString(serverNodeUUID)
                 } catch (IllegalArgumentException e) {
                     throw new RuntimeException("Cluster mode: 'rundeck.server.uuid' in framework.properties was not " +
                             "a valid UUID: ${serverNodeUUID}. ")
                 }
                 servletContext.setAttribute("SERVER_UUID", serverNodeUUID)
                 log.warn("Cluster mode enabled, this server's UUID: ${serverNodeUUID}")
             }
             //auth tokens stored in file
             def tokensfile = properties.getProperty("rundeck.tokens.file")
             if (tokensfile) {
                 Properties userTokens = new Properties()
                 try {
                     new File(tokensfile).withInputStream {
                         userTokens.load(it)
                     }
                 } catch (IOException e) {
                     log.error("Unable to load static tokens file: "+e.getMessage())
                 }
                 Properties tokens = new Properties()
                 userTokens.each { k, v ->
                    tokens[v]=k
                 }
                 servletContext.setAttribute("TOKENS_FILE_PATH", new File(tokensfile).absolutePath)
                 servletContext.setAttribute("TOKENS_FILE_PROPS", tokens)
                 if (tokens) {
                     log.debug("Loaded ${tokens.size} tokens from tokens file: ${tokensfile}...")
                 }
             }

            def result=timer("FrameworkService extractEmbeddedPlugins"){
                frameworkService.extractEmbeddedPlugins(grailsApplication)
            }
            if(!result.success){
                log.error("Failed extracting embedded plugins: "+result.message)
                result?.logs?.each {
                    log.error(it)
                }
            }else{
                result?.logs?.each {
                    log.debug(it)
                }
            }

            //import filesystem projects if using DB storage
            if((grailsApplication.config.rundeck?.projectsStorageType?:'db') == 'db'){
                log.debug("importing existing filesystem projects")
                timer("ProjectManagerService importProjectsFromProjectManager"){
                    projectManagerService.importProjectsFromProjectManager(filesystemProjectManager)
                }
            }
         }

         //initialize manually to avoid circular reference problem with spring
         timer("Initialized WorkflowService"){
             workflowService.initialize()
         }
         timer("Initialized ScmService"){
             scmService.initialize()
         }

         if(grailsApplication.config.loglevel.default){
             servletContext.setAttribute("LOGLEVEL_DEFAULT", grailsApplication.config.loglevel.default)
         }else{
             servletContext.setAttribute("LOGLEVEL_DEFAULT", "INFO")
         }

         if('true' == grailsApplication.config.rss.enabled){
             servletContext.setAttribute("RSS_ENABLED", 'true')
             log.info("RSS feeds enabled")
         }else{
             log.info("RSS feeds disabled")
         }
         if(grailsApplication.config.execution.follow.buffersize){
             servletContext.setAttribute("execution.follow.buffersize",grailsApplication.config.execution.follow.buffersize)
         }else{
             servletContext.setAttribute("execution.follow.buffersize",(50*1024).toString())
         }
         if(grailsApplication.config.output.markdown.enabled){
             servletContext.setAttribute("output.markdown.enabled",grailsApplication.config.output.markdown.enabled=="true"?"true":"false")
         }else{
             servletContext.setAttribute("output.markdown.enabled","false")
         }
         if(grailsApplication.config.nowrunning.interval){
             servletContext.setAttribute("nowrunning.interval",grailsApplication.config.nowrunning.interval)
         }else{
             servletContext.setAttribute("nowrunning.interval",(15).toString())
         }
         if(grailsApplication.config.output.download.formatted){
             servletContext.setAttribute("output.download.formatted",grailsApplication.config.output.download.formatted =="true" ? "true":"false")
         }else{
             servletContext.setAttribute("output.download.formatted","true")
         }
         if(grailsApplication.config.logging.ant.metadata){
             servletContext.setAttribute("logging.ant.metadata",grailsApplication.config.logging.ant.metadata =="true" ? "true":"false")
         }else{
             servletContext.setAttribute("logging.ant.metadata","true")
         }
         def defaultLastLines = grailsApplication.config.rundeck.gui.execution.tail.lines.default
         defaultLastLines = defaultLastLines instanceof String ? defaultLastLines.toInteger() : defaultLastLines
         if(!defaultLastLines || !(defaultLastLines instanceof Integer) || defaultLastLines < 1){
             if(defaultLastLines){
                log.warn("Invalid value for rundeck.gui.execution.tail.lines.default: Not a positive Integer: ${defaultLastLines}")
             }
             grailsApplication.config.rundeck.gui.execution.tail.lines.default = 20
         }
         def maxLastLines = grailsApplication.config.rundeck.gui.execution.tail.lines.max
         maxLastLines = maxLastLines instanceof String ? maxLastLines.toInteger() : maxLastLines
         if(!maxLastLines || !(maxLastLines instanceof Integer) || maxLastLines < 1){
             grailsApplication.config.rundeck.gui.execution.tail.lines.max = 500
         }
         healthCheckRegistry?.register("quartz.scheduler.threadPool",new HealthCheck() {
             @Override
             protected com.codahale.metrics.health.HealthCheck.Result check() throws Exception {
                 def size = quartzScheduler.getMetaData().threadPoolSize

                 def jobs = quartzScheduler.getCurrentlyExecutingJobs().size()
                 if( size > jobs ){
                     com.codahale.metrics.health.HealthCheck.Result.healthy()
                 }  else{
                     com.codahale.metrics.health.HealthCheck.Result.unhealthy("${jobs}/${size} threads used")
                 }
             }
         })
         int dbHealthTimeout = configurationService.getInteger("metrics.datasource.health.timeout", 5)
         healthCheckRegistry?.register("dataSource.connection.time", new HealthCheck() {
             @Override
             protected com.codahale.metrics.health.HealthCheck.Result check() throws Exception {
                 long start=System.currentTimeMillis()
                 def valid = dataSource.connection.isValid(60)
                 long dur=System.currentTimeMillis()-start
                 if(dur<(dbHealthTimeout*1000L)){
                     com.codahale.metrics.health.HealthCheck.Result.healthy("Datasource connection healthy with timeout ${dbHealthTimeout} seconds")
                 }  else{
                     com.codahale.metrics.health.HealthCheck.Result.unhealthy("Datasource connection timeout after ${dbHealthTimeout} seconds")
                 }
             }
         })

         int dbPingTimeout = configurationService.getInteger("metrics.datasource.ping.timeout", 60)
         metricRegistry.register(MetricRegistry.name("dataSource.connection","pingTime"),new CallableGauge<Long>({
             long start=System.currentTimeMillis()
             def valid = dataSource.connection.isValid(dbPingTimeout)
             System.currentTimeMillis()-start
         }))
         //set up some metrics collection for the Quartz scheduler
         metricRegistry.register(MetricRegistry.name("rundeck.scheduler.quartz","runningExecutions"),new CallableGauge<Integer>({
             quartzScheduler.getCurrentlyExecutingJobs().size()
         }))
         def counter = metricRegistry.counter(MetricRegistry.name("rundeck.scheduler.quartz", "scheduledJobs"))
         quartzScheduler.getListenerManager().addSchedulerListener(new MetricsSchedulerListener(counter))

         if (configurationService.executionModeActive) {
             log.info("Rundeck is ACTIVE: executions can be run.")
         } else {
             log.info("Rundeck is in PASSIVE MODE: No executions can be run.")
         }

         //configure System.out and System.err so that remote command execution will write to a specific print stream
         if(Environment.getCurrent() != Environment.TEST){

             def ThreadBoundOutputStream newOut = ThreadBoundOutputStream.bindSystemOut()
             def ThreadBoundOutputStream newErr = ThreadBoundOutputStream.bindSystemErr()

             executionService.sysThreadBoundOut=newOut
             executionService.sysThreadBoundErr=newErr
             executionUtilService.sysThreadBoundOut=newOut
             executionUtilService.sysThreadBoundErr=newErr
             executionService.defaultLogLevel=servletContext.getAttribute("LOGLEVEL_DEFAULT")


             if(configurationService.getBoolean("reportService.startup.cleanupReports", false)) {
                 timer("reportService.fixReportStatusStrings") {
                     reportService.fixReportStatusStrings()
                 }
             }

             def cleanupMode = configurationService.getString(
                     'executionService.startup.cleanupMode',
                     'async'
             )
             if ('sync' == cleanupMode) {
                 timer("executionService.cleanupRunningJobs") {
                     executionService.cleanupRunningJobs(clusterMode ? serverNodeUUID : null)
                 }
             } else {
                 log.debug("executionService.cleanupRunningJobs: starting asynchronously")
                 executionService.cleanupRunningJobsAsync(clusterMode ? serverNodeUUID : null)
             }

             if (clusterMode && configurationService.getBoolean(
                     "scheduledExecutionService.startup.claimScheduledJobs",
                     false
             )) {
                 timer("scheduledExecutionService.claimScheduledJobs") {
                     scheduledExecutionService.claimScheduledJobs(serverNodeUUID)
                 }
             }

             if(configurationService.executionModeActive) {
                 def rescheduleMode = configurationService.getString(
                         'scheduledExecutionService.startup.rescheduleMode',
                         'async'
                 )
                 if ('sync' == rescheduleMode) {
                     timer("scheduledExecutionService.rescheduleJobs") {
                         scheduledExecutionService.rescheduleJobs(clusterMode ? serverNodeUUID : null)
                     }
                 } else {
                     log.debug("scheduledExecutionService.rescheduleJobs: starting asynchronously")
                     scheduledExecutionService.rescheduleJobsAsync(clusterMode ? serverNodeUUID : null)
                 }
             }

             def resumeMode = configurationService.getString("logFileStorageService.startup.resumeMode", "")
             if ('sync' == resumeMode) {
                 timer("logFileStorageService.resumeIncompleteLogStorage") {
                     logFileStorageService.resumeIncompleteLogStorage(clusterMode ? serverNodeUUID : null)
                 }
             } else if ('async' == resumeMode) {
                 log.debug("logFileStorageService.resumeIncompleteLogStorage: resuming asynchronously")
                 logFileStorageService.resumeIncompleteLogStorageAsync(clusterMode ? serverNodeUUID : null)
             }else{
                 log.debug("logFileStorageService.resumeIncompleteLogStorage: skipping per configuration")
             }
         }
         log.info("Rundeck startup finished in ${System.currentTimeMillis()-bstart}ms")
     }

     def destroy = {
     }
} 



