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
    self.scriptSteps = ko.observable({});

    /**
     * Return the step given the key
     * @param key
     * @returns {*}
     */
    self.step = function (key) {
        return self.scriptSteps()[key];
    };

    self.filterPlugins = ko.observableArray([]);
    self.loadStepFilterPlugins = function (data) {
        //bind in the input data
        ko.mapping.fromJS({filterPlugins: data}, {
            filterPlugins: {
                key: function (data) {
                    return ko.utils.unwrapObservable(data.type);
                },
                create: function (options) {
                    return new StepFilterPlugin(options.data);
                }
            }
        }, self);
    };

    self.modalStepFilters = ko.observable();
    self.modalFilterEdit = ko.observable();
    self.modalFilterEditNewType = ko.observable();
    self.addFilterPopup = function (step) {
        "use strict";
        self.modalStepFilters(step);
        //show modal dialog to add a filter to the given step
        jQuery('#addLogFilterPluginModal').modal('show');
    };
    //use selected filter plugin to add a filter for the current modal step
    self.addSelectedFilterPopup = function (filter) {
        "use strict";
        jQuery('#addLogFilterPluginModal').modal('hide');
        self.editFilterPopup(self.modalStepFilters(), null, filter.type());
    };


    self.editFilterPopup = function (step, stepfilter, newtype) {
        "use strict";
        self.modalStepFilters(step);
        self.modalFilterEdit(stepfilter);
        self.modalFilterEditNewType(newtype);
        //show modal dialog to add a filter to the given step

        var params = {num: step.num()};
        if (getCurSEID()) {
            params['scheduledExecutionId'] = getCurSEID();
        }
        if (stepfilter) {
            params.index = stepfilter.index();
        } else {
            params.newfiltertype = newtype;
        }
        jQuery('#editLogFilterPluginModalForm').load(
            _genUrl(appLinks.workflowEditStepFilter, params),
            function (data) {
                jQuery('#editLogFilterPluginModal').modal('show');
            }
        );
    };

    /**
     * Bind a new script step to a key and apply Knockout bindings to the element
     * @param key unique key
     * @param elemId dom element ID
     * @param data binding data
     */
    self.bindScriptStepKey = function (key, elemId, data) {
        var step = new ScriptStep(data);
        self.scriptSteps()[key] = step;
        ko.applyBindings(step, document.getElementById(elemId));
    };
    /**
     * Filters for steps by identifier string
     * @type {*}
     */
    self.stepFilters = ko.observable({});


    /**
     * Return the step filters the key
     * @param key
     * @returns {*}
     */
    self.stepFilter = function (key) {
        return self.stepFilters()[key];
    };

    self.bindStepFilters = function (key, elemId, data) {
        "use strict";
        var filters = new WorkflowStep(data);
        self.stepFilters()[key] = filters;
        ko.applyBindings(filters, document.getElementById(elemId));
    };

    self.reset = function () {
        "use strict";
        self.scriptSteps({});
        self.stepFilters({});
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

    self.guessAceMode = ko.computed(function () {
        if (self.invocationString().startsWith('powershell') || self.fileExtensionDotted() === '.ps') {
            return 'powershell'
        }
        if (self.invocationString().startsWith('cmd.exe') || self.fileExtensionDotted() === '.bat') {
            return 'batchfile'
        }
        return 'sh';
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
/**
 * plugin description info
 * @param data
 * @constructor
 */
function StepFilterPlugin(data) {
    "use strict";
    var self = this;
    self.type = ko.observable(data.type);
    self.title = ko.observable(data.title);
    self.description = ko.observable(data.description);
    self.selected = ko.observable(false);
    self.descriptionFirstLine = ko.computed(function () {
        var desc = self.description();
        if (desc) {
            return desc.indexOf('\n') ? desc.substring(0, desc.indexOf('\n')) : desc;
        }
        return desc;
    });
    ko.mapping.fromJS(data, {}, this);
}
/**
 * A single filter instance,
 * @param data
 * @constructor
 */
function StepFilter(data) {
    "use strict";
    var self = this;
    self.type = ko.observable(data.type);
    self.config = ko.observable(data.config);
    self.index = ko.observable(data.index);
    ko.mapping.fromJS(data, {}, this);
}
/**
 * A list of filters for a step
 * @param data
 * @constructor
 */
function WorkflowStep(data) {
    "use strict";
    var self = this;
    self.num = ko.observable(data.num);
    self.filters = ko.observableArray([]);
    self.addFilter = function (type, config) {
        self.filters.push(new StepFilter({type: type, config: config}));
    };
    self.addFilterPopup = function () {
        workflowEditor.addFilterPopup(self);
    };
    self.addFilterTest = function () {
        self.addFilter('mask-passwords', {});
    };
    self.editFilter = function (filter) {
        workflowEditor.editFilterPopup(self, filter);
    };

    //bind in the input data
    ko.mapping.fromJS(data, {
        filters: {
            // key: function (data) {
            //     return ko.utils.unwrapObservable(data.stepctx);
            // },
            create: function (options) {
                return new StepFilter(options.data);
            }
        }
    }, this);
}