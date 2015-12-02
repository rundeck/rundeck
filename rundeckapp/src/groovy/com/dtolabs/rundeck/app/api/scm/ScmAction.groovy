package com.dtolabs.rundeck.app.api.scm

import grails.validation.Validateable
import org.codehaus.groovy.grails.web.json.JSONObject
import org.grails.databinding.BindUsing

/**
 * Input for performing an action
 */
@Validateable
class ScmAction {
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


    @Override
    public String toString() {
        return "ScmAction{" +
                "input=" + input +
                ", jobIds=" + jobIds +
                ", selectedItems=" + selectedItems +
                ", deletedItems=" + deletedItems +
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
        return scmAction
    }
    static Closure validateJson = { data, boolean inputOnly=false ->
        String errormsg = ''
        if (JSONObject.NULL != data.input && !(data.input instanceof Map)) {
            errormsg += " json: expected 'input' to be a map"
        }
        if(!inputOnly) {
            if (JSONObject.NULL != data.jobs && !(data.jobs instanceof Collection)) {
                errormsg += " json: expected 'jobs' to be a list"
            }
            if (JSONObject.NULL != data.items && !(data.items instanceof Collection)) {
                errormsg += " json: expected 'items' to be a list"
            }
            if (JSONObject.NULL != data.deleted && !(data.deleted instanceof Collection)) {
                errormsg += " json: expected 'deleted' to be a list"
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
        return scmAction
    }

    private static List<String> stringList(data) {
        def data2=[]
        if(data && data!=JSONObject.NULL && data instanceof Collection) {
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
