/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

//= require momentutil
//= require vendor/knockout.min
//= require vendor/knockout-mapping
//= require ko/binding-popover
//= require ko/binding-url-path-param


/**
 * Info about an indexed step within a particular job
 * @param parent
 * @param ndx 0 based index
 * @param data
 * @constructor
 */
function JobStepInfo(parent,ndx,data){
    "use strict";
    var self = this;
    /**
     * Owner is a JobWorkflow
     */
    self.parent = parent;
    self.ndx = ndx;
    self.jobId = ko.observable(data.id);
    self.ehJobId = ko.observable(data.ehId);
    self.type = ko.observable(data.type||'unknown-step-type');
    self.stepident = ko.observable(data.stepident||'(Unknown)');
    self.ehType = ko.observable(data.ehType);
    self.ehStepident = ko.observable(data.ehStepident);
    self.ehKeepgoingOnSuccess = ko.observable(data.ehKeepgoingOnSuccess);
}

/**
 * A job containing a sequence of steps
 * @param multi multiworkflow object
 * @param workflow array of JobStepInfo
 * @param id job id
 * @constructor
 */
function JobWorkflow(multi,workflow,id){
    "use strict";
    var self=this;
    self.id=id;
    self.multi=multi;
    self.workflow=workflow||[];

    /**
     * Insert a step
     * @param ndx index
     * @param data step
     */
    self.insert=function(ndx,data){
        self.workflow[ndx]=data;
    };
}
/**
 * A step in an execution identified by a stepctx string,
 * which corresponds to a particular job step within the hierarchy
 * @param stepctx step context string for this step
 * @param data data
 * @constructor
 */
