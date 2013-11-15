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

var StepFlow=Class.create({
    flow:null,
    targetElement:null,
    initialize: function(flow, targetElement,params){
        this.flow = flow;
        this.targetElement= targetElement;
        Object.extend(this, params);
    },

    showStepOutput: function (elem, stepctx, evt) {
        var wfstep = $(elem).up('.wfstepstate');
        if (wfstep) {
            $(wfstep).toggleClassName('collapsed');
        }
    },
    bindStepOutput: function (elem, stepctx) {
        Event.observe(elem, 'click', this.showStepOutput.bind(this).curry(elem, stepctx));
    },
    updateStepState: function (root, stepctx, step) {
        this.flow.withMatch(root, '.execstate.step[data-stepctx=' + stepctx + ']', function (elem) {
            $(elem).setAttribute('data-execstate', step.executionState);
        });
        this.flow.withMatch(root, '.stepident[data-stepctx=' + stepctx + ']', function (elem) {
            var type = this.workflow.contextType(stepctx);
            $(elem).innerHTML = '<i class="rdicon icon-small ' + type + '"></i> ' + this.workflow.renderContextString(stepctx);
        });
        var me = this;
        this.flow.withMatch(root, '.stepaction[data-stepctx=' + stepctx + ']', function (elem) {
            if ($(elem).getAttribute('data-bound') != 'true') {
                me.bindStepOutput($(elem), stepctx);
                $(elem).setAttribute('data-bound', 'true');
            }
        });
        if (step.errorMessage) {
            this.flow.withMatch(root, '.errmsg.step[data-stepctx=' + stepctx + ']', function (elem) {
                $(elem).innerHTML = step.errorMessage;
                $(elem).show();
            });
        }

    },
    bindNodeOutput: function (elem, stepctx, node) {
        Event.observe(elem, 'click', this.flow.showOutput.bind(this.flow).curry(elem,this.targetElement+'_output', stepctx, node));
    },
    newNodeState: function (stepctx, node, nstate) {
        var div = new Element('div');
        div.addClassName('textbtn textbtn-default');
        var nspan = new Element('span');
        nspan.addClassName('execstate isnode');
        nspan.setAttribute('data-node', node);
        nspan.setAttribute('data-stepctx', stepctx);
        nspan.setAttribute('data-execstate', nstate.executionState);
        var errspan = new Element('span');
        errspan.addClassName('errmsg isnode');
        errspan.setAttribute('data-node', node);
        errspan.setAttribute('data-stepctx', stepctx);
        errspan.style.display = 'none';
        div.appendChild(nspan);
        div.appendChild(errspan);
        this.bindNodeOutput(div, stepctx, node);
        return div;
    },
    setNodeState: function (stepctx, node, nstate, elem) {
        $(elem).innerHTML = node;
        $(elem).setAttribute('data-execstate', nstate.executionState);
        if ($(elem).getAttribute('data-bound') != 'true') {
            this.bindNodeOutput($(elem).parentNode, stepctx, node);
            $(elem).setAttribute('data-bound', 'true');
        }
    },
    addNodeState: function (root, stepctx, node, nstate) {
        var nstates = $(root).down('.wfstepstate[data-stepctx=' + stepctx + '] .nodestates');
        var newstate = this.newNodeState(stepctx, node, nstate);
        nstates.appendChild(newstate);
        var execstate = newstate.down('.execstate');
        this.setNodeState(stepctx, node, nstate, execstate);
    },
    updateNodeState: function (root, stepctx, node, nstate) {
        this.flow.withOrWithoutMatch(root, '.execstate.isnode[data-stepctx=' + stepctx + '][data-node=' + node + ']',
            this.setNodeState.bind(this).curry(stepctx, node, nstate),
            this.addNodeState.bind(this).curry(root, stepctx, node, nstate)
        );

        if (nstate.errorMessage) {
            this.flow.withMatch(root, '.errmsg.isnode[data-stepctx=' + stepctx + '][data-node=' + node + ']', function (elem) {
                $(elem).innerHTML = nstate.errorMessage;
                $(elem).show();
            });
        }
    },
    updateWorkflow: function (currentwf, ctx) {
        var count = parseInt(currentwf.stepCount);
        if (!currentwf.steps) {
            return;
        }
        for (var i = 1; i <= count; i++) {
            var step = currentwf.steps[i - 1];
            var stepctx = ctx + (ctx ? '/' : '') + i;
            this.updateStepState(this.targetElement, stepctx, step);

            if (step.hasSubworkflow) {
                this.updateWorkflow(step.workflow, stepctx);
            } else if (step.nodeStep) {
                var nodeset = step.stepTargetNodes ? step.stepTargetNodes : currentwf.targetNodes;
                for (var n = 0; n < nodeset.length; n++) {
                    var node = nodeset[n];
                    var nstate = step.nodeStates ? step.nodeStates[node] : null;
                    if (!nstate) {
                        nstate = { executionState: 'WAITING' };
                    }
                    this.updateNodeState(this.targetElement, stepctx, node, nstate);
                }
            }
        }
    },
    updateState: function(model){
        this.updateWorkflow(model,'');
    }
});
var NodeFlow=Class.create({
    flow: null,
    targetElement: null,
    initialize: function (flow, targetElement, params) {
        this.flow = flow;
        this.targetElement = targetElement;
        Object.extend(this, params);
    },
    withOverallNodeStateElem: function(node,func,wofunc){
        this.flow.withOrWithoutMatch(this.targetElement, '.wfnodestate[data-node=' + node + '] [data-template=overall]',
            func,
            wofunc
        );
    },
    withStepNodeStateElem: function(node,stepctx,func,wofunc){
        this.flow.withOrWithoutMatch(this.targetElement, '.wfnodestate[data-node=' + node + '] [data-template=step][data-stepctx='+stepctx+']',
            func,
            wofunc
        );
    },
    stepStateForCtx: function(model,stepctx){
        var a,b;
        var s = stepctx.indexOf("/");
        if (s > 0) {
            a = stepctx.substr(0, s);
            b = stepctx.substr(s + 1);
        }else{
            a=stepctx;
            b=null;
        }
        var ndx=parseInt(a)-1;
        var step = model.steps[ndx];
        if(b && step.workflow){
            return this.stepStateForCtx(step.workflow,b);
        }else{
            return step;
        }
    },
    updateNodeRowForStep: function(node,stepctx,step,elem){
        var data={nodename:node,stepctx:stepctx};
        var type = this.flow.workflow.contextType(stepctx);
        data['type']=type;
        data['stepident'] =  this.flow.workflow.renderContextString(stepctx);
        data['substepctx']=stepctx.indexOf("/")>0?stepctx.substring(0,stepctx.lastIndexOf("/")+1):'';
        data['mainstepctx']= stepctx.indexOf("/") > 0 ? stepctx.substring(stepctx.lastIndexOf("/")+1) : stepctx;
        if(step.endTime && step.startTime){
            data['duration']=moment.duration(moment(step.endTime).diff(moment(step.startTime))).humanize();
//            data['duration']=moment(step.endTime).from(moment(step.startTime));
        }
        Object.extend(data,step);
        this.flow.bindDom(elem,data);
    },
    /**
     * Return true if state B should be used instead of a
     * @param a
     * @param b
     */
    stateCompare:function(a,b){
        if(a==b){
            return false;
        }
        var states=['SUCCEEDED', 'WAITING', 'FAILED','ABORTED','RUNNING','RUNNING_HANDLER'];
        var ca = states.indexOf(a);
        var cb = states.indexOf(b);
        if(ca<0){
            return true;
        }
        return cb>ca;
    },
    updateNodeState: function(model,node,nodestate){
        var lastFound=null;
        var lastFoundCtx=null;
        var count = nodestate.length;
        for (var i = 0; i < count; i++) {
            var stepstate = nodestate[i];
            var stepStateForCtx = this.stepStateForCtx(model, stepstate.stepctx);
            var found = stepStateForCtx.nodeStates[node];
            this.withStepNodeStateElem(node, stepstate.stepctx,
                this.updateNodeRowForStep.bind(this).curry(node, stepstate.stepctx, found),
                null);
            if(!lastFound || this.stateCompare(lastFound.executionState, found.executionState)){
                lastFound=found;
                lastFoundCtx= stepstate.stepctx;
            }
        }
        this.withOverallNodeStateElem(node, this.updateNodeRowForStep.bind(this).curry(node, lastFoundCtx, lastFound), null);
    },
    updateNodes: function(model){
        if(!model.nodes || !model.allNodes){
            return;
        }
        var count = model.allNodes.length;
        for (var i = 0; i < count; i++) {
            var node = model.allNodes[i];
            this.updateNodeState(model,node, model.nodes[node]);
        }
    },
    updateState: function (model) {
        this.updateNodes(model);
    }
});
/**
 * State of workflow, step oriented
 */
