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

public class JobExec extends CommandExec implements IWorkflowJobItem{

    String jobName
    String jobGroup
    String jobIdentifier
    static transients=['jobIdentifier']

    static constraints = {
        jobName(nullable: false, blank: false)
        jobGroup(nullable: true, blank: true)
    }

    public String toString() {
        return "jobref(name=\"${jobName}\" group=\"${jobGroup}\" argString=\"${argString}\")"
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

    public CommandExec createClone(){
        JobExec ce = new JobExec(this.properties)
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
        return map
    }
}