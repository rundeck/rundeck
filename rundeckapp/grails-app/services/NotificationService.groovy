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

        if(source.notifications && source.notifications.find{it.eventTrigger=='on'+trigger}){
            Notification n = source.notifications.find{it.eventTrigger=='on'+trigger}
            if(n.type!='email'){
                System.err.println("Unsupported notification type: "+n.type);
                return false
            }
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
            return true
        }
        return false
    }

}