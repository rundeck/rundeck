//= require momentutil
//= require knockout.min
//= require knockout-mapping
/*
 Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/**
 * Represents a (node, stepctx) state.
 * @param data state data object
 * @param node RDNode object
 * @param flow NodeFlowViewModel object
 * @constructor
 */
function RDNodeStep(data, node, flow){
    var self = this;
    self.node = node;
    self.flow = flow;
    self.stepctx = data.stepctx;
    self.type = flow.workflow.contextType(data.stepctx);
    self.stepident = flow.workflow.renderContextString(data.stepctx);
    self.stepctxdesc = "Workflow Step: " + data.stepctx;
    self.parameters = ko.observable(data.parameters || null);
    self.followingOutput = ko.observable(false);
    self.outputLineCount = ko.observable(-1);
    self.startTime = ko.observable(data.startTime || null);
    self.updateTime = ko.observable(data.updateTime || null);
    self.endTime = ko.observable(data.endTime || null);
    self.duration = ko.observable(data.duration || -1);
    self.executionState = ko.observable(data.executionState || null);
    self.parameterizedStep = ko.observable(data.stepctx.indexOf('@')>=0);
    self.startTimeSimple=ko.pureComputed(function(){
        return MomentUtil.formatTimeSimple(self.startTime());
    });
    self.startTimeFormat=function(format){
        return MomentUtil.formatTime(self.startTime(),format);
    };
    self.endTimeSimple=ko.pureComputed(function(){
        return MomentUtil.formatTimeSimple(self.endTime());
    });
    self.endTimeFormat= function (format) {
        return MomentUtil.formatTime(self.endTime(), format);
    };
    self.durationCalc=ko.pureComputed(function(){
        var dur=self.duration();
        if(dur<0 && self.endTime() && self.startTime()){
            return moment(self.endTime()).diff(moment(self.startTime()));
        }else{
            return dur;
        }
    });
    self.durationSimple=ko.pureComputed(function(){
        return MomentUtil.formatDurationSimple(self.durationCalc());
    });
}
/**
 * Return true if state B should be used instead of a
 * @param a
 * @param b
 */
RDNodeStep.stateCompare= function (a, b) {
    if (a == b) {
        return false;
    }
    var states = ['SUCCEEDED', 'NONE','NOT_STARTED', 'WAITING', 'FAILED', 'ABORTED', 'RUNNING', 'RUNNING_HANDLER'];
    var ca = states.indexOf(a);
    var cb = states.indexOf(b);
    if (ca < 0) {
        return true;
    }
    return cb > ca;
};
RDNodeStep.completedStates= ['SUCCEEDED', 'FAILED', 'ABORTED', 'NONE_SUCCEEDED','PARTIAL_SUCCEEDED'];
RDNodeStep.runningStates= ['RUNNING','RUNNING_HANDLER'];

