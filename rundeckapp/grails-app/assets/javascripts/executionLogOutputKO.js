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
//= require knockout-foreachprop
//= require knockout-mapping

function LogViewOptions (data) {
    const self = this
    self.styleMode = ko.observable(data.styleMode || 'normal')
    self.styleModesAvailable = ko.observable(data.styleModesAvailable || ['normal', 'black'])
    self.followmode = ko.observable(data.followmode || 'tail')
    self.showNodeCol = ko.observable(data.showNodeCol !== undefined ? data.showNodeCol : true)
    self.showStep = ko.observable(data.showStep !== undefined ? data.showStep : true)
    self.showTime = ko.observable(data.showTime !== undefined ? data.showTime : true)
    self.showAnsicolor = ko.observable(data.showAnsicolor !== undefined ? data.showAnsicolor : true)
    self.wrapLines = ko.observable(data.wrapLines !== undefined ? data.wrapLines : true)
    self.showNodeInset = ko.observable(data.showNodeInset !== undefined ? data.showNodeInset : true)
    self.followmodeNode =
        ko.pureComputed({
            read: () => self.followmode() === 'node',
            write: (val) => self.followmode(val ? 'node' : 'tail'),
            owner: self
        })
}

function LogOutput (data) {
    const self = this
    self.execFollowingControl = data.followControl
    self.options = new LogViewOptions(data.options || {})
    self.running = ko.observable(false)
    self.loadingFile = ko.observable(false)
    self.paused = ko.observable(false)
    self.fileLoadMessage = ko.observable()
    self.fileLoadError = ko.observable()
    self.fileLoadPercentage = ko.observable(0)
    self.fileLoadText = ko.pureComputed(function () {
        const msg = self.fileLoadMessage()
        const pct = self.fileLoadPercentage()
        return (msg ? msg : '') + (pct ? `${pct}%` : '')
    })
    self.fileLoadPercentWidth = ko.pureComputed(function () {
        const pct = self.fileLoadPercentage() || 0
        return `${pct}%`
    })

    self.pauseLoading = function () {
        self.fileLoadMessage("Stopping... ")
        self.paused(true);
        self.execFollowingControl.pauseLoading(function () {
            self.running(false)
            self.fileLoadMessage("Loading Stopped. ")
        })
    }
    self.resumeLoading = function () {
        self.fileLoadMessage("Resuming... ")
        self.paused(false);
        self.running(true)
        self.execFollowingControl.resumeLoading()
    }
    self.beginFollowingOutput = function (id) {

        if (!self.loadingFile() && !self.running() && !self.execFollowingControl.isCompleted(id)) {
            self.running(true)
            self.execFollowingControl.onLoadComplete = function () {
                self.running(false)
                self.paused(false);
            }
            self.execFollowingControl.beginFollowingOutput(id)
        }
    }

    /**
     * TODO: directly control dom view with observable values
     */
    if (data.bindFollowControl && self.execFollowingControl) {
        self.options.showTime.subscribe((value) => self.execFollowingControl.setColTime(value))
        self.options.showNodeCol.subscribe((value) => self.execFollowingControl.setColNode(value))
        self.options.showStep.subscribe((value) => self.execFollowingControl.setColStep(value))
        self.options.showAnsicolor.subscribe((value) => _setAnsiColor(value))
        self.options.wrapLines.subscribe((value) => self.execFollowingControl.setLogWrap(value))
        self.options.followmode.subscribe((value) => self.execFollowingControl.resetMode(value))
        self.execFollowingControl.onLoadingFile = self.loadingFile
        self.execFollowingControl.onFileloadMessage = self.fileLoadMessage
        self.execFollowingControl.onFileloadPercentage = self.fileLoadPercentage
        self.execFollowingControl.onFileloadError= self.fileLoadError
    }
}
