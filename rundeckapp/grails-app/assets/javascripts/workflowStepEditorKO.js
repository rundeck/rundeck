//= require knockout.min
//= require knockout-mapping
/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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
 * Created with IntelliJ IDEA.
 * User: greg
 * Date: 9/17/14
 * Time: 12:01 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 * Manages editing script step entries, using a unique key for each entry
 * @constructor
 */
function WorkflowEditor() {
    var self = this;
    /**
     * Steps keyed by identifier string
     * @type {*}
     */
    self.steps = ko.observable({});

    /**
     * Return the step given the key
     * @param key
     * @returns {*}
     */
    self.step = function (key) {
        return self.steps()[key];
    };

    /**
     * Bind a new script step to a key and apply Knockout bindings to the element
     * @param key unique key
     * @param elemId dom element ID
     * @param data binding data
     */
    self.bindKey = function (key, elemId, data) {
        var step = new ScriptStep(data);
        self.steps()[key] = step;
        ko.applyBindings(step, document.getElementById(elemId));
    };
}
/**
 * Manage preview string for script invocation
 * @param data
 * @constructor
 */
function ScriptStep(data) {
    var self = this;

    /**
     * Invocation string
     * @type {*}
     */
    self.invocationString = ko.observable('');

    self.fileExtension = ko.observable('');

    self.args = ko.observable('');

    self.argsQuoted = ko.observable(false);

    self.argStringAsQuoted = ko.computed(function () {
        var isq = self.argsQuoted() ? '"' : '';
        return self.args() ? isq + self.args() + isq : '';
    });

    self.fileExtensionDotted = ko.computed(function () {
        var ext = self.fileExtension();
        return ext? (ext.charAt(0)=='.'?ext:'.'+ext):'';
    });
    self.scriptfileText = ko.computed(function () {
        return self.fileExtensionDotted() ? "scriptfile" + self.fileExtensionDotted() : 'scriptfile';
    });
    self.argStringAsQuotedWithScriptfile = ko.computed(function () {
        var isq = self.argsQuoted() ? '"' : '';
        return isq
            + '<em>' + self.scriptfileText() +'</em> '
            + self.args()
            + isq;
    });

    /**
     * Return the preview HTML for the script invocation.
     * @type {*}
     */
    self.invocationPreviewHtml = ko.computed(function () {
        var text = '';
        if (self.invocationString() && self.invocationString().indexOf('${scriptfile}') >= 0) {
            text += self.invocationString().split('\$\{scriptfile\}').join('<em>' + self.scriptfileText() +'</em>') + ' ' + self.argStringAsQuoted();
        } else if (self.invocationString()) {
            text += self.invocationString() + ' ' + self.argStringAsQuotedWithScriptfile();
        } else {
            text += self.argStringAsQuotedWithScriptfile();
        }
        return text;
    });

    //bind in the input data
    ko.mapping.fromJS(data, {}, this);
}
