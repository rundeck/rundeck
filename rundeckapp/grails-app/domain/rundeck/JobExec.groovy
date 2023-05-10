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

package rundeck

import com.dtolabs.rundeck.execution.IWorkflowJobItem
import rundeck.data.constants.WorkflowStepConstants

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
    String jobProject
    String jobIdentifier
    String argString
    String uuid
    String nodeFilter
    Boolean nodeKeepgoing
    Integer nodeThreadcount
    Boolean nodeStep
    String nodeRankAttribute
    Boolean nodeRankOrderAscending
    Boolean nodeIntersect
    Boolean failOnDisable
    Boolean childNodes
    Boolean importOptions
    Boolean useName
    Boolean ignoreNotifications
    static transients = ['jobIdentifier']

    static constraints = {
        jobName(nullable: true, blank: true, maxSize: 1024)
        jobGroup(nullable: true, blank: true, maxSize: 2048)
        jobProject(nullable: true, blank: true, maxSize: 2048)
        argString(nullable: true, blank: true)
        nodeStep(nullable: true)
        nodeKeepgoing(nullable: true)
        nodeFilter(nullable: true, maxSize: 1024)
        nodeThreadcount(nullable: true)
        nodeRankAttribute(nullable: true, maxSize: 256)
        nodeRankOrderAscending(nullable: true)
        nodeIntersect(nullable: true)
        failOnDisable(nullable: true)
        importOptions(nullable: true)
        uuid(nullable: true)
        useName(nullable:true)
        ignoreNotifications(nullable: true)
        childNodes(nullable: true)
    }

    static mapping = {
        argString type: 'text'
        jobName type: 'string'
        jobGroup type: 'string'
        jobProject type: 'string'
        nodeFilter type: 'text'
        nodeRankAttribute type: 'text'
        uuid type: 'text'
    }

    public String toString() {
        return "jobref((uuid=\"${uuid}\" name=\"${jobName}\" group=\"${jobGroup}\" project=\"${jobProject}\" argString=\"${argString}\" " +
                "nodeStep=\"${nodeStep}\"" +
                "nodeFilter=\"${nodeFilter}\"" +
                "nodeKeepgoing=\"${nodeKeepgoing}\"" +
                "nodeThreadcount=\"${nodeThreadcount}\"" +
                "nodeRankAttribute=\"${nodeRankAttribute}\"" +
                "nodeRankOrderAscending=\"${nodeRankOrderAscending}\"" +
                "nodeIntersect=\"${nodeIntersect}\"" +
                ")" + (errorHandler ? " [handler: ${errorHandler}" : '')
    }

    public String summarize() {
        return "job: ${this.getJobIdentifier()}${argString?' -- '+argString:''}"
    }

    public Map getConfiguration() { null }

    public String getPluginType() {
        return WorkflowStepConstants.TYPE_JOB_REF
    }

    public String getJobIdentifier() {
        if(!useName && uuid){
            return uuid
        }
        return (null==jobGroup?'':jobGroup+"/")+jobName;
    }
    public void setJobIdentifier(){
        //noop
    }

    /**
     * Find the referenced Job, using the uuid if uuid is specified and useName is false, otherwise using job name/group
     * @param project current project, required when reference does not specify project
     * @return found job, or null
     */
    public ScheduledExecution findJob(String project) {
        if (!useName && uuid) {
            return ScheduledExecution.findByUuid(uuid)
        } else {
            return ScheduledExecution.findByProjectAndJobNameAndGroupPath(
                    jobProject ?: project,
                    jobName,
                    jobGroup ?: null
            )
        }
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
        if(jobProject){
            map.jobref.project = jobProject
        }
        if(uuid){
        	map.jobref.uuid = uuid
        }
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
        if(failOnDisable){
            map.jobref.failOnDisable = failOnDisable
        }
        if(childNodes){
            map.jobref.childNodes = childNodes
        }
        if(importOptions){
            map.jobref.importOptions = importOptions
        }
        if(ignoreNotifications){
            map.jobref.ignoreNotifications = ignoreNotifications
        }
        if(nodeFilter){
            map.jobref.nodefilters=[filter:nodeFilter]
            def dispatch=[:]
            if(null!=nodeThreadcount && nodeThreadcount>0){
                dispatch.threadcount=nodeThreadcount
            }
            if(null!=nodeKeepgoing){
                dispatch.keepgoing=!!nodeKeepgoing
            }
            if(nodeRankAttribute){
                dispatch.rankAttribute=nodeRankAttribute
            }
            if(null!=nodeRankOrderAscending){
                dispatch.rankOrder=nodeRankOrderAscending?'ascending':'descending'
            }
            if(null!=nodeIntersect){
                dispatch.nodeIntersect=nodeIntersect
            }
            if(dispatch){
                map.jobref.nodefilters.dispatch=dispatch
            }
        } else if (null != nodeIntersect) {
            map.jobref.nodefilters = [dispatch: [nodeIntersect: nodeIntersect]]
        }
        if(useName){
            map.jobref.useName="true"
        }
        map.enabled=enabled
        return map
    }
    /**
    * Return map representation without details
     */
    public Map toDescriptionMap(){
        final Map map = [jobref: [group: jobGroup ? jobGroup : '', name: jobName]]
        if(jobProject){
            map.jobref.project = jobProject
        }
        if(uuid){
        	map.jobref.uuid = uuid
        }
        if(nodeStep){
            map.jobref.nodeStep="true"
        }
        if (errorHandler) {
            map.errorhandler = errorHandler.toDescriptionMap()
        }
        if (description) {
            map.description = description
        }
        if(useName){
            map.jobref.useName="true"
        }
        return map
    }

    static JobExec jobExecFromMap(Map map) {
        def exec = new JobExec()
        updateFromMap(exec, map)
        return exec
    }

    static void updateFromMap(JobExec exec, Map map) {
        exec.jobGroup=map.jobref.group
        exec.jobName=map.jobref.name
        if (map.jobref.project || map.project) {
            exec.jobProject = map.jobref.project ?: map.project
        }
        if(map.jobref.uuid){
        	exec.uuid = map.jobref.uuid
        }
        if(map.jobref.args){
            exec.argString=map.jobref.args
        }
        if(map.jobref.nodeStep in ['true',true]){
            exec.nodeStep=true
        }else{
            exec.nodeStep=false
        }
        if(map.jobref.failOnDisable){
            if (map.jobref.failOnDisable in ['true', true]) {
                exec.failOnDisable = true
            }
        }else if(map.failOnDisable){
            if (map.failOnDisable in ['true', true]) {
                exec.failOnDisable = true
            }
        }
        if(map.jobref.childNodes){
            if (map.jobref.childNodes in ['true', true]) {
                exec.childNodes = true
            }
        }else if(map.childNodes){
            if (map.childNodes in ['true', true]) {
                exec.childNodes = true
            }
        }
        if(map.jobref.importOptions){
            if (map.jobref.importOptions in ['true', true]) {
                exec.importOptions = true
            }
        } else if(map.importOptions) {
            if (map.importOptions in ['true', true]) {
                exec.importOptions = true
            }
        }

        if(map.jobref.ignoreNotifications){
            if (map.jobref.ignoreNotifications in ['true', true]) {
                exec.ignoreNotifications = true
            }
        }
        exec.keepgoingOnSuccess = !!map.keepgoingOnSuccess
        exec.description=map.description?.toString()
        exec.enabled=map.enabled!=null?map.enabled:true
        if(map.jobref.nodefilters instanceof Map){
            exec.nodeFilter=map.jobref.nodefilters.filter?.toString()
            if(exec.nodeFilter){
                def dispatch = map.jobref.nodefilters.dispatch
                if(dispatch?.threadcount){
                    if(dispatch.threadcount instanceof Integer){
                        exec.nodeThreadcount= dispatch.threadcount ?: 1
                    }else{
                        exec.nodeThreadcount = Integer.parseInt(dispatch.threadcount.toString()) ?: 1
                    }
                }
                if(null!=dispatch?.keepgoing){
                    if (dispatch.keepgoing in ['true', true]) {
                        exec.nodeKeepgoing=true
                    }else{
                        exec.nodeKeepgoing=false
                    }
                }
                if (null != dispatch?.rankOrder) {
                    exec.nodeRankOrderAscending = (dispatch.rankOrder == 'ascending')
                }

                exec.nodeRankAttribute= dispatch?.rankAttribute
            }
            if(map.jobref.nodefilters.dispatch && null!=map.jobref.nodefilters.dispatch?.nodeIntersect){
                if (map.jobref.nodefilters.dispatch.nodeIntersect in ['true', true]) {
                    exec.nodeIntersect=true
                }else{
                    exec.nodeIntersect=false
                }
            }
        }
        if(map.jobref.useName in ['true',true] || (map.jobref.useName == null && map.jobref.name && !map.jobref.uuid)){
            exec.useName=true
        }else{
            exec.useName=false
        }
        //nb: error handler is created inside Workflow.fromMap
    }
}
