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
        string += ". "
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
                if (step['nodeStep'] && this.nodeSteppluginDescriptions) {
                    title = this.nodeSteppluginDescriptions[step['type']].title;
                } else if (!step['nodeStep'] && this.wfSteppluginDescriptions) {
                    title = this.wfSteppluginDescriptions[step['type']].title;
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
    var t = context.split(RDWorkflow.contextStringSeparator);
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
        return m[4]
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

RDWorkflow.test = function(){
    console.assert(RDWorkflow.paramsForContextId('2@node=a') == 'node=a');
    console.assert(RDWorkflow.stepNumberForContextId('2@node=a') == 2);
    console.assert(RDWorkflow.workflowIndexForContextId('2@node=a') == 1);
    console.assert(RDWorkflow.isErrorhandlerForContextId('2@node=a') == false);
    console.assert(RDWorkflow.paramsForContextId('2') == null);
    console.assert(RDWorkflow.stepNumberForContextId('2') == 2);
    console.assert(RDWorkflow.workflowIndexForContextId('2') == 1);
    console.assert(RDWorkflow.isErrorhandlerForContextId('2') == false);
    console.assert(RDWorkflow.paramsForContextId('2e') == null);
    console.assert(RDWorkflow.stepNumberForContextId('2e') == 2);
    console.assert(RDWorkflow.workflowIndexForContextId('2e') == 1);
    console.assert(RDWorkflow.isErrorhandlerForContextId('2e') == true);
    console.assert(RDWorkflow.paramsForContextId('2e@blah=c') == 'blah=c');
    console.assert(RDWorkflow.stepNumberForContextId('2e@blah=c') == 2);
    console.assert(RDWorkflow.workflowIndexForContextId('2e@blah=c') == 1);
    console.assert(RDWorkflow.isErrorhandlerForContextId('2e@blah=c') == true);
    console.assert(RDWorkflow.stepNumberForContextId('1e@blah=c') == 1);
    console.assert(RDWorkflow.workflowIndexForContextId('1e@blah=c') == 0);
    console.assert(RDWorkflow.isErrorhandlerForContextId('1e@blah=c') == true);

    //parse context id
    console.assert(RDWorkflow.parseContextId('1').length==1,'wrong length')
    console.assert(RDWorkflow.parseContextId('1/1').length==2,'wrong length')
    console.assert(RDWorkflow.parseContextId('1/1/1').length==3,'wrong length')
    console.assert(RDWorkflow.parseContextId('1/2/3')[0]=='1','wrong value')
    console.assert(RDWorkflow.parseContextId('1/2/3')[1]=='2','wrong value')
    console.assert(RDWorkflow.parseContextId('1/2/3')[2]=='3','wrong value')
    console.assert(RDWorkflow.parseContextId('1e@abc/2/3')[0]=='1e@abc','wrong value')
    console.assert(RDWorkflow.parseContextId('1/2e@asdf=xyz/3')[1]=='2e@asdf=xyz','wrong value')
}