function RDNode(name, steps,flow){
    var self=this;
    self.flow= flow;
    self.name=name;
    self.state=ko.observable();
    self.steps=ko.observableArray([]).extend({ rateLimit: 200 });
    self.expanded=ko.observable(false);
    var mapping = {
        'steps': {
            key: function (data) {
                return ko.utils.unwrapObservable(data.stepctx);
            },
            create: function (options) {
                return new RDNodeStep(options.data, self, flow);
            }
        }
    };
    self.durationMs=ko.observable(-1);
    self.toggleExpand=function(){
        self.expanded(!self.expanded());
        if(self.expanded()){
            flow.selectedNodes.push(self.name);
            flow.loadStateForNode(self);
        }else {
            flow.selectedNodes.remove(self.name);
        }
    };
    self.duration=ko.pureComputed(function(){
        //sum up duration of all completed steps
       var ms=self.durationMs();
        if(ms<0) {
            ko.utils.arrayForEach(self.steps(), function (x) {
                if (!x.parameterizedStep()) {
                    var ms2 = x.duration();
                    if (ms2 >= 0 && ms < 0) {
                        ms = ms2;
                    } else if (ms2 >= 0) {
                        ms += ms2;
                    }
                }
            });
        }
       return ms;
    });
    self.durationSimple=ko.pureComputed(function(){
       return MomentUtil.formatDurationSimple(self.duration());
    });
    self.summary=ko.observable();
    self.summaryState=ko.observable();
    self.currentStep=ko.observable();
    //date string indicating last updated
    self.lastUpdated=ko.observable(null);
    self.findStepForCtx=function(stepctx){

        var found=ko.utils.arrayFilter(self.steps(),function(e){
            return e.stepctx==stepctx;
        });
        if(found && found.length==1){
            return found[0];
        }
        return null;
    };
    self.summaryDescriptionForState=function(data){
        var state = data.summaryState;
        if (state=='RUNNING') {
            return "Running";
        } else if (state=='RUNNING_HANDLER') {
            return ("Running");
        } else if (state=='SUCCEEDED') {
            return ("All Steps OK");
        } else if (state=='FAILED') {
            if(data.FAILED){
                return (data.FAILED + " " + flow.pluralize(data.FAILED, "Step") + " FAILED");
            }
            return ("Failed");
        } else if (state=='WAITING' && data.WAITING) {
            return("Waiting to run " + (data.WAITING||"some") + " " + flow.pluralize(data.WAITING, "Step"));
        } else if (state=='NOT_STARTED') {
            return("No steps were run");
        } else if (state=='PARTIAL_NOT_STARTED') {
            return((data.PARTIAL_NOT_STARTED||"Some") + " " + flow.pluralize(data.PARTIAL_NOT_STARTED, "Step") + " not run");
        }else if(state=='WAITING' ){
            return("Waiting");
        } else if (state=='PARTIAL_SUCCEEDED') {
            return ((data.PARTIAL_SUCCEEDED||"Some") + " did not succeed");
        } else if(state=='NONE_SUCCEEDED'){
            return("No steps succeeded");
        } else if(state=='NONE') {
          return("No Steps");
        }
        return null;
    };
    /**
     * Determine summary data for the node, sets the summaryState and summary and currentStep
     */
    self.summarize=function(){
        var currentStep=null;

        //step summary info
        var summarydata = {
            total: 0,
            SUCCEEDED: 0,
            FAILED: 0,
            WAITING: 0,
            NOT_STARTED: 0,
            RUNNING: 0,
            RUNNING_HANDLER: 0,
            other: 0,
            duration_ms_total: 0,
            pending: self.flow.pendingStepsForNode(self.name)
        };

        var testStates= ['SUCCEEDED', 'FAILED', 'WAITING', 'NOT_STARTED', 'RUNNING', 'RUNNING_HANDLER'];
        //determine count for step states
        ko.utils.arrayForEach(self.steps(),function(step){
            var z = step.executionState();

            if(!step.parameterizedStep()) {
                summarydata.total++;
                if (testStates.indexOf(z) >= 0 && null != summarydata[z]) {
                    summarydata[z]++;
                } else {
                    summarydata['other']++;
                }
            }
            if (!currentStep && RDNodeStep.stateCompare('NONE', step.executionState())
                || currentStep && RDNodeStep.stateCompare(currentStep.executionState(), step.executionState())) {
                currentStep = step;
            }
        });
        self.currentStep(currentStep);

        //based on step states set the summary for this node
        if (summarydata.total > 0) {
            if (summarydata.RUNNING > 0) {
                self.summary("Running");
                self.summaryState("RUNNING");
            } else if (summarydata.RUNNING_HANDLER > 0) {
                self.summary("Running");
                self.summaryState("RUNNING_HANDLER");
            } else if (summarydata.total == summarydata.SUCCEEDED && summarydata.pending < 1) {
                self.summary("All Steps OK");
                self.summaryState("SUCCEEDED");
            } else if (summarydata.FAILED > 0) {
                self.summary(summarydata.FAILED + " " + flow.pluralize(summarydata.FAILED, "Step") + " FAILED");
                self.summaryState("FAILED");
            } else if (summarydata.WAITING > 0) {
                self.summary("Waiting to run " + summarydata.WAITING + " " + flow.pluralize(summarydata.WAITING, "Step"));
                self.summaryState("WAITING");
            } else if (summarydata.NOT_STARTED == summarydata.total && summarydata.pending < 1) {
                self.summary("No steps were run");
                self.summaryState("NOT_STARTED");
            } else if (summarydata.NOT_STARTED > 0) {
                self.summary(summarydata.NOT_STARTED + " " + flow.pluralize(summarydata.NOT_STARTED, "Step") + " not run");
                self.summaryState("PARTIAL_NOT_STARTED");
            }else if(summarydata.pending > 0 ){
                self.summary("Waiting");
                self.summaryState("WAITING");
            } else if (summarydata.SUCCEEDED > 0) {
                self.summary((summarydata.total - summarydata.SUCCEEDED) + " did not succeed");
                self.summaryState("PARTIAL_SUCCEEDED");
            } else {
                self.summary("No steps succeeded");
                self.summaryState("NONE_SUCCEEDED");
            }
        } else if(summarydata.pending > 0){
            self.summary("Waiting");
            self.summaryState("WAITING");
        } else {
            self.summary("No Steps");
            self.summaryState("NONE");
        }
    };
    self.currentStepFromData=function(data){
        self.currentStep(new RDNodeStep(data, self, self.flow));
    };
    self.updateSummary=function(nodesummary){
        if(nodesummary.lastUpdated && self.lastUpdated()==nodesummary.lastUpdated
            && nodesummary.summaryState==self.summaryState()){
            return;
        }
        self.lastUpdated(nodesummary.lastUpdated);
        self.summaryState(nodesummary.summaryState);
        self.summary(self.summaryDescriptionForState(nodesummary));

        self.durationMs(nodesummary.duration);
        if(nodesummary.currentStep){
            self.currentStepFromData(nodesummary.currentStep);
        }else{
            self.currentStep(null);
        }
    };
    /**
     * Update from direct ajax response
     * @param steps
     */
    self.loadData=function(data){
        self.updateSummary(data.summary);
        self.updateSteps(data.steps);
    };
    self.updateSteps=function(steps){
        ko.mapping.fromJS({steps: steps}, mapping, this);
        self.summarize();
    };
    if(steps){
        self.updateSteps(steps);
    }
}

