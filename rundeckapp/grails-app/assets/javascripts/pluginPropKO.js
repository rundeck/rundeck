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
//= require ui/toggle

"use strict";

function PluginEditor(data) {
    var self = this;
    self.service = ko.observable(data.service);
    self.provider = ko.observable(data.provider);
    self.config = ko.observable(data.config);
    self.formId = data.formId;
    self.formPrefixes = data.formPrefixes;
    self.inputFieldPrefix = data.inputFieldPrefix;

    self.loadPluginEditView = function (service, name, params, data, report) {
        return jQuery.ajax(
            {
                url: _genUrl(
                    appLinks.pluginPropertiesForm,
                    jQuery.extend({
                            service: service,
                            name: name
                        },
                        params
                    )),
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({config: data, report: report}),
                succes: function () {
                    self.config(null);
                }
            }
        );
    };
    self.getConfigData = function () {
        if (self.config() !== null) {
            return self.config().data ? self.config().config : {};
        } else {
            //serialize form data
            return jQueryFormData(jQuery('#' + self.formId), self.formPrefixes);
        }
    };
    self.getConfigReport = function () {
        if (self.config() !== null) {
            return self.config().data ? self.config().report : {};
        }
    };
    self.loadActionSelect = function (val) {
        if (!val) {
            return;
        }
        self.loadPluginEditView(self.service(), val, {inputFieldPrefix: self.inputFieldPrefix}, self.getConfigData(), self.getConfigReport()).success(function (data) {
            jQuery('#' + self.formId).html(data).show();
            //TODO: ko binding?
        });
    };
    self.provider.subscribe(self.loadActionSelect);
    self.init = function () {
        self.loadActionSelect(self.provider());
    };
}

function PluginProperty(data) {
    var self = this;
    self.project = ko.observable(data.project);
    self.name = ko.observable(data.name);
    self.service = ko.observable(data.service);
    self.provider = ko.observable(data.provider);
    self.fieldname = ko.observable(data.fieldname);
    self.fieldid = ko.observable(data.fieldid);
    self.fieldtype = ko.observable(data.fieldtype);
    self.idkey = ko.observable(data.idkey);
    self.util = ko.observable({});
    self.type = ko.observable(data.type);
    self.renderingOptions = ko.observable(data.renderingOptions||{});

    self.getField = function () {
        return jQuery('#' + self.fieldid());
    };
    self.value = ko.observable(data.value);
    self.toggle = new UIToggle({value: false});
    self.getAssociatedProperty = function () {
        var associated = self.renderingOptions()['associatedProperty'];
        var idkey = self.idkey();
        if (associated && idkey && typeof(PluginSet) === 'object' && typeof(PluginSet[idkey]) === 'object') {
            return PluginSet[idkey][associated];

        }
        return null;
    };
}