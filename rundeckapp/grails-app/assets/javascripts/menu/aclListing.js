/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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
    self.uploadField = ko.observable(data.uploadField);
    self.name = ko.observable(data.name);
    self.nameError = ko.observable(false);
    self.check = function () {
        self.nameError(!self.name());
        return !self.nameError();
    };
    self.fileChanged = function (obj, event) {
        var files = event.currentTarget.files;
        // console.log("changed: ", files, event);
        if (!self.name() && files.length > 0) {
            var name = files[0].name;
            // if (name.endsWith('.aclpolicy')) {
            //     name = name.substr(0, name.length - 10);
            // }
            self.name(name);
        }
    };

}
function PolicyDocument(data) {
    var self = this;
    self.name = ko.observable(data.name);
    self.description = ko.observable(data.description);
    self.valid = ko.observable(data.valid);
    self.wasSaved = ko.observable(data.wasSaved ? true : false);
    self.savedSize = ko.observable(data.savedSize);
    self.showValidation = ko.observable(false);
    self.validation = ko.observable(data.validation);

    self.toggleShowValidation = function () {
        self.showValidation(!self.showValidation());
    };

    ko.mapping.fromJS(data,{},self);
}

function PolicyFiles(data) {
    var self = this;
    self.policies = ko.observableArray();
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
    ko.mapping.fromJS(data, self.bindings, self);
}