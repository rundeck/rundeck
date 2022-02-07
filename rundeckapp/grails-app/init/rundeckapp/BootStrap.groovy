package rundeckapp

import com.codahale.metrics.MetricFilter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.health.HealthCheck
import com.codahale.metrics.health.HealthCheckRegistry
import com.dtolabs.launcher.Setup

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
import com.dtolabs.rundeck.app.api.ApiMarshallerRegistrar
import com.dtolabs.rundeck.core.Constants
import com.dtolabs.rundeck.core.VersionConstants
import com.dtolabs.rundeck.core.config.Features
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream
import com.dtolabs.rundeck.util.quartz.MetricsSchedulerListener
import com.fasterxml.jackson.databind.ObjectMapper
import grails.converters.JSON
import grails.events.bus.EventBus
import grails.plugin.springsecurity.SecurityFilterPosition
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.util.Environment
import groovy.sql.Sql
import org.grails.plugins.metricsweb.CallableGauge
import org.quartz.Scheduler
import rundeck.services.feature.FeatureService
import webhooks.Webhook

import javax.servlet.ServletContext
import java.nio.charset.Charset
import java.text.SimpleDateFormat

class BootStrap {

    public static final String WORKFLOW_CONFIG_FIX973 = 'workflowConfigFix973'
    def grailsApplication
    def scheduledExecutionService
    def executionService
    def executionUtilService
    def frameworkService
    def workflowService
    def logFileStorageService
    def rundeckFilesystemProjectImporter
    def reportService
    def configurationService
    def fileUploadService
    def filterInterceptor
    Scheduler quartzScheduler
    MetricRegistry metricRegistry
    def messageSource
    def scmService
    HealthCheckRegistry healthCheckRegistry
    def dataSource
    ApiMarshallerRegistrar apiMarshallerRegistrar
    def authenticationManager
    def EventBus grailsEventBus
    def configStorageService
    FeatureService featureService

    def timer(String name,Closure clos){
        long bstart=System.currentTimeMillis()
        log.debug("BEGIN: ${name}")
        def res=clos()
        log.debug("${name} in ${System.currentTimeMillis()-bstart}ms")
        return res
    }

