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

//= require knockout.min
//= require knockout-mapping

function RundeckPage(data) {
    "use strict";
    var self = this;
    self.project = ko.observable(data.project);
    self.path = ko.observable(data.path);
    self.baseUrl = ko.observable(data.baseUrl);
    self.basei18nUrl = ko.observable(data.basei18nUrl);
    self.lang = ko.observable(data.lang);
    self.pluginBaseUrl = function (plugin) {
        return self.baseUrl() + '/' + plugin;
    };
    self.pluginBasei18nUrl = function (plugin) {
        return self.basei18nUrl() + '/' + plugin;
    };
    self.onPageLoad = function () {

    };
    function supports_html5_storage() {
        try {
            return 'localStorage' in window && window['localStorage'] !== null;
        } catch (e) {
            return false;
        }
    }
    self.persistUserSetting = function (key, val) {
        if (supports_html5_storage()) {
            localStorage[key] = val;
        }
        // return jQuery.ajax({
        //     url: _genUrl(appLinks.userAddFilterPref, {filterpref: key + "=" + val}),
        //     method: 'POST',
        //     beforeSend: _createAjaxSendTokensHandler('uiplugin_tokens'),
        //     success: function () {
        //         console.log("saved successful for project " );
        //     },
        //     error: function () {
        //         console.log("save failed for project " );
        //     }
        // }).success(_createAjaxReceiveTokensHandler('uiplugin_tokens'));
    };
    self.removeUserSetting = function (key) {
        if (supports_html5_storage()) {
            localStorage.removeItem(key);
        }
    };
    self.loadUserSetting=function (key) {
        if (supports_html5_storage()) {
            return localStorage[key];
        }
        // return jQuery.ajax({
        //     url: _genUrl(appLinks.userLoadUserPrefAll, key && {key: key } || {}),
        //     method: 'GET',
        //     success: function (data) {
        //         console.log("load data pref  " + data);
        //     },
        //     error: function () {
        //         console.log("load failed for user pref ");
        //     }
        // })
    };
    /**
     * Called last at end of page load
     */
    self.onPageLoad = function () {
        jQuery(document).trigger(
            jQuery.Event('load.rundeck.page', {
                relatedTarget: self
            }));
    }
}
jQuery(function () {
    window.rundeckPage = new RundeckPage(loadJsonData('uipluginData'));
});
