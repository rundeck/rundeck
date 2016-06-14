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
     * indicates if observing was started
     */
    self.observing = false;
    /**
     * container of observer subscriptions, keyed by option name
     */
    self.observers = {};
    /**
     * container of true/false whether the option should automatically trigger reload of dependents at startup, keyed
     * by option name
     */
    self.autoreload = {};
    /**
     * container of true/false whether the option should automatically load at startup, keyed by option name
     */
    self.loadonstart = {};
    /**
     * field observer frequency (in seconds)
     */
    self.observeFreq = 0.5;
    /**
     * indicates cyclic dependencies
     */
    self.cyclic = false;

    /**
     * Setup dependencies using the loaded options
     * @param joboptions
     */
    self.setupOptions = function (joboptions) {
        ko.utils.arrayForEach(joboptions.options(), function (opt) {
            self.addOption(opt);
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
        self.loader.loadRemoteOptionValues(option,self.options).then(function (data) {
            console.log("loaded data",data);
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
     * notify that a value changed for an option by name, will reload dependents if any
     * @param name
     * @param value
     */
    self.optionValueChanged = function (name, value) {
        //trigger reload
        if (self.dependents[name] && !self.cyclic) {
            for (var i = 0; i < self.dependents[name].length; i++) {
                var dependentName = self.dependents[name][i];
                var skip = false;

                // determine if we should reload the dependent option
                // do not reload iff: any its dependencies does not have value, and is required
                for (var j = 0; j < self.dependencies[dependentName].length; j++) {
                    var dependencyName = self.dependencies[dependentName][j];
                    var option = self.options[dependencyName];
                    if (!option.value() && option.required()) {
                        skip = true;
                        break;
                    }
                }
                if (!skip) {
                    self.loadRemoteOptionValues(dependentName);
                }
            }
        }
    };

    //
    // self.observeMultiCheckbox = function (name, e) {
    //     var roc = this;
    //     if (!$(e)) {
    //         throw "not found: " + e;
    //     }
    //     Element.observe(e, 'change', function (evt, value) {
    //         roc.optionValueChanged(name, value);
    //     });
    // };
    //
    // self.setFieldMultiId = function (name, id) {
    //
    //     if (self.observing) {
    //         var found = $(id).select("input[type='checkbox']");
    //         if (found) {
    //             found.each(self.observeMultiCheckbox.bind(this, name));
    //         }
    //         var auto = self.doOptionAutoReload(name);
    //         if (!auto && self.options[name]) {
    //             //if already observing, and value now differs, trigger reload
    //             self.optionValueChanged(name, '');
    //         }
    //     }
    // };


    self.doOptionAutoReload = function (name) {
        if (self.autoreload[name]) {
            //trigger change immediately
            var value = self.options[name].value();
            self.optionValueChanged(name, value);
            return true;
        }
        return false;
    };

    /**
     * set autoreload value for the option
     * @param name
     * @param value
     */
    self.setOptionAutoReload = function (name, value) {
        self.autoreload[name] = value;
    };

    /**
     * load remote option dataset from json data
     * @param data
     */
    self.loadData = function (data) {
        for (var opt in data) {
            var params = data[opt];
            if (params['optionDependencies']) {
                self.addOptionDependencies(opt, params['optionDependencies']);
            }
            if (params['optionDeps']) {
                self.addOptionDeps(opt, params['optionDeps']);
            }
            if (params['optionAutoReload']) {
                self.setOptionAutoReload(opt, params['optionAutoReload']);
            }
            if (params['hasUrl']) {
                if (params['loadonstart']) {
                    self.loadonstart[opt] = true;
                }
                if (params['optionAutoReload']) {
                    self.setOptionAutoReload(opt, true);
                }
                // if (params['fieldMultiId']) {
                //     self.setFieldMultiId(opt, params['fieldMultiId']);
                // }
                // } else {
                //     self.addLocalOption(opt);
            }
        }
    };

    self.unsubscribeAll = function () {
        //XXX
        console.log("unsubscribeAll not implemented");
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
     * starts observing all option fields for changes
     */
    self.observeChanges = function () {
        //TODO: observe
        for (var i = 0; i < self.names.length; i++) {
            var name = self.names[i];
            if (self.loadonstart[name]) {
                self.loadRemoteOptionValues(name);
            }
        }
        for (var i = 0; i < self.names.length; i++) {
            self.observeChangesFor(self.names[i]);
        }
        self.observing = true;
        for (var i = 0; i < self.names.length; i++) {
            self.doOptionAutoReload(self.names[i]);
        }
    };

    /**
     * Setup ko subscriptions and begin autoloading
     */
    self.begin = function () {
        self.observeChanges();
    };
}

function RemoteOptionLoader(data) {
    "use strict";
    var self = this;
    self.url = data.url;
    self.fieldPrefix = data.fieldPrefix;
    //load remote values
    self.loadRemoteOptionValues = function (opt,options) {
        opt.loading(true);
        var params = {option: opt.name(), selectedvalue: opt.value()};
        //
        if(null!=options){
            for(var xopt in options){
                if(xopt!=opt.name()){
                    params[self.fieldPrefix+xopt]=options[xopt].value();
                }
            }
        }
        return jQuery.ajax({
            method: 'GET',
            type:'json',
            url: _genUrl(self.url, params),
            success: function (data, status, jqxhr) {
                // self.addReloadRemoteOptionValues(opt,data);
                opt.loading(false);
            },
            failure: function (data, status, jqxhr) {
                opt.loading(false);
                alert("error: " + status);
            }
        });
    }
}