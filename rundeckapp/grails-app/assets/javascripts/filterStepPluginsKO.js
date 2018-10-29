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


function StepPluginsFilter(data) {
    var self = this;
    self.stepDescriptions = ko.observableArray(data.stepDescriptions);
    self.stepFilterValue = ko.observable("");
    self.currentFilter = ko.observable();
    self.currentPropertyFilter = ko.observable("");
    self.filterStepDescriptions = function () {
        var filterValue = self.stepFilterValue() ? self.stepFilterValue().split("=") : "";
        var prop = filterValue.length > 1 ? filterValue[0] : "title";
        var value = filterValue.length > 1 ? filterValue[1] : filterValue[0];
        self.currentPropertyFilter(prop);
        self.currentFilter(value);
    };

    self.isVisible = function(typedesc){
        var arrayFiltered = ko.utils.arrayFilter(self.stepDescriptions(), function (descr) {
            var propertyFilterValue = self.currentPropertyFilter() ?
                self.currentPropertyFilter().split(":") : undefined;

            var filterByProps = propertyFilterValue && propertyFilterValue.length == 2;

            if(!filterByProps) {
                return descr[self.currentPropertyFilter() || "title"] &&
                    descr[self.currentPropertyFilter() || "title"].toLowerCase()
                        .indexOf(self.currentFilter() ? self.currentFilter().toLowerCase() : undefined) >= 0
                    && typedesc == descr.name;
            } else if(filterByProps) {
                return descr.properties && Array.isArray(descr.properties) && descr.properties.any(function(t){
                    return t[propertyFilterValue[1]] && t[propertyFilterValue[1]].toLowerCase()
                        .indexOf(self.currentFilter() ? self.currentFilter().toLowerCase() : undefined) >= 0;
                }) && typedesc == descr.name;
            }
        });

        return arrayFiltered.length > 0 || (!self.currentFilter() || self.currentFilter() === "");
    };

    self.isDefaultStepsVisible = function(defaultStepTitle){
        if (self.currentPropertyFilter() && self.currentPropertyFilter() !== 'title') return false;

        return defaultStepTitle.toLowerCase()
            .indexOf(self.currentFilter() ? self.currentFilter().toLowerCase() : undefined) >= 0 || (!self.currentFilter() || self.currentFilter() === "");
    }
}
