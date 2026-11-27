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

package rundeck.services

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.config.Features
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.workflow.WorkflowStrategy
import com.dtolabs.rundeck.core.execution.workflow.steps.CustomFieldsAdapter
import com.dtolabs.rundeck.core.http.ApacheHttpClient
import com.dtolabs.rundeck.core.http.HttpClient
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.server.plugins.services.NotificationPluginProviderService
import grails.async.Promises
import grails.converters.JSON
import grails.events.annotation.Subscriber
import grails.events.bus.EventBusAware
import grails.gorm.transactions.Transactional
import org.springframework.transaction.annotation.Propagation
import grails.util.Holders
import groovy.json.JsonBuilder
import grails.web.mapping.LinkGenerator
import groovy.transform.PackageScope
import groovy.xml.MarkupBuilder
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.apache.http.HttpResponse
import org.apache.http.auth.UsernamePasswordCredentials
import org.rundeck.app.AppConstants
import org.rundeck.app.data.model.v1.execution.ExecutionData
import org.rundeck.app.data.providers.v1.user.UserDataProvider
import org.rundeck.app.data.providers.v1.execution.ExecutionDataProvider
import org.rundeck.app.data.providers.v1.job.JobDataProvider
import org.rundeck.app.spi.RundeckSpiBaseServicesProvider
import org.rundeck.app.spi.Services
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.Execution
import rundeck.Notification
import rundeck.ScheduledExecution
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState
import rundeck.data.notification.SendNotificationEvent
import rundeck.data.util.ExecReportUtil

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/*
 * NotificationService.java
 *
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: May 17, 2010 6:03:16 PM
 * $Id$
 */

public class NotificationService implements ApplicationContextAware, EventBusAware{
    boolean transactional = false

    static final String POST = "post"
    static final String GET = "get"

    def defaultThreadTO = 120000
    def grailsLinkGenerator

    ApplicationContext applicationContext
    def grailsApplication
    def mailService
    def pluginService
    def NotificationPluginProviderService notificationPluginProviderService
    def FrameworkService frameworkService
    def LoggingService loggingService
    def apiService
    def executionService
    def workflowService
    OrchestratorPluginService orchestratorPluginService
    def featureService
    def configurationService
    def storageService
    UserDataProvider userDataProvider
    ExecutionDataProvider executionDataProvider

    def ValidatedPlugin validatePluginConfig(String project, String name, Map config) {
        return pluginService.validatePlugin(name, notificationPluginProviderService, project,config, PropertyScope.Instance, PropertyScope.Project)
    }
    def ValidatedPlugin validatePluginConfig(String name, Map projectProps, Map config) {
        return pluginService.validatePlugin(
            name,
            notificationPluginProviderService,
            PropertyResolverFactory.pluginPrefixedScoped(
                PropertyResolverFactory.instanceRetriever(config),
                PropertyResolverFactory.instanceRetriever(projectProps),
                frameworkService.getFrameworkProperties()
            ),
            PropertyScope.Instance,
            PropertyScope.Project
        )
    }