function WorkflowStepInfo(multiworkflow,stepctx,data){
    "use strict";
    var self = this;
    /**
     * The MultiWorkflow
     */
    self.multiworkflow=multiworkflow;
    /**
     * The step context
     */
    self.stepctx = stepctx;
    self.stepctxArray = ko.observableArray(RDWorkflow.parseContextId(stepctx));
    /**
     * the job referenced by this step
     * @type {null}
     */
    self.job = data.job;
    /**
     * the job referenced by an error handler
     */
    self.ehJob = data.ehJob;
    /**
     * The JobStepInfo containing the details of the step
     */
    self.jobstep = ko.observable();
    /**
     * Job ID
     */
    self.jobId = ko.observable(data.id);
    /**
     * ID of errorhandler Job
     */
    self.ehJobId = ko.observable(data.ehId);
    /**
     * Step type info
     */
    self.type = ko.observable(data.type||'unknown-step-type');
    /**
     * Step type info
     */
    self.ehType = ko.observable(data.ehType);
    /**
     * Step identity string
     */
    self.stepident = ko.observable(data.stepident||'Step: '+stepctx);
    /**
     * Error handler step identity
     */
    self.ehStepident = ko.observable(data.ehStepident);
    self.ehKeepgoingOnSuccess = ko.observable(data.ehKeepgoingOnSuccess);

    self.hasParent=ko.pureComputed(function(){
       return self.stepctxArray().length>1;
    });

    /**
     * Return true if this step is a job ref step or has a parent job
     */
    self.hasLink=ko.pureComputed(function () {
        var has = self.hasParent();
        var isjob = self.type() == 'job';
        return has || isjob;
    });

    /**
     * Return appropriate JobID for linking this step,
     * for a non-job reference, this is the parent Job ID.
     * For a
     */
    self.linkJobId=ko.pureComputed(function(){
        if(self.type()=='job'){
            return self.jobId();
        }else if(self.hasParent()){
            return self.parentJobId();
        }else{
            return self.multiworkflow.jobId;
        }
    });
    /**
     * Return the title for linked job
     */
    self.linkTitle=ko.pureComputed(function(){
        if(self.type()=='job'){
            return self.stepident();
        }else if(self.hasParent()){
            return self.parentJobTitle();
        }else{
            return 'Current Job';
        }
    });
    self.parentJobId=ko.pureComputed(function(){
        var has=self.hasParent();
        var parent=self.parentStepInfo();
        if(has && parent){
            if(parent.isErrorhandler()){
                return parent.ehJobId();
            }
            return parent.jobId()||parent.ehJobId();
        }
        return null;
    });
    self.parentJobTitle=ko.pureComputed(function(){
        var has=self.hasParent();
        var parent=self.parentStepInfo();
        if(has && parent){
            if(parent.isErrorhandler()){
                return parent.ehStepident();
            }
            return parent.jobId() && parent.stepident() ||parent.ehStepident();
        }
        return null;
    });

    self.parentStepInfo=ko.computed(function(){
        var ctx=self.stepctxArray();
        if(ctx.length>1) {
            var parent = RDWorkflow.createContextId(ctx.slice(0, -1));
            return self.multiworkflow.getStepInfoForStepctx(parent);
        }
        return null;
    });
    /**
     * Step number
     */
    self.stepnum=ko.pureComputed(function(){
        var stepctxArray = self.stepctxArray();
        if(stepctxArray.length>0){
            return RDWorkflow.stepNumberForContextId(stepctxArray[stepctxArray.length-1]);
        }else{
            return null;
        }
    });
    /**
     * Step is error handler
     */
    self.isErrorhandler=ko.pureComputed(function(){
        var stepctxArray = self.stepctxArray();
        if(stepctxArray.length>0){
            for(var i =0;i<stepctxArray.length;i++){
                if(RDWorkflow.isErrorhandlerForContextId(stepctxArray[i])){
                    return true;
                }
            }
        }
        return false;
    });

    /**
     * Computed name like "1. stepident"
     */
    self.stepdesc=ko.pureComputed(function(){
        var num = self.stepnum();
        if(!num){
            return null;
        }
        return num+". "+self.stepident();
    });
    /**
     * Computed name like "1. stepident"
     */
    self.stepdescFull=ko.pureComputed(function(){
        var num = self.stepnum();
        if(!num){
            return null;
        }
        var text= self.stepdesc();
        if(self.isErrorhandler() && self.ehType()){
            text=text+' ! '+self.ehStepident();
        }
        return text;
    });
    /**
     * full context string
     */
    self.stepctxString=ko.pureComputed(function(){
        return self.stepctx;
    });
    /**
     * Clean context string
     */
    self.stepctxClean=ko.pureComputed(function(){
        return 'Workflow step: '+RDWorkflow.cleanContextId(self.stepctx)
    });
    /**
     * Clean context string
     */
    self.stepctxPathFull=ko.computed(function(){
        var ctx=self.stepctxArray();
        if(ctx.length>1){
            var obj=self.parentStepInfo();
            return obj.stepctxPathFull()+' / '
                    // + ctx[ctx.length-1]
                    + self.stepdescFull()
                ;
        }else{
            return self.stepdescFull();
        }
    });
    /**
     * When a JobStepInfo is set for this step, update our details
     */
    self.jobstep.subscribe(function(newval){
        if(newval) {
            self.type(newval.type());
            self.stepident(newval.stepident());
            self.jobId(newval.jobId());
            self.ehJobId(newval.ehJobId());
            self.ehType(newval.ehType());
            self.ehStepident(newval.ehStepident());
            self.ehKeepgoingOnSuccess(newval.ehKeepgoingOnSuccess());
        }
    });
}
/**
 * Cache of loaded job workflow data, keyed by job ID
 * @param url remote URL for loading job workflow data
 * @param data any preloaded data
 * @constructor
 */
function JobWorkflowsCache(url,data){
    "use strict";
    var self=this;
    /**
     * Remote url
     */
    self.url=url;
    /**
     * loaded data: ID -> ko.observable(Object)
     */
    self.jobs=jQuery.extend({},data);
    /**
     * Load remote data for a job ID, add it to the cache
     * @param id
     * @returns {*}
     */
    self.load=function(id){
        window._rundeck.rundeckClient.jobWorkflowGet(id).then(function(resp) {
            self.add(id, resp.workflow);
        })
    };
    /**
     * add job ID data
     * @param id job ID
     * @param value data
     */
    self.add=function(id,value){
        if(self.jobs[id] && ko.isObservable(self.jobs[id])){
            self.jobs[id](value);
        }else {
            self.jobs[id] = ko.observable(value);
        }
    };
    /**
     * One time subscription to observable, will be disposed after first call
     * @param obs observable
     * @param then callback
     */
    self.tempSubscribe=function(obs,then){
        var sub;
        sub=obs.subscribe(function(data){
            sub.dispose();
            then(data);
        });
    };
    /**
     * Get data for an ID
     * @param id ID
     * @param then callback for result of data, may be called immediately if data is available
     */
    self.getJob=function(id,then){
        if(self.jobs[id] && ko.isObservable(self.jobs[id]) && self.jobs[id]()){
            then(self.jobs[id]());
        }else if(ko.isObservable(self.jobs[id])){
            self.tempSubscribe(self.jobs[id],then);
        }else{
            self.jobs[id]=ko.observable();
            self.tempSubscribe(self.jobs[id],then);
            self.load(id);
        }
    };
}

