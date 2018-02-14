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

function JobOptionsInput(data) {
    var self = this;
    self.id = ko.observable(data.id);
    self.project = ko.observable(data.project);
    self.error = ko.observable();
    self.loading = ko.observable(false);
    self.hasContent = ko.observable(false);
    self.contentId = ko.observable(data.contentId);
    self.displayId = ko.observable(data.displayId);
    self.loadOptions = data.loadOptions || {};


    self._jobExecUnloadHandlers = [];
    self._registerJobExecUnloadHandler = function (handler) {
        self._jobExecUnloadHandlers.push(handler);
    };
    self.contentElem = function () {
        return jQuery('#' + self.contentId());
    };
    self.displayElem = function () {
        return jQuery('#' + self.displayId());
    };
    self.showDisplay = function () {
        self.displayElem().modal('show');
    };
    self.hideDisplay = function () {
        self.displayElem().modal('hide');
    };
    self.unloadExec = function () {
        if (self._jobExecUnloadHandlers.length > 0) {
            for (var i = 0; i < self._jobExecUnloadHandlers.length; i++) {
                self._jobExecUnloadHandlers[i].call();
            }
            self._jobExecUnloadHandlers.clear();
        }
        self.hasContent(false);

        self.displayElem().modal('hide');
        // jQuery('#execDiv').modal('hide');
        // clearHtml('execDivContent');

        self.loading(false);
    };

    self.requestError = function (item, message) {
        self.unloadExec();
        self.error("Failed request: " + item + ". Result: " + message);
    };

    self.loadExec = function () {
        self.error(null);
        var params = jQuery.extend({id: self.id()}, self.loadOptions);

        self.loading(true);
        self.contentElem().load(_genUrl(appLinks.scheduledExecutionExecuteFragment, params),
            function (response, status, xhr) {
                if (status === "success") {
                    self.loadedFormSuccess();
                    self.showDisplay();
                } else {
                    self.requestError("executeFragment for [" + self.id() + "]", xhr.statusText);
                }
            });
    };
    self.loadExecValidate = function (eparams) {
        self.error(null);
        var params = eparams;
        self.loading(true);
        self.contentElem().load(_genUrl(appLinks.scheduledExecutionExecuteFragment, params),
            function (response, status, xhr) {
                if (status === "success") {
                    self.loadedFormSuccess();
                } else {
                    self.requestError("executeFragment for [" + id + "]", xhr.statusText);
                }
            });
    };

    self.getFormData = function () {
        return new FormData(self.contentElem().find('form')[0]);
    };
    self.getFormDataObj = function (rmprefixes, reqprefixes) {
        return jQueryFormData(self.contentElem(), rmprefixes || [], reqprefixes);
    };
    self.execSubmit = function (target) {
        var data = self.getFormData();
        jQuery.ajax({
            url: target,
            type: 'POST',
            data: data,
            contentType: false,
            dataType: 'json',
            processData: false,
            success: function (result) {
                if (result.id) {
                    if (result.follow && result.href) {
                        document.location = result.href;
                    } else {
                        if (!pageActivity.selected()) {
                            pageActivity.activateNowRunningTab();
                        }
                        self.unloadExec();
                    }
                } else if (result.error === 'invalid') {
                    // reload form for validation
                    self.loadExecValidate(Form.serialize(self.contentId()) + "&dovalidate=true");
                } else {
                    self.unloadExec();
                    self.error(result.message ? result.message : result.error ? result.error : "Failed request");
                }
            },
            error: function (data, jqxhr, err) {
                self.requestError("runJobInline", err);
            }
        });
    };

    self.loadedFormSuccess = function () {
        if ($('execFormCancelButton')) {
            Event.observe($('execFormCancelButton'), 'click', function (evt) {
                Event.stop(evt);
                self.unloadExec();
                return false;
            }, false);
            $('execFormCancelButton').name = "_x";
        }
        if ($('execFormRunButton')) {
            Event.observe($('execFormRunButton'), 'click', function (evt) {
                Event.stop(evt);
                self.execSubmit(appLinks.scheduledExecutionRunJobInline);
                $('formbuttons').loading(message('job.starting.execution'));
                return false;
            }, false);
        }
        jQuery('#showScheduler').on('shown.bs.popover', function () {
            if ($('scheduleAjaxButton')) {
                Event.observe($('scheduleAjaxButton'), 'click', function (evt) {
                    Event.stop(evt);
                    if (isValidDate()) {
                        toggleAlert(true);
                        self.execSubmit(appLinks.scheduledExecutionScheduleJobInline);
                        $('formbuttons').loading(message('job.scheduling.execution'));
                    } else {
                        toggleAlert(false);
                    }
                    return false;
                }, false);
            }
        });

        //setup option handling
        //setup option edit
        var joboptiondata = loadJsonData('jobOptionData');
        var joboptions = new JobOptions(joboptiondata);

        if (document.getElementById('optionSelect')) {
            ko.applyBindings(joboptions, document.getElementById('optionSelect'));
        }

        var remoteoptionloader = new RemoteOptionLoader({
            url: appLinks.scheduledExecutionLoadRemoteOptionValues,
            id: self.id(),
            fieldPrefix: "extra.option."
        });
        var remotecontroller = new RemoteOptionController({
            loader: remoteoptionloader
        });
        remotecontroller.setupOptions(joboptions);

        remotecontroller.loadData(loadJsonData('remoteOptionData'));
        self._registerJobExecUnloadHandler(remotecontroller.unsubscribeAll);
        joboptions.remoteoptions = remotecontroller;
        remotecontroller.begin();

        jQuery('input').on('keydown', function (evt) {
            return noenter(evt);
        });
        self.loading(false);
    }
}

function JobOptionPropertySelector(data) {
    var self = this;
    self.propkey = data.propkey;
    self.modalid = data.modalid;
    self.modalContentid = data.modalContentid;
    self.idkey = data.idkey;
    self.jobOptionsInput = null;

    self.getProperty = function () {
        if (self.propkey) {
            return PluginProperties[self.propkey];
        }
        return null;
    };
    self.actionClick = function (data, evt) {var elem = jQuery(evt.target);

        var associated = self.getProperty().getAssociatedProperty();


        self.jobOptionsInput = new JobOptionsInput({
            id: associated.value(),
            project: self.getProperty().project(),
            contentId: self.modalContentid,
            displayId: self.modalid,
            loadOptions: {hideActionButtons: true, hideExtraInput: true}
        });
        self.jobOptionsInput.loading.subscribe(function (val) {
            if (val) {
                elem.button('loading').addClass('active');
            } else {
                elem.button('reset').removeClass('active');
            }
        });
        self.jobOptionsInput.displayElem().find('._btn_actionSave').on('click', self.actionSave);
        self.jobOptionsInput.loadExec();
    };

    self.actionSave = function (evt) {

        evt.preventDefault();
        self.jobOptionsInput.hideDisplay();
        var formDataObj = self.jobOptionsInput.getFormDataObj(['extra.option.'], ['extra.option.']);
        console.log("save,.,.", formDataObj);
        self.getProperty().value(JSON.stringify(formDataObj));
        return false;
    }


}