    private Map loadExecutionViewPlugins() {
        def pluginDescs = [node: [:], workflow: [:]]

        frameworkService.getNodeStepPluginDescriptions().each { desc ->
            pluginDescs['node'][desc.name] = desc
        }
        frameworkService.getStepPluginDescriptions().each { desc ->
            pluginDescs['workflow'][desc.name] = desc
        }
        def wfstrat = pluginService.listPlugins(
                WorkflowStrategy,
                frameworkService.rundeckFramework.workflowStrategyService
        ).collect {
            it.value.description
        }.sort { a, b -> a.name <=> b.name }
        [

                stepPluginDescriptions: pluginDescs,
                orchestratorPlugins   : orchestratorPluginService.getOrchestratorPlugins(),
                strategyPlugins       : wfstrat,
                logFilterPlugins      : pluginService.listPlugins(LogFilterPlugin),
        ]
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
    def Map listNotificationPluginsDynamicProperties(String project, Services services){
        ScheduledExecution.withNewSession {
            def plugins = pluginService.listPlugins(NotificationPlugin, notificationPluginProviderService)
            def result = [:]
            plugins.forEach { name, plugin ->
                def dynamicProperties = pluginService.getDynamicProperties(
                        frameworkService.getRundeckFramework(),
                        ServiceNameConstants.Notification,
                        plugin.name,
                        project,
                        services
                )
                if (dynamicProperties) {
                    result.put(name, dynamicProperties)
                } else {
                    result.put(name, [:])
                }
            }
            result
        }
    }

    /**
     * Dispatch a job notification, optionally on its own thread.
     *
     * Must not run in a transaction. The work is done inside {@code withNewTransaction} blocks below --
     * on a separate thread when NOTIFICATIONS_OWN_THREAD is enabled -- while this method only waits for
     * completion, for up to notification.threadTimeOut (120s by default). A transaction here, whether
     * inherited from the caller or created by this method, would hold the calling thread's database
     * connection checked out and idle for that whole wait; the server closes it after wait_timeout and
     * the caller then fails on COMMIT.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void asyncTriggerJobNotification(String trigger, schedUuid, Map content){
        if(trigger && schedUuid){
            if(featureService.featurePresent(Features.NOTIFICATIONS_OWN_THREAD)){
                def notificationTask = Promises.task {
                    resolveThenDeliver(trigger, schedUuid, content)
                }
                try{
                    notificationTask.get(configurationService.getLong("notification.threadTimeOut", defaultThreadTO), TimeUnit.MILLISECONDS)
                }catch(TimeoutException toe){
                    log.error("Error sending notification " , toe)
                    notificationTask.cancel(true)
                }
            }else{
                resolveThenDeliver(trigger, schedUuid, content)
            }

        }
    }

    /**
     * Replace template variables in the text.
     * @param templateText
     * @param context data context
     * @return replaced text
     */
    def String renderTemplate(String templateText, Map context){
        return DataContextUtils.replaceDataReferencesInString(templateText, context, null, false, false)
    }
    /**
     * write log output to a temp file, optionally formatted.
     * @param e
     * @param isFormatted
     * @return
     */
    def File copyExecOutputToTempFile(Execution e, boolean isFormatted, String attachedExtension){
        def reader = loggingService.getLogReader(e)
        if (reader.state == ExecutionFileState.NOT_FOUND|| reader.state == ExecutionFileState.ERROR|| reader.state !=
            ExecutionFileState.AVAILABLE) {
            return null
        }
        SimpleDateFormat logFormater = new SimpleDateFormat("HH:mm:ss", Locale.US);
        logFormater.timeZone = TimeZone.getTimeZone("GMT")
        def iterator = reader.reader
        iterator.openStream(0)
        def lineSep = System.getProperty("line.separator")
        File temp = File.createTempFile("output-${e.id}",".${attachedExtension}")
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
    /**
     * write log output to a StringBuffer
     * @param e
     * @param isFormatted
     * @return
     */
    def StringBuffer copyExecOutputToStringBuffer(Execution e, boolean isFormatted){

        StringBuffer output = new StringBuffer()
        def reader = loggingService.getLogReader(e)
        if (reader.state == ExecutionFileState.NOT_FOUND|| reader.state == ExecutionFileState.ERROR|| reader.state !=
            ExecutionFileState.AVAILABLE) {
            return null
        }
        SimpleDateFormat logFormater = new SimpleDateFormat("HH:mm:ss", Locale.US);
        logFormater.timeZone = TimeZone.getTimeZone("GMT")
        def lineSep = System.getProperty("line.separator")
        def iterator = reader.reader
        iterator.openStream(0)
        iterator.findAll { it.eventType == LogUtil.EVENT_TYPE_LOG }.each { LogEvent msgbuf ->
            def message = msgbuf.message
            if(message.contains("\033")){
                message=message.decodeAnsiColorStrip()
            }

            output << (isFormatted ? "${logFormater.format(msgbuf.datetime)} [${msgbuf.metadata?.user}@${msgbuf.metadata?.node} ${msgbuf.metadata?.stepctx ?: '_'}][${msgbuf.loglevel}] ${message}" : message)
            output << lineSep
        }
        iterator.close()
        return output
    }
    private static Map<String,String> toStringStringMap(Map input){
        def map = new HashMap<String, String>()
        for (Object o : input.keySet()) {
            map.put(o.toString(),input.get(o)? input.get(o).toString():'')
        }
        return map;
    }

    /**
     * Load the job, resolve its notifications in a short transaction, then deliver them with none open.
     *
     * The transaction ends before any SMTP, HTTP or plugin call. That is the point: the previous shape
     * held it open across those sends, and since the server closes a connection idle longer than its
     * wait_timeout, the transaction's own COMMIT then failed on a dead socket.
     *
     * A session is held for the whole call so that plugin notifications, which hand third-party code the
     * Execution entity, can still traverse associations during delivery. The email and webhook paths do
     * not need it -- their database reads are all resolved in the transaction.
     */
    protected void resolveThenDeliver(String trigger, schedUuid, Map content) {
        ScheduledExecution.withNewSession {
            List<Closure<Boolean>> deliveries = ScheduledExecution.withNewTransaction {
                ScheduledExecution scheduledExecution = ScheduledExecution.findByUuid(schedUuid)
                null != scheduledExecution ? resolveNotifications(trigger, scheduledExecution, content) : []
            }
            deliverNotifications(deliveries)
        }
    }

    /**
     * Resolve and then deliver the notifications for a trigger.
     *
     * Kept for callers that want both phases in one call and are already inside a suitable
     * transaction or session. {@link #asyncTriggerJobNotification} runs the two phases separately so
     * that only the resolve phase is transactional -- see that method for why.
     */
    boolean triggerJobNotification(String trigger, ScheduledExecution source, Map content){
        deliverNotifications(resolveNotifications(trigger, source, content))
    }

    /**
     * Resolve the notifications for a trigger into a list of deliveries, doing all database reads here
     * and none in the returned closures.
     *
     * Every closure captures values already resolved, so delivery needs no transaction and -- with the
     * mail template's queries and the log read now hoisted out of the send -- no Hibernate session
     * either, except for third-party plugin code which may still walk associations on the Execution it
     * is handed.
     *
     * @return one closure per notification, each returning whether it sent, or null if it does not
     *         report a result
     */
    protected List<Closure<Boolean>> resolveNotifications(String trigger, ScheduledExecution source, Map content){
        List<Closure<Boolean>> deliveries = []
        if(source.notifications && source.notifications.find{it.eventTrigger=='on'+trigger}){
            def notes = source.notifications.findAll{it.eventTrigger=='on'+trigger}
            notes.each{ Notification n ->
                try{

                    frameworkService.getPluginControlService(source.project).
                        checkDisabledPlugin(n.type, ServiceNameConstants.Notification)

                if(n.type=='email'){
                    //sending notification of a status trigger for the Job
                    def Execution exec = content.execution
                    def mailConfig = n.mailConfiguration()

                    def configSubject=mailConfig.subject
                    def configAttachLog=mailConfig.attachLog
                    def configAttachLogInFile=mailConfig.attachLogInFile
                    def configAttachLogInline=mailConfig.attachLogInline
                    final state = exec.executionState
                    def statMsg=[
                            (ExecutionService.EXECUTION_ABORTED):'KILLED',
                            (ExecutionService.EXECUTION_FAILED):'FAILURE',
                            (ExecutionService.EXECUTION_RUNNING):'STARTING',
                            (ExecutionService.EXECUTION_SUCCEEDED):'SUCCESS',
                            (ExecutionService.EXECUTION_TIMEDOUT):'TIMEDOUT',
                            (ExecutionService.EXECUTION_MISSED):'MISSED',
                            (ExecutionService.EXECUTION_FAILED_WITH_RETRY):'FAILED WITH RETRY'
                    ]

                    String eventStatus = statMsg[state]
                    if(trigger=='avgduration'){
                        eventStatus='AVERAGE DURATION EXCEEDED'
                    }

                    def execMap = null
                    Map context = null
                    (context, execMap) = generateNotificationContext(content.execution, content, source)
                    context = DataContextUtils.addContext("notification", [trigger: trigger, eventStatus: eventStatus], context)

                    def destarr=[]
                    def destrecipients=mailConfig.recipients
                    if(destrecipients){
                        if(destrecipients.indexOf('${')>=0){
                            try {
                                destrecipients=DataContextUtils.replaceDataReferencesInString(destrecipients, context, null, true)
                            } catch (DataContextUtils.UnresolvedDataReferenceException e) {
                                log.error("Cannot send notification email: "+e.message +
                                                  ", context: user: "+ exec.user+", job: "+source.generateFullName());

                            }
                        }
                        destarr = destrecipients.split(', *') as List
                    }

                    def isFormatted =false

                    if( configurationService.getBoolean("mail.${source.project}.${source.jobName}.template.log.formatted",false)){
                        isFormatted=true
                    }else if( configurationService.getBoolean("mail.${trigger}.template.log.formatted",false)){
                        isFormatted=true
                    }else if( configurationService.getBoolean("mail.template.log.formatted",false)){
                        isFormatted = true
                    }
                    boolean allowUnsanitized = checkAllowUnsanitized(exec.project)
                    StringBuffer outputBuffer = null
                    def attachlogbody = false
                    def attachlog=false
                    if (trigger != 'start' && configAttachLog in ['true',true]) {
                        if (configAttachLogInline in ['true',true]) {
                            attachlogbody = true
                        }
                        if (configAttachLogInFile in ['true',true]) {
                            attachlog = true
                        }

                        //for old versions support
                        if(!attachlog && !attachlogbody){
                            attachlog = true
                        }
                    }

                    //set up templates
                    def subjecttmpl='${notification.eventStatus} [${exec.project}] ${job.group}/${job.name} ${exec' +
                            '.argstring}'
					if(configurationService.getString("mail.${source.project}.${source.jobName}.template.subject")) {
						subjecttmpl= configurationService.getString("mail.${source.project}.${source.jobName}.template.subject")
					}else if(configurationService.getString("mail.${trigger}.template.subject")){
                        subjecttmpl= configurationService.getString("mail.${trigger}.template.subject")
                    }else if (configurationService.getString("mail.template.subject")) {
                        subjecttmpl=configurationService.getString("mail.template.subject")
                    }
                    if(configSubject){
                        subjecttmpl= configSubject
                    }
                    def subjectmsg = renderTemplate(subjecttmpl, context)

                    def htmlemail=null
                    def templatePaths=[]
                    String projectTemplateBody = configurationService.getString("mail.${source.project}.${source.jobName}.template.body")
                    String triggerTemplateBody = configurationService.getString("mail.${trigger}.template.body")
                    String templateBody = configurationService.getString("mail.template.body")

                    if(projectTemplateBody) {
						htmlemail = renderTemplate(projectTemplateBody, context)
					}else if (triggerTemplateBody) {
                        htmlemail = renderTemplate(triggerTemplateBody, context)
                    }else if (templateBody) {
                        htmlemail = renderTemplate(templateBody, context)
                    }

                    String projectTemplateFile = configurationService.getString("mail.${source.project}.${source.jobName}.template.file")
                    String triggerTemplateFile = configurationService.getString("mail.${trigger}.template.file")
                    String templateFile = configurationService.getString("mail.template.file")

                    if(projectTemplateFile) {
						templatePaths << projectTemplateFile
					}else if(triggerTemplateFile){
                        templatePaths << triggerTemplateFile
                    }
                    if(templateFile){
                        templatePaths << templateFile
                    }
                    for (String templatePath : templatePaths) {
                        if (templatePath.indexOf('${') >= 0) {
                            templatePath = renderTemplate(templatePath, context)
                        }
                        def template = new File(templatePath)
                        if (template.isFile()) {

                            //check if the custom template is calling ${logoutput.data}
                            def templateLogOutput=false
                            if(template.text.indexOf('${logoutput.data}')>=0){
                                templateLogOutput=true
                            }
                            def contextOutput=[:]
                            if(attachlogbody && templateLogOutput){
                                //just attached the output if the template uses ${logoutput.data}
                                outputBuffer=copyExecOutputToStringBuffer(exec,isFormatted)
                                if(allowUnsanitized) {
                                    contextOutput['logoutput'] = ["data":outputBuffer.toString()]
                                } else {
                                    contextOutput['logoutput'] = ["data":outputBuffer.toString().encodeAsSanitizedHTML()]
                                }
                            }else if(templateLogOutput) {
                                // add null value if template uses ${logoutput.data} and the attachlogbody is disabled
                                contextOutput['logoutput'] = ["data":""]
                            }

                            if(!contextOutput.isEmpty()) {
                                context = DataContextUtils.merge(context, contextOutput)
                            }

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

                    def attachedExtension = "log"
                    def attachedContentType = "text/plain"
                    String projectLogExtension = configurationService.getString("mail.${source.project}.${source.jobName}.template.log.extension")
                    String triggerLogExtension = configurationService.getString("mail.${trigger}.template.log.extension")
                    String logExtension = configurationService.getString("mail.template.log.extension")

                    if( projectLogExtension ){
                        attachedExtension=projectLogExtension
                    }else if( triggerLogExtension){
                        attachedExtension=triggerLogExtension
                    }else if( logExtension){
                        attachedExtension = logExtension
                    }

                    String projectLogContentType = configurationService.getString("mail.${source.project}.${source.jobName}.template.log.contentType")
                    String triggerLogContentType = configurationService.getString("mail.${trigger}.template.log.contentType")
                    String logContentType = configurationService.getString("mail.template.log.contentType")

                    if( projectLogContentType ){
                        attachedContentType=projectLogContentType
                    }else if( triggerLogContentType){
                        attachedContentType=triggerLogContentType
                    }else if( logContentType){
                        attachedContentType = logContentType
                    }
                    File outputfile
                    if(attachlog){
                        //copy data to temp file
                        outputfile=copyExecOutputToTempFile(exec,isFormatted,attachedExtension)
                    }

                    // Resolved here, before the send, rather than by the template. The mail body is
                    // rendered inside mailService.sendMail below, so a query issued from the template runs
                    // mid-send -- inside the transaction that spans the SMTP conversation, during which the
                    // server can close the connection. Same reasoning as averageDuration.
                    // Null-tolerant: a mocked ExecutionService returns null for unstubbed methods, and the
                    // template defaults each count to 0 anyway.
                    Map executionCounts = executionService.getJobExecutionCounts(source) ?: [:]

                    // Read before the send rather than from inside the mail closure below. That closure runs
                    // while the message is being built and sent, so this read -- log storage I/O plus the
                    // lookup backing it -- happened inside the transaction that spans the SMTP conversation.
                    // It also re-read the same content once per recipient.
                    // Guarded on !htmlemail because the default view is the only body that consumes it here;
                    // when a custom template file uses ${logoutput.data} the buffer was already filled above.
                    if (!htmlemail && attachlogbody && outputBuffer == null) {
                        outputBuffer = copyExecOutputToStringBuffer(exec, isFormatted)
                    }

                    // Recipient expansion resolved here; only the sends themselves are deferred.
                    List<String> resolvedRecipients = []
                    destarr.each{String recipient->
                        //try to expand property references
                        String sendTo=recipient
                        if(sendTo.indexOf('${')>=0){
                            try {
                                sendTo=DataContextUtils.replaceDataReferencesInString(recipient, context, null, true)
                            } catch (DataContextUtils.UnresolvedDataReferenceException e) {
                                log.error("Cannot send notification email: "+e.message +
                                                  ", context: user: "+ exec.user+", job: "+source.generateFullName());
                                return
                            }
                        }
                        resolvedRecipients << sendTo
                    }

                    deliveries << { ->
                        boolean sent = false
                        resolvedRecipients.each{String sendTo->
                        try{
                            mailService.sendMail{
                              multipart (attachlog && outputfile!=null)
                              to sendTo
                              subject subjectmsg
                                if(htmlemail){
                                    html(htmlemail)
                                }else{
                                    body(
                                            view: "/execution/mailNotification/status",
                                            model: loadExecutionViewPlugins() + [
                                                    execution         : exec,
                                                    scheduledExecution: source,
                                                    msgtitle          : subjectmsg,
                                                    execstate         : state,
                                                    nodestatus        : content.nodestatus,
                                                    jobref            : content.jobref,
                                                    allowUnsanitized  : allowUnsanitized,
                                                    logOutput         : outputBuffer!=null? outputBuffer.toString(): null,
                                                    // Supplied so the template does not query for it while the
                                                    // mail body is being rendered -- that happens inside this
                                                    // transaction, mid-send. Taken from the job map already built
                                                    // for the notification context, so both report the same value.
                                                    averageDuration   : execMap?.job?.averageDuration ?: 0
                                            ] + executionCounts
                                    )
                                }
                                if(attachlog && outputfile != null){
                                    attachBytes "${source.jobName}-${exec.id}.${attachedExtension}", attachedContentType, outputfile.getText("UTF-8").bytes
                                }
                            }
                            sent = true
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
                        sent
                    }
                }else if(n.type=='url'){    //sending notification of a status trigger for the Job
                    Execution exec = content.execution
                    //iterate through the URLs, and submit a POST to the destination with the XML Execution result
                    final state = exec.executionState
                    String payloadStr = n.format == "json" ? createJsonNotificationPayload(trigger,exec) : createXmlNotificationPayload(trigger,exec)
                    if (log.traceEnabled){
                        log.trace("Posting webhook notification[${n.eventTrigger},${state},${exec.id}]; to URLs: ${n.content}")
                    }
                    Map urlsConfiguration = n.urlConfiguration()
                    String urls = urlsConfiguration.urls
                    String method = urlsConfiguration.httpMethod
                    def urlarr = urls.split(",") as List
                    // Token expansion resolved here: it reads the execution and job, so it must not run
                    // after the transaction closes or in the middle of the POSTs.
                    List<String> expandedUrls = urlarr.collect{String urlstr->
                        expandWebhookNotificationUrl(urlstr,exec,source,trigger, content?.export)
                    }
                    String eventTrigger = n.eventTrigger
                    String format = n.format

                    deliveries << { ->
                        def webhookfailure=false
                        expandedUrls.each{String newurlstr->
                        try{
                            def result= postDataUrl(newurlstr, format,payloadStr, trigger, state, exec.id.toString(), method)
                            if(!result.success){
                                webhookfailure=true
                                log.error("Notification failed [${eventTrigger},${state},${exec.id}]; URL ${newurlstr}: ${result.error}")
                            }else if (log.traceEnabled) {
                                log.trace("Notification succeeded [${eventTrigger},${state},${exec.id}]; URL ${newurlstr}")
                            }
                        } catch (Throwable t) {
                            webhookfailure=true
                            log.error("Notification failed [${eventTrigger},${state},${exec.id}]; URL ${newurlstr}: " + t.message);
                            if (log.traceEnabled) {
                                log.trace("Notification failed", t)
                            }
                        }
                        }
                        !webhookfailure
                    }
                }else if (n.type) {

                    def execMap = null
                    Map context = null
                    (context, execMap) = generateNotificationContext(content.execution, content, source)

                    String pluginType = n.type
                    Map pluginConfig = n.configuration
                    deliveries << { ->
                        triggerPlugin(trigger,execMap,pluginType, source.project,pluginConfig, content,context)
                    }
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

        return deliveries
    }

    /**
     * Run resolved deliveries. Performs the external I/O -- SMTP, HTTP, plugins -- and no database work.
     *
     * Each delivery is isolated: a failure in one does not stop the others, matching the per-notification
     * try/catch that wrapped both phases before they were split.
     *
     * @param deliveries closures from {@link #resolveNotifications}
     * @return the result of the last delivery that reported one, preserving the previous behaviour where
     *         each notification overwrote didsend and types that report nothing left it unchanged
     */
    protected boolean deliverNotifications(List<Closure<Boolean>> deliveries) {
        boolean didsend = false
        deliveries.each { Closure<Boolean> delivery ->
            try {
                Boolean result = delivery.call()
                if (result != null) {
                    didsend = result
                }
            } catch (Throwable t) {
                log.error("Error delivering notification: ${t.class}: " + t.message, t)
                if (log.traceEnabled) {
                    log.trace("Notification delivery failed", t)
                }
            }
        }
        didsend
    }

    String createXmlNotificationPayload(String trigger, Execution exec) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        xml.'notification'(trigger:trigger,status:exec.executionState,executionId:exec.id){
            renderApiExecutions(grailsLinkGenerator,[exec], [:], delegate)
        }
        writer.flush()
        return writer.toString()
    }

    String createJsonNotificationPayload(String triggerName, Execution exec) {

        JsonBuilder b = new JsonBuilder()
        b {
            trigger(triggerName)
            status(exec.executionState)
            executionId(exec.id)
            execution { this.renderApiExecutionsJson(grailsLinkGenerator,[exec], [single:true], delegate) }
        }
        return b.toString()
    }
/*
    * Render execution list xml given a List of executions, and a builder delegate
    */
    private def renderApiExecutions(LinkGenerator grailsLinkGenerator, List execlist, paging, delegate) {
        apiService.renderExecutionsXml(execlist.collect{ Execution e->
            [
                execution:e,
                href: grailsLinkGenerator.link(controller: 'execution', action: 'show', id: e.id, absolute: true,
                        params: [project: e.project]),
                status: e.executionState,
                summary: ExecReportUtil.summarizeJob(e)
            ]
        },paging,delegate)
    }
    /*
    * Render execution list json given a List of executions, and a builder delegate
    */
    private def renderApiExecutionsJson(LinkGenerator grailsLinkGenerator, List execlist, paging, delegate) {
        apiService.renderExecutionsJson(execlist.collect{ Execution e->
            [
                    execution:e,
                    href: grailsLinkGenerator.link(controller: 'execution', action: 'show', id: e.id, absolute: true,
                                                   params: [project: e.project]),
                    status: e.executionState,
                    summary: ExecReportUtil.summarizeJob(e)
            ]
        },paging,delegate)
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
        def user = userDataProvider.getInfoFromUser(exec.user)
        if (user && user.email) {
            userData['user.email'] = user.email
        }
        if (user && user.firstname) {
            userData['user.firstName'] = user.firstname
        }
        if (user && user.lastname) {
            userData['user.lastName'] = user.lastname
        }
        //pass data context
        def dcontext = content.context?.getSharedDataContext()?.consolidate()?.getData(ContextView.global())?.getData() ?: [:] //usage of modified global context
        def mailcontext = DataContextUtils.addContext("job", userData, null)
        def context = DataContextUtils.merge(dcontext, mailcontext)
        def exportcontext = content.export ?:[:]
        context = DataContextUtils.merge(context, exportcontext)
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
        def modifiedSuccessNodeList = executionService.getEffectiveSuccessNodeList(e)
        def emap = [
            id: e.id,
            href: grailsLinkGenerator.link(controller: 'execution', action: 'show', id: e.id, absolute: true,
                    params: [project: e.project]),
            status: e.executionState,
            user: e.user,
            dateStarted: e.dateStarted,
            'dateStartedUnixtime': e.dateStarted.time,
            'dateStartedW3c': w3cDateValue( e.dateStarted),
            description: e.scheduledExecution.description?:'',
            argstring: e.argString,
            project: e.project,
            failedNodeListString: e.failedNodeList,
            failedNodeList: e.failedNodeList?.split(",") as List,
            succeededNodeListString: modifiedSuccessNodeList.join(','),
            succeededNodeList: modifiedSuccessNodeList,
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

    protected getEffectiveSuccessNodeList(Execution e){
        def modifiedSuccessNodeList = []
        if(e.succeededNodeList) {
            List<String> successNodeList = e.succeededNodeList?.split(',')
            def nodeSummary = workflowService.requestStateSummary(e, successNodeList)
            if(nodeSummary) {
                successNodeList.each { node ->
                    if (nodeSummary.workflowState.nodeSummaries[node]?.summaryState == 'SUCCEEDED') {
                        modifiedSuccessNodeList.add(node)
                    }
                }
            }
        }
        modifiedSuccessNodeList
    }

    /**
     * Builds the job section of a notification context.
     *
     * @param scheduledExecution the job the notification is for
     * @param content the trigger's content map; when it carries an 'averageDuration' key that value
     *        is used instead of querying for it
     * @return the job data map
     */
    protected Map exportJobdata(ScheduledExecution scheduledExecution, Map content = null) {
        def job = [
                id: scheduledExecution.extid,
                href: grailsLinkGenerator.link(controller: 'scheduledExecution', action: 'show',
                        id: scheduledExecution.extid, absolute: true,
                        params: [project: scheduledExecution.project]),
                name: scheduledExecution.jobName,
                group: scheduledExecution.groupPath ?: '',
                schedule : scheduledExecution.scheduled? scheduledExecution.generateCrontabExression():'',
                project: scheduledExecution.project,
                description: scheduledExecution.description
        ]
        // Prefer a value captured where the trigger fired. The fallback keeps callers that do not
        // supply one working, but it runs inside the notification transaction -- the one that also
        // spans SMTP and HTTP sends, during which Aurora can close the connection out from under it.
        //
        // containsKey rather than truthiness on purpose: a genuine average of 0, for a job with no
        // completed executions yet, must not be mistaken for "not supplied" and re-trigger the query.
        // Left untyped, and the comparison null-tolerant, to match the previous behaviour exactly:
        // getAverageDuration is declared to return Long, so a null must not blow up here.
        def averageDuration = content?.containsKey('averageDuration')
                ? content.averageDuration
                : executionService.getAverageDuration(scheduledExecution.uuid)
        if ((averageDuration ?: 0) > 0) {
            job.averageDuration = averageDuration
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
    private boolean triggerPlugin(String trigger, Map data,String type, String project, Map config, Map content, Map context){
        if (context && config) {
            DescribedPlugin described = pluginService.getPluginDescriptor(type, notificationPluginProviderService)
            if(described?.description) {
                CustomFieldsAdapter customFieldsAdapter = CustomFieldsAdapter.create(described.description)
                config = DataContextUtils.replaceDataReferences(
                        config,
                        context,
                        null,
                        false,
                        false,
                        customFieldsAdapter.&convertInput,
                        customFieldsAdapter.&convertOutput
                )
            }
        }

        config = config?.each {
            if(!it.value){
                it.value=null
            }
        }
        Map<Class, Object> servicesMap = [:]
        servicesMap.put(KeyStorageTree, content.context?.storageTree)

        def services = new RundeckSpiBaseServicesProvider(
                services: servicesMap
        )
        //load plugin and configure with config values

        def pluginConfigFactory = PropertyResolverFactory.pluginPrefixedScoped(
            PropertyResolverFactory.instanceRetriever(config),
            frameworkService.getProjectPropertyResolver(project),
            frameworkService.getFrameworkProperties()
        )
        def result = pluginService.configurePlugin(
            type,
            notificationPluginProviderService,
            pluginConfigFactory,
            PropertyScope.Instance,
            services
        )
        if (!result?.instance) {
            return false
        }
        def plugin=result.instance
        /*
        * contains unmapped configuration values only
         */
        def allConfig = pluginService.getPluginConfiguration(type, notificationPluginProviderService, pluginConfigFactory, PropertyScope.Instance)

        //invoke plugin
        //TODO: use executor
        if (!plugin.postNotification(trigger, data, allConfig)) {
            log.error("Notification Failed: " + type);
            return false
        }
        true
    }

    String expandWebhookNotificationUrl(String url,Execution exec, ScheduledExecution job, String trigger, Map export){
        def state= exec.executionState
        def props = export ?: [:]

        /**
         * Expand the URL string's embedded property references of the form
         * ${job.PROPERTY} and ${execution.PROPERTY}.  available properties are
         * limited
         */
         props << [
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

    static Map postDataUrl(String url, String format, String payload, String trigger, String status, String id, String httpMethod = POST, rptCount=1, backoff=2){
        int count=0;
        int wait=1000;
        int timeout=15
        boolean complete=false;
        def resultCode
        def resultReason
        def error
        String contentType = format == "json" ? "application/json" : "text/xml"
        String secureDigest = createSecureDigest(url,trigger,id)
        for(count=0;count<rptCount;count++){
            if(count>0){
                //wait
                try {
                    Thread.sleep(wait)
                } catch (InterruptedException e) {
                }
                wait *= backoff
            }
            HttpClient<HttpResponse> httpClient = new ApacheHttpClient()
            httpClient.setFollowRedirects(true)
            httpClient.setTimeout(timeout*1000)

            try{
                URL urlo = new URL(url)
                httpClient.setUri(urlo.toURI())
                if(urlo.userInfo){
                    UsernamePasswordCredentials cred = new UsernamePasswordCredentials(urlo.userInfo)
                    httpClient.setBasicAuthCredentials(cred.userName,cred.password)
                }
            }catch(MalformedURLException e){
                throw new Exception("Failed to configure base URL for authentication: "+e.getMessage(),e)
            }

            if(httpMethod != GET) {
                httpClient.setMethod(HttpClient.Method.POST)
                httpClient.addPayload(contentType,payload)
            }

            httpClient.addHeader("X-RunDeck-Notification-Trigger", trigger)
            httpClient.addHeader("X-RunDeck-Notification-Execution-ID", id)
            httpClient.addHeader("X-RunDeck-Notification-Execution-Status", status)
            if(secureDigest) httpClient.addHeader("X-RunDeck-Notification-SHA256-Digest", secureDigest)
            try {
                httpClient.execute { response ->
                    resultCode = response.statusLine.statusCode
                    resultReason = response.statusLine.reasonPhrase
                    if (resultCode >= 200 && resultCode <= 300) {
                        complete=true
                    } else {
                        error="server response: ${resultCode} ${resultReason}"
                    }
                }
            }catch (Throwable e){
                error="Error making request: "+e.message
            }
            if(complete){
                break
            }

        }
        if(!complete){
            return [success:complete,error:"Unable to ${httpMethod?.toUpperCase()} notification after ${count} tries: ${trigger} for execution ${id} (${status}): ${error}"]
        }
        return [success:complete]
    }

    static String createSecureDigest(String postUrl, String trigger, id) {
        String key = Holders.config.getProperty("rundeck.notification.webhookSecurityKey",String.class)
        if(!key) return null
        MessageDigest digest = DigestUtils.getSha256Digest();
        digest.update(postUrl.bytes)
        digest.update(trigger.bytes)
        digest.update(id.bytes)
        new String(Hex.encodeHex(digest.digest(key.bytes)))
    }

    @PackageScope
    boolean checkAllowUnsanitized(String project) {
        if(frameworkService.getRundeckFramework().hasProperty(AppConstants.FRAMEWORK_OUTPUT_ALLOW_UNSANITIZED)) {
            if ("true" != frameworkService.getRundeckFramework().
                    getProperty(AppConstants.FRAMEWORK_OUTPUT_ALLOW_UNSANITIZED)) return false
            def projectConfig = frameworkService.getRundeckFramework().projectManager.loadProjectConfig(project)
            if(projectConfig.hasProperty(AppConstants.PROJECT_OUTPUT_ALLOW_UNSANITIZED)) {
                return "true" == projectConfig.getProperty(AppConstants.PROJECT_OUTPUT_ALLOW_UNSANITIZED)
            }
            return false
        }
        return false
    }

    def generateNotificationContext(Execution exec, Map content, ScheduledExecution source){
        def appUrl = grailsLinkGenerator.link(action: 'home', controller: 'menu',absolute: true)
        def projUrl = grailsLinkGenerator.link(action: 'index', controller: 'menu', params: [project:  exec.project], absolute: true)

        def execMap = generateExecutionData(exec, content)
        def jobMap=exportJobdata(source, content)
        Map context = generateContextData(exec, content)

        //used for plugins types
        execMap.job=jobMap
        execMap.context=context

        def contextMap=[:]
        execMap.projectHref = projUrl
        contextMap['job'] = toStringStringMap(jobMap)
        contextMap['execution']=toStringStringMap(execMap)
        contextMap['rundeck']=['href': appUrl]

        if(!context?.containsKey("globals")) {
            // Put globals in context.
            Map<String, String> globals = frameworkService.getProjectGlobals(source.project);
            contextMap.put("globals", globals ? globals : new HashMap<>());

        }

        context = DataContextUtils.merge(context, contextMap)

        [context, execMap]
    }

    @Subscriber('trigger-notification')
    void sendNotificationsForExecution(SendNotificationEvent evt) {
        if(evt.jobUuid) {
            ScheduledExecution.withNewSession {
                def job = ScheduledExecution.findByUuid(evt.jobUuid)
                def execution = Execution.findByUuid(evt.executionUuid)
                if (job.notificationSet) {
                    def contextBuilder = ExecutionContextImpl.builder()
                    contextBuilder.with {
                        storageTree(storageService.storageTreeWithContext(evt.authContext))
                        framework(frameworkService.rundeckFramework)
                        frameworkProject(job.project)
                        if(job.getWorkflowData()) {
                            workflowData(execution.getWorkflowData())
                        }
                    }
                    contextBuilder.authContext(evt.authContext)
                    triggerJobNotification(
                            evt.trigger, job,
                            [execution: execution,
                             context  : contextBuilder.build()]
                    )
                }
            }
        }
    }
}