function NodeFlowViewModel(workflow,outputUrl,nodeStateUpdateUrl){
    var self=this;
    self.workflow=workflow;
    self.errorMessage=ko.observable();
    self.statusMessage=ko.observable();
    self.stateLoaded=ko.observable(false);
    self.pendingNodeSteps=ko.observable({});
    self.nodes=ko.observableArray([ ]).extend({ rateLimit: 500 });
    self.selectedNodes=ko.observableArray([ ]);
    self.followingStep=ko.observable();
    self.followingControl=null;
    self.followOutputUrl= outputUrl;
    self.nodeStateUpdateUrl= nodeStateUpdateUrl;
    self.completed=ko.observable();
    self.executionState=ko.observable();
    self.executionStatusString=ko.observable();
    self.retryExecutionId=ko.observable();
    self.retryExecutionState=ko.observable();
    self.retryExecutionUrl=ko.observable();
    self.retryExecutionAttempt=ko.observable();
    self.retry=ko.observable();
    self.execDuration=ko.observable();
    self.jobAverageDuration=ko.observable();
    self.startTime=ko.observable();
    self.endTime=ko.observable();
    self.executionId=ko.observable();
    self.outputScrollOffset=0;
    self.activeTab=ko.observable("summary");
    self.failed=ko.pureComputed(function(){ return self.executionState()=='FAILED'; });
    self.totalSteps=ko.pureComputed(function(){ return self.workflow.workflow.length; });
    self.activeNodes=ko.pureComputed(function(){
        return ko.utils.arrayFilter(self.nodes(), function (n) {
            return n.summaryState() != 'NONE';
        });
    });
    self.totalNodeCount=ko.observable(0);
    self.nodeIndex={};
    self.totalNodes=ko.pureComputed(function(){
        var nodes = ko.utils.arrayFilter(self.nodes(),function(n){return n.summaryState()!='NONE';});
        return nodes?nodes.length:0;
    });
    self.jobPercentage=ko.pureComputed(function(){
        if(self.jobAverageDuration()>0){
            return 100*(self.execDuration()/self.jobAverageDuration());
        }else{
            return -1;
        }
    });
    self.jobPercentageFixed=ko.pureComputed(function(){
        var pct = self.jobPercentage();
        if(pct>=0){
            return pct.toFixed(0)
        }else{
            return '0';
        }
    });
    self.jobOverrunDuration=ko.pureComputed(function(){
        var jobAverageDuration = self.jobAverageDuration();
        var execDuration = self.execDuration();
        if(jobAverageDuration > 0 && execDuration > jobAverageDuration){
            return MomentUtil.formatDurationSimple(execDuration - jobAverageDuration);
        }else{
            return '';
        }
    });
    self.completedNodes=ko.computed(function(){
        var completed=new Array();
        ko.utils.arrayForEach(self.nodes(), function (n) {
            if(RDNodeStep.completedStates.indexOf(n.summaryState())>=0){
                completed.push(n);
            }
        });
        return completed;
    });
    self.succeededNodes=ko.computed(function(){
        var completed=new Array();
        ko.utils.arrayForEach(self.nodes(), function (n) {
            if(n.summaryState()=='SUCCEEDED'){
                completed.push(n);
            }
        });
        return completed;
    });

    self.runningNodes=ko.computed(function(){
        var completed=new Array();
        ko.utils.arrayForEach(self.nodes(), function (n) {
            if(RDNodeStep.runningStates.indexOf(n.summaryState())>=0){
                completed.push(n);
            }
        });
        return completed;
    });
    self.waitingNodes=ko.computed(function(){
        var completed=new Array();
        ko.utils.arrayForEach(self.nodes(), function (n) {
            if(n.summaryState()=='WAITING'){
                completed.push(n);
            }
        });
        return completed;
    });
    self.notstartedNodes=ko.computed(function(){
        var completed=new Array();
        ko.utils.arrayForEach(self.nodes(), function (n) {
            if(n.summaryState()=='NOT_STARTED'){
                completed.push(n);
            }
        });
        return completed;
    });
    self.partialNodes=ko.computed(function(){
        var completed=new Array();
        ko.utils.arrayForEach(self.nodes(), function (n) {
            if(n.summaryState() == 'PARTIAL_NOT_STARTED' || n.summaryState() == 'PARTIAL_SUCCEEDED'){
                completed.push(n);
            }
        });
        return completed;
    });
    self.failedNodes=ko.computed(function(){
        var completed=new Array();
        ko.utils.arrayForEach(self.nodes(), function (n) {
            if(n.summaryState()=='FAILED'){
                completed.push(n);
            }
        });
        return completed;
    });
    self.percentageFixed = function(a,b){
        return (b==0)?0:(100*(a/b)).toFixed(0);
    };

    self.stopShowingOutput= function () {
        if(self.followingControl){
            self.followingControl.stopFollowingOutput();
            self.followingControl=null;
        }
    };
    self.showOutput= function (nodestep) {
        var node=nodestep.node.name;
        var stepctx=nodestep.stepctx;
        var sel = '.wfnodeoutput[data-node="' + node + '"]';
        if (stepctx) {
            sel += '[data-stepctx=\'' + stepctx + '\']';
        } else {
            sel += '[data-stepctx=]';
        }
        var targetElement = jQuery.find(sel);
        if (!targetElement || targetElement.length!=1) {
            throw "could not find output area";
        }
        targetElement=targetElement[0];
        var params = {nodename: node};
        if (stepctx) {
            Object.extend(params, {stepctx: stepctx});
        }
        var ctrl = new FollowControl(null, null, {
            parentElement: targetElement,
            extraParams: '&' + Object.toQueryString(params),
            appLinks: {tailExecutionOutput: self.followOutputUrl},
            finishedExecutionAction: false,
            autoscroll:false,
            onAppend:function(){
                nodestep.outputLineCount(ctrl.getLineCount());
            }
        });
        ctrl.workflow = self.workflow;
        ctrl.setColNode(false);
        ctrl.setColStep(null == stepctx);
        ctrl.beginFollowingOutput();
        return ctrl;
    };

    self.toggleOutputForNodeStep = function (nodestep,callback) {
        self.stopShowingOutput();
        //toggle following output for selected nodestep
        var node = nodestep.node;
        var stepctx=RDWorkflow.cleanContextId(nodestep.stepctx);
        //
        var postload=function(){
            node.expanded(true);
            //find correct nodestep given the context string, strip of parameters
            var nodestep=node.findStepForCtx(stepctx);
            if(!nodestep){
                throw "failed to find step for context: "+stepctx +" and node: "+node.name;
            }
            nodestep.followingOutput(true);
            ko.utils.arrayForEach(self.nodes(), function (n) {
                ko.utils.arrayForEach(n.steps(), function (step) {
                    //disable following for every other nodestep
                    if(step != nodestep){
                        step.followingOutput(false);
                    }
                });
            });
            var ctrl = self.showOutput(nodestep);
            self.followingStep(nodestep);
            self.followingControl=ctrl;
            if(typeof(callback)=='function'){callback();}
        };
        if(!node.expanded() && !nodestep.followingOutput()){
            //need to load the step states for the node before showing the output
            return self.loadStateForNode(node).then(postload);
        }else if(!nodestep.followingOutput()){
            postload();
        }else{
            nodestep.followingOutput(false);
            self.followingStep(null);
        }

    };
    self.scrollTo= function (element,offx,offy) {
        var x = element.x ? element.x : element.offsetLeft,
            y = element.y ? element.y : element.offsetTop;
        window.scrollTo(x+(offx?offx:0), y+(offy?offy:0));
    };

    self.scrollToNodeStep=function(node,stepctx){
        var elem = $$('.wfnodestep[data-node="' + node + '"][data-stepctx="' + stepctx + '"]');
        if (elem) {
            jQuery('#tab_link_flow a').tab('show');
            //scroll to
            self.scrollTo($(elem[0]));
        }
    };
    self.scrollToNode=function(node){
        var elem = jQuery.find('.wfnodestate.container[data-node="' + node + '"]');
        if (elem) {
            jQuery('#tab_link_flow a').tab('show');
            //scroll to
            self.scrollTo(elem[0]);
        }
    };
    self.scrollToOutput=function(nodestep){
        if(!nodestep.followingOutput()){
            self.toggleOutputForNodeStep(nodestep,function(){
                self.scrollToNode(nodestep.node.name);
            });
        }else {
            self.scrollToNode(nodestep.node.name);
        }
    };

    self.pluralize = function (count, singular, plural) {
        return count == 1 ? singular : null != plural ? plural : (singular + 's');
    };
    self.stepStateForCtx=function (model, stepctx) {
        if(typeof(stepctx)=='string'){
            stepctx = RDWorkflow.parseContextId(stepctx);
        }
        var stepid = stepctx[0];

        var ndx = RDWorkflow.workflowIndexForContextId(stepid);
        var params = RDWorkflow.paramsForContextId(stepid);
        var step = model.steps[ndx];
        if(params && step.parameterStates && step.parameterStates[params]){
            step = step.parameterStates[params];
        }
        if (stepctx.length>1 && step.workflow) {
            return self.stepStateForCtx(step.workflow, stepctx.slice(1));
        } else {
            return step;
        }
    };
    self.pendingStepsForNode = function(node){
        var pendingData=self.pendingNodeSteps();
        if(pendingData[node] != null){
            return pendingData[node];
        }else if(pendingData['_other'] != null){
            return pendingData['_other'];
        }else{
            return 0;
        }
    };
    /**
     * Recursively count the number of steps that may be pending for the given nodes
     * @param workflowData workflow structure
     * @param nodes set of all nodes
     * @param pending result dataset object
     * @returns {*} the dataset object
     */
    self.countPendingStepsForNodes = function (workflowData, nodes, pending) {
        if(typeof(pending)=='undefined'){
            pending = {};
        }
        for (var k = 0; k < workflowData.steps.length; k++) {
            var step = workflowData.steps[k];

            if (step.hasSubworkflow) {
                self.countPendingStepsForNodes(step.workflow, nodes, pending);
            }
            if (step.hasSubworkflow && step.nodeStep && step.parameterStates || step.nodeStep && !step.hasSubworkflow) {
                //node step may have empty targetNodes => implies targets unknown, so node may be pending for this step
                //otherwise if targetNodes contains nodes => only they might be pending for this step
                var targetNodes = null==workflowData.targetNodes || workflowData.targetNodes.length==0 ? nodes : workflowData.targetNodes;

                if(null != step.nodeStates){
                    //include only nodes that do not have states for this step
                    targetNodes=ko.utils.arrayFilter(targetNodes,function(el){
                        return step.nodeStates[el] == null;
                    });
                }else if(step.parameterStates !=null ){
                    //include only nodes that do not have parameter states for this step
                    targetNodes = ko.utils.arrayFilter(targetNodes, function (el) {
                        return step.parameterStates["node="+el] == null;
                    });
                }

                //consider this step pending for these nodes
                ko.utils.arrayForEach(targetNodes,function(node){
                    if (null == pending[node]) {
                        pending[node] = 1;
                    } else {
                        pending[node]++;
                    }
                });
            }
        }
        return pending;
    };

    self.extractNodeStepStates=function(node,steps,model){
        var count= steps?steps.length:0;
        var newsteps=[];
        for (var i = 0; i < count; i++) {
            var stepstate = steps[i];
            var stepStateForCtx = self.stepStateForCtx(model, stepstate.stepctx);
            var found = stepStateForCtx.nodeStates[node];
            found.stepctx=stepstate.stepctx;
            newsteps.push(found);
        }
        return newsteps;
    };
    self.updateNodes=function(model){
        if ( !model.allNodes) {
            return;
        }
        self.stateLoaded(true);
        //determine count of unevaluated steps
        self.pendingNodeSteps(model.completed ? {'_other':0} : self.countPendingStepsForNodes(model, model.allNodes));
        var nodeList= model.allNodes;
        var count = nodeList.length;
        self.totalNodeCount(count);
        for (var i = 0; i < count; i++) {
            var node = nodeList[i];
            //var data = model.nodes[node];

            var nodeSummary = model.nodeSummaries[node];
            var nodesteps =null;//= model.steps && model.steps.length>0?self.extractNodeStepStates(node,data,model):null;
            if(!nodesteps && model.nodeSteps && model.nodeSteps[node]){
                nodesteps=model.nodeSteps[node];
            }
            var nodea = self.findNode(node);

            if (nodea && nodesteps) {
                nodea.updateSteps(nodesteps);
            } else if(nodea) {
                nodea.updateSummary(nodeSummary);
            } else {
                var rdNode = new RDNode(node, nodesteps, self);
                rdNode.updateSummary(nodeSummary);
                self.nodes.push(rdNode);
                self.nodeIndex[rdNode.name]=rdNode;
            }
        }
    };

    self.loadStateForNode=function(node){
        var obj=self;
        return jQuery.ajax({
            url:_genUrl(self.nodeStateUpdateUrl,{node:node.name}),
            dataType:'json',
            success: function (data,status,jqxhr) {
                if(data.error){
                    obj.errorMessage( "Failed to load state: " + (jqxhr.responseJSON && jqxhr.responseJSON.error? jqxhr.responseJSON.error: err),jqxhr.responseJSON);
                }else{
                    node.loadData(data);
                }
            },
            error: function (jqxhr,status,err) {
                obj.errorMessage( "Failed to load state: " + (jqxhr.responseJSON && jqxhr.responseJSON.error? jqxhr.responseJSON.error: err),jqxhr.responseJSON);
            }
        });
    };
    self.formatTimeAtDate=function(text){
        return MomentUtil.formatTimeAtDate(text);
    };
    self.formatDurationHumanize=function(ms){
        return MomentUtil.formatDurationHumanize(ms);
    };
    self.formatDurationMomentHumanize=function(ms){
        return MomentUtil.formatDurationMomentHumanize(ms);
    };
    self.addNode=function(node,steps){
        var rdNode = new RDNode(node, steps, self);
        self.nodes.push(rdNode);
        self.nodeIndex[rdNode.name]=rdNode;
    };
    self.findNode=function(node){
        return self.nodeIndex[node];
    };


    self.execDurationSimple = ko.pureComputed(function () {
        return MomentUtil.formatDurationSimple(self.execDuration());
    });
    self.execDurationHumanized = ko.pureComputed(function () {
        return MomentUtil.formatDurationHumanize(self.execDuration());
    });
}


