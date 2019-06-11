/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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
//= require knockout.min
//= require knockout-mapping
//= require knockout-onenter
//= require storageBrowseKO
//= require koBind

function ProjectDefaultPlugin (data) {
    var self = this
    self.service = ko.observable(data.service)
    self.type = ko.observable(data.type)
    self.index = ko.observable(data.index)
}

function EditProject (data) {
    var self = this
    self.create = ko.observable(data.create || !data.name)
    self.name = ko.observable(data.name)
    self.defaults = {
        'NodeExecutor': new ProjectDefaultPlugin({service: 'NodeExecutor', type: data.defaultNodeExec}),
        'FileCopier': new ProjectDefaultPlugin({service: 'FileCopier', type: data.defaultFileCopier})
    }
}

jQuery(function () {
    var projectData = loadJsonData('projectDataJSON')
    window.projectEditor = new EditProject(projectData)
    initKoBind(null, {editProject: projectEditor})
})
