import java.io.File;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream
import org.springframework.web.context.support.WebApplicationContextUtils
import org.springframework.web.context.WebApplicationContext
import org.apache.log4j.Logger
import org.apache.log4j.LogManager
import org.apache.log4j.Level
import org.apache.log4j.net.SocketAppender
import grails.util.GrailsUtil
import com.dtolabs.launcher.Setup
import org.codehaus.groovy.grails.plugins.web.filters.FilterConfig
import org.codehaus.groovy.grails.plugins.web.filters.FilterToHandlerAdapter

class BootStrap {

    def grailsApplication
    def scheduledExecutionService
    def executionService
    def filterInterceptor

     def init = { servletContext ->

         filterInterceptor.handlers.sort { FilterToHandlerAdapter handler1,
                                           FilterToHandlerAdapter handler2 ->
             FilterConfig filter1 = handler1.filterConfig
             FilterConfig filter2 = handler2.filterConfig
             filter1.name <=> filter2.name
         }

         def String rdeckBase
         if(!grailsApplication.config.rdeck.base){
             //look for system property
             rdeckBase=System.getProperty('rdeck.base')
             log.warn("using rdeck.base system property: ${rdeckBase}");
         }else{
             rdeckBase=grailsApplication.config.rdeck.base
             log.warn("using rdeck.base config property: ${rdeckBase}");
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
             def configDir = Constants.getFrameworkConfigDir(rdeckBase)
             File fprops = new File(configDir, "framework.properties")
             if (!fprops.exists()){
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
                     def urlstr= url.toExternalForm().replaceAll('/+$','')
                     setup.getParameters().properties["framework.server.url"]=urlstr
                     setup.getParameters().properties["framework.rundeck.url"]= urlstr
                 }else{
                     //determine hostname
                     hostname=InetAddress.getLocalHost().getHostName()
                     if(hostname=~/^[\d\.]+$/){
                         hostname="localhost"
                     }
                     port="4440"
                 }
                 setup.getParameters().setNodeArg(hostname)
                 setup.getParameters().properties["framework.server.port"]=port

                 setup.performSetup()
                 log.info("Rundeck initialization complete.")
             }
             if (! fprops.exists()) {
                 throw new RuntimeException("framework configuration file not found: " + fprops.getAbsolutePath())
             }
             InputStream is = new FileInputStream(fprops);
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
             log.info("loaded configuration: " + fprops.getAbsolutePath())

             if(properties.containsKey("framework.node")){
                 servletContext.setAttribute("FRAMEWORK_NODE", properties.getProperty("framework.node"))
             }
         }


         //initialize execution service plugin
//         com.dtolabs.rundeck.core.execution.ExecutionServiceFactory.instance().setDefaultExecutorClass(WorkflowExecutionItem.class,WorkflowExecutor.class)
//         com.dtolabs.rundeck.core.execution.ExecutionServiceFactory.instance().setDefaultExecutor(JobExecutionItem.class,executionService)


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
             if(maxLastLines){
                log.warn("Invalid value for rundeck.gui.execution.tail.lines.max: Not a positive Integer ${maxLastLines}")
             }
             grailsApplication.config.rundeck.gui.execution.tail.lines.max = 100
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

             executionService.cleanupRunningJobs()
             scheduledExecutionService.rescheduleJobs()
             scheduledExecutionService.convertNonWorkflowJobs()
         }

        
         if (GrailsUtil.environment == "development") {
             
         }

     }

     def destroy = {
     }
} 



