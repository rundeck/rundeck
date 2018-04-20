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

//= require knockout.min
//= require knockout-mapping
//= require ko/binding-url-path-param
//= require knockout-foreachprop
//= require ko/binding-message-template
//= require ko/binding-popover

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
                ko.utils.arrayFirst(self.policies(), function (val) {
                    return val.name() === self.name();
                }) !== null
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
    var self = this;
    self.name = ko.observable(data.name);
    self.id = ko.observable(data.id);
    self.description = ko.observable(data.description);
    self.valid = ko.observable(data.valid);
    self.wasSaved = ko.observable(data.wasSaved ? true : false);
    self.savedSize = ko.observable(data.savedSize);
    self.showValidation = ko.observable(false);
    self.validation = ko.observable(data.validation);
    self.meta = ko.observable(data.meta);

    self.toggleShowValidation = function () {
        self.showValidation(!self.showValidation());
    };

    self.resume = function () {
        var count = self.meta().count;
        if(count > 1){
            return '('+count+' Policies)';
        }
        return '('+count+' Policy)';

    };


    ko.mapping.fromJS(data,{},self);
}

function PolicyFiles(data) {
    var self = this;
    self.policies = ko.observableArray();
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
    self.valid = ko.computed(function () {
        var policies = self.policies();
        if (policies.length < 1) {
            return true;
        }
        return ko.utils.arrayFirst(policies, function (p) {
            return !p.valid();
        }) === null;
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

    ko.mapping.fromJS(data, self.bindings, self);
}