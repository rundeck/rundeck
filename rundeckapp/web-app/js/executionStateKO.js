/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

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
    self.node= node;
    self.flow= flow;
    self.stepctx=data.stepctx;
    self.type=ko.observable();
    self.followingOutput=ko.observable(false);
    self.outputLineCount=ko.observable(-1);
    self.type=ko.computed(function(){
        return flow.workflow.contextType(self.stepctx);
    });
    self.stepident=ko.computed(function(){
        return flow.workflow.renderContextString(self.stepctx);
    });
    self.startTimeSimple=ko.computed(function(){
        return flow.formatTimeSimple(self.startTime());
    });
    self.startTimeFormat=function(format){
        return flow.formatTime(self.startTime(),format);
    };
    self.endTimeSimple=ko.computed(function(){
        return flow.formatTimeSimple(self.endTime());
    });
    self.endTimeFormat= function (format) {
        return flow.formatTime(self.endTime(), format);
    };
    self.duration=ko.computed(function(){
        if(self.endTime() && self.startTime()){
            return moment(self.endTime()).diff(moment(self.startTime()));
        }else{
            return -1;
        }
    });
    self.durationSimple=ko.computed(function(){
        return flow.formatDurationSimple(self.duration());
    });
    self.stepctxdesc = ko.computed(function () {
        return "Workflow Step: "+self.stepctx;
    });
    ko.mapping.fromJS(data, {}, this);
}
/**
 * Return true if state B should be used instead of a
 * @param a
 * @param b
 */
