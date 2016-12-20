package rundeck.services

import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import com.dtolabs.rundeck.server.plugins.DescribedPlugin
import com.dtolabs.rundeck.server.plugins.ValidatedPlugin
import com.dtolabs.rundeck.server.plugins.services.NotificationPluginProviderService
import groovy.xml.MarkupBuilder
import org.apache.commons.httpclient.Header
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.StringRequestEntity
import org.apache.commons.httpclient.params.HttpClientParams
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.Execution
import rundeck.Notification
import rundeck.ScheduledExecution
import rundeck.User
import rundeck.controllers.ExecutionController
import rundeck.services.logging.ExecutionLogState

import java.text.SimpleDateFormat

/*
* Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*        http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
/*
 * NotificationService.java
 *
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: May 17, 2010 6:03:16 PM
 * $Id$
 */

public class NotificationService implements ApplicationContextAware{
    boolean transactional = false
    def grailsLinkGenerator

    ApplicationContext applicationContext
    def grailsApplication
    def mailService
    def pluginService
    def NotificationPluginProviderService notificationPluginProviderService
    def FrameworkService frameworkService
    def LoggingService loggingService

    def ValidatedPlugin validatePluginConfig(String project, String name, Map config) {
        return pluginService.validatePlugin(name, notificationPluginProviderService,
                frameworkService.getFrameworkPropertyResolver(project, config), PropertyScope.Instance, PropertyScope.Project)
    }
    def ValidatedPlugin validatePluginConfig(String name, Map projectProps, Map config) {
        return pluginService.validatePlugin(name, notificationPluginProviderService,
                frameworkService.getFrameworkPropertyResolverWithProps(projectProps, config), PropertyScope.Instance, PropertyScope.Project)
    }
    /**
     *
     * @param name
     * @return map containing [instance:(plugin instance), description: (map or Description), ]
     */
    def DescribedPlugin getNotificationPluginDescriptor(String name) {
        return pluginService.getPluginDescriptor(name, notificationPluginProviderService)
    }
    def Map listNotificationPlugins(){
        return pluginService.listPlugins(NotificationPlugin,notificationPluginProviderService)
    }
    def boolean triggerJobNotification(String trigger, schedId, Map content){
        if(trigger && schedId){
            ScheduledExecution.withNewSession {
                def ScheduledExecution sched = ScheduledExecution.get(schedId)
                if(null!=sched){
                    return triggerJobNotification(trigger,sched,content)
                }
            }
        }
        return false
    }
    /**
     * Replace template variables in the text.
     * @param templateText
     * @param context data context
     * @return replaced text
     */
    def String renderTemplate(String templateText, Map context){
        return DataContextUtils.replaceDataReferences(templateText,context,null,false,false)
    }
    /**
     * write log output to a temp file, optionally formatted.
     * @param e
     * @param isFormatted
     * @return
     */
    def File copyExecOutputToTempFile(Execution e, boolean isFormatted){
        def reader = loggingService.getLogReader(e)
        if (reader.state == ExecutionLogState.NOT_FOUND||reader.state == ExecutionLogState.ERROR||reader.state !=
                ExecutionLogState.AVAILABLE) {
            return null
        }
        SimpleDateFormat logFormater = new SimpleDateFormat("HH:mm:ss", Locale.US);
        logFormater.timeZone = TimeZone.getTimeZone("GMT")
        def iterator = reader.reader
        iterator.openStream(0)
        def lineSep = System.getProperty("line.separator")
        File temp = File.createTempFile("output-${e.id}",".txt")
        temp.deleteOnExit()
        temp.withWriter {Writer w->
            iterator.findAll { it.eventType == LogUtil.EVENT_TYPE_LOG }.each { LogEvent msgbuf ->
                def message = msgbuf.message
                if(message.contains("\033")){
                    message=message.decodeAnsiColorStrip()
                }
                w << (isFormatted ? "${logFormater.format(msgbuf.datetime)} [${msgbuf.metadata?.user}@${msgbuf.metadata?.node} ${msgbuf.metadata?.stepctx ?: '_'}][${msgbuf.loglevel}] ${message}" : message)
                w << lineSep
            }
        }
        iterator.close()
        return temp
    }
    private static Map<String,String> toStringStringMap(Map input){
        def map = new HashMap<String, String>()
        for (Object o : input.keySet()) {
            map.put(o.toString(),input.get(o)? input.get(o).toString():'')
        }
        return map;
    }
    def boolean triggerJobNotification(String trigger,ScheduledExecution source, Map content){
        def didsend = false
        if(source.notifications && source.notifications.find{it.eventTrigger=='on'+trigger}){
            def notes = source.notifications.findAll{it.eventTrigger=='on'+trigger}
            notes.each{ Notification n ->
                try{
                if(n.type=='email'){
                    //sending notification of a status trigger for the Job
                    def Execution exec = content.execution
                    def mailConfig = n.mailConfiguration()
                    def destarr=mailConfig.recipients?.split(",") as List
                    def configSubject=mailConfig.subject
                    def configAttachLog=mailConfig.attachLog
                    final state = ExecutionService.getExecutionState(exec)
                    def statMsg=[
                            (ExecutionService.EXECUTION_ABORTED):'KILLED',
                            (ExecutionService.EXECUTION_FAILED):'FAILURE',
                            (ExecutionService.EXECUTION_RUNNING):'STARTING',
                            (ExecutionService.EXECUTION_SUCCEEDED):'SUCCESS',
                            (ExecutionService.EXECUTION_TIMEDOUT):'TIMEDOUT',
                    ]

                    //prep execution data
                    def appUrl = grailsLinkGenerator.link(action: 'home', controller: 'menu',absolute: true)
                    def projUrl = grailsLinkGenerator.link(action: 'index', controller: 'menu', params: [project:  exec.project], absolute: true)

                    def execMap = generateExecutionData(exec, content)
                    def jobMap=exportJobdata(source)
                    Map context = generateContextData(exec, content)
                    def contextMap=[:]

                    execMap.projectHref = projUrl

                    contextMap['job'] = toStringStringMap(jobMap)
                    contextMap['execution']=toStringStringMap(execMap)
                    contextMap['rundeck']=['href': appUrl]

                    context = DataContextUtils.merge(context, contextMap)
                    context = DataContextUtils.addContext("notification", [trigger: trigger, eventStatus: statMsg[state]], context)

                    //set up templates
                    def subjecttmpl='${notification.eventStatus} [${exec.project}] ${job.group}/${job.name} ${exec' +
                            '.argstring}'
					if(grailsApplication.config.rundeck.mail."${source.project}"?."${source.jobName}"?.template?.subject) {
						subjecttmpl= grailsApplication.config.rundeck.mail."${source.project}"?."${source.jobName}"?.template?.subject.toString()
					}else if(grailsApplication.config.rundeck.mail."${trigger}"?.template?.subject){
                        subjecttmpl= grailsApplication.config.rundeck.mail."${trigger}".template.subject.toString()
                    }else if (grailsApplication.config.rundeck.mail.template.subject) {
                        subjecttmpl=grailsApplication.config.rundeck.mail.template.subject.toString()
                    }
                    if(configSubject){
                        subjecttmpl= configSubject
                    }
                    def subjectmsg = renderTemplate(subjecttmpl, context)

                    def htmlemail=null
                    def templatePaths=[]
					if(grailsApplication.config.rundeck.mail."${source.project}"."${source.jobName}".template.body) {
						htmlemail = renderTemplate(grailsApplication.config.rundeck.mail."${source.project}"."${source.jobName}".template.body.toString(), context)
					}else if (grailsApplication.config.rundeck.mail."${trigger}".template.body) {
                        htmlemail = renderTemplate(grailsApplication.config.rundeck.mail."${trigger}".template.body.toString(), context)
                    }else if (grailsApplication.config.rundeck.mail.template.body) {
                        htmlemail = renderTemplate(grailsApplication.config.rundeck.mail.template.body.toString(), context)
                    }
					if(grailsApplication.config.rundeck.mail."${source.project}"."${source.jobName}".template.file) {
						templatePaths << grailsApplication.config.rundeck.mail."${source.project}"."${source.jobName}".template.file.toString()
					}else if(grailsApplication.config.rundeck.mail."${trigger}".template.file){
                        templatePaths << grailsApplication.config.rundeck.mail."${trigger}".template.file.toString()
                    }
                    if(grailsApplication.config.rundeck.mail.template.file){
                        templatePaths << grailsApplication.config.rundeck.mail.template.file.toString()
                    }
                    for (String templatePath : templatePaths) {
                        if (templatePath.indexOf('${') >= 0) {
                            templatePath = renderTemplate(templatePath, context)
                        }
                        def template = new File(templatePath)
                        if (template.isFile()) {
                            if (template.name.endsWith('.md') || template.name.endsWith('.markdown')) {
                                htmlemail = renderTemplate(template.text, context).decodeMarkdown()
                            } else {
                                htmlemail = renderTemplate(template.text, context)
                            }
                            break
                        }
                    }
                    if(templatePaths && !htmlemail ){
                        log.warn("Notification templates searched but not found: " + templatePaths+ ", " +
                                "using default");
                    }
                    def attachlog=false
                    if (trigger != 'start' && configAttachLog in ['true',true]) {
                        attachlog = true
                    }
                    def isFormatted =false
					if( grailsApplication.config.rundeck.mail."${source.project}"."${source.jobName}".template.log.formatted in [true,'true']){
						isFormatted=true
					}else if( grailsApplication.config.rundeck.mail."${trigger}".template.log.formatted in [true,'true']){
                        isFormatted=true
                    }else if( grailsApplication.config.rundeck.mail.template.log.formatted in [true,'true']){
                        isFormatted = true
                    }
                    File outputfile
                    if(attachlog){
                        //copy data to temp file
                        outputfile=copyExecOutputToTempFile(exec,isFormatted)
                    }
                    destarr.each{String recipient->
                        //try to expand property references
                        String sendTo=recipient
                        if(sendTo.indexOf('${')>=0){
                            try {
                                sendTo=DataContextUtils.replaceDataReferences(recipient, context ,null,true)
                            } catch (DataContextUtils.UnresolvedDataReferenceException e) {
                                log.error("Cannot send notification email: "+e.message +
                                        ", context: user: "+ exec.user+", job: "+source.generateFullName());
                                return
                            }
                        }
                        try{
                            mailService.sendMail{
                              multipart (attachlog && outputfile!=null)
                              to sendTo
                              subject subjectmsg
                                if(htmlemail){
                                    html(htmlemail)
                                }else{
                                    body(view: "/execution/mailNotification/status", model: [execution: exec,
                                            scheduledExecution: source, msgtitle: subjectmsg, execstate: state,
                                            nodestatus: content.nodestatus])
                                }
                                if(attachlog && outputfile != null){
                                    attachBytes "${source.jobName}-${exec.id}.txt", "text/plain", outputfile.getText("UTF-8").bytes
                                }
                            }
                            didsend = true
                        }catch(Throwable t){
                            log.error("Error sending notification email to ${sendTo} for Execution ${exec.id}: "+t.getMessage());
                            if (log.traceEnabled) {
                                log.trace("Error sending notification email to ${sendTo} for Execution ${exec.id}: " + t.getMessage(), t)
                            }
                        }
                    }

                    if (null != outputfile) {
                        outputfile.delete()
                    }
                }else if(n.type=='url'){    //sending notification of a status trigger for the Job
                    def Execution exec = content.execution
                    //iterate through the URLs, and submit a POST to the destination with the XML Execution result
                    final state = ExecutionService.getExecutionState(exec)
                    def writer = new StringWriter()
                    def xml = new MarkupBuilder(writer)

                    xml.'notification'(trigger:trigger,status:state,executionId:exec.id){
                        new ExecutionController().renderApiExecutions(grailsLinkGenerator,[exec], [:], delegate)
                    }
                    writer.flush()
                    String xmlStr=  writer.toString()
                    if (log.traceEnabled){
                        log.trace("Posting webhook notification[${n.eventTrigger},${state},${exec.id}]; to URLs: ${n.content}")
                    }
                    def urlarr = n.content.split(",") as List
                    def webhookfailure=false
                    urlarr.each{String urlstr->
                        //perform token expansion within URL.
                        String newurlstr=expandWebhookNotificationUrl(urlstr,exec,source,trigger)
                        try{
                            def result= postDataUrl(newurlstr, xmlStr, trigger, state, exec.id.toString())
                            if(!result.success){
                                webhookfailure=true
                                log.error("Notification failed [${n.eventTrigger},${state},${exec.id}]; URL ${newurlstr}: ${result.error}")
                            }else if (log.traceEnabled) {
                                log.trace("Notification succeeded [${n.eventTrigger},${state},${exec.id}]; URL ${newurlstr}")
                            }
                        } catch (Throwable t) {
                            webhookfailure=true
                            log.error("Notification failed [${n.eventTrigger},${state},${exec.id}]; URL ${newurlstr}: " + t.message);
                            if (log.traceEnabled) {
                                log.trace("Notification failed", t)
                            }
                        }
                    }
                    didsend=!webhookfailure
                }else if (n.type) {
                    def Execution exec = content.execution
                    def execMap = generateExecutionData(exec,content)
                    def jobMap = exportJobdata(source)
                    Map context=generateContextData(exec,content)
                    execMap.job=jobMap
                    execMap.context=context
                    Map config= n.configuration
                    if (context && config) {
                        config = DataContextUtils.replaceDataReferences(config, context)
                    }
                    didsend=triggerPlugin(trigger,execMap,n.type, frameworkService.getFrameworkPropertyResolver(source.project, config))
                }else{
                    log.error("Unsupported notification type: " + n.type);
                }
                }catch(Throwable t){
                    log.error("Error sending notification: ${n}: ${t.class}: "+t.message,t);
                    if (log.traceEnabled) {
                        log.trace("Notification failed",t)
                    }
                }
            }
        }

        return didsend
    }
    /**
     * Creates a datacontext map from the execution's original context, and user profile data.
     * @param exec
     * @param content
     * @return
     */
    private Map generateContextData(Execution exec,Map content){

        //data context for property refs in email
        def userData = [:]
        //add user context data
        def user = User.findByLogin(exec.user)
        if (user && user.email) {
            userData['user.email'] = user.email
        }
        if (user && user.firstName) {
            userData['user.firstName'] = user.firstName
        }
        if (user && user.lastName) {
            userData['user.lastName'] = user.lastName
        }
        //pass data context
        def dcontext = content['context']?.dataContext ?: [:]
        def mailcontext = DataContextUtils.addContext("job", userData, null)
        def context = DataContextUtils.merge(dcontext, mailcontext)
        context
    }
    /**
     * Create execution data map
     * @param exec
     * @param content
     * @return
     */
    private Map generateExecutionData(Execution exec,Map content){

        //prep execution data
        def execMap=exportExecutionData(exec)
        //TBD: nodestatus will migrate to execution data
        if (content['nodestatus']) {
            execMap['nodestatus'] = content['nodestatus']
        }
        execMap
    }
    /**
     * renders a java date as the W3C format used by dc:date in RSS feed
     */
    private String w3cDateValue (Date date){
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        dateFormater.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormater.format(date);
    }

