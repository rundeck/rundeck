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

import com.dtolabs.rundeck.app.support.DomainIndexHelper

class BaseReport {

    String node
    String title
    String status
    String actionType
    String ctxProject
    String ctxType
    String ctxName
    String maprefUri
    String reportId
    String tags
    String author
    Date dateStarted
    Date dateCompleted 
    String message

    static mapping = {
        message type: 'text'
        title type: 'text'

        DomainIndexHelper.generate(delegate) {
            index 'EXEC_REPORT_IDX_0', [/*'class',*/ 'ctxProject', 'dateCompleted', /*'jcExecId', 'jcJobId'*/]
            index 'EXEC_REPORT_IDX_1', ['ctxProject'/*, 'jcJobId'*/]
            index 'BASE_REPORT_IDX_2', [/*'class',*/ 'ctxProject', 'dateCompleted', 'dateStarted']
        }
    }
   static constraints = {
        reportId(nullable:true, maxSize: 1024+2048 /*jobName + groupPath size limitations from ScheduledExecution*/)
        tags(nullable:true)
        node(nullable:true)
        maprefUri(nullable:true)
        ctxName(nullable:true)
        ctxType(nullable:true)
        status(nullable:false, maxSize: 256)
        actionType(nullable:false, maxSize: 256)
    }
    public static final ArrayList<String> exportProps = [
            'node',
            'title',
            'status',
            'actionType',
            'ctxProject',
            'reportId',
            'tags',
            'author',
            'message',
            'dateStarted',
            'dateCompleted'
    ]

    def Map toMap(){
        def map=this.properties.subMap(exportProps)
        if(map.status=='timeout'){
            map.status='timedout'
        }
        if(map.actionType=='timeout'){
            map.actionType='timedout'
        }
        map
    }

    static buildFromMap(BaseReport obj, Map data) {
        data.each { k, v ->
            if ((k == 'status' || k == 'actionType')) {
                if (v == 'timedout') {
                    //XXX: use 'timeout' internally for timedout status, due to previous varchar(7) length limitations on
                    // the field :-Σ
                    v = 'timeout'
                }else if (v == 'succeeded') {
                    v='succeed'
                }else if (v.toString().length()>7) {
                    v='other'
                }
            }
            obj[k] = v
        }
    }

    static BaseReport fromMap(Map data) {
        def BaseReport report = new BaseReport()
        buildFromMap(report, data.subMap(exportProps))
        report
    }
}
