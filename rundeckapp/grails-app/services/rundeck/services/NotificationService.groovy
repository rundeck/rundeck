package rundeck.services

import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import com.dtolabs.rundeck.server.plugins.RundeckPluginRegistry
import com.dtolabs.rundeck.server.plugins.services.NotificationPluginProviderService
import groovy.xml.MarkupBuilder

import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.params.HttpClientParams
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.Header

import org.apache.commons.httpclient.methods.StringRequestEntity
import grails.util.GrailsWebUtil
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.web.context.support.WebApplicationContextUtils
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.springframework.web.context.request.RequestContextHolder
import rundeck.ScheduledExecution
import rundeck.Notification
import rundeck.Execution
import rundeck.controllers.ExecutionController
import com.fasterxml.jackson.databind.ObjectMapper

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

    ApplicationContext applicationContext
    def grailsApplication
    def mailService
    def RundeckPluginRegistry rundeckPluginRegistry
    def NotificationPluginProviderService notificationPluginProviderService
    def FrameworkService frameworkService

    def NotificationPlugin getNotificationPlugin(String name) {
        def bean= rundeckPluginRegistry.loadPluginByName(name, notificationPluginProviderService)
        if (bean ) {
            return (NotificationPlugin) bean
        }
        log.error("Notification plugin not found: ${name}")
        return null
    }
    def Map validatePluginConfig(String name, Map config){
        def Map pluginDesc=getNotificationPluginDescriptor(name)
        if(pluginDesc && pluginDesc.description instanceof Description){
            return frameworkService.validateDescription(pluginDesc.description,'',config)
        }else{
            return null
        }
    }
    /**
     *
     * @param name
     * @return map containing [instance:(plugin instance), description: (map or Description), ]
     */
    def Map getNotificationPluginDescriptor(String name) {
        def bean= rundeckPluginRegistry.loadPluginDescriptorByName(name, notificationPluginProviderService)
        if (bean ) {
            return (Map) bean
        }
        log.error("Notification plugin not found: ${name}")
        return null
    }
    private NotificationPlugin configureNotificationPlugin(String name, Map configuration) {
        def bean= rundeckPluginRegistry.configurePluginByName(name, notificationPluginProviderService,configuration)
        if (bean ) {
            return (NotificationPlugin) bean
        }
        log.error("Notification plugin not found: ${name}")
        return null
    }
    def Map listNotificationPlugins(){
        def plugins=[:]
        plugins=rundeckPluginRegistry.listPluginDescriptors(NotificationPlugin, notificationPluginProviderService)
        //clean up name of any Groovy plugin without annotations that ends with "NotificationPlugin"
        plugins.each {key,Map plugin->
            def desc = plugin.description
            if(desc && desc instanceof Map){
                if(desc.name.endsWith("NotificationPlugin")){
                    desc.name=desc.name.replaceAll(/NotificationPlugin$/,'')
                }
            }
        }
//        System.err.println("listed plugins: ${plugins}")

        plugins
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
    private Object doWithMockRequest(Closure clos){

        def requestAttributes = RequestContextHolder.getRequestAttributes()
        boolean unbindrequest = false
        // outside of an executing request, establish a mock version
        if (!requestAttributes) {
            def servletContext = ServletContextHolder.getServletContext()
            def applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext)
            requestAttributes = GrailsWebUtil.bindMockWebRequest(applicationContext)
            unbindrequest = true
        }

        //prep execution data
        def result
        try {
            result = clos.call()
        } finally {
            if (unbindrequest) {
                RequestContextHolder.setRequestAttributes(null)
            }
        }
        result
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
                    def destarr=n.content.split(",") as List
                    def subjectmsg="${exec.status == 'true' ? 'SUCCESS' : 'FAILURE'} [${exec.project}] ${source.groupPath?source.groupPath+'/':''}${source.jobName}${exec.argString?' '+exec.argString:''}"
                    destarr.each{recipient->
                        try{
                            mailService.sendMail{
                              to recipient
                              subject subjectmsg
                              body( view:"/execution/mailNotification/status", model: [execution: exec,scheduledExecution:source, msgtitle:subjectmsg,nodestatus:content.nodestatus])
                            }
                        }catch(Exception e){
                            log.error("Error sending notification email: "+e.getMessage());
                        }
                    }
                    didsend= true
                }else if(n.type=='url'){    //sending notification of a status trigger for the Job
                    def Execution exec = content.execution
                    //iterate through the URLs, and submit a POST to the destination with the XML Execution result
                    final state = ExecutionController.getExecutionState(exec)
                    String xmlStr = doWithMockRequest {
                        def writer = new StringWriter()
                        def xml = new MarkupBuilder(writer)

                        xml.'notification'(trigger:trigger,status:state,executionId:exec.id){
                            new ExecutionController().renderApiExecutions([exec], [:], delegate)
                        }
                        writer.flush()
                        writer.toString()
                    }
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
                    //prep execution data
                    def Map execMap=doWithMockRequest {
                        new ExecutionController().exportExecutionData([content.execution])[0]
                    }
                    //TBD: nodestatus will migrate to execution data
                    if(content['nodestatus']){
                        execMap['nodestatus'] = content['nodestatus']
                    }

                    didsend=triggerPlugin(trigger,execMap,n.type, n.configuration)
                }else{
                    log.error("Unsupported notification type: " + n.type);
                }
                }catch(Throwable t){
                    log.error("Error sending notification: ${n}: "+t.message);
                    if (log.traceEnabled) {
                        log.trace("Notification failed",t)
                    }
                }
            }
        }

        return didsend
    }

    /**
     * Perform a plugin notification
     * @param trigger trigger name
     * @param data data content for the plugin
     * @param content content for notification
     * @param type plugin type
     * @param config user configuration
     */
    private boolean triggerPlugin(String trigger, Map data,String type, Map config){
        //replace exec info data references in config???

        //load plugin and configure with config values
        def plugin = configureNotificationPlugin(type, config)
        if (!plugin) {
            log.error("No Notification plugin found of type: " + type)
            return false
        }

        //invoke plugin
        //TODO: use executor
        if (!plugin.postNotification(trigger, data, config)) {
            log.error("Notification Failed: " + type);
            return false
        }
        true
    }

    String expandWebhookNotificationUrl(String url,Execution exec, ScheduledExecution job, String trigger){
        def state=ExecutionController.getExecutionState(exec)
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