    protected Map exportExecutionData(Execution e) {
        e = Execution.get(e.id)
        def emap = [
            id: e.id,
            href: grailsLinkGenerator.link(controller: 'execution', action: 'follow', id: e.id, absolute: true,
                    params: [project: e.project]),
            status: ExecutionService.getExecutionState(e),
            user: e.user,
            dateStarted: e.dateStarted,
            'dateStartedUnixtime': e.dateStarted.time,
            'dateStartedW3c': w3cDateValue( e.dateStarted),
            description: e.scheduledExecution.description?:'',
            argstring: e.argString,
            project: e.project,
            failedNodeListString: e.failedNodeList,
            failedNodeList: e.failedNodeList?.split(",") as List,
            succeededNodeListString: e.succeededNodeList,
            succeededNodeList: e.succeededNodeList?.split(",") as List,
            loglevel: ExecutionService.textLogLevels[e.loglevel] ?: e.loglevel
        ]
        if (null != e.dateCompleted) {
            emap.dateEnded = e.dateCompleted
            emap['dateEndedUnixtime'] = e.dateCompleted.time
            emap['dateEndedW3c'] = w3cDateValue(e.dateCompleted)
        }
        emap['abortedby'] = e.cancelled?e.abortedby:null
        emap
    }

    protected Map exportJobdata(ScheduledExecution scheduledExecution) {
        def job = [
                id: scheduledExecution.extid,
                href: grailsLinkGenerator.link(controller: 'scheduledExecution', action: 'show',
                        id: scheduledExecution.extid, absolute: true,
                        params: [project: scheduledExecution.project]),
                name: scheduledExecution.jobName,
                group: scheduledExecution.groupPath ?: '',
                project: scheduledExecution.project,
                description: scheduledExecution.description
        ]
        if (scheduledExecution.totalTime >= 0 && scheduledExecution.execCount > 0) {
            def long avg = Math.floor(scheduledExecution.totalTime / scheduledExecution.execCount)
            job.averageDuration = avg
        }
        job
    }