/**
 * Manage display data for execution workflow steps
 * @param parent owner is a NodeFlowViewModel
 * @param data config data: 'dynamicStepDescriptionDisabled' (true/false) if true, do not load any dynamic data.
 *      'workflow' preloaded top level workflow info
 *      'id' job/execution ID
 * @constructor
 */
function MultiWorkflow(workflowInfo,data){
    "use strict";
    var self=this;
    /**
     * workflowInfo is a RDWorkflow
     */
    self.workflowInfo=workflowInfo;
    /**
     * If true, do not load data dynamically
     */
    self.dynamicStepDescriptionDisabled=data.dynamicStepDescriptionDisabled;
    /**
     * Cache for loaded data
     * @type {JobWorkflowsCache}
     */
    self.cache=new JobWorkflowsCache(data.url,{});
    /**
     * JobWorkflow if initial workflow is loaded
     * @type {JobWorkflow}
     */
    self.job=null;
    /**
     * ID of job or execution
     */
    self.jobId=data.id;
    /**
     * Placeholder indicating a JobWorkflow data has or will be loaded. Object: Job ID->JobWorkflow
     * @type {{}}
     */
    self.workflowsets={};
    /**
     * context-string-id -> WorkflowStepInfo
     * @type {{}}
     */
    self.stepinfoset={};

    /**
     * Load or return a JobWorkflow with all workflow data filled in
     * @param id job ID
     * @param callback called with the JobWorkflow as a parameter after loading
     * @returns {JobWorkflow}
     */
    self.loadJob=function(id,callback){
        if(self.workflowsets[id] && self.workflowsets[id].workflow.length>0){
            //job data was already loaded, so return existing
            callback(self.workflowsets[id]);
            return self.workflowsets[id];
        }
        //create or retrieve an entry in the workflowsets indicating loading for the job will start
        self.workflowsets[id] = self.workflowsets[id] || new JobWorkflow(self, [], id);
        var job = self.workflowsets[id];
        //ask cache to load or return cached data
        self.cache.getJob(id,function(data){
            //if this is the first time loading this data, we need to fill in the workflow for the job
            var job = self.fillJobWorkflow(id,data);
            //result will be JobWorkflow
            callback(job);
        });
        return job;
    };

    /**
     * Given ID and possible preloaded workflow data, update the job cache
     * @param id job ID
     * @param workflow preloaded workflow, or null may cause remote load if not already in the cache
     */
    self.updateJobCache=function(id,workflow){
        if(id && !self.workflowsets[id]){
            if(workflow) {
                //if workflow is available, put it in the cache
                self.cache.add(id, workflow);
            }
            self.loadJob(id,function(){});
        }
    };
    /**
     * Fill in a JobWorkflow's workflow given ID and steps, if not already filled in
     * @param id job ID
     * @param steps load workflow step data
     * @returns {JobWorkflow}
     */
    self.fillJobWorkflow=function (id, steps) {
        "use strict";
        if(self.workflowsets[id] && self.workflowsets[id].workflow.length>0){
            //job data was already loaded, so return existing
            return self.workflowsets[id];
        }
        var job = self.workflowsets[id] || new JobWorkflow(self, [], id);
        self.workflowsets[id] = job;

        for (var x = 0; x < steps.length; x++) {
            var stepdata={
                type: _wfTypeForStep(steps[x]),
                stepident: _wfStringForStep(steps[x]),
                id:steps[x].jobId
            };
            if(steps[x].errorhandler){
                //errorhandler info for the job
                stepdata.ehType=_wfTypeForStep(steps[x].errorhandler);
                stepdata.ehStepident=_wfStringForStep(steps[x].errorhandler);
                stepdata.ehKeepgoingOnSuccess=_wfStringForStep(steps[x].errorhandler.keepgoingOnSuccess);
            }
            if(steps[x].ehJobId){
                //errorhandler job id
                stepdata.ehId=steps[x].ehJobId;
            }

            job.insert(x,new JobStepInfo(job,x,stepdata));

            if (stepdata.type == 'job') {
                //load job reference if not already in progress
                self.updateJobCache(stepdata.id,steps[x].workflow);
            }
            //do the same for error handler job reference
            if (stepdata.ehId) {
                self.updateJobCache(stepdata.ehId,steps[x].ehWorkflow);
            }
        }

        return job;
    };

    /**
     * Get stepctx info for a parent job reference, with loaded workflow for the job
     * @param parentctx
     * @param callback called with WorkflowStepInfo parameter
     */
    self.getParentJobStepInfoForStepctx=function(parentctx,callback){
        if(parentctx) {
            //load higher level
            self.getStepInfoForStepctx(parentctx, function (parentinfo) {
                if(parentinfo.type()=='job' && parentinfo.jobId()){
                    //load subjob for this step
                    self.loadJob(parentinfo.jobId(),function(job){
                        parentinfo.job=job;
                        if(parentinfo.ehJobId()) {
                            self.loadJob(parentinfo.ehJobId(), function (job) {
                                parentinfo.ehJob = job;
                                callback(parentinfo);
                            });
                        }else {
                            callback(parentinfo);
                        }
                    });
                }else if(parentinfo.ehJobId()){
                    self.loadJob(parentinfo.ehJobId(),function(job){
                        parentinfo.ehJob=job;
                        callback(parentinfo);
                    });
                }else{
                    console.log("stepctx was not job: "+parentctx,parentinfo);
                    //callback(stepinfo);
                }
            });
        }else {
            //parent is top level job
            self.loadJob(self.jobId, function(job){
                callback(new WorkflowStepInfo(self,'',{id:self.jobId,type:'job',job:job}));
            });
        }
    };
    /**
     * Look up step context info for a context string. If not dynamic, returns the placeholder object,
     * otherwise looks up step info dynamically and returns WorkflowStepInfo via callback
     * @param stepctx context string
     * @param callback called with WorkflowStepInfo
     * @returns {*} WorkflowStepInfo which may not have loaded contents, or placeholder object
     */
    self.getStepInfoForStepctx=function(stepctx,callback){
        "use strict";
        if(self.dynamicStepDescriptionDisabled){
            if(!self.stepinfoset[stepctx]) {
                self.stepinfoset[stepctx] = new WorkflowStepInfo(self, stepctx, {
                    type: self.workflowInfo.contextType(stepctx),
                    stepident: self.workflowInfo.renderContextString(stepctx)
                });
            }
            return self.stepinfoset[stepctx];
        }
        if(self.stepinfoset[stepctx]){

            var stepinfo = self.stepinfoset[stepctx];
            if(typeof(callback)=='function' && stepinfo.jobstep()){
                callback(stepinfo);
            }else if (typeof(callback)=='function'){
                var remove;
                remove=stepinfo.jobstep.subscribe(function(newval){
                    if(newval) {
                        remove.dispose();
                        callback(stepinfo);
                    }
                });
            }
            return stepinfo;
        }
        var info = new WorkflowStepInfo(self,stepctx,{});
        self.stepinfoset[stepctx] = info;
        var ctx = RDWorkflow.parseContextId(stepctx);
        var lastctx=ctx.pop();
        var ndx = RDWorkflow.workflowIndexForContextId(lastctx);

        //get the parent workflow, and then fill in the current step
        self.getParentJobStepInfoForStepctx(RDWorkflow.createContextId(ctx),function(parentjobinfo){
            //TODO: currently node summary state context string does not indicate errorHandler, but
            //in case it does in the future, force use of correct job info
            var iseh=ctx.length>0?RDWorkflow.isErrorhandlerForContextId(ctx[ctx.length-1]):false;
            var job = iseh?parentjobinfo.ehJob:(parentjobinfo.job||parentjobinfo.ehJob);
            var jobstep = job && job.workflow[ndx];
            info.parent=job;
            info.jobstep(jobstep);

            if(typeof(callback)=='function'){
                callback(info);
            }
        });

        return info;
    };
    /**
     * Initialize with preloaded data
     * @param data
     */
    self.initialLoad=function(data){
        //only trigger load if containing a preloaded workflow
        if(data.workflow) {
            self.updateJobCache(data.id, data.workflow);
        }
    };
    self.initialLoad(data);
}
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
    self.stepinfo=ko.observable(flow.multiWorkflow.getStepInfoForStepctx(data.stepctx));
    self.type = ko.observable(flow.workflow.contextType(data.stepctx));
    self.stepident = ko.observable(flow.workflow.renderContextString(data.stepctx));
    self.stepctxdesc = ko.observable("Workflow Step: " + data.stepctx);
    self.parameters = ko.observable(data.parameters || null);
    self.followingOutput = ko.observable(false);
    self.hovering = ko.observable(false);
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
        self.dedupeSteps(data.steps);
        self.updateSummary(data.summary);
        self.updateSteps(data.steps);
    };
    self.dedupeSteps=function(steps) {
        var seen = []
        var dupes = []

        steps.forEach(function(s) {
            if (seen.indexOf(s.stepctx) > -1) {
                dupes.push(s)
            }

            seen.push(s.stepctx)
        })

        dupes.forEach(function(d) {
            steps.splice(steps.indexOf(d), 1)
        })
    };
    self.updateSteps=function(steps){
        ko.mapping.fromJS({steps: steps}, mapping, this);
        self.summarize();
    };
    if(steps){
        self.updateSteps(steps);
    }
}

