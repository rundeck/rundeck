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
        this.flow.bindNodeOutput(div, this.targetElement + '_output',stepctx, node);
        return div;
    },
    setNodeState: function (stepctx, node, nstate, elem) {
        $(elem).innerHTML = node;
        $(elem).setAttribute('data-execstate', nstate.executionState);
        if ($(elem).getAttribute('data-bound') != 'true') {
            this.flow.bindNodeOutput($(elem).parentNode, this.targetElement + '_output', stepctx, node);
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
    actions:null,
    initialize: function (flow, targetElement, params) {
        this.flow = flow;
        this.targetElement = targetElement;
        Object.extend(this, params);
    },
    withOverallNodeStateElem: function(node,func,wofunc){
        this.flow.withOrWithoutMatch(this.targetElement, '.wfnodestate[data-node=' + node + '] .wfnodeoverall',
            func,
            wofunc
        );
    },
    withStepNodeStateElem: function(node,stepctx,func,wofunc){
        this.flow.withOrWithoutMatch(this.targetElement, '.wfnodestate[data-node=' + node + '] .wfnodestep[data-stepctx='+stepctx+']',
            func,
            wofunc
        );
    },
    /**
     * find first child of parent that has a greater stepctx that the given one
     * @param parent
     * @param stepctx
     */
    findNodeRowStepPeer: function (parent, stepctx) {
        var me = this;
        return $(parent).select('.wfnodestep[data-stepctx]').detect(function (e) {
            var ctx2 = $(e).getAttribute('data-stepctx');
            var compareStepCtx = me.flow.compareStepCtx(stepctx, ctx2);
            return compareStepCtx<0;
        });
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
    zeroNumber:function(num){
        if(num<10){
            return '0'+num;
        }
        return num;
    },
    durationData: function(duration){
        var data={};
        data['duration_moment'] = duration;
        data['duration_ms'] = duration.asMilliseconds();
        data['duration_humanized'] = duration.humanize();
        data['duration'] = duration.hours() + '.' + this.zeroNumber(duration.minutes()) + ':' + this.zeroNumber(duration.seconds());
//            data['duration']=moment(step.endTime).from(moment(step.startTime));
        return data;
    },
    dataForNodeRowForStep: function(node,stepctx,step){
        var data={nodename:node,stepctx:stepctx};
        var type =null;
        if(stepctx){
            type= this.flow.workflow.contextType(stepctx);
            data['type']=type;
            data['stepident'] =  this.flow.workflow.renderContextString(stepctx);
            data['substepctx']=stepctx.indexOf("/")>0?stepctx.substring(0,stepctx.lastIndexOf("/")+1):'';
            data['mainstepctx']= stepctx.indexOf("/") > 0 ? stepctx.substring(stepctx.lastIndexOf("/")+1) : stepctx;
            data['stepctxdesc']="Workflow Step: "+stepctx;
        }
        if(step.endTime && step.startTime){
            var duration = moment.duration(moment(step.endTime).diff(moment(step.startTime)));
            Object.extend(data,this.durationData(duration));
        }
        Object.extend(data, step);
        return data;
    },

    updateNodeRowForStep: function (node, stepctx, data, elem) {
        this.flow.bindDom(elem,data,this);
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
    showOutputForNodeStep:function(elem,stepctx,node,data){
        var me=this;
        var sel = '.wfnodeoutput[data-node=' + node + ']';
        if (stepctx) {
            sel += '[data-stepctx=' + stepctx + ']';
        } else {
            sel += '[data-stepctx=]';
        }
        var output = $(me.targetElement).down(sel);
        $(me.targetElement).select('.wfnodeoutput').each(Element.hide);
        $(me.targetElement).select('.wfnodestep').each(function(elem){
            $(elem).removeClassName('open');
        });
        if (me.flow.selectedElem == elem) {
            if (!stepctx) {
                //node view
                $(output).up('.wfnodestate').select('.wfnodesteps').each(Element.show);
            }
            //finish follow for elem.
            me.flow.stopShowingOutput();
            return false;
        } else {
            var ctrl = me.flow.showOutput(elem, output, stepctx, node);
            $(output).show();
            if (me.flow.selectedOutputStatusId) {
                me.updateNodeRowForStep(node, stepctx, data, me.flow.selectedOutputStatusId);
            }
            if (!stepctx) {
                //node view
                $(output).up('.wfnodestate').select('.wfnodesteps').each(Element.hide);
            }
            return true;
        }
    },
    pluralize:function(count,singular,plural){
        return count==1?singular:null!=plural?plural:(singular+'s');
    },
    summarizeNodeData: function(summarydata){
        if(summarydata.total>0){
            if(summarydata.RUNNING>0){
                summarydata.summary= "Running";
                summarydata.summaryState= "RUNNING";
            }else if(summarydata.RUNNING_HANDLER>0){
                summarydata.summary= "Running";
                summarydata.summaryState= "RUNNING_HANDLER";
            }else if(summarydata.total==summarydata.SUCCEEDED){
                summarydata.summary= "All Steps OK";
                summarydata.summaryState= "SUCCEEDED";
            }else if(summarydata.FAILED>0){
                summarydata.summary = summarydata.FAILED + " "+this.pluralize(summarydata.FAILED,"Step")+" FAILED";
                summarydata.summaryState = "FAILED";
            }else if(summarydata.WAITING>0){
                summarydata.summary = "Waiting to run "+ summarydata.WAITING+" "+ this.pluralize(summarydata.WAITING, "Step");
                summarydata.summaryState = "WAITING"
            } else if (summarydata.NOT_STARTED == summarydata.total) {
                summarydata.summary = "No steps were run";
                summarydata.summaryState = "NOT_STARTED";
            } else if(summarydata.NOT_STARTED>0){
                summarydata.summary = summarydata.NOT_STARTED+" "+ this.pluralize(summarydata.NOT_STARTED, "Step")+" not run";
                summarydata.summaryState = "NOT_STARTED";
            }else if (summarydata.SUCCEEDED > 0) {
                summarydata.summary = (summarydata.total- summarydata.SUCCEEDED) +" did not succeed" ;
                summarydata.summaryState = "PARTIAL_SUCCEEDED";
            }else{
                summarydata.summary = "No steps succeeded";
                summarydata.summaryState = "NONE_SUCCEEDED";
            }
        }else{
            summarydata.summary="No steps";
            summarydata.summaryState="NONE";
        }
        if (summarydata.duration_ms_total) {
            var duration = moment.duration(summarydata.duration_ms_total);
            Object.extend(summarydata, this.durationData(duration));
        }
        return summarydata;
    },
    updateNodeState: function(model,node,nodestate){
        var lastFound=null;
        var lastFoundCtx=null;
        var count = nodestate.length;
        var me=this;
        this.withOverallNodeStateElem(node, null, function(){
            //create node section
            //clonetemplate
            var clone = me.flow.template(me.targetElement,'node');
            me.flow.bindDom(clone,{nodename:node},me);
            $(clone).show();
        });
        var summarydata={
            total:count,
            SUCCEEDED:0,
            FAILED:0,
            WAITING:0,
            NOT_STARTED:0,
            RUNNING:0,
            RUNNING_HANDLER:0,
            other:0,
            duration_ms_total:0
        }
        for (var i = 0; i < count; i++) {
            var stepstate = nodestate[i];
            var stepStateForCtx = this.stepStateForCtx(model, stepstate.stepctx);
            var found = stepStateForCtx.nodeStates[node];
            found= me.dataForNodeRowForStep(node, stepstate.stepctx, found);
            ['SUCCEEDED','FAILED','WAITING','NOT_STARTED','RUNNING','RUNNING_HANDLER'].each(function(z){
                if (null!=summarydata[z] && z==found.executionState) {
                    summarydata[z]++;
                } else{
                    summarydata['other']++;
                }
            });
            summarydata.duration_ms_total+=found.duration_ms? found.duration_ms:0;

            this.withStepNodeStateElem(node, stepstate.stepctx,
                function(elem){
                    me.updateNodeRowForStep(node, stepstate.stepctx, found,elem);
                },
                function(){
                    //clonetemplate
                    var clone = me.flow.template(me.targetElement,'step');
                    if(!clone){
                        return;
                    }
                    //remove from automatic attached parent
                    clone.parentNode.removeChild(clone);
                    me.withOverallNodeStateElem(node,  function (overall) {
                        var loc=$(overall).parentNode.down('.wfnodesteps');
                        var peer = me.findNodeRowStepPeer(loc,stepstate.stepctx);
                        if(!peer){
                            $(loc).appendChild(clone);
                        }else{
                            $(peer).insert({before:clone});
                        }
                        me.updateNodeRowForStep(node,stepstate.stepctx,found,clone);
                        $(clone).show();
                    });
                });
            if(!lastFound || this.stateCompare(lastFound.executionState, found.executionState)){
                lastFound=found;
                lastFoundCtx= stepstate.stepctx;
            }
        }
        //define summary data
        var summary=Object.extend(lastFound,this.summarizeNodeData(summarydata));
        this.withOverallNodeStateElem(node, this.updateNodeRowForStep.bind(this).curry(node, lastFoundCtx, summary), null);
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
    selectedOutputStatusId:null,
    targetElement:null,
    loadUrl:null,
    outputUrl:null,
    shouldUpdate:false,
    timer:null,
    selectedElem:null,
    selectedFollowControl:null,
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
            return func(elem);
        } else if(!elem && null!=wofunc && typeof(wofunc=='function')){
            return wofunc();
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
    splitFirst: function(str,char){
        var s = str.indexOf(char);
        if (s > 0) {
            var a = str.substr(0, s);
            var b = str.substr(s + 1);
            return [a,b];
        }else{
            return [str];
        }
    },
    bindResolveData: function(data,key){
        var valarr = this.splitFirst(key,'.');
        if(valarr.length>1){
            var result=data[valarr[0]];
            if(typeof(result)=='object'){
                return this.bindResolveData(result,valarr[1]);
            }else{
                return null;
            }
        }else{
            return data[key]
        }
    },
    /**
     * Bind data to an element via the 'data-bind' attribute
     * @param data
     * @param e
     */
    bindElemData: function(data,e){
        if(!$(e).hasAttribute('data-bind')){
            return;
        }
        var attr = $(e).getAttribute('data-bind');
        var s = attr.split(',');
        for(var x=0;x< s.length;x++){
            var t=s[x];
            var valarr=this.splitFirst(t,":");
            var val = this.bindResolveData(data,valarr.length>1?valarr[1]:valarr[0]);
            var format = $(e).hasAttribute('data-bind-format') ? $(e).getAttribute('data-bind-format') : null;
            if (format) {
                var s2 = this.splitFirst(format, ":");
                if (s2.length > 1) {
                    var a = s2[0];
                    var b = s2[1];
                    if (a == 'moment' && typeof(moment)=='function') {
                        var time = moment(val);
                        if (time.isValid()) {
                            val = time.format(b);
                        } else {
                            val = '';
                        }
                    }
                }
            }
            var target=valarr.length>1?valarr[0]:'html';
            var stringval= val;
            if(typeof(val) == 'undefined'){
                stringval='';
            }else if(typeof(val)=='object'){
                stringval=Object.toJSON(val);
            }
            if(target=='title'){
                $(e).setAttribute('title', stringval);
            }else if(target=='template' && typeof(val)=='object'){
                //apply each property as key/value to the template
                var templ = $(e).hasAttribute('data-bind-template') ? $(e).getAttribute('data-bind-template') : null;
                if(!templ){
                    return;
                }
                for(var k in val){
                    var clone=this.template($(e),templ);
                    this.bindDom(clone,{key:k,value:val[k]},null);
                    $(clone).show();
                }
            }else{
                $(e).innerHTML = stringval;
            }
        }

    },
    /**
     * Bind class attribute from the data based on the 'data-bind-class' attribute
     * @param data
     * @param e
     */
    bindElemClass: function (data,e) {
        if(!$(e).hasAttribute('data-bind-class')){
            return;
        }
        var classname = $(e).getAttribute('data-bind-class');
        var val = data[classname];
        if (typeof(val) == 'undefined') {
            return;
        }
        if ($(e).hasAttribute('data-bound-class')) {
            var boundClass = $(e).getAttribute('data-bound-class');
            boundClass.split(' ').each(function(val){
                $(e).removeClassName(val);
            });
        }
        $(e).setAttribute('data-bound-class', val);
        $(e).addClassName(val);
    },
    bindElemAttr: function (data,e) {
        if(!$(e).hasAttribute('data-bind-attr')){
            return;
        }
        var val = $(e).getAttribute('data-bind-attr');
        var arr = val.split(",");
        for (var x = 0; x < arr.length; x++) {
            var s = arr[x].indexOf(":");
            if (s > 0) {
                var a = arr[x].substr(0, s);
                var b = arr[x].substr(s + 1);
                if (typeof(data[b]) != 'undefined') {
                    $(e).setAttribute(a, data[b]);
                }
            }
        }
    },
    bindElemAction: function (data,parent,e) {
        if(!$(e).hasAttribute('data-bind-action')){
            return;
        }
        var val = $(e).getAttribute('data-bind-action');
        var action = parent.actions ? parent.actions[val] : null;
        if (!action) {
            return;
        }
        if (typeof(action) == 'function') {
            //bind action on node name
            if ($(e).getAttribute('data-bound-click') != 'true') {
                Event.observe(e, 'click', function (evt) {
                    action(parent,e,data);
                });
                $(e).setAttribute('data-bound-click','true')
            }
        }
    },
    /**
     * Binds data values from an object to various parts of the dom.
     * @param elem
     * @param data
     * @param parent parent object
     */
    bindDom: function (elem, data,parent) {
        this.bindElemData(data,elem);
        this.bindElemClass(data,elem);
        this.bindElemAttr(data,elem);
        if(parent){
        this.bindElemAction(data,parent,elem);
        }
        $(elem).select('[data-bind]').each(this.bindElemData.bind(this).curry(data));
        $(elem).select('[data-bind-class]').each(this.bindElemClass.bind(this).curry(data));
        $(elem).select('[data-bind-attr]').each(this.bindElemAttr.bind(this).curry(data));
        if (parent) {
        $(elem).select('[data-bind-action]').each(this.bindElemAction.bind(this).curry(data,parent));
        }
    },
    /**
     * Find a template node within the given node and return it
     * @param elem target node containing the template
     * @param templ name of the template node
     * @returns template node, or null
     */
    templateNode: function(elem,templ){
        return this.withOrWithoutMatch(elem, '[data-template='+templ+']',
            function (elem) {
                return elem;
            },
            null
        );
    },
    /**
     * Find the parent to attache a cloned template for the given template name
     * @param elem target node containing the template
     * @param templ name of the template
     * @returns found node, or the parent node of the template node
     */
    templateParent: function(elem,templ){
        var parent=$(elem).up('[data-template-parent='+templ+']');
        return parent?parent:$(elem).parentNode;
    },
    /**
     * Create a clone of a template node, which has data-template=(template name), and attach it to the discovered parent node, which is
     * either the immediate parentNode, or an ancestor node with data-template-parent=(template name).
     * @param e target node containing the template node
     * @param templ name of the template
     * @returns cloned node, or null if the template was not found
     */
    template: function(e,templ){
        var elem=this.templateNode(e,templ);
        if(!elem){
            return null;
        }
        var clone = $(elem).clone(true);
        clone.removeAttribute('data-template');
        var parent = this.templateParent(elem, templ);
        $(parent).appendChild(clone);
        return clone;
    },

    bindNodeOutput: function (elem, targetElement,stepctx, node) {
        Event.observe(elem, 'click', this.showOutput.bind(this).curry(elem, targetElement, stepctx, node));
    },
    compareStepCtx: function(ctx1,ctx2){
        var s1=ctx1.split('/');
        var s2=ctx2.split('/');
        var x;
        for (x = 0; x < s1.length && x < s2.length; x++) {
            var i1=s1.indexOf('e')>0?s1.substring(0,s1.length-2):s1;
            var i2=s2.indexOf('e')>0?s2.substring(0,s2.length-2):s2;
            var int1=parseInt(i1);
            var int2=parseInt(i2);
            if(int1!=int2){
                return int1 < int2 ? -1 : 1;
            }
        }
        if(s1.length!=s2.length){
            return s1.length<s2.length?-1:1;
        }
        return 0;
    },
    stopShowingOutput: function () {
        if (this.selectedElem) {
            $(this.selectedElem).removeClassName('active');
            this.selectedElem=null;
        }
        if (this.selectedFollowControl) {
            $(this.selectedFollowControl).stopFollowingOutput();
            this.selectedFollowControl=null;
        }
        if (this.selectedOutputStatusId) {
            $(this.selectedOutputStatusId).hide();
        }
    },
    showOutput: function (elem, targetElement,stepctx, node, evt) {
        if (!$(targetElement)) {
            return null;
        }
        this.stopShowingOutput();
        $(elem).addClassName('active');
        this.selectedElem = elem;
        var state = this;
        var params= {nodename: node};
        if(stepctx){
            Object.extend(params,{stepctx:stepctx});
        }
        var ctrl = new FollowControl(null,null,{
            parentElement:targetElement,
            extraParams:'&'+Object.toQueryString(params),
            appLinks:{tailExecutionOutput:this.outputUrl},
            finishedExecutionAction:false
        });
        ctrl.workflow = this.workflow;
        ctrl.setColNode(false);
        ctrl.setColStep(null==stepctx);
        ctrl.beginFollowingOutput();
        this.selectedFollowControl=ctrl;
        if(this.selectedOutputStatusId){
            $(this.selectedOutputStatusId).show();
        }
        return ctrl;
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