    /**
     * Perform a plugin notification
     * @param trigger trigger name
     * @param data data content for the plugin
     * @param content content for notification
     * @param type plugin type
     * @param config user configuration
     */
    private boolean triggerPlugin(String trigger, Map data,String type, PropertyResolver resolver){

        //load plugin and configure with config values
        def result = pluginService.configurePlugin(type, notificationPluginProviderService, resolver, PropertyScope.Instance)
        if (!result?.instance) {
            return false
        }
        def plugin=result.instance
        /*
        * contains unmapped configuration values only
         */
        def config=result.configuration
        def allConfig = pluginService.getPluginConfiguration(type, notificationPluginProviderService, resolver, PropertyScope.Instance)

        //invoke plugin
        //TODO: use executor
        if (!plugin.postNotification(trigger, data, allConfig)) {
            log.error("Notification Failed: " + type);
            return false
        }
        true
    }

    String expandWebhookNotificationUrl(String url,Execution exec, ScheduledExecution job, String trigger){
        def state=ExecutionService.getExecutionState(exec)
        /**
         * Expand the URL string's embedded property references of the form
         * ${job.PROPERTY} and ${execution.PROPERTY}.  available properties are
         * limited
         */
        def props=[
            job:[id:job.extid,name:job.jobName,group:job.groupPath?:'',project:job.project],
            execution:[id:exec.id,status:state,user:exec.user],
            notification:[trigger:trigger]
        ]
        def invalid = []
        def keys= props.keySet().join('|')
        String srcUrl = url.replaceAll("(\\\$\\{(${keys})\\.(.+?)\\})",
            {Object[] group ->
                if (props.containsKey(group[2])&& props[group[2]].containsKey(group[3])) {
                    props[group[2]][group[3]]?.toString()?.encodeAsURL()
                } else {
                    invalid << group[0]
                    group[0]
                }
            }
        )
        if (invalid) {
            log.error("invalid expansion: " + invalid);
        }
        return srcUrl
    }

