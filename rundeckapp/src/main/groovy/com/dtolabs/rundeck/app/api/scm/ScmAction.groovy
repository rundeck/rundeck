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

package com.dtolabs.rundeck.app.api.scm

import com.dtolabs.rundeck.util.JsonUtil
import grails.validation.Validateable

/**
 * Input for performing an action
 */
class ScmAction implements Validateable {
    /**
     * Input field key/value
     */
    Map<String, String> input
    /**
     * selected items
     */
    List<String> selectedItems
    /**
     * selected items
     */
    List<String> jobIds
    /**
     * items to delete
     */
    List<String> deletedItems
    /**
     * Jobs to delete for import
     */
    List<String> deletedJobs


    @Override
    public String toString() {
        return "ScmAction{" +
                "input=" + input +
                ", jobIds=" + jobIds +
                ", selectedItems=" + selectedItems +
                ", deletedItems=" + deletedItems +
                ", deletedJobs=" + deletedJobs +
                '}';
    }

    /**
     * Return an error message string to indicate error
     */
    static Closure validateXml = { xml ->
        String errormsg = ''
        if (!xml?.input) {
            errormsg += " xml: expected 'input' element"
        }
        if (!xml?.jobs) {
            errormsg += " xml: expected 'jobs' element"
        }
        if (!xml?.items) {
            errormsg += " xml: expected 'items' element"
        }
        errormsg ?: null
    }
    static Closure parseWithXml = { xml ->
        //input map

        def scmAction = new ScmAction()
        scmAction.input = [:]
        xml?.input?.entry?.each {
            scmAction.input[it.'@key'.text()] = it.text()
        }

        //jobs
        scmAction.jobIds = xml?.jobs?.job?.collect {
            it.'@jobId'.text()
        }
        //items
        scmAction.selectedItems = xml?.items?.item?.collect {
            it.'@itemId'.text()
        }
        //deleted
        scmAction.deletedItems = xml?.deleted?.item?.collect {
            it.'@itemId'.text()
        }
        //deleted
        scmAction.deletedJobs = xml?.deletedJobs?.job?.collect {
            it.'@jobId'.text()
        }
        return scmAction
    }
    static Closure validateJson = { data, boolean inputOnly=false ->
        String errormsg = ''
        if (JsonUtil.jsonNull(data.input) != null && !(data.input instanceof Map)) {
            errormsg += " json: expected 'input' to be a map"
        }
        if(!inputOnly) {
            if (JsonUtil.jsonNull(data.jobs) != null && !(data.jobs instanceof Collection)) {
                errormsg += " json: expected 'jobs' to be a list"
            }
            if (JsonUtil.jsonNull(data.items) != null && !(data.items instanceof Collection)) {
                errormsg += " json: expected 'items' to be a list"
            }
            if (JsonUtil.jsonNull(data.deleted) != null && !(data.deleted instanceof Collection)) {
                errormsg += " json: expected 'deleted' to be a list"
            }
            if (JsonUtil.jsonNull(data.deletedJobs) != null && !(data.deletedJobs instanceof Collection)) {
                errormsg += " json: expected 'deletedJobs' to be a list"
            }
        }
        errormsg ?: null
    }
    static Closure parseWithJson = { data ->

        def scmAction = new ScmAction()
        //input map
        scmAction.input = stringMap(data.input)
        //items values
        scmAction.jobIds = stringList(data.jobs)
        scmAction.selectedItems = stringList(data.items)
        scmAction.deletedItems = stringList(data.deleted)
        scmAction.deletedJobs = stringList(data.deletedJobs)
        return scmAction
    }

    private static List<String> stringList(data) {
        def data2=[]
        if (data && JsonUtil.jsonNull(data) != null && data instanceof Collection) {
            for (i in data) {
                data2 << i.toString()
            }
        }
        data2
    }

    private static Map<String,String> stringMap(Map data) {
        def data2=[:]
        data?.each{k,v->
            data2[k.toString()]=v.toString()
        }
        data2
    }
}
