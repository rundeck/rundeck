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
    update: function (data) {
        //compare
        this.model = data;
        $(this.targetElement).innerHTML = this.model.executionState;
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
