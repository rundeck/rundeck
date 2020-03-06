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

package com.dtolabs.rundeck.app.support

import grails.validation.Validateable
import org.rundeck.app.components.jobs.JobQueryInput

/*
 * ScheduledExecutionQuery.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: Feb 12, 2010 1:02:43 PM
 * $Id$
 */
public class ScheduledExecutionQuery extends BaseQuery implements JobQueryInput, Validateable{

    String jobFilter
    String jobExactFilter
    String projFilter
    String groupPath
    String groupPathExact

    String descFilter
    String loglevelFilter
    String idlist
    Boolean scheduledFilter
    Boolean scheduleEnabledFilter
    Boolean executionEnabledFilter
    String serverNodeUUIDFilter

    Integer daysAhead
    Boolean runJobLaterFilter

    /**
     * text filters
     */
    public final static TEXT_FILTERS = [
                job:'jobName',
                desc:'description',
            ]
    /**
     * equality filters
     */
    public final static EQ_FILTERS=[
                loglevel:'loglevel',
                proj:'project',
                jobExact:'jobName',
                serverNodeUUID:'serverNodeUUID'
            ]
    /**
     * Boolean filters
     */
    public final static  BOOL_FILTERS=[
            'executionEnabled':'executionEnabled',
            'scheduleEnabled':'scheduleEnabled',
            ]

    /**
     * Scheduled filter
     */
    public final static IS_SCHEDULED_FILTER = [
            'scheduled':'scheduled'
    ]

    /**
     * all filters
     */
    public final static  ALL_FILTERS = [ :]
    public final static  X_FILTERS = [ :]
    static{
            ALL_FILTERS.putAll(TEXT_FILTERS)
            ALL_FILTERS.putAll(EQ_FILTERS)
            ALL_FILTERS.putAll(BOOL_FILTERS)
            ALL_FILTERS.putAll(IS_SCHEDULED_FILTER)
            X_FILTERS.putAll(ALL_FILTERS)
            X_FILTERS.put('group','groupPath')
    }


    static constraints={

        sortBy(nullable: true)
        sortOrder(inList:["ascending","descending"],nullable: true)
        max(min:0,nullable: true)
        offset(min:0,nullable: true)

        jobFilter(nullable: true)
        jobExactFilter(nullable: true)
        projFilter(nullable: true)
        groupPath(nullable: true)
        groupPathExact(nullable: true)
        descFilter(nullable: true)
        loglevelFilter(nullable: true)
        idlist(nullable: true)
        scheduledFilter(nullable: true)
        scheduleEnabledFilter(nullable: true)
        executionEnabledFilter(nullable: true)
        serverNodeUUIDFilter(size: 36..36, blank: true, nullable: true, validator: { val, obj ->
            if (null == val) return true;
            try { return null != UUID.fromString(val) } catch (IllegalArgumentException e) {
                return false
            }
        })
        daysAhead(nullable: true)
        runJobLaterFilter(nullable: true)
    }


    public String toString(){
        StringBuffer sb = new StringBuffer()
        sb.append("ScheduledExecutionQuery[")
        ALL_FILTERS.each{k,v->
            if(this[k+'Filter']){
                sb.append(k)
                sb.append('Filter: ')
                sb.append("'${this[k+'Filter']}',")
            }
        }
        if(this['groupPath']){
            sb.append('groupPath: ')
            sb.append("'${this['groupPath']}',")
        }
        if(this['groupPathExact']){
            sb.append('groupPathExact: ')
            sb.append("'${this['groupPathExact']}',")
        }
        sb.append("]")
        return sb.toString()
    }
    /**
     * validate filter
     */
    public void configureFilter(){
       
    }

    public Map asExecQueryParams() {
        def map = [:]
        def convert=[
                proj: 'projFilter',
                job: 'jobFilter',
        ]
        ['proj','groupPath','groupPathExact','job'].each {
            def c=convert[it]?:it
            if(this[c]){
                map["${it}Filter"]= this[c]
            }
        }

        map
    }

    public Map asReportQueryParams() {
        def map = [:]
        if(projFilter){
            map.projFilter=projFilter
        }
        if(groupPath=='*'){

        }else if (groupPath){
            map['jobFilter']=groupPath
        }else if(groupPathExact && jobFilter){
            map['jobFilter'] = groupPathExact+'/'+jobFilter
        }

        map
    }

}
