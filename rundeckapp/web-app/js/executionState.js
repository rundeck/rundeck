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
 * State of workflow, step oriented
 */
var FlowState = Class.create({
    executionId:null,
    selectedOutputStatusId:null,
    targetElement:null,
    retry:5,
    loadUrl:null,
    loadUrlParamsBase: {},
    loadUrlParams:null,
    outputUrl:null,
    shouldUpdate:false,
    updateCompleted:false,
    updateRunning:false,
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
            appendHtml($(this.targetElement + '_log'),"<br>");
            appendText($(this.targetElement + '_log'), text);
        }
    },
    showError: function(text){
        this.logWarn(text);
    },
    updateState: function(model){
        if(this.updaters){
            for(var i=0;i<this.updaters.length;i++){
                try {
                    this.updaters[i].updateState(model);
                } catch (e) {
                    console.log("error on updateState: ", e);
                }
            }
        }
    },
    updateError: function(error,model){
        if(this.updaters){
            for(var i=0;i<this.updaters.length;i++){
                if(typeof(this.updaters[i].updateError)=='function'){
                    try {
                        this.updaters[i].updateError(error, model);
                    } catch (e) {
                        console.log("error on updateError: ", e);
                    }
                }
            }
        }
    },
    update: function (json) {
        var data=json.state;
        //compare
        if (data.error=='pending' ) {
            this.retry--;
        }else if(data.error){
            this.retry=-1;
            this.shouldUpdate=false;
        }
        if(!data.error){
            this.updateState(json);
        }else{
            this.updateError(data.error,json);
        }
        if (data.error && this.retry>=0 || !json.completed && this.shouldUpdate) {
            this.timer = setTimeout(this.callUpdate.bind(this), this.reloadInterval);
        } else {
            this.stopFollowing(json.completed);
        }
    },
    callUpdate: function(){
        var state=this;
        this.updateRunning=true;
        var url = _genUrl(state.loadUrl, state.loadUrlParams);
        jQuery.ajax({
            url: url,
            dataType:'json',
            success: function (data,status,jqxhr) {
                state.update(data);
            },
            error: function (jqxhr,status,err) {
                state.updateError( "Failed to load state: " + (jqxhr.responseJSON && jqxhr.responseJSON.error? jqxhr.responseJSON.error: err),jqxhr.responseJSON);
            }
        });
    },
    beginFollowing: function(){
        if(!this.updateCompleted && !this.updateRunning){
            this.shouldUpdate=true;
            this.callUpdate();
        }
    },
    stopFollowing: function(completed){
        this.shouldUpdate=false;
        clearTimeout(this.timer);
        this.timer = null;
        this.updateCompleted=completed;
        this.updateRunning=false;
    },
    addUpdater: function(updater){
        if(null==this.updaters){
            this.updaters = [];
        }
        this.updaters.push( updater);
    }
});
