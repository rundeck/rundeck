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


function JobRunFormOptions (data) {
    var self = this
    self.nodeFilter = data.nodeFilter
    self.follow = ko.observable(typeof (data.follow) === 'undefined' || data.follow)
    self.debug = ko.observable(data.debug === true || data.debug === 'true')
    self.loglevel = ko.pureComputed(function () {
        return self.debug() ? 'DEBUG' : 'NORMAL'
    })

    self.canOverrideFilter = ko.observable(data.canOverrideFilter)
    self.changeTargetNodes = ko.observable(data.changeTargetNodes)
    self.nodeOverride = ko.observable(data.nodeOverride || 'cherrypick')
    self.hasSelectedNodes = ko.observable(data.hasSelectedNodes)
    self.hasDynamicFilter = ko.observable(data.hasDynamicFilter)
    self.selectedNodes = ko.observableArray(data.selectedNodes || [])
    self.allNodes = data.allNodes || []
    self.groups = data.groups
    self.grouptags = data.grouptags

    self.selectAllNodes = function () {
        self.selectedNodes([].concat(self.allNodes))
    }
    self.selectNoNodes = function () {
        self.selectedNodes([])
    }

    self.deselectNodes = function (nodes) {
        nodes.forEach((e) => {
            while (self.selectedNodes.indexOf(e) >= 0) {
                self.selectedNodes.splice(self.selectedNodes.indexOf(e), 1)
            }
        })
    }
    self.groupSelectAll = function (dom) {
        let group = jQuery(dom).data('group')
        if (!group || !self.groups || !self.groups[group]) {
            return
        }
        self.selectedNodes(
            self.selectedNodes().concat(
                self.groups[group].filter(function (e) {
                    return self.selectedNodes.indexOf(e) < 0
                })
            )
        )
    }
    self.groupSelectNone = function (dom) {
        let group = jQuery(dom).data('group')
        if (!group || !self.groups || !self.groups[group]) {
            return
        }
        self.deselectNodes(self.groups[group])

    }
    self.isNodeCherrypick = ko.pureComputed(function () {
        return self.nodeOverride() === 'cherrypick'
    })
    self.isCherrypickVisible = ko.pureComputed(function () {
        let cherrypick = self.isNodeCherrypick()
        let changeTargetNodes = self.changeTargetNodes()
        return changeTargetNodes && cherrypick
    })
    self.isNodeFilterVisible = ko.pureComputed(function () {
        let cherrypick = self.isNodeCherrypick()
        let changeTargetNodes = self.changeTargetNodes()
        return changeTargetNodes && !cherrypick
    })


}
