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
            return 0;
        }
    });
    self.durationSimple=ko.computed(function(){
        var ms = self.duration();
        if(!self.startTime() || !self.endTime()){
            return '';
        }
        return flow.formatDurationSimple(ms);
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
       var ms=0;
       ko.utils.arrayForEach(self.steps(),function(x){
            ms += x.duration();
       });
       return ms;
    });
    self.durationSimple=ko.computed(function(){
        //format duration
        var ms=self.duration();
        if(ms<1){
            return '';
        }
       return flow.formatDurationSimple(ms);
    });
    self.summary=ko.observable();
    self.summaryState=ko.observable();
    self.currentStep=ko.observable();
    self.summarize=function(){
        var currentStep=null;
        var summarydata = {
            total: self.steps().length,
            SUCCEEDED: 0,
            FAILED: 0,
            WAITING: 0,
            NOT_STARTED: 0,
            RUNNING: 0,
            RUNNING_HANDLER: 0,
            other: 0,
            duration_ms_total: 0
        };
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
        if (summarydata.total > 0) {
            if (summarydata.RUNNING > 0) {
                self.summary("Running");
                self.summaryState("RUNNING");
            } else if (summarydata.RUNNING_HANDLER > 0) {
                self.summary("Running");
                self.summaryState("RUNNING_HANDLER");
            } else if (summarydata.total == summarydata.SUCCEEDED) {
                self.summary("All Steps OK");
                self.summaryState("SUCCEEDED");
            } else if (summarydata.FAILED > 0) {
                self.summary(summarydata.FAILED + " " + flow.pluralize(summarydata.FAILED, "Step") + " FAILED");
                self.summaryState("FAILED");
            } else if (summarydata.WAITING > 0) {
                self.summary("Waiting to run " + summarydata.WAITING + " " + flow.pluralize(summarydata.WAITING, "Step"));
                self.summaryState("WAITING");
            } else if (summarydata.NOT_STARTED == summarydata.total) {
                self.summary("No steps were run");
                self.summaryState("NOT_STARTED");
            } else if (summarydata.NOT_STARTED > 0) {
                self.summary(summarydata.NOT_STARTED + " " + flow.pluralize(summarydata.NOT_STARTED, "Step") + " not run");
                self.summaryState("NOT_STARTED");
            } else if (summarydata.SUCCEEDED > 0) {
                self.summary((summarydata.total - summarydata.SUCCEEDED) + " did not succeed");
                self.summaryState("PARTIAL_SUCCEEDED");
            } else {
                self.summary("No steps succeeded");
                self.summaryState("NONE_SUCCEEDED");
            }
        } else {
            self.summary("No steps");
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
    self.nodes=ko.observableArray([
    ]);
    self.followingStep=ko.observable();
    self.followingControl=null;
    self.followOutputUrl= outputUrl;
    self.stopShowingOutput= function () {
        if(self.followingControl){
            self.followingControl.stopFollowingOutput();
            self.followingControl=null;
        }
    };
    self.showOutput= function (stepctx, node) {
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
            autoscroll:false
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
            var ctrl = self.showOutput(nodestep.stepctx, nodestep.node.name);
            self.followingStep(nodestep);
            self.followingControl=ctrl;
        }else{
            self.followingStep(null);
        }
    };

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
    }
    self.extractNodeStepStates=function(node,steps,model){
        var count=steps.length;
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
    self.formatTime=function(text,format){
        var time = moment(text);

        if (time.isValid()) {
            return time.format(format);
        } else {
            return '';
        }
    }
    self.formatTimeSimple=function(text){
        return self.formatTime(text,'h:mm:ss a');
    }
    self.formatDurationSimple=function(ms){
        var duration = moment.duration(ms);
        var m = duration.minutes();
        var s = duration.seconds();
        return duration.hours() + '.' + (m<10?'0'+m:m) + ':' + (s<10?'0'+s:s);
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
}