var FlowState = Class.create({
    model:{},
    executionId:null,
    targetElement:null,
    loadUrl:null,
    outputUrl:null,
    shouldUpdate:false,
    timer:null,
    selectedElem:null,
    reloadInterval:3000,
    updaters:null,
    initialize: function (eid, elem, params) {
        this.executionId = eid;
        this.targetElement = elem;
        Object.extend(this, params);
    },
    withOrWithoutMatch: function (root, selector, func, wofunc) {
        var elem = $(root).down(selector);
        if (elem && null!=func && typeof(func)=='function') {
            func(elem);
        } else if(null!=wofunc && typeof(wofunc=='function')){
            wofunc();
        }
    },
    withoutMatch: function (root, selector, func) {
        this.withOrWithoutMatch(root, selector, null,func);
    },
    withMatch: function (root, selector, func) {
        this.withOrWithoutMatch(root,selector,func,
            null//this.logWarn.bind(this).curry("No match " + selector)
        );
    },
    /**
     * Binds data values from an object to various parts of the dom.
     * @param elem
     * @param data
     */
    bindDom: function (elem, data) {
        $(elem).select('[data-bind]').each(function (e) {
            var attr = $(e).getAttribute('data-bind');
            var val= data[attr];
            var format = $(e).hasAttribute('data-bind-format')?$(e).getAttribute('data-bind-format'):null;
            if(format){
                var s = format.indexOf(":");
                if(s>0){
                    var a=format.substr(0,s);
                    var b=format.substr(s+1);
                    if(a=='moment'){
                        var time = moment(val);
                        if(time.isValid()){
                            val = time.format(b);
                        }else{
                            val='';
                        }
                    }
                }
            }
            $(e).innerHTML = val;
        });
        $(elem).select('[data-bind-class]').each(function (e) {
            var classname = $(e).getAttribute('data-bind-class');
            var val = data[classname];
            if ($(e).hasAttribute('data-bound-class')) {
                var boundClass = $(e).getAttribute('data-bound-class');
                $(e).removeClassName(boundClass);
            } else {
                $(e).setAttribute('data-bound-class', val);
            }
            $(e).addClassName(val);
        });
        $(elem).select('[data-bind-attr]').each(function (e) {
            var val = $(e).getAttribute('data-bind-attr');
            var arr = val.split(",");
            for (var x = 0; x < arr.length; x++) {
                var s = arr[x].indexOf(":");
                if (s > 0) {
                    var a = arr[x].substr(0, s);
                    var b = arr[x].substr(s + 1);
                    $(e).setAttribute(a, data[b]);
                }
            }
        });
    },
    updateOutput: function (elem, data) {
        $(elem).innerHTML = '';
        for (var i = 0; i < data.entries.length; i++) {
            $(elem).innerHTML += data.entries[i].log + '\n';
        }
    },
    showOutput: function (elem, targetElement,stepctx, node, evt) {
        if ($(targetElement)) {
            if (this.selectedElem) {
                $(this.selectedElem).removeClassName('active');
            }
            $(elem).addClassName('active');
            this.selectedElem = elem;
            var state = this;
            new Ajax.Request(this.outputUrl,
                {
                    parameters: {nodename: node, stepctx: stepctx},
                    onSuccess: function (transport) {
                        var data = transport.responseJSON;
                        state.updateOutput(targetElement, data);
                    }
                }
            )
        }
    },
    logWarn: function (text) {
        if ($(this.targetElement + '_log')) {
            $(this.targetElement + '_log').innerHTML += "<br>" + text;
        }
    },
    showError: function(text){
        this.logWarn(text);
    },
    updateState: function(model){
        if(this.updaters){
            for(var i=0;i<this.updaters.length;i++){
                this.updaters[i].updateState(model);
            }
        }
    },
    update: function (data) {
        //compare
        if(data.error){
            this.showError(data.error);
            return;
        }
        this.model = data;
        if($(this.targetElement + '_json')){
            $(this.targetElement + '_json').innerHTML = Object.toJSON(this.model);
        }
        this.updateState(this.model);
        if (!this.model.completed && this.shouldUpdate) {
            this.timer = setTimeout(this.callUpdate.bind(this), this.reloadInterval);
        } else {
            this.stopFollowing();
        }
    },
    callUpdate: function(){
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
        this.callUpdate();
    },
    stopFollowing: function(){
        this.shouldUpdate=false;
        clearTimeout(this.timer);
        this.timer = null;
    },
    addUpdater: function(updater){
        if(null==this.updaters){
            this.updaters = [];
        }
        this.updaters.push( updater);
    }
});
