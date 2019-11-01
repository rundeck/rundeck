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
    /**
     * Check match for field of object
     * @param obj object
     * @param field field name
     * @param val value
     * @returns {*|boolean}
     */
    self.checkMatch = function (obj, field, val) {
        return obj[field] && obj[field].toLowerCase().indexOf(val ? val.toLowerCase() : undefined) >= 0
    }
    /**
     * Computed array of matching results
     */
    self.arrayFiltered = ko.pureComputed(function () {
        return ko.utils.arrayFilter(self.stepDescriptions(), function (descr) {
            var propertyFilterValue = self.currentPropertyFilter() ?
                                      self.currentPropertyFilter().split(":") : undefined;

            var filterByProps = propertyFilterValue && propertyFilterValue.length == 2;

            if (!filterByProps) {
                //just search all title, description, name for the input
                return self.checkMatch(descr, 'title', self.currentFilter())
                       || self.checkMatch(descr, 'name', self.currentFilter())
                       || self.checkMatch(descr, 'description', self.currentFilter())

            } else if (filterByProps) {
                return descr.properties && Array.isArray(descr.properties) && descr.properties.any(function (t) {
                    return self.checkMatch(t, propertyFilterValue[1], self.currentFilter())
                })
            }
        });
    })

    /**
     * Check plugin name is in filtered results
     * @param pluginName
     * @returns {boolean}
     */
    self.isVisible = function (pluginName) {
        return (!self.currentFilter() || self.currentFilter() === "") ||
               self.arrayFiltered().findIndex((desc) => desc.name === pluginName) >=
               0
    };

    /**
     * Check default plugin should be visible, if no filter or if title or description of a default plugin matches query
     * @param title
     * @param description
     * @returns {boolean|*}
     */
    self.isDefaultStepsVisible = function (title, description) {
        return (!self.currentFilter() || self.currentFilter() === "")
               || self.checkMatch({title: title}, 'title', self.currentFilter())
               || self.checkMatch({description: description}, 'description', self.currentFilter())
               || false
    }
}
