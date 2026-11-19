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

    /**
     * latest request sequence number per option name; a response is only applied when its
     * captured sequence still equals the latest one for that option
     */
    self.requestSeq = {};

    /**
     * request handle (e.g. jqXHR) of the in-flight load per option name
     */
    self.inflight = {};

    /**
     * true per option name while its latest load is in flight
     */
    self.pending = {};

    /**
     * true per option name when a load is waiting to be issued: either it was deferred because an
     * option it depends on was loading, or its in-flight load was invalidated by a change of a
     * non-direct upstream option, and no reload has been issued since
     */
    self.needsReload = {};

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
     * Invalidate the in-flight load of an option: bumps its sequence so any late response or error
     * is treated as stale, aborts the request when possible, and clears the loading indicator.
     * @param name option name
     * @returns {boolean} true if a load was in flight
     */
    self.invalidateRequest = function (name) {
        var wasPending = !!self.pending[name];
        self.requestSeq[name] = (self.requestSeq[name] || 0) + 1;
        var req = self.inflight[name];
        delete self.inflight[name];
        delete self.pending[name];
        if (req && typeof(req.abort) === 'function') {
            req.abort();
        }
        if (wasPending && self.options[name]) {
            self.options[name].loading(false);
        }
        return wasPending;
    };

    /**
     * Invalidate the in-flight loads of all transitive dependents of an option (cycle safe).
     * Options deeper than the direct dependents that lose an in-flight load are flagged
     * so they are reloaded once their upstream option finishes loading, if nothing else reloads them.
     * @param name option name
     */
    self.invalidateDependents = function (name) {
        var visited = {};
        visited[name] = true;
        var direct = self.dependents[name] || [];
        var queue = direct.slice();
        while (queue.length > 0) {
            var dep = queue.shift();
            if (visited[dep]) {
                continue;
            }
            visited[dep] = true;
            var wasPending = self.invalidateRequest(dep);
            if (wasPending && direct.indexOf(dep) < 0) {
                self.needsReload[dep] = true;
            }
            queue = queue.concat(self.dependents[dep] || []);
        }
    };

    /**
     * reload dependents of an option whose in-flight load was dropped and not reloaded in the meantime
     * @param name option name that just finished loading
     */
    self.reloadDroppedDependents = function (name) {
        var deps = self.dependents[name] || [];
        for (var i = 0; i < deps.length; i++) {
            if (self.needsReload[deps[i]]) {
                delete self.needsReload[deps[i]];
                self.reloadOptionIfRequirementsMet(deps[i]);
            }
        }
    };

    /**
     * @param name option name
     * @returns {boolean} true if any option this option depends on has a load in flight or deferred
     */
    self.hasPendingDependency = function (name) {
        var deps = self.dependencies[name] || [];
        for (var i = 0; i < deps.length; i++) {
            if (self.pending[deps[i]] || self.needsReload[deps[i]]) {
                return true;
            }
        }
        return false;
    };

    /**
     * Called when the latest load of an option failed: its flagged dependents are not reloaded, since
     * their request would be built from upstream values that could not be refreshed.
     * @param name option name whose load failed
     */
    self.dependencyLoadFailed = function (name) {
        var deps = self.dependents[name] || [];
        for (var i = 0; i < deps.length; i++) {
            if (self.needsReload[deps[i]] && !self.hasPendingDependency(deps[i])) {
                delete self.needsReload[deps[i]];
                self.options[deps[i]].loading(false);
                //its own deferred dependents will not be reloaded either
                self.dependencyLoadFailed(deps[i]);
            }
        }
    };

    /**
     * reload the values for an option by name (calls _loadRemoteOptionValues). Only the response of the
     * latest request for the option is applied; responses and errors of superseded requests are ignored.
     * While an option it depends on is still loading, the request is deferred until that load finishes,
     * so it is never built from a mix of new and outdated upstream values.
     * @param name
     */
    self.loadRemoteOptionValues = function (name) {
        //stop observing option name if doing so
        // self.stopObserving(name);
        var option = self.options[name];
        self.invalidateRequest(name);
        if (self.hasPendingDependency(name)) {
            self.needsReload[name] = true;
            option.loading(true);
            return;
        }
        delete self.needsReload[name];
        var seq = self.requestSeq[name];
        var request = {
            isCurrent: function () {
                return self.requestSeq[name] === seq;
            },
            complete: function () {
                if (self.requestSeq[name] === seq) {
                    delete self.pending[name];
                    delete self.inflight[name];
                }
            },
            failed: function () {
                if (self.requestSeq[name] === seq) {
                    self.dependencyLoadFailed(name);
                }
            }
        };
        self.pending[name] = true;
        var result = self.loader.loadRemoteOptionValues(option, self.options, request);
        if (request.isCurrent()) {
            self.inflight[name] = result;
        }
        result.then(function (data) {
            if (!request.isCurrent()) {
                return;
            }
            request.complete();
            option.loadRemote(data);
            if (data && data.err && data.err.message) {
                //the server reports remote URL failures in a successful response
                self.dependencyLoadFailed(name);
            } else {
                self.reloadDroppedDependents(name);
            }
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
            //drop any in-flight or deferred load: it would be built from unset required values
            self.invalidateRequest(name);
            delete self.needsReload[name];
            self.options[name].loading(false);
            self.dependencyLoadFailed(name);
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
            //responses computed from the previous value of this option are stale for every transitive dependent
            self.invalidateDependents(name);
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
    /**
     * load remote values
     * @param opt Option to load
     * @param options all options, keyed by name, used as request parameters
     * @param request optional staleness handle: isCurrent() tells if this request is still the latest one
     *  for the option, complete() is called when the request finishes while current
     * @returns the jqXHR of the request
     */
    self.loadRemoteOptionValues = function (opt, options, request) {
        var isCurrent = function () {
            return !request || request.isCurrent();
        };
        var finish = function () {
            if (request && request.complete) {
                request.complete();
            }
            //show loading spinner at least for a little while
            setTimeout(function () {
                //a newer request for the same option owns the loading indicator
                if (isCurrent()) {
                    opt.loading(false);
                }
            }, 200);
        };
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
                if (isCurrent()) {
                    finish();
                }
            },
            error: function (jqxhr, status, message) {
                //superseded or aborted requests must not report errors nor touch the loading indicator
                if (status === 'abort' || !isCurrent()) {
                    return;
                }
                finish();
                opt.remoteError({error: "ERROR loading result from Rundeck server: " + status + ": " + message});
                if (request && request.failed) {
                    request.failed();
                }
            }
        });
    }
}
