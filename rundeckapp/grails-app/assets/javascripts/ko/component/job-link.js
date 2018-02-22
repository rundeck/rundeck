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

//= require asset-ko-template-loader
"use strict";


ko.components.register('job-link', {
    viewModel: function (params) {
        // Data: value is either null, 'like', or 'dislike'
        var self = this;
        self.id = params.id;
        self.project = params.project;
        self.action = params.action;
        self.loaded = ko.observable(false);
        self.jobName = ko.observable();
        self.jobGroup = ko.observable();
        self.jobData = ko.observable();
        self.linkHref = ko.observable();
        self.notfound = ko.observable(true);
        self.unset = ko.pureComputed(function () {
            return !self.id();
        });
        self.displayName = ko.pureComputed(function () {
            var group = self.jobGroup();

            var name = self.jobName();
            if (group) {
                return group + '/' + name;
            }
            return name;
        });
        self.doAction = function () {
            if (typeof(self.action) === 'function') {
                return self.action();
            }
            return true;
        };
        self.loadJobData = function (val) {
            if (!val) {
                self.notfound(false);
                self.loaded(true);
                return;
            }
            jQuery.ajax({
                url: _genUrl(appLinks.menuJobsAjax, {idlist: val, project: self.project()}),
                dataType: 'json',
                success: function (data) {
                    if (data && jQuery.isArray(data) && data.length === 1) {
                        self.jobName(data[0].name);
                        self.jobGroup(data[0].group);
                        self.linkHref(data[0].permalink);
                        self.notfound(false);
                    } else {
                        //cannot find given id, indcate it is not set
                        self.notfound(true);
                    }
                    self.loaded(true);
                }
            })
        };
        self.id.subscribe(self.loadJobData);
        if (self.id()) {
            self.loadJobData(self.id());
        }

    },
    template: {assetTemplate: 'job-link.html'}

});