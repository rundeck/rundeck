import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream
import org.springframework.web.context.support.WebApplicationContextUtils
import org.springframework.web.context.WebApplicationContext
import org.apache.log4j.Logger
import org.apache.log4j.LogManager
import org.apache.log4j.Level
import org.apache.log4j.net.SocketAppender
import com.dtolabs.rundeck.services.InputServer
import com.dtolabs.rundeck.services.ReportAppender
import grails.util.GrailsUtil
import com.dtolabs.rundeck.execution.WorkflowExecutionItem
import com.dtolabs.rundeck.execution.WorkflowExecutor
import com.dtolabs.rundeck.execution.JobExecutionItem
import com.dtolabs.launcher.Setup


class BootStrap {

     def reportService
    def grailsApplication
    def scheduledExecutionService
    def executionService

     static InputServer inputServer

     def init = { servletContext ->
         def String rdeckBase
         if(!grailsApplication.config.rdeck.base){
             //look for system property
             rdeckBase=System.getProperty('rdeck.base')
             System.err.println("using rdeck.base system property: ${rdeckBase}");
         }else{
             rdeckBase=grailsApplication.config.rdeck.base
             System.err.println("using rdeck.base config property: ${rdeckBase}");
         }
         if("test"!=GrailsUtil.environment){
             if(!rdeckBase){
                 throw new RuntimeException("config file did not contain property: rdeck.base")
             }
             servletContext.setAttribute("RDECK_BASE", rdeckBase)
             log.info("config: rdeck.base = " + rdeckBase)

             File basedir = new File(rdeckBase)
             if(!basedir.isDirectory()) {
                 basedir.mkdir()
             }
             //see if initialization system property is set
             if (!new File(basedir,"etc").isDirectory()){
                 log.info("Performing rundeck first-run initialization...")
                 //setup the base dir
                 Setup setup=new Setup()
                 setup.getParameters().setBaseDir(basedir.getAbsolutePath())
                 setup.getParameters().setHomeDir(basedir.getAbsolutePath())
                 //determine hostname and port from grails config if available
                 String hostname
                 String port
                 if(grailsApplication.config.grails.serverURL){
                     URL url = new URL(grailsApplication.config.grails.serverURL)
                     hostname=url.getHost()
                     port=url.getPort().toString()
                     setup.getParameters().properties["framework.server.url"]=url.toExternalForm()
                     setup.getParameters().properties["framework.rundeck.url"]=url.toExternalForm()
                 }else{
                     //determine hostname
                     hostname=InetAddress.getLocalHost().getHostName()
                     if(hostname=~/^[\d\.]+$/){
                         hostname="localhost"
                     }
                     port="8080"
                 }
                 setup.getParameters().setNodeArg(hostname)
                 setup.getParameters().properties["framework.server.port"]=port

                 setup.performSetup()
                 log.info("Rundeck initialization complete.")
             }

             File f = new File(rdeckBase, "etc/framework.properties")
             if (! f.exists()) {
                 throw new RuntimeException("framework configuration file not found: " + f.getAbsolutePath())
             }
             InputStream is = new FileInputStream(f);
             Properties properties = new Properties()
             try{
                properties.load(is)
             }finally{
                 if(null!=is){
                     is.close()
                 }
             }

             Properties props2 = com.dtolabs.rundeck.core.utils.PropertyUtil.expand(properties)
             servletContext.setAttribute("FRAMEWORK_PROPERTIES", props2)
             log.info("loaded configuration: " + f.getAbsolutePath())

             if(properties.containsKey("framework.node")){
                 servletContext.setAttribute("FRAMEWORK_NODE", properties.getProperty("framework.node"))
             }
         }


         //initialize execution service plugin
         com.dtolabs.rundeck.core.execution.ExecutionServiceFactory.instance().setDefaultExecutorClass(WorkflowExecutionItem.class,WorkflowExecutor.class)
         com.dtolabs.rundeck.core.execution.ExecutionServiceFactory.instance().setDefaultExecutor(JobExecutionItem.class,executionService)


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
             log.info("Execution Output Follow buffer size = "+grailsApplication.config.execution.follow.buffersize)
         }else{
             servletContext.setAttribute("execution.follow.buffersize",(50*1024).toString())
             log.info("Execution Output Follow buffer size (default) = "+(50*1024).toString())
         }
         if(grailsApplication.config.output.markdown.enabled){
             servletContext.setAttribute("output.markdown.enabled",grailsApplication.config.output.markdown.enabled=="true"?"true":"false")
         }else{
             servletContext.setAttribute("output.markdown.enabled","true")
         }
         if("true" != servletContext.getAttribute("output.markdown.enabled")){
             log.info("Execution Output Markdown is disabled.")
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

         //configure mapped role definitions
         if(grailsApplication.config.mappedRoles && grailsApplication.config.mappedRoles instanceof Map){
             def rolemap=[:]
             rolemap.putAll(grailsApplication.config.mappedRoles)
             servletContext.setAttribute("MAPPED_ROLES",rolemap)
         }else{
             servletContext.setAttribute("MAPPED_ROLES",[:])
         }


         //configure System.out and System.err so that remote command execution will write to a specific print stream
         if("test"!=GrailsUtil.environment){
             PrintStream oldout = System.out;
             PrintStream olderr = System.err;

             def ThreadBoundOutputStream newOut = new ThreadBoundOutputStream(oldout)
             def ThreadBoundOutputStream newErr = new ThreadBoundOutputStream(olderr)

             System.setOut(new PrintStream(newOut));
             System.setErr(new PrintStream(newErr));

             def WebApplicationContext appCtx = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext)
             appCtx.executionService.sysThreadBoundOut=newOut
             appCtx.executionService.sysThreadBoundErr=newErr
             appCtx.executionService.defaultLogLevel=servletContext.getAttribute("LOGLEVEL_DEFAULT")

             System.err.println("Installed outputstreams.")

             System.err.println("--------------------------------------")
             executionService.cleanupRunningJobs()
             scheduledExecutionService.rescheduleJobs()
             scheduledExecutionService.convertNonWorkflowJobs()
         }

