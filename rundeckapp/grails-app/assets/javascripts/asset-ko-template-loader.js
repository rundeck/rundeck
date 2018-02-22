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


if (typeof(ko) !== 'undefined') {
    var baseUrl = appLinks.assetBase + 'ko/component/';
    ko.components.loaders.unshift({
        /**
         * load ko component templates defined like: {assetTemplate: 'path/to/file.html'}
         * from the base path 'html/ko/component' in the assets dir.
         * @param name
         * @param templateConfig
         * @param callback
         */
        loadTemplate: function (name, templateConfig, callback) {
            if (!templateConfig.assetTemplate) {
                callback(null);
                return;
            }

            jQuery.get(
                _genUrl(baseUrl + templateConfig.assetTemplate),
                function (markupString) {
                    markupString = markupString.replace(
                        /\$\$([$\w.()+]+)/g,
                        function (match, g1) {
                            return "<span data-bind=\"text: " + g1 + "\"></span>";
                        }
                    ).replace(
                        /%{2}([^<>]+?)(?:\|\|([^<>\s]+?))?%{2}/g,
                        function (match, g1, g2) {
                            if (g2) {
                                return "<span data-bind=\"messageCodeTemplate: " + g2 + "\">" + g1 + "</span>";
                            } else {
                                return "<span data-bind=\"messageValue: true\">" + g1 + "</span>";
                            }
                        }
                    );
                    ko.components.defaultLoader.loadTemplate(name, markupString, callback);
                }
            );
        }
    })
}