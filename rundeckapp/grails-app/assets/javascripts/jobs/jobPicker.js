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

"use strict";

/**
 * Choose a job within a modal
 * @param data
 * @constructor
 */
function JobRefInput(data) {
    var self = this;
    self.project = ko.observable(data.project);
    self.error = ko.observable();
    self.contentId = ko.observable(data.contentId);
    self.displayId = ko.observable(data.displayId);
    self.loading = ko.observable(false);
    self.loadOptions = data.loadOptions || {};

    self.chosenUuid = ko.observable();
    self.chosenName = ko.observable();
    self.chosenGroup = ko.observable();
    self.chosen = ko.observable(false);

    self.jobChosenElement = function (event) {
        var data = jQuery(event.target).data();
        self.jobChosen(data.jobId, data.jobName, data.jobGroup);
    };
    self.jobChosen = function (uuid, name, group) {

        self.chosenUuid(uuid);
        self.chosenName(name);
        self.chosenGroup(group);
        self.chosen(true);
    };
    self.contentElem = function () {

        return jQuery('#' + self.contentId());
    };
    self.displayElem = function () {

        return jQuery('#' + self.displayId());
    };
    self.hideDisplay = function () {

        self.displayElem().modal('hide');
    };
    self.showDisplay = function () {

        self.displayElem().modal('show');
    };
    self.clearContent = function () {
        self.contentElem().html('');
    };
    self.loadJobChooserModal = function () {
        var project = self.project();

        // pick a project
        // if (typeof(projectid) !== 'undefined' && projectid && jQuery('#' + projectid).length == 1) {
        //     project = jQuery('#' + projectid).val();
        // }
        // jQuery(elem).button('loading').addClass('active');
        self.loading(true);
        return jQuery.ajax({
            url: _genUrl(appLinks.menuJobsPicker, {
                jobsjscallback: 'observe',
                runAuthRequired: true,
                projFilter: project
            }),
            success: function (resp, status, jqxhr) {
                self.loading(false);
                // jQuery(elem).button('reset').removeClass('active');
                self.contentElem().html(resp);
                self.contentElem().on('click', '[data-observe="jobChosen"]', self.jobChosenElement);
                self.showDisplay();
            },
            error: function (resp, status, jqxhr) {
                self.loading(false);
                self.error("Error performing request: menuJobsPicker: " + transport);
                // jQuery(elem).button('reset').removeClass('active');
            }
        });
    }


}

function JobRefPropertySelector(data) {
    var self = this;
    self.propkey = data.propkey;
    self.modalid = data.modalid;
    self.modalContentid = data.modalContentid;
    self.idkey = data.idkey;
    self.input = null;

    self.getProperty = function () {
        if (self.propkey) {
            return PluginProperties[self.propkey];
        }
        return null;
    };
    self.actionClick = function (data, evt) {
        var elem = jQuery(evt.target);
        self.input = new JobRefInput({
            project: self.getProperty().project(),
            contentId: self.modalContentid,
            displayId: self.modalid
        });

        //TODO: associated properties to select multiple components
        var component = self.getProperty().renderingOptions()['selectionComponent'];
        if (!component || component === 'uuid') {
            self.input.chosenUuid.subscribe(self.getProperty().value);
        }
        if (component === 'name') {
            self.input.chosenName.subscribe(self.getProperty().value);
        }
        if (component === 'group') {
            self.input.chosenGroup.subscribe(self.getProperty().value);
        }
        self.input.loading.subscribe(function (val) {
            if (val) {
                elem.button('loading').addClass('active');
            } else {
                elem.button('reset').removeClass('active');
            }
        });
        self.input.loadJobChooserModal();
        self.input.chosen.subscribe(function (val) {
            if (val) {
                self.input.hideDisplay();
                self.input.clearContent();
            }
        });
    };
}