         /****************************************
          * Transplanted Reportcenter init content
          ****************************************/

         //get the  common Logger, and configure the level and report appender
         Logger commonLogger = LogManager.getLoggerRepository().getLogger("com.dtolabs.rundeck.log.common")
         Logger commonLogger2 = LogManager.getLoggerRepository().getLogger("com.dtolabs.rundeck.log.internal")
         ReportAppender appender = new ReportAppender(reportService)
         commonLogger.addAppender(appender)
         commonLogger.setLevel(Level.INFO)
         commonLogger2.addAppender(appender)
         commonLogger2.setLevel(Level.INFO)
         if (GrailsUtil.environment == "development") {
             for (i in 1..1) {
                 def long now = System.currentTimeMillis()
                 new ExecReport(
                     title: "Test Exec",
                     status: "succeed",
                     actionType: "succeed",
                     ctxProject: "Test",
                     ctxType: "ProjectBuilder",
                     ctxName: "elements",
                     ctxCommand: "Build",
                     ctxController: "Builder",
                     reportId: "test1",
                     author: "greg",
                     dateStarted: new Date(now - (1000 * 60 * 30)),
                     dateCompleted: new Date(now - (1000 * 60 * 30)),
                     message: """Start: "tests all fileutil commands" commands: testFileutilAvailable,testFileutilCopy,testFileutilExecutable,testFileutilLink,testFileutilLs,testFileutilMkdir,testFileutilMove,testFileutilNewer,testFileutilOlder,testFileutilReadable,testFileutilWriteable,testFileutilTouch,testFileutilRemove,testFileutilRmdir""",
                     node: "localhost").save()
                 new ExecReport(
                     title: "Test Exec2",
                     status: "succeed",
                     actionType: "succeed",
                     ctxProject: "Test",
                     ctxCommand: "Build",
                     ctxController: "Builder",
                     author: "greg",
                     dateStarted: new Date(now - (1000 * 60 * 10)),
                     dateCompleted: new Date(now - (1000 * 60 * 10)),
                     message: """Start: "tests all fileutil commands" commands: testFileutilAvailable,testFileutilCopy,testFileutilExecutable,testFileutilLink,testFileutilLs,testFileutilMkdir,testFileutilMove,testFileutilNewer,testFileutilOlder,testFileutilReadable,testFileutilWriteable,testFileutilTouch,testFileutilRemove,testFileutilRmdir""",
                     node: "localhost").save()
                 new ExecReport(
                     title: "Test Job 1",
                     status: "succeed",
                     actionType: "succeed",
                     ctxProject: "Test",
                     ctxCommand: "Build",
                     ctxController: "Builder",
                     tags: "dev1,blah2",
                     jcJobId: "5",
                     jcExecId: "5",
                     author: "greg",
                     dateStarted: new Date(now - (1000 * 60 * 60)),
                     dateCompleted: new Date(now - (1000 * 60 * 30)),
                     message: "Job Completed",
                     node: "localhost").save()
                 new ExecReport(
                     title: "Test Job 2",
                     status: "fail",
                     actionType: "fail",
                     ctxProject: "Test",
                     ctxCommand: "Build",
                     ctxController: "Builder",
                     jcJobId: "6",
                     jcExecId: "6",
                     author: "greg",
                     dateStarted: new Date(now - (1000 * 60 * 60 * 4)),
                     dateCompleted: new Date(now - (1000 * 60 * 60 * 2)),
                     message: "Job Failed",
                     node: "somenode").save()
                 new ExecReport(
                     title: "Test Build Job",
                     status: "fail",
                     actionType: "succeed",
                     ctxProject: "Test",
                     ctxCommand: "Build",
                     ctxController: "Builder",
                     jcExecId: "6",
                     author: "greg",
                     dateStarted: new Date(now - (1000 * 60 * 60 * 4)),
                     dateCompleted: new Date(now - (1000 * 60 * 60 * 2)),
                     message: "Build Job Succeeded",
                     node: "somenode").save()
                 new ExecReport(
                     title: "Test Adhoc Job",
                     status: "fail",
                     actionType: "fail",
                     ctxProject: "Test",
//                ctxCommand:"Build",
//                ctxController:"Builder",
                     adhocExecution: true,
                     adhocScript: "uname -a",
                     jcExecId: "6",
                     author: "greg",
                     dateStarted: new Date(now - (1000 * 60 * 60 * 4)),
                     dateCompleted: new Date(now - (1000 * 60 * 60 * 2)),
                     message: "Build Job Succeeded",
                     node: "somenode").save()
                 new ExecReport(
                     title: "Test2 Adhoc Job",
                     status: "succeed",
                     actionType: "succeed",
                     ctxProject: "Test",
//                ctxCommand:"Build",
//                ctxController:"Builder",
                     adhocExecution: true,
                     adhocScript: "blah",
                     jcExecId: "9",
                     author: "greg",
                     dateStarted: new Date(now - (1000 * 60 * 60 * 4)),
                     dateCompleted: new Date(now - (1000 * 60 * 60 * 2)),
                     message: "ok",
                     node: "elfnode").save()

             }
         }

         //start the report server if port is set and is not disabled
         if ("true" != grailsApplication.config.reportservice.log4j.disabled.toString()) {
             int port = -1
             if (!grailsApplication.config.reportservice.log4j.port) {
                 port = 1055
                 log.warn("'reportservice.log4j.port' configuration property not set: using default listen port (1055)")
             } else {
                 def t = grailsApplication.config.reportservice.log4j.port
                 port = Integer.parseInt(t.toString())
             }

             if("test"!=GrailsUtil.environment){
                 inputServer = new InputServer(port)

                 if (servletContext['inputServer']) {
                     servletContext['inputServer'].finish()
                     servletContext['inputServer'] = null
                 }
                 servletContext['inputServer'] = inputServer
                 try {
                     inputServer.begin()
                     log.error("Started log4j report input server on port: ${port}")
                 } catch (IOException e) {
                     log.error("FAILED to start log4j report input server on port: ${port}: " + e.getMessage())
                 }

             }
         } else {
             System.err.println("log4j report input server is disabled.")
         }
     }

     def destroy = {
     }
} 



