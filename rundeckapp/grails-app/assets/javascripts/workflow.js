/*
 * Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the License.
 */
/**
 * Represents a workflow
 */
var RDWorkflow = Class.create({
    workflow:null,
    nodeSteppluginDescriptions:null,
    wfSteppluginDescriptions:null,
    initialize: function(wf,params){
        this.workflow=wf;
        Object.extend(this, params);
    },
    contextType: function (ctx) {
        var string = "";
        var step = this.workflow[RDWorkflow.workflowIndexForContextId(ctx[0])];
        if (typeof(step) != 'undefined') {
            if (step['exec']) {
                return 'command';
            } else if (step['jobref']) {
                return 'job';
            } else if (step['script']) {
                return 'script';
            } else if (step['scriptfile']) {
                return 'scriptfile';
            } else if (step['type']) {//plugin
                if (step['nodeStep'] ) {
                    return 'node-step-plugin plugin';
                } else if (null != step['nodeStep'] && !step['nodeStep'] ) {
                    return 'workflow-step-plugin plugin';
                }else{
                    return 'plugin';
                }
            }
        }
        return 'console';
    },
    renderContextStepNumber: function (ctx) {
        if (typeof(ctx) == 'string') {
            ctx = RDWorkflow.parseContextId(ctx);
        }
        var string = '';
        string += RDWorkflow.stepNumberForContextId(ctx[0]);
        if (ctx.length > 1) {
//                string += "/" + ctx.slice(1).join("/")
        }
        string += ". ";
        return string;
    },
    renderContextString: function (ctx) {
        if(typeof(ctx)=='string'){
            ctx= RDWorkflow.parseContextId(ctx);
        }
        var string = "";
        var step = this.workflow[RDWorkflow.workflowIndexForContextId(ctx[0])];
        if (typeof(step) != 'undefined') {
            if(step['description']){
                string = step['description'];
            }else if (step['exec']) {
//                        string+=' $ '+step['exec'];
                string = 'Command';
            } else if (step['jobref']) {
                string = (step['jobref']['group'] ? step['jobref']['group'] + '/' : '') + step['jobref']['name'];
            } else if (step['script']) {
                string = "Script";
            } else if (step['scriptfile']) {
                string = step['scriptfile'];
            } else if (step['type']) {//plugin
                var title = "Plugin " + step['type'];
                if (step['nodeStep'] && this.nodeSteppluginDescriptions && this.nodeSteppluginDescriptions[step['type']]) {
                    title = this.nodeSteppluginDescriptions[step['type']].title || title;
                } else if (!step['nodeStep'] && this.wfSteppluginDescriptions && this.wfSteppluginDescriptions[step['type']]) {
                    title = this.wfSteppluginDescriptions[step['type']].title || title;
                }
                string = title;
            }
        }else{
            return "[?]";
        }
        return string;
    }
});
/**
 * remove escaping and halt processing at any break chars, returns object with 'text' (unescaped text), 'bchar' (seen break char or null),
 * 'rest' (remaining escaped text after first seen breakchar or null)
 * @param input input
 * @param echar escape char (e.g. '\\')
 * @param chars chars to process as escaped
 * @param breakchars chars to halt processing
 * @returns {{text: string, bchar: *, rest: *}}
 */
RDWorkflow.unescape=function(input,echar,chars,breakchars){
    "use strict";
    var arr=[];
    var e=false;
    var i=0;
    var bchar=null;
    for(;i<input.length;i++){
        var c = input.charAt(i);
        if(c==echar){
            if(e){
                arr.push(echar);
                e=false;
            }else{
                e=true;
            }
        }else if(chars.indexOf(c)>=0){
            if(e){
                arr.push(c);
                e=false;
            }else if(breakchars.indexOf(c)>=0){
                bchar=c;
                break;
            }else{
                arr.push(c);
            }
        }else{
            if(e){
                arr.push(echar);
                e=false;
            }
            arr.push(c);
        }
    }
    return {text:arr.join(""),bchar:bchar,rest:i<=input.length-1?input.substring(i+1):null};
};
/**
 *
 * @param input
 * @param sep
 * @returns {Array}
 */
RDWorkflow.splitEscaped=function(input,sep){
    "use strict";
    var parts=[];

    var rest=input;
    while(rest){
        var result=RDWorkflow.unescape(rest,'\\',['\\','/'],[sep]);
        parts.push(result.text);
        rest=result.rest;
    }
    return parts;
};
/**
 * Returns array of step context strings given the context identifier
 * @param context
 * @returns {*}
 */
RDWorkflow.contextStringSeparator = '/';
RDWorkflow.parseContextId= function (context) {
    if (context == null) {
        return null;
    }
    //split context into project,type,object
    var t = RDWorkflow.splitEscaped(context,RDWorkflow.contextStringSeparator);
    var i = 0;
    var vals = new Array();
    for (i = 0; i < t.length; i++) {
        var x = t[i];
        vals.push(x);
    }
    return vals;
};

/**
 * Return the parameter string for the context id. If id is "1@abc=xyz,tyf=lmn", then returns "abc=xyz,tyf=lmn"
 * @param ctxid
 * @returns {*}
 */
RDWorkflow.paramsForContextId= function (ctxid) {
    var m = ctxid.match(/^(\d+)(e)?(@(.+))?$/);
    if (m[4]) {
        return m[4].replace(/\\([/@,=])/g, '$1');
    }
    return null;
}
;
RDWorkflow.isErrorhandlerForContextId= function (ctxid) {
    var m = ctxid.match(/^(\d+)(e)?(@.+)?$/);
    if (m[2] == 'e') {
        return true
    }
    return false;
}
;
RDWorkflow.stepNumberForContextId= function (ctxid) {
    var m = ctxid.match(/^(\d+)(e)?(@.+)?$/);
    if (m[1]) {
        return parseInt(m[1]);
    }
    return null;
};
RDWorkflow.workflowIndexForContextId = function (ctxid) {
    var m = RDWorkflow.stepNumberForContextId(ctxid);
    if (m!=null) {
        return m - 1;
    }
    return null;
};
/**
 * removes error handler/parameters from the context path
 * @param context
 */
RDWorkflow.cleanContextId = function (context) {
    var parts=RDWorkflow.parseContextId(context);
    for (var i=0;i<parts.length;i++){
        parts[i]=RDWorkflow.stepNumberForContextId(parts[i]);
    }
    return parts.join(RDWorkflow.contextStringSeparator);
};
