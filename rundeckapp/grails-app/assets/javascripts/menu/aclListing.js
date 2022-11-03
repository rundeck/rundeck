/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

//= require vendor/knockout.min
//= require vendor/knockout-mapping
//= require ko/binding-url-path-param
//= require knockout-foreachprop
//= require ko/binding-message-template
//= require ko/binding-popover
//= require util/pager
//= require util/loader

function PolicyUpload(data) {
    "use strict";
    var self = this;
    self.name = ko.observable(data.name);
    self.nameFixed = ko.observable();
    self.idFixed = ko.observable();
    self.nameError = ko.observable(false);
    self.hasFile = ko.observable(false);
    self.fileError = ko.observable(false);
    self.overwriteError = ko.observable(false);
    self.overwrite = ko.observable(false);
    self.policies = ko.observableArray(data.policies);
    self.check = function () {
        self.nameError(!self.name() && !self.nameFixed());

        if (!self.nameError() && !self.overwrite()) {
            //check existing policies
            self.overwriteError(
                self.policies().some(function (val) {
                    return val.name() === self.name();
                })
            );
        }
        if (self.overwrite()) {
            self.overwriteError(false);
        }
        self.fileError(!self.hasFile());

        return !self.nameError() && !self.overwriteError() && !self.fileError();
    };
    self.fileChanged = function (obj, event) {
        var files = event.currentTarget.files;
        // console.log("changed: ", files, event);
        if (!self.name() && files.length > 0) {
            var name = files[0].name;
            if (name.endsWith('.aclpolicy')) {
                name = name.substr(0, name.length - 10);
            }
            self.name(name);
        }
        self.hasFile(files.length === 1);
    };
    self.name.subscribe(function (val) {
        if (val) {
            self.nameError(false);
        }
    });
    self.overwrite.subscribe(function (val) {
        if (val) {
            self.overwriteError(false);
        }
    });
    self.hasFile.subscribe(function (val) {
        if (val) {
            self.fileError(false);
        }
    });
    self.reset = function () {
        self.name(data.name);
        self.nameFixed(null);
        self.idFixed(null);
        self.nameError(false);
        self.overwriteError(false);
        self.overwrite(false);
        self.hasFile(false);
        self.fileError(false);
    };
    self.showUploadModal = function (id, policy) {
        self.nameFixed(policy.name());
        self.idFixed(policy.id());
        self.overwrite(true);
        jQuery('#' + id).modal('show');
    };
    self.cancelUploadModal = function (id) {
        self.reset();
        jQuery('#' + id).modal('hide');
    };
}
function PolicyDocument(data) {
    let self = this;
    self.name = ko.observable(data.name);
    self.id = ko.observable(data.id);
    self.description = ko.observable(data.description);
    self.valid = ko.observable(data.valid);
    self.wasSaved = ko.observable(data.wasSaved ? true : false);
    self.savedSize = ko.observable(data.savedSize);
    self.showValidation = ko.observable(false);
    self.validation = ko.observable(data.validation);
    self.meta = ko.observable(data.meta||{
        policies: ko.observableArray(),
        count: ko.observable(0)
    });

    self.toggleShowValidation = function () {
        self.showValidation(!self.showValidation());
    };

    self.resume = ko.computed(function(){
        let meta = self.meta()
        if(!meta){
            return ''
        }
        let count = ko.utils.unwrapObservable(meta.count);
        if(count<1){
            return ''
        }
        if(count > 1){
            return `(${count} Policies)`;
        }
        return `(${count} Policy)`;
    })
    //nb: it seems like somehow including knockout-mapping.js or other js file multiple times causes a race where
    //the ko.mapping gets removed after init, but before the Loadable gets called, so we preserve a reference here
    self.komapping=ko.mapping

    self.loadData = function (data) {
        self.komapping.fromJS(data, {}, self)
    }
    self.loader = new Loadable(self.loadData,(data && data.meta && data.meta.count)?true:false)
    self.loadData(data)
}

function PolicyFiles(data,loadableEndpoint) {
    let self = this;
    self.search=ko.observable()
    self.policies = ko.observableArray();
    self.filtered = new FilteredView({
        content:self.policies,
    })
    self.paging = new PagedView({
        content: self.filtered.filteredContent,
        max: 30,
        offset: 0,
    })
    self.pagingEnabled=ko.observable(true)
    self.fileUpload = data.fileUpload;
    self.bindings = {
        policies: {
            key: function (data) {
                return ko.utils.unwrapObservable(data.name);
            },
            create: function (options) {
                "use strict";
                return new PolicyDocument(options.data);
            }
        }
    };
    self.policiesView=ko.pureComputed(function (){
        if(self.pagingEnabled()){
            return self.paging.page()
        }else{
            return self.filtered.filteredContent();
        }
    })

    self.getSearchFilter=function(){
        return{
            enabled:function(){
                return self.search();
            },
            filter:function(policies){
                let search = self.search()
                let regex
                if (search.charAt(0) === '/' && search.charAt(search.length - 1) === '/') {
                    regex = new RegExp(search.substring(1, search.length - 1),'i');
                }else{
                    //simple match which is case-insensitive, escaping special regex chars
                    regex = new RegExp(search.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'),'i');
                }
                return ko.utils.arrayFilter(policies,function(val,ndx){
                    let name = val.name();
                    if(name.match(regex)){
                        return true;
                    }
                    let desc = ko.unwrap(val.meta()? val.meta().description :'')
                    if(desc && desc.match(regex)){
                        return true
                    }
                    let policies1 = val.meta() && val.meta().policies && ko.unwrap(val.meta().policies)
                    if(policies1 && policies1.length > 0){
                        for(let i=0;i<policies1.length;i++) {
                            let desc = policies1[i].description? policies1[i].description() :'';
                            if(desc && desc.match(regex)){
                                return true
                            }
                            let by = policies1[i].by?policies1[i].by():null
                            if(by && by.match(regex)){
                                return true;
                            }
                        }
                    }
                    return false;
                });
            }
        }

    };
    self.valid = ko.computed(function () {
        var policies = self.policies();
        if (policies.length < 1) {
            return true;
        }
        return !policies.some(function (p) {
            return !p.valid();
        })
    });
    self.selectedPolicy = ko.observable();
    self.showModal = function (id, policy) {
        self.selectedPolicy(policy);
        jQuery('#' + id).modal('show');
    };
    self.showUploadModal = function (id, policy) {
        if (self.fileUpload) {
            self.fileUpload.showUploadModal(id, policy);
        }
    };
    self.loadMeta=function (policies){
        let needsLoad=policies.findAll(a=>!a.loader.loaded())
        needsLoad.forEach(a=>a.loader.begin())
        return jQuery.ajax({
            url:loadableEndpoint,
            dataType:'json',
            contentType:'json',
            method:'POST',
            data:JSON.stringify({files:needsLoad.map(a=>a.id())})
        }).done(function (data){
            data.forEach(function(d){
                let found=needsLoad.find(a=>a.id()===d.id)
                if(found){
                    found.loader.onData(d)
                }
            })
        }).fail(function(data, jqxhr, err){
            needsLoad.forEach(a=>a.loader.onError("Error loading policy information: "+err))
        })
    }
    self.init = function () {
        if(loadableEndpoint) {
            self.policiesView.subscribe(function(val){
                self.loadMeta(val)
            })
            self.loadMeta(self.policiesView())
        }
    }

    ko.mapping.fromJS(data, self.bindings, self);
    self.filtered.filters.push(self.getSearchFilter())
    self.init()
}
