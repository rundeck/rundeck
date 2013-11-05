/*
 * Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>
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

/**
 * State of workflow
 */
var FlowState = Class.create({
    model:{},
    executionId:null,
    targetElement:null,
    loadUrl:null,
    shouldUpdate:false,
    timer:null,
    initialize: function (eid, elem, params) {
        this.executionId = eid;
        this.targetElement = elem;
        Object.extend(this, params);
    },
    withOrWithoutMatch: function (root, selector, func, wofunc) {
        var elem = $(root).down(selector);
        if (elem && typeof(func)=='function') {
            func(elem);
        } else if(typeof(wofunc=='function')){
            wofunc();
        }
    },
    withoutMatch: function (root, selector, func) {
        this.withOrWithoutMatch(root, selector, null,func);
    },
    withMatch: function (root, selector, func) {
        this.withOrWithoutMatch(root,selector,func,
            this.logWarn.bind(this).curry("No match " + selector)
        );
    },
    updateStepState: function (root,stepctx, step) {
        this.withMatch(root, '.execstate.step[data-stepctx=' + stepctx + ']', function (elem) {
            $(elem).setAttribute('data-execstate', step.executionState);
        });
        if (step.errorMessage) {
            this.withMatch(root, '.errmsg.step[data-stepctx=' + stepctx + ']', function (elem) {
                $(elem).innerHTML = step.errorMessage;
                $(elem).show();
            });
        }
    },
    newNodeState: function(stepctx, node, nstate){
        var div = new Element('div');
        var nspan = new Element('span');
        nspan.addClassName('execstate isnode');
        nspan.setAttribute('data-node',node);
        nspan.setAttribute('data-stepctx',stepctx);
        nspan.setAttribute('data-execstate', nstate.executionState);
        var errspan = new Element('span');
        errspan.addClassName('errmsg isnode');
        errspan.setAttribute('data-node', node);
        errspan.setAttribute('data-stepctx', stepctx);
        errspan.style.display='none';
        div.appendChild(nspan);
        div.appendChild(errspan);
        return div;
    },
    setNodeState:function(node,nstate, elem){
        $(elem).innerHTML = node;
        $(elem).setAttribute('data-execstate', nstate.executionState);
    },
    addNodeState: function(root,stepctx,node,nstate){
        var nstates= $(root).down('.wfstepstate[data-stepctx=' + stepctx + '] .nodestates');
        var newstate= this.newNodeState(stepctx, node, nstate);
        nstates.appendChild(newstate);
        var execstate=newstate.down('.execstate');
        this.setNodeState(node, nstate, execstate);
    },
    updateNodeState: function (root, stepctx,node,nstate) {
        this.withOrWithoutMatch(root, '.execstate.isnode[data-stepctx=' + stepctx + '][data-node=' + node + ']',
            this.setNodeState.bind(this).curry(node,nstate),
            this.addNodeState.bind(this).curry(root, stepctx, node, nstate)
        );

        if (nstate.errorMessage) {
            this.withMatch(root, '.errmsg.isnode[data-stepctx=' + stepctx + '][data-node=' + node + ']', function (elem) {
                $(elem).innerHTML = nstate.errorMessage;
                $(elem).show();
            });
        }
    },
    logWarn: function (text) {
        if ($(this.targetElement + '_log')) {
            $(this.targetElement + '_log').innerHTML += "<br>" + text;
        }
    },
    updateWorkflow: function (currentwf,ctx) {
        var count = parseInt(currentwf.stepCount);
        if (!currentwf.steps) {
            return;
        }
        for (var i = 1; i <= count; i++) {
            var step = currentwf.steps[i - 1];
            var stepctx = ctx + (ctx ? '/' : '') + i;
            this.updateStepState(this.targetElement,stepctx,step);

            if (step.hasSubworkflow) {
                this.updateWorkflow(step.workflow, stepctx);
            }else if (step.nodeStep){
                var nodeset=step.stepTargetNodes? step.stepTargetNodes :currentwf.targetNodes;
                for(var n=0;n<nodeset.length;n++){
                    var node = nodeset[n];
                    var nstate= step.nodeStates?step.nodeStates[node]:null;
                    if(!nstate){
                        nstate={ executionState: 'WAITING' };
                    }
                    this.updateNodeState(this.targetElement, stepctx, node, nstate);
                }
            }
        }
    },
    update: function (data) {
        //compare
        this.model = data;
        if($(this.targetElement + '_json')){
            $(this.targetElement + '_json').innerHTML = Object.toJSON(this.model);
        }
        this.updateWorkflow(this.model,'');
    },
    callUpdate: function(){
        if(!this.shouldUpdate){
            clearInterval(this.timer);
            this.timer=null;
            return;
        }
        var state=this;
        new Ajax.Request(this.loadUrl,{
            evalScripts: true,
            evalJSON: true,
            onSuccess: function (transport) {
                var data = transport.responseJSON;
                state.update(data);
            }
        });
    },
    beginFollowing: function(){
        this.shouldUpdate=true;
        this.timer=setInterval(this.callUpdate.bind(this),3000);
    },
    stopFollowing: function(){
        this.shouldUpdate=false;
        clearInterval(this.timer);
        this.timer = null;
    }
});
