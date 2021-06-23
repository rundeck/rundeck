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

//= require vendor/knockout.min
//= require vendor/knockout-mapping
//= require knockout-onenter
//= require ko/binding-url-path-param
//= require ko/binding-message-template
//= require ko/handler-bootstrapPopover
//= require ko/handler-bootstrapTooltip
//= require menu/project-model
//= require koBind

function ProjectHome(name, data) {
    "use strict";
    var self = this;
    self.name = ko.observable(name);
    self.baseUrl = data.baseUrl;
    self.project = ko.observable();
    self.load = function () {
        var params = {projects: self.name()};
        return jQuery.ajax({
            type: 'GET',
            contentType: 'json',
            headers: {'x-rundeck-ajax': 'true'},
            url: _genUrl(self.baseUrl, params),
            success: function (data, status, jqxhr) {
                if (data.projects && data.projects.length === 1) {
                    ko.mapping.fromJS(jQuery.extend(data.projects[0], {loaded: true}), null, self.project);
                }
            }
        });
    };
}

/**
 * START page init
 */
function init() {
    var data = loadJsonData('projectData');
    window.projectHome  = new ProjectHome(data.project, {baseUrl: appLinks.menuHomeAjax});
    projectHome.project(
        new Project({name: data.project, page: 'projectHome'})
    );
    projectHome.load();
    initKoBind(null,{projectHome:projectHome})
}
jQuery(init);
