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

//= require util/testing
//= require menu/joboptions
//= require menu/job-remote-options

jQuery(function () {
    "use strict";
    function mkopt(data) {
        var defdata = {
            name: 'test',
            description: 'x',
            required: true,
            enforced: true,
            values: ['a', 'b'],
            defaultValue: 'a',
            defaultStoragePath: null,
            multivalued: false,
            defaultMultiValues: null,
            delimiter: null,
            selectedMultiValues: null,
            fieldName: 'extra.option.test',
            fieldId: 'a_bc',
            hasError: null,
            hasRemote: false,
            optionDepsMet: true,
            secureInput: false,
            hasExtended: false,
            value: 'test value'
        };
        if (data) {
            jQuery.extend(defdata, data);
        }
        return new Option(defdata);
    }

    function mkval(v, l) {
        "use strict";
        return new OptionVal({value: v, label: l || v});
    }

    new TestHarness("job-remote-optionsTest.js", {
            initTest: function (pref) {
                var control = new RemoteOptionController({loader: 'xyz'});
                this.assert("names length", 0, control.names.length);
                this.assert("options length", 0, Object.keys(control.options).length);
                this.assert("options length", 0, Object.keys(control.dependents).length);
                this.assert("options length", 0, Object.keys(control.dependencies).length);
                this.assert("options length", 0, Object.keys(control.observers).length);
                this.assert("cyclic", false, control.cyclic);
                this.assert("loader", 'xyz', control.loader);
            },
            setupTest: function (pref) {
                var opts = new JobOptions({});
                var localOpt = mkopt({name: 'opt1'});
                var remoteOpt = mkopt({name: 'opt2', hasRemote: true});
                opts.options([
                    localOpt,
                    remoteOpt
                ]);
                var control = new RemoteOptionController({});
                control.setupOptions(opts);
                this.assert("names length", 2, control.names.length);
                this.assert("options length", 2, Object.keys(control.options).length);
                this.assert("remoteLoadCallback", true, remoteOpt.remoteLoadCallback != null);
                this.assert("remoteLoadCallback", null, localOpt.remoteLoadCallback);
            },
            loadData_cyclicTest: function (pref) {
                var opts = new JobOptions({});
                var localOpt = mkopt({name: 'opt1'});
                var remoteOpt = mkopt({name: 'opt2', hasRemote: true});
                opts.options([
                    localOpt,
                    remoteOpt
                ]);
                var control = new RemoteOptionController({});
                control.setupOptions(opts);
                var optConfig = {
                    optionsDependenciesCyclic: true
                };
                control.loadData(optConfig);

                this.assert("cyclic", true, control.cyclic);
            },
            loadData_basicTest: function (pref) {
                var opts = new JobOptions({});
                var localOpt = mkopt({name: 'opt1'});
                var remoteOpt = mkopt({name: 'opt2', hasRemote: true});
                opts.options([
                    localOpt,
                    remoteOpt
                ]);
                var control = new RemoteOptionController({});
                control.setupOptions(opts);
                var optConfig = {
                    options: {
                        opt1: {
                            optionDependencies: [],
                            optionDeps: ['opt2'],
                            hasUrl: false
                        },
                        opt2: {
                            optionDependencies: ['opt1'],
                            optionDeps: [],
                            hasUrl: true
                        }
                    }
                };
                control.loadData(optConfig);

                this.assert("cyclic", false, control.cyclic);
                this.assert("dependencies", {opt1: [], opt2: ['opt1']}, control.dependencies);
                this.assert("dependents", {opt1: ['opt2'], opt2: []}, control.dependents);

                this.assert("autoreload1", true, control.shouldAutoReload('opt1'));
                this.assert("loadonstart1", false, control.shouldLoadOnStart('opt1'));

                this.assert("autoreload2", true, control.shouldAutoReload('opt2'));
                this.assert("loadonstart2", false, control.shouldLoadOnStart('opt2'));
            },
            reloadOptionIfRequirementsMet_missingRequired_Test: function (pref) {
                var opts = new JobOptions({});
                var opt1 = mkopt({name: 'opt1', required: true, value: ''});
                var opt2 = mkopt({name: 'opt2', required: true, value: 'xyz'});
                var opt3 = mkopt({name: 'opt3', hasRemote: true, value: ''});
                opts.options([
                    opt1,
                    opt2,
                    opt3
                ]);
                var control = new RemoteOptionController({});
                control.setupOptions(opts);
                var optConfig = {
                    options: {
                        opt1: {
                            optionDependencies: [],
                            optionDeps: ['opt3'],
                            optionAutoReload: false,
                            loadonstart: true,
                            hasUrl: false
                        },
                        opt2: {
                            optionDependencies: [],
                            optionDeps: ['opt3'],
                            optionAutoReload: false,
                            loadonstart: true,
                            hasUrl: false
                        },
                        opt3: {
                            optionDependencies: ['opt1', 'opt2'],
                            optionDeps: [],
                            optionAutoReload: true,
                            loadonstart: true,
                            hasUrl: true
                        }
                    }
                };
                control.loadData(optConfig);
                control.reloadOptionIfRequirementsMet('opt3');
                //opt2 should have error
                this.assert("should see dependency error", {message: 'options.remote.dependency.missing.required'}, opt3.remoteError());
            },
            reloadOptionIfRequirementsMet_notmissingRequired_Test: function (pref) {
                var opts = new JobOptions({});
                var opt1 = mkopt({name: 'opt1', required: true, value: 'zzz'});
                var opt2 = mkopt({name: 'opt2', required: true, value: 'xyz'});
                var opt3 = mkopt({name: 'opt3', hasRemote: true, value: ''});
                opts.options([
                    opt1,
                    opt2,
                    opt3
                ]);
                var toload = [];
                var didcallback = false;
                var loader = {
                    loadRemoteOptionValues: function (option, opts) {
                        toload.push(option.name());
                        return {
                            then: function (func) {
                                didcallback = true;
                            }
                        };
                    }
                };
                var control = new RemoteOptionController({loader: loader});
                control.setupOptions(opts);
                var optConfig = {
                    options: {
                        opt1: {
                            optionDependencies: [],
                            optionDeps: ['opt3'],
                            optionAutoReload: false,
                            loadonstart: true,
                            hasUrl: false
                        },
                        opt2: {
                            optionDependencies: [],
                            optionDeps: ['opt3'],
                            optionAutoReload: false,
                            loadonstart: true,
                            hasUrl: false
                        },
                        opt3: {
                            optionDependencies: ['opt1', 'opt2'],
                            optionDeps: [],
                            optionAutoReload: true,
                            loadonstart: true,
                            hasUrl: true
                        }
                    }
                };
                control.loadData(optConfig);
                control.reloadOptionIfRequirementsMet('opt3');
                //opt2 should have error
                this.assert("should load option", ['opt3'], toload);
                this.assert("should do callback", true, didcallback);
            },
        createOption_singleValue_enforced_autodefault_Test: function (pref) {

            var opt1 = mkopt({
                name: 'opt1',
                required: true,
                values: ['abc'],
                enforced: true,
                defaultValue: null,
                value: ''
            });

            this.assert("option value should be set by single allowed value", 'abc', opt1.value());
        },
        createOption_singleValue_enforced_withdefault_Test: function (pref) {

            var opt1 = mkopt({
                name: 'opt1',
                required: true,
                values: ['abc'],
                enforced: true,
                defaultValue: 'abc',
                value: ''
            });

            this.assert("option value should be set by single allowed value", 'abc', opt1.value());
        },
        createOption_singleValue_notenforced_withdefault_Test: function (pref) {

            var opt1 = mkopt({
                name: 'opt1',
                required: true,
                values: ['abc'],
                enforced: false,
                defaultValue: 'abc',
                value: ''
            });

            this.assert("option value should be set by default value", 'abc', opt1.value());
        },
        reloadOptionIfRequirementsMet_singleValue_autoDefault_EnforcedRequired_Test: function (pref) {
            var opts = new JobOptions({});
            var opt1 = mkopt({
                name: 'opt1',
                required: true,
                values: ['abc'],
                enforced: true,
                defaultValue: null,
                value: ''
            });
            var opt3 = mkopt({name: 'opt3', hasRemote: true, value: ''});
            opts.options([
                opt1,
                opt3
            ]);
            var toload = [];
            var didcallback = false;
            var loader = {
                loadRemoteOptionValues: function (option, opts) {
                    toload.push(option.name());
                    return {
                        then: function (func) {
                            didcallback = true;
                        }
                    };
                }
            };
            var control = new RemoteOptionController({loader: loader});
            control.setupOptions(opts);
            var optConfig = {
                options: {
                    opt1: {
                        optionDependencies: [],
                        optionDeps: ['opt3'],
                        optionAutoReload: false,
                        loadonstart: true,
                        hasUrl: false
                    },

                    opt3: {
                        optionDependencies: ['opt1'],
                        optionDeps: [],
                        optionAutoReload: true,
                        loadonstart: true,
                        hasUrl: true
                    }
                }
            };
            control.loadData(optConfig);
            control.reloadOptionIfRequirementsMet('opt3');
            //opt3 should not have error
            this.assert("should load option", ['opt3'], toload);
            this.assert("should do callback", true, didcallback);
        },
        reloadOptionIfRequirementsMet_singleValue_WithDefault_NotEnforcedRequired_Test: function (pref) {
            var opts = new JobOptions({});
            var opt1 = mkopt({
                name: 'opt1',
                required: true,
                values: ['abc'],
                enforced: false,
                defaultValue: 'abc',
                value: ''
            });
            var opt3 = mkopt({name: 'opt3', hasRemote: true, value: '', defaultValue:''});
            opts.options([
                opt1,
                opt3
            ]);
            var toload = [];
            var didcallback = false;
            var loader = {
                loadRemoteOptionValues: function (option, opts) {
                    toload.push(option.name());
                    return {
                        then: function (func) {
                            didcallback = true;
                        }
                    };
                }
            };
            var control = new RemoteOptionController({loader: loader});
            control.setupOptions(opts);
            var optConfig = {
                options: {
                    opt1: {
                        optionDependencies: [],
                        optionDeps: ['opt3'],
                        optionAutoReload: false,
                        loadonstart: true,
                        hasUrl: false
                    },

                    opt3: {
                        optionDependencies: ['opt1'],
                        optionDeps: [],
                        optionAutoReload: true,
                        loadonstart: true,
                        hasUrl: true
                    }
                }
            };
            control.loadData(optConfig);
            control.reloadOptionIfRequirementsMet('opt3');
            //opt3 should not have error
            this.assert("option should have value", 'abc', opt1.value());
            this.assert("should load option", ['opt3'], toload);
            this.assert("should do callback", true, didcallback);
        },
        reloadOptionIfRequirementsMet_singleValue_NotEnforcedRequired_Test: function (pref) {
            var opts = new JobOptions({});
            var opt1 = mkopt({
                name: 'opt1',
                required: true,
                values: ['abc'],
                enforced: false,
                defaultValue: 'abc',
                value: 'abc'
            });
            var opt3 = mkopt({name: 'opt3', hasRemote: true, value: '', defaultValue:'',values:null});
            opts.options([
                opt1,
                opt3
            ]);
            var toload = [];
            var didcallback = false;
            var loader = {
                loadRemoteOptionValues: function (option, opts) {
                    toload.push(option.name());
                    return {
                        then: function (func) {
                            didcallback = true;
                        }
                    };
                }
            };
            var control = new RemoteOptionController({loader: loader});
            control.setupOptions(opts);
            var optConfig = {
                options: {
                    opt1: {
                        optionDependencies: [],
                        optionDeps: ['opt3'],
                        optionAutoReload: false,
                        loadonstart: true,
                        hasUrl: false
                    },

                    opt3: {
                        optionDependencies: ['opt1'],
                        optionDeps: [],
                        optionAutoReload: true,
                        loadonstart: true,
                        hasUrl: true
                    }
                }
            };
            control.loadData(optConfig);
            control.reloadOptionIfRequirementsMet('opt3');
            //opt3 should not have error
            this.assert("option should have value", 'abc', opt1.value());
            this.assert("should load option", ['opt3'], toload);
            this.assert("should do callback", true, didcallback);
        },
        reloadOptionIfRequirementsMet_nullDeps_Test: function (pref) {
            var opts = new JobOptions({});
            var opt1 = mkopt({name: 'opt1', required: true, value: 'zzz'});
            var opt2 = mkopt({name: 'opt2', required: true, value: 'xyz'});
            var opt3 = mkopt({name: 'opt3', hasRemote: true, value: ''});
            opts.options([
                opt1,
                opt2,
                opt3
            ]);
            var toload = [];
            var didcallback = false;
            var loader = {
                loadRemoteOptionValues: function (option, opts) {
                    toload.push(option.name());
                    return {
                        then: function (func) {
                            didcallback = true;
                        }
                    };
                }
            };
            var control = new RemoteOptionController({loader: loader});
            control.setupOptions(opts);
            var optConfig = {
                options: {
                    opt1: {
                        //optionDependencies: [],
                        optionDeps: ['opt3'],
                        optionAutoReload: false,
                        loadonstart: true,
                        hasUrl: false
                    },
                    opt2: {
                        optionDependencies: [],
                        optionDeps: ['opt3'],
                        optionAutoReload: false,
                        loadonstart: true,
                        hasUrl: false
                    },
                    opt3: {
                        optionDependencies: ['opt1', 'opt2'],
                        optionDeps: [],
                        optionAutoReload: true,
                        loadonstart: true,
                        hasUrl: true
                    }
                }
            };
            control.loadData(optConfig);

            this.assert("expect undefined dependencies", undefined, control.dependencies['opt1']);
            
            control.reloadOptionIfRequirementsMet('opt1');
            //opt2 should have error
            this.assert("should load option", ['opt1'], toload);
            this.assert("should do callback", true, didcallback);
        },
            optionValueChanged_Test: function (pref) {
                var self = this;
                this.testMatrix("optionValueChanged({0})", [
                    [{cyclic: false, deps: ['a', 'b']}, ['a', 'b']],
                    [{cyclic: true, deps: ['a', 'b']}, []],
                    [{cyclic: false, deps: []}, []]
                ], function (data) {
                    var control = new RemoteOptionController({loader: 'loader'});
                    var toreload = [];
                    control.reloadOptionIfRequirementsMet = function (name) {
                        toreload.push(name);
                    };
                    control.cyclic = data.cyclic;
                    control.dependents['opt3'] = data.deps;

                    control.optionValueChanged('opt3', 'xyz');
                    return toreload;
                });

            },
            unsubscribeAll_Test: function () {
                var self = this;

                var control = new RemoteOptionController({loader: 'loader'});
                var diddispose = 0;
                var doit = function () {
                    diddispose++;
                };
                control.observers = {
                    a: {
                        dispose: doit
                    }, b: {
                        dispose: doit
                    }
                };
                control.unsubscribeAll();
                self.assert('should unsubscribe all', 2, diddispose);

            }
        }
    )
});