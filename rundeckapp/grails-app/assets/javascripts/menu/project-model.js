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


function ProjectAuth(data) {
    "use strict";
    var self = this;
    self.jobCreate = ko.observable(false);
    self.admin = ko.observable(false);
    self.mapping = {};
    if (data) {
        ko.mapping.fromJS(data, self.mapping, self);
    }
}
function ProjectReadme(data) {
    "use strict";
    var self = this;
    self.readmeHTML = ko.observable(null);
    self.motdHTML = ko.observable(null);

    self.mapping = {};
    if (data) {
        ko.mapping.fromJS(data, self.mapping, self);
    }
}
function Project(data) {
    var self = this;
    self.name = ko.observable(data.name);
    self.execCount = ko.observable(data.execCount || 0);
    self.failedCount = ko.observable(data.failedCount || 0);
    self.userCount = ko.observable(data.userCount || 0);
    self.description = ko.observable(data.description);
    self.auth = ko.observable(new ProjectAuth());
    self.readme = ko.observable(new ProjectReadme());
    self.loaded = ko.observable(false);
    self.readmeDisplay = ko.observable(data.readmeDisplay || []);
    self.motdDisplay = ko.observable(data.motdDisplay || []);
    self.page = ko.observable(data.page || []);
    self.showReadme = ko.computed(function () {
        var page = self.page();
        var rddisplay = self.readmeDisplay();
        var content = self.readme().readmeHTML();
        return content && rddisplay.indexOf(page) >= 0;
    });
    self.showMotd = ko.computed(function () {
        var page = self.page();
        var rddisplay = self.motdDisplay();
        var content = self.readme().motdHTML();
        return content && rddisplay.indexOf(page) >= 0;
    });
    self.showMessage = ko.computed(function () {
        var rd = self.readme();
        var showm= self.showMotd();
        var showr = self.showReadme();
        return rd && (showm || showr);
    });
    self.mapping = {
        auth: {
            create: function (options) {
                "use strict";
                return new ProjectAuth(options.data);
            }
        },
        readme: {
            create: function (options) {
                "use strict";
                return new ProjectReadme(options.data);
            }
        }
    };
    if (data) {
        ko.mapping.fromJS(data, self.mapping, self);
    }
}