    static Map postDataUrl(String url, String xmlstr, String trigger, String status, String id, rptCount=1, backoff=2){
        int count=0;
        int wait=1000;
        int timeout=15
        boolean complete=false;
        def resultCode
        def resultReason
        def error
        for(count=0;count<rptCount;count++){
            if(count>0){
                //wait
                try {
                    Thread.sleep(wait)
                } catch (InterruptedException e) {
                }
                wait *= backoff
            }
            final HttpClientParams params = new HttpClientParams()
            params.setConnectionManagerTimeout(timeout * 1000)
            params.setSoTimeout(timeout * 1000)
            def HttpClient client = new HttpClient(params)
            def PostMethod method = new PostMethod(url)
            method.setRequestHeader(new Header("X-RunDeck-Notification-Trigger", trigger))
            method.setRequestHeader(new Header("X-RunDeck-Notification-Execution-ID", id))
            method.setRequestHeader(new Header("X-RunDeck-Notification-Execution-Status", status))
            method.setRequestEntity(new StringRequestEntity(xmlstr, "text/xml", "UTF-8"))
            try {
                resultCode = client.executeMethod(method);
                resultReason = method.getStatusText();

                if (resultCode >= 200 && resultCode <= 300) {
                    complete=true
                } else {
                    error="server response: ${resultCode} ${resultReason}"
                }
            }catch (Throwable e){
                error="Error making request: "+e.message
            } finally {
                method.releaseConnection();
            }
            if(complete){
                break
            }

        }
        if(!complete){
            return [success:complete,error:"Unable to POST notification after ${count} tries: ${trigger} for execution ${id} (${status}): ${error}"]
        }
        return [success:complete]
    }

}