RDNodeStep.stateCompare= function (a, b) {
    if (a == b) {
        return true;
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
    self.steps=ko.observableArray([]);
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
    }
    self.toggleExpand=function(){ self.expanded(!self.expanded()); };
    self.duration=ko.computed(function(){
        //sum up duration of all completed steps
       var ms=-1;
       ko.utils.arrayForEach(self.steps(),function(x){
           var ms2 = x.duration();
           if(ms2>=0 && ms<0){
               ms=ms2;
           }else if(ms2>=0){
               ms += ms2;
           }
       });
       return ms;
    });
    self.durationSimple=ko.computed(function(){
        //format duration
       return flow.formatDurationSimple(self.duration());
    });
    self.summary=ko.observable();
    self.summaryState=ko.observable();
    self.currentStep=ko.observable();

    /**
     * Determine summary data for the node, sets the summaryState and summary and currentStep
     */
    self.summarize=function(){
        var currentStep=null;

        //step summary info
        var summarydata = {
            total: self.steps().length,
            SUCCEEDED: 0,
            FAILED: 0,
            WAITING: 0,
            NOT_STARTED: 0,
            RUNNING: 0,
            RUNNING_HANDLER: 0,
            other: 0,
            duration_ms_total: 0,
            pending: self.flow.pendingSteps()
        };

        //determine count for step states
        ko.utils.arrayForEach(self.steps(),function(step){
            ['SUCCEEDED', 'FAILED', 'WAITING', 'NOT_STARTED', 'RUNNING', 'RUNNING_HANDLER'].each(function (z) {
                if (null != summarydata[z] && step.executionState()==z) {
                    summarydata[z]++;
                } else {
                    summarydata['other']++;
                }
            });
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
    self.updateSteps=function(steps){
        ko.mapping.fromJS({steps: steps}, mapping, this);
        self.summarize();
    }
    self.updateSteps(steps);
}

function NodeFlowViewModel(workflow,outputUrl){
    var self=this;
    self.workflow=workflow;
    self.errorMessage=ko.observable();
    self.stateLoaded=ko.observable(false);
    self.pendingSteps=ko.observable(0);
    self.nodes=ko.observableArray([ ]);
    self.followingStep=ko.observable();
    self.followingControl=null;
    self.followOutputUrl= outputUrl;
    self.completed=ko.observable();
    self.executionState=ko.observable();
    self.execDuration=ko.observable();
    self.jobAverageDuration=ko.observable();
    self.startTime=ko.observable();
    self.endTime=ko.observable();
    self.executionId=ko.observable();
    self.outputScrollOffset=0;
    self.failed=ko.computed(function(){ return self.executionState()=='FAILED'; });
    self.totalSteps=ko.computed(function(){ return self.workflow.workflow.length; });
    self.activeNodes=ko.computed(function(){
        return ko.utils.arrayFilter(self.nodes(), function (n) {
            return n.summaryState() != 'NONE';
        });
    });
    self.totalNodes=ko.computed(function(){
        var nodes = ko.utils.arrayFilter(self.nodes(),function(n){return n.summaryState()!='NONE';});
        return nodes?nodes.length:0;
    });
    self.jobPercentage=ko.computed(function(){
        if(self.jobAverageDuration()>0){
            return 100*(self.execDuration()/self.jobAverageDuration());
        }else{
            return -1;
        }
    });
    self.jobPercentageFixed=ko.computed(function(){
        var pct = self.jobPercentage();
        if(pct>=0){
            return pct.toFixed(0)
        }else{
            return '0';
        }
    });
    self.jobOverrunDuration=ko.computed(function(){
        var jobAverageDuration = self.jobAverageDuration();
        var execDuration = self.execDuration();
        if(jobAverageDuration > 0 && execDuration > jobAverageDuration){
            return self.formatDurationSimple(execDuration - jobAverageDuration);
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
    })
    self.succeededNodes=ko.computed(function(){
        var completed=new Array();
        ko.utils.arrayForEach(self.nodes(), function (n) {
            if(n.summaryState()=='SUCCEEDED'){
                completed.push(n);
            }
        });
        return completed;
    })

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
        var sel = '.wfnodeoutput[data-node=' + node + ']';
        if (stepctx) {
            sel += '[data-stepctx=' + stepctx + ']';
        } else {
            sel += '[data-stepctx=]';
        }
        var targetElement = $(document.body).down(sel);
        if (!$(targetElement)) {
            return null;
        }
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

    self.toggleOutputForNodeStep = function (nodestep) {
        self.stopShowingOutput();
        //toggle following output for selected nodestep
        nodestep.followingOutput(!nodestep.followingOutput());
        ko.utils.arrayForEach(self.nodes(), function (n) {
            ko.utils.arrayForEach(n.steps(), function (step) {
                //disable following for every other nodestep
                if(step != nodestep){
                    step.followingOutput(false);
                }
            });
        });
        if(nodestep.followingOutput()){
            var ctrl = self.showOutput(nodestep);
            self.followingStep(nodestep);
            self.followingControl=ctrl;
            nodestep.node.expanded(true);
        }else{
            self.followingStep(null);
        }
    };
    self.scrollTo= function (element,offx,offy) {
        element = $(element);
        var x = element.x ? element.x : element.offsetLeft,
            y = element.y ? element.y : element.offsetTop;
        window.scrollTo(x+(offx?offx:0), y+(offy?offy:0));
    }

    self.scrollToNodeStep=function(node,stepctx){
        var elem = $$('.wfnodestep[data-node=' + node + '][data-stepctx=' + stepctx + ']');
        if (elem) {
            jQuery('#tab_link_flow a').tab('show');
            //scroll to
            self.scrollTo($(elem[0]));
//            $(elem[0]).scrollTo();
        }
    };
    self.scrollToNode=function(node){
        var elem = $$('.wfnodesteps[data-node=' + node + ']');
        if (elem) {
            jQuery('#tab_link_flow a').tab('show');
            //scroll to
            self.scrollTo($(elem[0]));
//            $(elem[0]).scrollTo();
        }
    };
    self.scrollToOutput=function(nodestep){
        if(!nodestep.followingOutput()){
            self.toggleOutputForNodeStep(nodestep);
        }
        self.scrollToNode(nodestep.node.name);
    }

    self.pluralize = function (count, singular, plural) {
        return count == 1 ? singular : null != plural ? plural : (singular + 's');
    };
    self.stepStateForCtx=function (model, stepctx) {
            var a, b;
            var s = stepctx.indexOf("/");
            if (s > 0) {
                a = stepctx.substr(0, s);
                b = stepctx.substr(s + 1);
            } else {
                a = stepctx;
                b = null;
            }
            var ndx = parseInt(a) - 1;
            var step = model.steps[ndx];
            if (b && step.workflow) {
                return self.stepStateForCtx(step.workflow, b);
            } else {
                return step;
            }
    };
    self.countPendingSteps = function (workflowData) {
        var pending = 0;
        var noTargets = !workflowData.targetNodes || workflowData.targetNodes.length < 1;
        for (var k = 0; k < workflowData.steps.length; k++) {
            var step = workflowData.steps[k];
            if (step.nodeStep && noTargets) {
                pending++;
            } else if (step.hasSubworkflow) {
                pending += self.countPendingSteps(step.workflow);
            }
        }
        return pending;
    };
    self.extractNodeStepStates=function(node,steps,model){
        var count= steps?steps.length:0;
        var newsteps=[];
        for (var i = 0; i < count; i++) {
            var stepstate = steps[i];
            var stepStateForCtx = this.stepStateForCtx(model, stepstate.stepctx);
            var found = stepStateForCtx.nodeStates[node];
            found.stepctx=stepstate.stepctx;
            newsteps.push(found);
        }
        return newsteps;
    }
    self.updateNodes=function(model){
        if (!model.nodes || !model.allNodes) {
            return;
        }
        self.stateLoaded(true);
        //determine count of unevaluated steps
        self.pendingSteps(model.completed ? 0 : self.countPendingSteps(model));
        var nodeList= model.allNodes;
        var count = nodeList.length;
        for (var i = 0; i < count; i++) {
            var node = nodeList[i];
            var data = model.nodes[node];

            var nodesteps = self.extractNodeStepStates(node, data, model);
            var nodea = self.findNode(node);
            if (nodea) {
                nodea.updateSteps(nodesteps);
            } else {
                self.addNode(node, nodesteps);
            }
        }
    }
    self.formatTime=function(text,format){
        var time = moment(text);

        if (text && time.isValid()) {
            return time.format(format);
        } else {
            return '';
        }
    }
    self.formatTimeSimple=function(text){
        return self.formatTime(text,'h:mm:ss a');
    }
    self.formatTimeAtDate=function(text){
        var time = moment(text);
        if(!text || !time.isValid()){
            return '';
        }
        var now=moment();
        var ms = now.diff(time);
        var since = moment.duration(ms);
        if(since.asDays()<1 && now.month()==time.month()){
            //same date
            return time.format('h:mm a');
        }if(since.asDays()<1 && now.month()!=time.month()){
            //within a day
            return time.format('h:mm a');
        }else if(since.asWeeks()<1){
            return time.format('ddd h:mm a');
        }else if(time.year()!=now.year()){
            return time.format('MMM do yyyy');
        }else{
            return time.format('M/d ha');
        }
    }
    self.formatDurationSimple=function(ms){
        if(ms<0){
            return '';
        }
        var duration = moment.duration(ms);
        var m = duration.minutes();
        var s = duration.seconds();
        return duration.hours() + '.' + (m<10?'0'+m:m) + ':' + (s<10?'0'+s:s);
    }
    self.formatDurationHumanize=function(ms){
        if(ms<0){
            return '';
        }
        var duration = moment.duration(ms);
        var valarr=[];
        if(duration.days()>0){
            valarr.push(duration.days()+'d');
        }
        if(duration.hours()>0){
            valarr.push(duration.hours()+'h');
        }
        if(duration.minutes()>0){
            valarr.push(duration.minutes()+'m');
        }
        if(duration.seconds()>0){
            var s = duration.seconds();
            if(duration.milliseconds()>0){
                s++;
            }
            valarr.push(s+'s');
        }
        return valarr.join(' ');
    }
    self.formatDurationMomentHumanize=function(ms){
        if(ms<0){
            return '';
        }
        var duration = moment.duration(ms);
        return duration.humanize();
    }
    self.addNode=function(node,steps){
        self.nodes.push(new RDNode(node, steps,self));
    };
    self.findNode=function(node){
        var len= self.nodes().length;
        for(var x=0;x<len;x++){
            var n= self.nodes()[x];
            if(n.name==node){
                return n;
            }
        }
        return null;
    };


    self.execDurationSimple = ko.computed(function () {
        var execDuration = self.execDuration();
        return self.formatDurationSimple(execDuration);
    });
    self.execDurationHumanized = ko.computed(function () {
        var execDuration = self.execDuration();
        return self.formatDurationHumanize(execDuration);
    });
}