    def init = { ServletContext servletContext ->
        // Marshal enums to "STRING" instead of {"enumType":"com.package.MyEnum", "name":"OBJECT"}
        JSON.registerObjectMarshaller(Enum, { Enum e -> e.toString() })

        //setup profiler logging
        if(!(grailsApplication.config.getProperty("grails.profiler.disable", Boolean.class, false)) && grailsApplication.mainContext.profilerLog) {
            //re-enable log output for profiler info, which is disabled by miniprofiler
            grailsApplication.mainContext.profilerLog.appenderNames = ["loggingAppender", 'miniProfilerAppender']
        }
        long bstart=System.currentTimeMillis()
        apiMarshallerRegistrar.registerApiMarshallers()
        //version info
        servletContext.setAttribute("version.build",VersionConstants.BUILD)
        servletContext.setAttribute("version.date",VersionConstants.DATE)
        servletContext.setAttribute("version.date_string",VersionConstants.DATE_STRING)
        def shortBuildDate = new SimpleDateFormat("yyyy-MM-dd").format(VersionConstants.DATE)
        servletContext.setAttribute("version.date_short", shortBuildDate)
        servletContext.setAttribute("version.number",VersionConstants.VERSION)
        servletContext.setAttribute("version.ident",VersionConstants.VERSION_IDENT)
        def appname=messageSource.getMessage('main.app.name',null,'',null) ?: messageSource.getMessage('main.app.default.name',null,'',null) ?: 'Rundeck'

        servletContext.setAttribute("app.ident",grailsApplication.metadata['build.ident'])
        log.info("Starting ${appname} ${servletContext.getAttribute('app.ident')} ($shortBuildDate) ...")
        if(Boolean.getBoolean('rundeck.bootstrap.build.info')){
            def buildInfo=grailsApplication.metadata.findAll{it.key?.startsWith('build.core.git.')}
            log.info("${appname} Build: ${buildInfo}")
        }
        /*filterInterceptor.handlers.sort { FilterToHandlerAdapter handler1,
                                          FilterToHandlerAdapter handler2 ->
            FilterConfig filter1 = handler1.filterConfig
            FilterConfig filter2 = handler2.filterConfig
            filter1.name <=> filter2.name
        }*/

        def String rdeckBase
        if(!grailsApplication.config.getProperty("rdeck.base",String.class)){
            //look for system property
            rdeckBase=System.getProperty('rdeck.base')
            log.info("using rdeck.base system property: ${rdeckBase}");
            def newconf= new ConfigObject()
            newconf.rdeck.base = rdeckBase
            grailsApplication.config.merge(newconf)
        }else{
            rdeckBase=grailsApplication.config.getProperty("rdeck.base",String.class)
            log.info("using rdeck.base config property: ${rdeckBase}");
        }
        def serverLibextDir = grailsApplication.config.getProperty("rundeck.server.plugins.dir",String.class, "${rdeckBase}/libext")
        File pluginsDir = new File(serverLibextDir)
        def clusterMode = false
        def serverNodeUUID = null
        def canApplyServerUpdates = true
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
                if (grailsApplication.config.getProperty("grails.serverURL", String.class)) {
                    URL url = new URL(grailsApplication.config.getProperty("grails.serverURL",String.class))
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
                setup.getParameters().properties["rundeck.server.uuid"] = System.getenv("RUNDECK_SERVER_UUID") ?: System.getProperty("rundeck.server.uuid",UUID.randomUUID().toString())
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
            clusterMode = grailsApplication.config.getProperty("rundeck.clusterMode.enabled", Boolean.class, false)
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

                String primaryServerId = configurationService.getString("primaryServerId")
                //If a primary server id is set then use this server to apply server updates, otherwise
                //allow this server to apply updates even though another server might be doing the same updates concurrently
                canApplyServerUpdates = primaryServerId ? primaryServerId == serverNodeUUID : true

                if(!primaryServerId) log.warn("Running in cluster mode without rundeck.primaryServerId set. Please set rundeck.primaryServerId to the UUID of the primary server in the cluster")
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
                def splitRegex = " *, *"
                userTokens.each { k, v ->
                    def roles='api_token_group'
                    def tokenTmp=v
                    def split = v.toString().split(splitRegex)
                    if (split.length > 1) {
                        tokenTmp = split[0]
                        def groupList = split.drop(1)
                        roles = groupList.join(',')
                    }
                    tokens[tokenTmp]=k+','+roles
                }
                servletContext.setAttribute("TOKENS_FILE_PATH", new File(tokensfile).absolutePath)
                servletContext.setAttribute("TOKENS_FILE_PROPS", tokens)
                if (tokens) {
                    log.debug("Loaded ${tokens.size()} tokens from tokens file: ${tokensfile}...")
                }
            }
            //begin import at bootstrap time
            rundeckFilesystemProjectImporter.bootstrap()
        }
        executionService.initialize()

        //initialize manually to avoid circular reference problem with spring
        timer("Initialized WorkflowService"){
            workflowService.initialize()
        }
        timer("Initialized ScmService"){
            scmService.initialize()
        }
        String logLevel = grailsApplication.config.getProperty("loglevel.default",String.class)
        if(logLevel){
            servletContext.setAttribute("LOGLEVEL_DEFAULT", logLevel)
        }else{
            servletContext.setAttribute("LOGLEVEL_DEFAULT", "INFO")
        }

        if(grailsApplication.config.getProperty("rss.enabled",Boolean.class, false)){
            servletContext.setAttribute("RSS_ENABLED", 'true')
            log.info("RSS feeds enabled")
        }else{
            log.info("RSS feeds disabled")
        }

        //Setup the correct authentication provider for the configured authentication mechanism
        if(grailsApplication.config.getProperty("rundeck.useJaas",Boolean.class, false)) {
            log.info("Using jaas authentication")
            SpringSecurityUtils.clientRegisterFilter("jaasApiIntegrationFilter", SecurityFilterPosition.OPENID_FILTER.order + 150)
            authenticationManager.providers.add(grailsApplication.mainContext.getBean("jaasAuthProvider"))
        } else {
            log.info("Using builtin realm authentication")
            authenticationManager.providers.add(grailsApplication.mainContext.getBean("realmAuthProvider"))
        }

        if(grailsApplication.config.getProperty("rundeck.security.authorization.preauthenticated.enabled",Boolean.class, false)
                || grailsApplication.config.getProperty("grails.plugin.springsecurity.useX509",Boolean.class, false)){
            authenticationManager.providers.add(grailsApplication.mainContext.getBean("preAuthenticatedAuthProvider"))
        }

