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


function JobEditor(data){
    const self=this
    self.jobName=ko.observable(data.jobName)
    self.groupPath=ko.observable(data.groupPath)
    self.uuid=ko.observable(data.uuid)
    self.href=ko.observable(data.href)
    self.errorTabs = ko.observableArray()

    self.optionError=ko.pureComputed(function () {
        return self.errorTabs.indexOf('option') >= 0
    })
    self.workflowError=ko.pureComputed(function () {
        return self.errorTabs.indexOf('workflow') >= 0
    })
    self.clearError = function (name) {
        if (self.errorTabs.indexOf(name) >= 0) {
            self.errorTabs.remove(name)
        }
    }
    self.addError = function (name) {
        if (self.errorTabs.indexOf(name) < 0) {
            self.errorTabs.push(name)
        }
    }
}