function NodeFlowViewModel(workflow, outputUrl, nodeStateUpdateUrl, multiworkflow, data) {
    var self=this;
    self.workflow=workflow;
    self.multiWorkflow=multiworkflow;
    self.errorMessage=ko.observable();
    self.statusMessage=ko.observable();
    self.stateLoaded=ko.observable(false);
    self.pendingNodeSteps=ko.observable({});
    self.nodes=ko.observableArray([ ]).extend({ rateLimit: 500 });
    self.selectedNodes=ko.observableArray([ ]);
    self.followingStep=ko.observable();
    self.execFollowingControl = data.followControl;
    self.followingControl=null;
    self.followOutputUrl= outputUrl;
    self.nodeStateUpdateUrl= nodeStateUpdateUrl;
    self.completed=ko.observable();
    self.partial=ko.observable();
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
    self.executionId = ko.observable(data.executionId);
    self.outputScrollOffset=0;
    self.activeView = ko.observable("nodes");
    /**
     * synonym with activeView, to maintain compatibility
     */
    self.activeTab = self.activeView;

    self.views = ko.observableArray(data.views)
    /** synonym for compatibility */
    self.tabs = self.views
    /**
     * returns the tabs that have showButton flag enabled
     */
    self.viewButtons = ko.pureComputed(function () {
        return ko.utils.arrayFilter(self.views(), (e) => e.showButton)
    })
    /**
     * Returns the tabs that have hasContent flag enabled
     */
    self.contentViews = ko.pureComputed(function () {
        return ko.utils.arrayFilter(self.views(), (e) => e.hasContent)
    })
    self.humanizedDisplay=ko.observable(false);
    self.logoutput = ko.observable(data.logoutput);
    self.activeTabData = ko.pureComputed(function () {
        const theTab = self.activeTab()
        return self.views().find((e) => e.id === theTab)
    })
    self.scheduled = ko.pureComputed(function () {
        return self.executionState() === 'SCHEDULED';
    });
    self.queued = ko.pureComputed(function () {
        return self.executionState() === 'QUEUED';
    });
    self.failed = ko.pureComputed(function () {
        return self.executionState() === 'FAILED';
    });
    self.incompleteState = ko.pureComputed(function () {
        return self.executionState() === 'OTHER' && self.executionStatusString() === 'incomplete';
    });
    self.totalSteps=ko.pureComputed(function(){ return self.workflow.workflow.length; });
    self.activeNodes=ko.pureComputed(function(){
        return ko.utils.arrayFilter(self.nodes(), function (n) {
            return n.summaryState() !== 'NONE';
        });
    });
    self.displayStatusString = ko.computed(function () {
        var statusString = self.executionStatusString();
        return statusString != null && self.executionState() != statusString.toUpperCase() && !self.incompleteState();
    });
    self.canKillExec = ko.computed(function () {
        return self.execFollowingControl && self.execFollowingControl.killjobauth
    });
    self.killRequested = ko.observable(false);
    self.killResponseData = ko.observable({});
    self.killExecAction = function () {
        if (self.execFollowingControl) {
            self.execFollowingControl.docancel().then(function (data) {
                self.killRequested(true);
                self.killResponseData(data);
            });
        }
    };
    self.markExecAction = function () {
        if (self.execFollowingControl) {
            self.execFollowingControl.doincomplete().then(function (data) {
                self.killRequested(true);
                self.killResponseData(data);
            });
        }
    };
    self.killStatusFailed = ko.computed(function () {
        "use strict";
        var req = self.killRequested();
        var data = self.killResponseData();
        if (!req) {
            return false;
        }
        return data && !data.cancelled;
    });
    self.killStatusPending = ko.computed(function () {
        "use strict";
        var req = self.killRequested();
        var data = self.killResponseData();
        if (!req) {
            return false;
        }
        return data && data.cancelled && data.abortstate === 'pending';
    });
    self.killStatusText = ko.computed(function () {
        var req = self.killRequested();
        var data = self.killResponseData();
        if (!req) {
            return "";
        }
        if (data && data.cancelled && data.abortstate === 'pending') {
            return data.reason || 'Killing Job...';
        } else if (data && data.cancelled && data.abortstate === 'aborted') {
            return data.reason || 'Killed.';
        } else {
            return (data ? data['error'] || data.reason : '') || 'Failed to Kill Job.';
        }
    });
    self.killedbutNotSaved = ko.computed(function () {
        "use strict";
        var req = self.killRequested();
        var data = self.killResponseData();
        if (!req) {
            return "";
        }
        return data && data.cancelled && data.status === 'db-error';
    });
    self.totalNodeCount=ko.observable(0);
    self.nodeIndex={};
    self.totalNodes=ko.pureComputed(function(){
        var nodes = ko.utils.arrayFilter(self.nodes(), function (n) {
            return n.summaryState() !== 'NONE';
        });
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
        /** Kick the event out to Vue Land and let the new viewer handle display */
        if (nodestep.executionState() !== 'NOT_STARTED')
            window._rundeck.eventBus.emit('ko-exec-show-output', nodestep)
        else
            nodestep.outputLineCount(0)
    }

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
        return true;
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
            var nodesteps =null;//= model.steps &&
                                // model.steps.length>0?self.extractNodeStepStates(node,data,model):null;
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
    self.toggleHumanizedDisplay=function(){
        self.humanizedDisplay(!self.humanizedDisplay());
    }
    self.startTimeAgo=ko.pureComputed(function () {
        let time=self.startTime()
        if(!time) return
        return moment(self.startTime()).fromNow();
    })
    self.endTimeAgo=ko.pureComputed(function () {
        let time=self.endTime()
        if(!time) return
        return moment(self.endTime()).fromNow();
    })

    self.execDurationSimple = ko.pureComputed(function () {
        return MomentUtil.formatDurationSimple(self.execDuration());
    });
    self.execDurationHumanized = ko.pureComputed(function () {
        return MomentUtil.formatDurationHumanize(self.execDuration());
    });
    self.execDurationDisplay=ko.pureComputed(function () {
        const human=self.humanizedDisplay();
        if(human) return MomentUtil.formatDurationHumanize(self.execDuration());
        return MomentUtil.formatDurationSimple(self.execDuration());
    })
    self.followFlowState = function (flowState,followNodes) {
        "use strict";
        flowState.addUpdater({
            updateError: function (error, data) {
                if (error !== 'pending') {
                    self.stateLoaded(false);
                    self.errorMessage(data.state.errorMessage ? data.state.errorMessage : error);
                } else {
                    self.statusMessage(data.state.errorMessage ? data.state.errorMessage : error);
                }
                ko.mapping.fromJS({
                    executionState: data.executionState,
                    executionStatusString: data.executionStatusString,
                    retryExecutionId: data.retryExecutionId,
                    retryExecutionUrl: data.retryExecutionUrl,
                    retryExecutionState: data.retryExecutionState,
                    retryExecutionAttempt: data.retryExecutionAttempt,
                    retry: data.retry,
                    completed: data.completed,
                    partial: data.partial,
                    execDuration: data.execDuration,
                    jobAverageDuration: data.jobAverageDuration,
                    startTime: data.startTime ? data.startTime : data.state ? data.state.startTime : null,
                    endTime: data.endTime ? data.endTime : data.state ? data.state.endTime : null
                }, {}, self);
            },
            updateState: function (data) {
                ko.mapping.fromJS({
                    executionState: data.executionState,
                    executionStatusString: data.executionStatusString,
                    retryExecutionId: data.retryExecutionId,
                    retryExecutionUrl: data.retryExecutionUrl,
                    retryExecutionState: data.retryExecutionState,
                    retryExecutionAttempt: data.retryExecutionAttempt,
                    retry: data.retry,
                    completed: data.completed,
                    partial: data.partial,
                    execDuration: data.execDuration,
                    jobAverageDuration: data.jobAverageDuration,
                    startTime: data.startTime ? data.startTime : data.state ? data.state.startTime : null,
                    endTime: data.endTime ? data.endTime : data.state ? data.state.endTime : null
                }, {}, self);
                if(followNodes) {
                    self.updateNodes(data.state);
                }
            }
        });
    }
}