        if(grailsApplication.config.getProperty("rundeck.security.authorization.preauthenticated.enabled",Boolean.class, false)){
            SpringSecurityUtils.clientRegisterFilter("rundeckPreauthFilter", SecurityFilterPosition.PRE_AUTH_FILTER.order - 10)
            log.info("Preauthentication is enabled")
        } else {
            log.info("Preauthentication is disabled")
        }

        if(grailsApplication.config.getProperty("rundeck.security.enforceMaxSessions", Boolean.class, false)) {
            SpringSecurityUtils.clientRegisterFilter("concurrentSessionFilter", SecurityFilterPosition.CONCURRENT_SESSION_FILTER.order)
        }

        String redirectUrl = grailsApplication.config.getProperty("rundeck.logout.redirect.url",String.class, '')
        if(!redirectUrl.isEmpty()) {
            log.debug("Setting logout url to: ${redirectUrl}")
            def logoutSuccessHandler = grailsApplication.mainContext.getBean("logoutSuccessHandler")
            logoutSuccessHandler.defaultTargetUrl = redirectUrl
        }

        String executionFollowBuffer = grailsApplication.config.getProperty("execution.follow.buffersize",String.class)
        if(executionFollowBuffer){
            servletContext.setAttribute("execution.follow.buffersize",executionFollowBuffer)
        }else{
            servletContext.setAttribute("execution.follow.buffersize",(50*1024).toString())
        }
        boolean markdownEnabled = grailsApplication.config.getProperty("output.markdown.enabled",Boolean.class, false)
        if(markdownEnabled){
            servletContext.setAttribute("output.markdown.enabled",markdownEnabled)
        }else{
            servletContext.setAttribute("output.markdown.enabled","false")
        }
        String nowrunningInterval = grailsApplication.config.getProperty("nowrunning.interval",String.class)
        if(nowrunningInterval){
            servletContext.setAttribute("nowrunning.interval",nowrunningInterval)
        }else{
            servletContext.setAttribute("nowrunning.interval",(15).toString())
        }
        boolean outputDownloadFormatted = grailsApplication.config.getProperty("output.download.formatted",Boolean.class, false)
        if(outputDownloadFormatted){
            servletContext.setAttribute("output.download.formatted",outputDownloadFormatted)
        }else{
            servletContext.setAttribute("output.download.formatted","true")
        }
        boolean loggingMetadata = grailsApplication.config.getProperty("logging.ant.metadata",Boolean.class, false)
        if(loggingMetadata){
            servletContext.setAttribute("logging.ant.metadata",loggingMetadata)
        }else{
            servletContext.setAttribute("logging.ant.metadata","true")
        }
        def defaultLastLines = grailsApplication.config.getProperty("rundeck.gui.execution.tail.lines.default",String.class, '')
        defaultLastLines = defaultLastLines instanceof String ? defaultLastLines.toInteger() : defaultLastLines
        if(!defaultLastLines || !(defaultLastLines instanceof Integer) || defaultLastLines < 1){
            if(defaultLastLines){
                log.warn("Invalid value for rundeck.gui.execution.tail.lines.default: Not a positive Integer: ${defaultLastLines}")
             }
             grailsApplication.config.rundeck.gui.execution.tail.lines.default = 20
         }
         def maxLastLines = grailsApplication.config.getProperty("rundeck.gui.execution.tail.lines.max",String.class, '')
         maxLastLines = maxLastLines instanceof String ? maxLastLines.toInteger() : maxLastLines
         if(!maxLastLines || !(maxLastLines instanceof Integer) || maxLastLines < 1){
             grailsApplication.config.rundeck.gui.execution.tail.lines.max = 500
         }
         if(featureService.featurePresent(Features.CLEAN_EXECUTIONS_HISTORY)){
             log.debug("Feature 'cleanExecutionHistoryJob' is enabled")
             if(featureService.featurePresent(Features.CLEAN_EXECUTIONS_HISTORY_ASYNC_START)){
                 frameworkService.rescheduleAllCleanerExecutionsJobAsync()
             }else{
                 frameworkService.rescheduleAllCleanerExecutionsJob()
             }
         } else {
             log.debug("Feature 'cleanExecutionHistoryJob' is disabled")
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
                def metricDataSource=dataSource.connection
                def valid = metricDataSource.isValid(60)
                long dur=System.currentTimeMillis()-start
                metricDataSource.close()
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
            def metricDataSource=dataSource.connection
            def valid = metricDataSource.isValid(dbPingTimeout)
            System.currentTimeMillis()-start
            metricDataSource.close()
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

        if (!Charset.defaultCharset().equals(Charset.forName("UTF-8"))) {
            log.warn("The JVM default encoding is not UTF-8: "
                    + Charset.defaultCharset().displayName()
                    + ", you may not see output as expected for multibyte locales. " +
                    "Specify -Dfile.encoding=UTF-8 in the JVM options."
            )
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
            def cleanupStatus = configurationService.getString(
                    'executionService.startup.cleanupStatus',
                    'incomplete'
            )
            if ('sync' == cleanupMode) {
                timer("executionService.cleanupRunningJobs") {
                    executionService.cleanupRunningJobs(clusterMode ? serverNodeUUID : null, cleanupStatus, new Date())
                }
            } else {
                log.debug("executionService.cleanupRunningJobs: starting asynchronously")

                executionService.cleanupRunningJobsAsync(
                        clusterMode ? serverNodeUUID : null,
                        cleanupStatus,
                        new Date()
                )
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

             logFileStorageService.cleanupDuplicates()
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
             fileUploadService.onBootstrap()

            if(grailsApplication.config.getProperty("dataSource.driverClassName",String.class,'')=='org.h2.Driver'){
                log.warn("[Development Mode] Usage of H2 database is recommended only for development and testing")
            }
            if(canApplyServerUpdates) {
                applyWorkflowConfigFix()
                ensureTypeOnAuthToken()
                ensureWebhookUuids()
            }

        }
        grailsEventBus.notify('rundeck.bootstrap')
        log.info("Rundeck startup finished in ${System.currentTimeMillis()-bstart}ms")
    }

    def applyWorkflowConfigFix() {
        if (grailsApplication.config.getProperty("rundeck.applyFix.${WORKFLOW_CONFIG_FIX973}",Boolean.class, false)
                || !configStorageService.hasFixIndicator(WORKFLOW_CONFIG_FIX973)) {
            try {
                log.info("$WORKFLOW_CONFIG_FIX973: applying... ")
                Map result = workflowService.applyWorkflowConfigFix973()
                if (result) {
                    if (!result.success) {
                        log.warn("$WORKFLOW_CONFIG_FIX973: fix process was finished with errors")
                    }
                    if (result.invalidCount == 0) {
                        log.info("$WORKFLOW_CONFIG_FIX973: No fix was needed. Storing fix application state.")
                    } else {
                        log.warn("$WORKFLOW_CONFIG_FIX973: Fixed ${result.invalidCount} workflows. Storing fix application state.")
                    }
                    final ObjectMapper mapper = new ObjectMapper()
                    String resultAsString = mapper.writeValueAsString(result)
                    configStorageService.writeFileResource(
                            configStorageService.getSystemFixIndicatorPath(WORKFLOW_CONFIG_FIX973),
                            new ByteArrayInputStream(resultAsString.bytes),
                            [:]
                    )
                } else {
                    log.error("$WORKFLOW_CONFIG_FIX973: The fix process did not return any results")
                }
            }catch(Throwable t){
                log.error("$WORKFLOW_CONFIG_FIX973: The fix process threw an exception: $t", t)
            }
        }
    }

    def ensureTypeOnAuthToken() {
        Sql sql = new Sql(dataSource)
        try {
            int updatedRows = sql.executeUpdate("UPDATE auth_token SET type = 'USER' WHERE type = ''")
            if(updatedRows) {
                log.info("Updated ${updatedRows} auth_token from type '' to 'USER'")
            }
        } catch(Exception ex) {
            log.warn("Unable to ensure all auth tokens have a type. Please run the following sql statement manually on your Rundeck database: ")
            log.warn("UPDATE auth_token SET type = 'USER' WHERE type = ''")
            log.error("Update execution error: ",ex)
        }
    }

    def ensureWebhookUuids() {
        Webhook.findByUuidIsNull().each { hook ->
            hook.uuid = UUID.randomUUID().toString()
            hook.save()
        }
    }

     def destroy = {
         metricRegistry?.removeMatching(MetricFilter.ALL)
         log.info("Rundeck Shutdown detected")
     }
}



