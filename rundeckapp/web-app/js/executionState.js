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

    updateWorkflow: function (currentwf,ctx) {
        var count = parseInt(currentwf.stepCount);
        if (currentwf.steps) {
            for (var i = 1; i <= count; i++) {
                var step = currentwf.steps[i - 1];
                var stepctx = ctx + (ctx ? '/' : '') + i;
                var stepstate = $(this.targetElement).down('.execstate.step[data-stepctx=' + stepctx + ']');
                if (stepstate) {
                    stepstate.innerHTML =  step.executionState;
                    stepstate.setAttribute('data-execstate', step.executionState);
                }else if($(this.targetElement + '_log')){
                    $(this.targetElement + '_log').innerHTML+="<br>No step state "+stepctx;
                }
                if(step.errorMessage){
                    var msg = $(this.targetElement).down('.errmsg.step[data-stepctx=' + stepctx + ']');
                    if(msg){
                        msg.innerHTML=step.errorMessage;
                        msg.show();
                    }
                }
                if (step.hasSubworkflow) {
                    this.updateWorkflow(step.workflow, stepctx);
                }else if (step.stepTargetNodes && step.nodeStates){
                    for(var n=0;n<step.stepTargetNodes.length;n++){
                        var node = step.stepTargetNodes[n];
                        var nstate=step.nodeStates[node];
                        if(nstate){
                            var nodestate = $(this.targetElement).down('.execstate.node[data-stepctx=' + stepctx + '][data-node='+node+']');
                            if (nodestate) {
                                nodestate.innerHTML = node+': '+nstate.executionState;
                                nodestate.setAttribute('data-execstate', nstate.executionState);
                            } else if ($(this.targetElement + '_log')) {
                                $(this.targetElement + '_log').innerHTML += "<br>No Node state " + stepctx + ": " + node;
                            }

                            if (nstate.errorMessage) {
                                var msg = $(this.targetElement).down('.errmsg.isnode[data-stepctx=' + stepctx + '][data-node=' + node + ']');
                                if (msg) {
                                    msg.innerHTML = nstate.errorMessage;
                                    msg.show();
                                }
                            }
                        }
                    }
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
