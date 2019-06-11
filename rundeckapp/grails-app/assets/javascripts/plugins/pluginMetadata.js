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



/**
 * plugin description info
 * @param data
 * @constructor
 */
function PluginMetadata(data) {
    "use strict";
    var self = this;
    self.type = ko.observable(data.type);
    self.title = ko.observable(data.title);
    self.description = ko.observable(data.description);
    self.iconSrc = ko.observable(data.iconSrc);
    self.providerMeta = ko.observable(data.providerMeta)
    self.selected = ko.observable(false);
    self.glyphicon  = ko.computed(function () {
        return self.providerMeta() && self.providerMeta().glyphicon
    })
    self.faicon = ko.computed(function () {
        return self.providerMeta() && self.providerMeta().faicon
    })
    self.fabicon = ko.computed(function () {
        return self.providerMeta() && self.providerMeta().fabicon
    })
    self.descriptionFirstLine = ko.computed(function () {
        var desc = self.description();
        if (desc) {
            return desc.indexOf('\n') > 0 ? desc.substring(0, desc.indexOf('\n')) : desc;
        }
        return desc;
    });
    ko.mapping.fromJS(data, {}, this);
}
