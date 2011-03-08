import com.sun.xml.internal.stream.XMLBufferListener
import groovy.xml.MarkupBuilder
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.params.HttpClientParams
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.Header
import org.apache.commons.httpclient.methods.RequestEntity
import org.apache.commons.httpclient.methods.StringRequestEntity
import grails.util.GrailsWebUtil
import org.springframework.web.context.support.WebApplicationContextUtils
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.springframework.web.context.request.RequestContextHolder

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

public class NotificationService {

    def mailService

    def boolean triggerJobNotification(String trigger, schedId, Map content){
        if(trigger && schedId){
            def ScheduledExecution sched = ScheduledExecution.get(schedId)
            if(null!=sched){
                return triggerJobNotification(trigger,sched,content)
            }
        }
        return false
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
                            System.err.println("Error sending notification email: "+e.getMessage());
                        }
                    }
                    didsend= true
                }else if(n.type=='url'){    //sending notification of a status trigger for the Job

                    def requestAttributes = RequestContextHolder.getRequestAttributes()
                    boolean unbindrequest = false
                    // outside of an executing request, establish a mock version
                    if (!requestAttributes) {
                        def servletContext = ServletContextHolder.getServletContext()
                        def applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext)
                        requestAttributes = GrailsWebUtil.bindMockWebRequest(applicationContext)
                        unbindrequest = true
                    }
                    def Execution exec = content.execution
                    def urlarr = n.content.split(",") as List
                    //iterate through the URLs, and submit a POST to the destination with the XML Execution result

                    def writer = new StringWriter()
                    def xml = new MarkupBuilder(writer)
                    final state = ExecutionController.getExecutionState(exec)

                    try {
                        xml.'notification'(trigger:trigger,status:state,executionId:exec.id){
                            new ExecutionController().renderApiExecutions([exec], delegate)
                        }

                    } finally {
                        if (unbindrequest) {
                            RequestContextHolder.setRequestAttributes(null)
                        }
                    }
                    writer.flush()
                    String xmlStr = writer.toString()
                    if (log.traceEnabled){
                        log.trace("Posting webhook notification[${n.eventTrigger},${state},${exec.id}]; to URLs: ${n.content}")
                    }
                    def webhookfailure=false
                    urlarr.each{String urlstr->
                        try{
                            if(!postDataUrl(urlstr,xmlStr,n.eventTrigger, state,exec.id.toString())){
                                webhookfailure=true
                                log.trace("Notification failed; URL ${urlstr}")
                            }else if (log.traceEnabled) {
                                log.trace("Notification succeeded; URL ${urlstr}")
                            }
                        } catch (Throwable t) {
                            webhookfailure=true
                            System.err.println("Notification failed; URL ${urlstr}: " + t.message);
                            if (log.traceEnabled) {
                                log.trace("Notification failed", t)
                            }
                        }
                    }
                    didsend=!webhookfailure
                }else{
                    System.err.println("Unsupported notification type: " + n.type);
                }
                }catch(Throwable t){
                    System.err.println("Error sending notification: ${n}: "+t.message);
                    if (log.traceEnabled) {
                        log.trace("Notification failed",t)
                    }
                }
            }
        }

        return didsend
    }

    static boolean postDataUrl(String url, String xmlstr, String notificationtype, String status, String id, rptCount=1, backoff=2){
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
            method.setRequestHeader(new Header("X-RunDeck-Notification-Type", notificationtype))
            method.setRequestHeader(new Header("X-RunDeck-Notification-Execution-Status", status))
            method.setRequestHeader(new Header("X-RunDeck-Notification-Execution-ID", id))
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
            System.err.println("Unable to POST notification after ${count} tries: ${notificationtype} for execution ${id} (${status}) to ${url}: ${error}");
        }
        return complete
    }

}