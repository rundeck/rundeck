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
    contextStringSeparator:'/',
    initialize: function(wf,params){
        this.workflow=wf;
        Object.extend(this, params);
    },
    parseContextId: function (context) {
        if (context == null) {
            return null;
        }
        //split context into project,type,object
        var t = context.split(this.contextStringSeparator);
        var i = 0;
        var vals = new Array();
        for (i = 0; i < t.length; i++) {
            var x = t[i];
            vals.push(x);
        }
        return vals;
    },
    workflowIndexForContextId: function (ctxid) {
        var id = ctxid;
        if (ctxid == ~/e$/) {
            id = ctxid.substring(0, ctxid.length - 2);
        }
        return parseInt(id) - 1;
    },
    contextType: function (ctx) {
        var string = "";
        var step = this.workflow[this.workflowIndexForContextId(ctx[0])];
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
    renderContextString: function (ctx) {
        if(typeof(ctx)=='string'){
            ctx=this.parseContextId(ctx);
        }
        var string = "";
        var step = this.workflow[this.workflowIndexForContextId(ctx[0])];
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
