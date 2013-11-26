package rundeck

import com.dtolabs.rundeck.execution.IWorkflowJobItem

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
* JobExec.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 26, 2010 4:49:19 PM
* $Id$
*/

public class JobExec extends WorkflowStep implements IWorkflowJobItem{

    String jobName
    String jobGroup
    String jobIdentifier
    String argString
    Boolean nodeStep
    static transients=['jobIdentifier']

    static constraints = {
        jobName(nullable: false, blank: false)
        jobGroup(nullable: true, blank: true)
        argString(nullable: true, blank: true)
        nodeStep(nullable: true)
    }

    static mapping = {
        argString type: 'text'
        jobName type: 'text'
        jobGroup type: 'text'
    }

    public String toString() {
        return "jobref(name=\"${jobName}\" group=\"${jobGroup}\" argString=\"${argString}\" nodeStep=\"${nodeStep}\")" + (errorHandler ? " [handler: ${errorHandler}" : '')
    }

    public String summarize() {
        return "job: ${this.getJobIdentifier()}${argString?' -- '+argString:''}"
    }


    public String getJobIdentifier() {
        return (null==jobGroup?'':jobGroup+"/")+jobName;
    }
    public void setJobIdentifier(){
        //noop
    }

    public JobExec createClone(){
        Map properties = new HashMap(this.properties)
        properties.remove('errorHandler')
        JobExec ce = new JobExec(properties)
        return ce
    }
    /**
    * Return canonical map representation
     */
    public Map toMap(){
        final Map map = [jobref: [group: jobGroup ? jobGroup : '', name: jobName]]
        if(argString){
            map.jobref.args=argString
        }
        if(nodeStep){
            map.jobref.nodeStep="true"
        }
        if (errorHandler) {
            map.errorhandler = errorHandler.toMap()
        } else if (keepgoingOnSuccess) {
            map.keepgoingOnSuccess = keepgoingOnSuccess
        }
        if (description) {
            map.description = description
        }
        return map
    }

    static JobExec jobExecFromMap(Map map){
        JobExec exec = new JobExec()
        exec.jobGroup=map.jobref.group
        exec.jobName=map.jobref.name
        if(map.jobref.args){
            exec.argString=map.jobref.args
        }
        if(map.jobref.nodeStep in ['true',true]){
            exec.nodeStep=true
        }
        exec.keepgoingOnSuccess = !!map.keepgoingOnSuccess
        exec.description=map.description?.toString()
        //nb: error handler is created inside Workflow.fromMap
        return exec
    }
}
