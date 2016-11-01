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

/**
 * Manages cascading reloads of remote option values based on a dependency graph
 */
function RemoteOptionController(data) {
    "use strict";
    var self = this;
    self.loader = data.loader;
    /**
     * container for array of Options, keyed by option name
     */
    self.options = {};
    /**
     * container for array of dependent option names, keyed by option name
     */
    self.dependents = {};
    /**
     * container for array of depdency option names, keyed by option name
     */
    self.dependencies = {};
    /**
     * list of option names
     */
    self.names = [];

    /**
     * container of observer subscriptions, keyed by option name
     */
    self.observers = {};

    /**
     * indicates cyclic dependencies
     */
    self.cyclic = false;

    function emptyValue(val) {
        return (!val || val === '');
    }
    /**
     * Setup dependencies using the loaded options
     * @param joboptions
     */
    self.setupOptions = function (joboptions) {
        ko.utils.arrayForEach(joboptions.options(), function (opt) {
            self.addOption(opt);
            if (opt.hasRemote()) {
                opt.setReloadCallback(self.reloadOptionIfRequirementsMet);
            }
        });
    };
    /**
     * register an option with parameters used for Ajax reload of the field (used to call _loadRemoteOptionValues
     * function)
     * @param opt Option object
     */
    self.addOption = function (opt) {
        self.options[opt.name()] = opt;
        self.names.push(opt.name());
    };

    /**
     * Non remote-values option
     * @param name
     */
    self.addLocalOption = function (name) {
        self.names.push(name);
    };

    /**
     * reload the values for an option by name (calls _loadRemoteOptionValues)
     * @param name
     */
    self.loadRemoteOptionValues = function (name) {
        //stop observing option name if doing so
        // self.stopObserving(name);
        var option = self.options[name];
        self.loader.loadRemoteOptionValues(option, self.options).then(function (data) {
            option.loadRemote(data);
        });
    };


    /**
     * define dependent option names for an option
     * @param name
     * @param depsArr
     */
    self.addOptionDeps = function (name, depsArr) {
        self.dependents[name] = depsArr;
    };

    /**
     * define dependency option names for an option
     * @param name
     * @param depsArr
     */
    self.addOptionDependencies = function (name, depsArr) {
        self.dependencies[name] = depsArr;
    };

    /**
     * reload the option values for an option if all required dependencies are set
     * @param name
     */
    self.reloadOptionIfRequirementsMet = function (name) {
        var skip = false;

        // reload iff: all of its required dependencies have a value
        var missing = [];
        if(self.dependencies[name]) {
            for (var j = 0; j < self.dependencies[name].length; j++) {
                var dependencyName = self.dependencies[name][j];
                var option = self.options[dependencyName];
                if (!option.value() && option.required()) {
                    skip = true;
                    missing.push(dependencyName);
                }
            }
        }
        if (!skip) {
            self.loadRemoteOptionValues(name);
        } else if (self.options[name]) {
            self.options[name].remoteError({
                message: message("options.remote.dependency.missing.required", [name, missing.join(", ")])
            });
        }
    };

    /**
     * notify that a value changed for an option by name, will reload dependents if any
     * @param name
     * @param value
     */
    self.optionValueChanged = function (name, value) {
        //trigger reload
        if (self.dependents[name] && !self.cyclic) {
            for (var i = 0; i < self.dependents[name].length; i++) {
                var dependentName = self.dependents[name][i];
                self.reloadOptionIfRequirementsMet(dependentName);
            }
        }
    };
    /**
     * True if the option should force dependent options to load at start
     * @param name
     * @returns {*}
     */
    self.shouldAutoReload = function (name) {
        var option = self.options[name];
        if (option.hasRemote()) {
            return !!(!self.cyclic && (self.dependents[name] || !emptyValue(option.value())));
        } else {
            return !!((self.dependents[name] && option.enforced()) || !emptyValue(option.value()));
        }
    };
    /**
     * True if The option should load remote values at start
     * @param name
     * @returns {*|boolean}
     */
    self.shouldLoadOnStart = function (name) {
        return self.options[name].hasRemote() && (!self.dependencies[name] || self.cyclic);
    };

    /**
     * Auto reload dependent options if necessary
     * @param name
     * @returns {boolean}
     */
    self.doOptionAutoReload = function (name) {
        if (self.shouldAutoReload(name)) {
            //trigger change immediately
            var value = self.options[name].value();
            self.optionValueChanged(name, value);
            return true;
        }
        return false;
    };

    /**
     * load remote option dataset from json data
     * @param data
     */
    self.loadData = function (data) {
        if(!data){
            return;
        }
        if (data['optionsDependenciesCyclic']) {
            self.cyclic = data['optionsDependenciesCyclic'];
        }
        for (var opt in data.options) {
            var params = data.options[opt];
            if (params['optionDependencies']) {
                self.addOptionDependencies(opt, params['optionDependencies']);
            }
            if (params['optionDeps']) {
                self.addOptionDeps(opt, params['optionDeps']);
            }

        }
    };

    self.unsubscribeAll = function () {
        "use strict";
        for(var p in self.observers){
            self.observers[p].dispose();
        }
        self.observers={};
    };
    /**
     * starts observing changes for option field by name
     * @param name
     */
    self.observeChangesFor = function (name) {
        // this.stopObserving(name);
        //observe field value change and trigger reloads
        self.observers[name] = self.options[name].value.subscribe(function (newval) {
            self.optionValueChanged(name, newval);
        });
    };
    /**
     * begin by loading on start values, reloading autoreload values, and then observe
     * changes.
     */
    self.begin = function () {
        for (var i = 0; i < self.names.length; i++) {
            var name = self.names[i];
            if (self.shouldLoadOnStart(name)) {
                self.loadRemoteOptionValues(name);
            }
            self.doOptionAutoReload(self.names[i]);
        }

        for (var i = 0; i < self.names.length; i++) {
            self.observeChangesFor(self.names[i]);
        }
    };

}

function RemoteOptionLoader(data) {
    "use strict";
    var self = this;
    self.url = data.url;
    self.fieldPrefix = data.fieldPrefix;
    self.id = data.id;
    //load remote values
    self.loadRemoteOptionValues = function (opt, options) {
        opt.loading(true);
        var params = {option: opt.name(), selectedvalue: opt.value(), id: self.id};
        //
        if (null != options) {
            for (var xopt in options) {
                if (xopt != opt.name()) {
                    params[self.fieldPrefix + xopt] = options[xopt].value();
                }
            }
        }
        return jQuery.ajax({
            method: 'GET',
            type: 'json',
            url: _genUrl(self.url, params),
            success: function (data, status, jqxhr) {
                //show loading spinner at least for a little while
                setTimeout(opt.loading.curry(false),200);
            },
            error: function (jqxhr, status, message) {
                setTimeout(opt.loading.curry(false),200);
                opt.remoteError({error: "ERROR loading result from Rundeck server: " + status + ": " + message});
            }
        });
    }
}