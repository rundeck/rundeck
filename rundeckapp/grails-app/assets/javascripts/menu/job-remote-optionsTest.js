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


    /**
     * Creates a loader whose requests are deferred: each call is recorded with a snapshot of the
     * other options' values, and is resolved or rejected manually by the test.
     */
    function mkDeferredLoader() {
        var loader = {requests: []};
        loader.loadRemoteOptionValues = function (opt, options, request) {
            var params = {};
            for (var n in options) {
                params[n] = options[n].value();
            }
            var req = {name: opt.name(), params: params, opt: opt, callbacks: [], request: request};
            loader.requests.push(req);
            return {
                then: function (func) {
                    req.callbacks.push(func);
                }
            };
        };
        //mirrors RemoteOptionLoader: a current request is completed before the callbacks run
        loader.resolve = function (req, data) {
            if (req.request && req.request.isCurrent()) {
                req.request.complete();
            }
            req.callbacks.forEach(function (cb) {
                cb(data);
            });
        };
        //mirrors RemoteOptionLoader: an error only touches the option when the request is still current
        loader.reject = function (req, message) {
            if (!req.request || req.request.isCurrent()) {
                req.request && req.request.complete();
                req.opt.remoteError({error: message});
                req.request && req.request.failed && req.request.failed();
            }
        };
        loader.forName = function (name) {
            return loader.requests.filter(function (r) {
                return r.name === name;
            });
        };
        return loader;
    }

    function mkdata(values) {
        return {
            values: values.map(function (v) {
                return {name: v, value: v, selected: true};
            })
        };
    }

    /**
     * Builds a controller over remote options; deps maps option name to its dependency names.
     */
    function mkChain(loader, names, deps) {
        var opts = new JobOptions({});
        var list = names.map(function (n) {
            return mkopt({name: n, hasRemote: true, required: true, enforced: false, values: null, defaultValue: '', value: ''});
        });
        opts.options(list);
        var control = new RemoteOptionController({loader: loader});
        control.setupOptions(opts);
        var config = {options: {}};
        names.forEach(function (n) {
            config.options[n] = {optionDependencies: deps[n] || [], optionDeps: []};
        });
        names.forEach(function (n) {
            (deps[n] || []).forEach(function (d) {
                config.options[d].optionDeps.push(n);
            });
        });
        control.loadData(config);
        names.forEach(function (n) {
            control.observeChangesFor(n);
        });
        return control;
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
            staleResponse_sameOption_ignored_Test: function () {
                var loader = mkDeferredLoader();
                var control = mkChain(loader, ['a'], {});
                var a = control.options['a'];
                var loadedWith = [];
                var origLoadRemote = a.loadRemote;
                a.loadRemote = function (data) {
                    loadedWith.push(data.values[0].value);
                    origLoadRemote(data);
                };
                control.loadRemoteOptionValues('a');
                control.loadRemoteOptionValues('a');
                this.assert("two requests", 2, loader.requests.length);
                //latest first, then the stale one resolves last
                loader.resolve(loader.requests[1], mkdata(['new']));
                loader.resolve(loader.requests[0], mkdata(['old']));
                this.assert("stale data not applied", ['new'], loadedWith);
                this.assert("latest value kept", 'new', a.value());
            },
            staleResponse_inOrder_applied_Test: function () {
                var loader = mkDeferredLoader();
                var control = mkChain(loader, ['a'], {});
                var a = control.options['a'];
                control.loadRemoteOptionValues('a');
                loader.resolve(loader.requests[0], mkdata(['one']));
                control.loadRemoteOptionValues('a');
                loader.resolve(loader.requests[1], mkdata(['two']));
                this.assert("latest value applied", 'two', a.value());
            },
            staleError_doesNotSetRemoteError_Test: function () {
                var loader = mkDeferredLoader();
                var control = mkChain(loader, ['a'], {});
                var a = control.options['a'];
                control.loadRemoteOptionValues('a');
                control.loadRemoteOptionValues('a');
                loader.reject(loader.requests[0], 'stale failure');
                this.assert("stale error ignored", undefined, a.remoteError());
                loader.reject(loader.requests[1], 'current failure');
                this.assert("current error reported", {error: 'current failure'}, a.remoteError());
            },
            staleResponse_loadingIndicatorKept_Test: function () {
                var loader = mkDeferredLoader();
                var control = mkChain(loader, ['a'], {});
                var a = control.options['a'];
                control.loadRemoteOptionValues('a');
                control.loadRemoteOptionValues('a');
                a.loading(true);
                loader.resolve(loader.requests[0], mkdata(['old']));
                this.assert("loading untouched by stale response", true, a.loading());
                this.assert("still pending", true, !!control.pending['a']);
            },
            invalidateDependents_transitiveAndCycleSafe_Test: function () {
                var control = new RemoteOptionController({loader: 'x'});
                control.dependents = {a: ['b', 'c'], b: ['d'], c: ['d'], d: ['a']};
                var invalidated = [];
                control.invalidateRequest = function (name) {
                    invalidated.push(name);
                    return false;
                };
                control.invalidateDependents('a');
                this.assert("every transitive dependent once, origin excluded", ['b', 'c', 'd'], invalidated);
            },
            cascade_staleVersionResponse_neverApplied_Test: function () {
                var loader = mkDeferredLoader();
                var deps = {
                    client: ['region'],
                    version: ['region', 'client'],
                    packages: ['region', 'client', 'version']
                };
                var control = mkChain(loader, ['region', 'client', 'version', 'packages'], deps);
                var o = control.options;
                var loadRemoteCalls = {version: [], packages: []};
                ['version', 'packages'].forEach(function (n) {
                    var orig = o[n].loadRemote;
                    o[n].loadRemote = function (data) {
                        loadRemoteCalls[n].push(data.values.map(function (v) {
                            return v.value;
                        }));
                        orig(data);
                    };
                });
                o.region.value('/EMEA');
                //the region change reloads client, version and packages
                var clientReq1 = loader.forName('client')[0];
                loader.resolve(clientReq1, mkdata(['/OLDCLIENT']));
                //client became /OLDCLIENT: version request #2 (stale once client changes again)
                var staleVersionReq = loader.forName('version')[loader.forName('version').length - 1];
                //user changes client while the version load is still pending
                o.client.value('/VERA');
                var freshVersionReq = loader.forName('version')[loader.forName('version').length - 1];
                this.assert("a new version request was issued", true, freshVersionReq !== staleVersionReq);
                this.assert("fresh version request sees the new client", '/VERA', freshVersionReq.params['client']);
                loader.resolve(freshVersionReq, mkdata(['/VER_R14']));
                this.assert("version selected from fresh response", '/VER_R14', o.version.value());
                //late stale version response must be ignored
                var callsBefore = loadRemoteCalls.version.length;
                loader.resolve(staleVersionReq, mkdata(['/JHP_R2.1']));
                this.assert("stale version not applied", callsBefore, loadRemoteCalls.version.length);
                this.assert("version not overwritten by stale response", '/VER_R14', o.version.value());
                var staleVersionUsed = loader.forName('packages').filter(function (r) {
                    return r.params['version'] === '/JHP_R2.1';
                });
                this.assert("packages never requested with the stale version", 0, staleVersionUsed.length);
                var lastPackages = loader.forName('packages')[loader.forName('packages').length - 1];
                this.assert("latest packages request uses current upstream values",
                    {region: '/EMEA', client: '/VERA', version: '/VER_R14'},
                    {region: lastPackages.params['region'], client: lastPackages.params['client'], version: lastPackages.params['version']});
            },
            cascade_deepDependentInvalidated_Test: function () {
                var loader = mkDeferredLoader();
                //packages depends only on version, version only on client
                var control = mkChain(loader, ['client', 'version', 'packages'], {
                    version: ['client'],
                    packages: ['version']
                });
                var o = control.options;
                var packagesApplied = [];
                var orig = o.packages.loadRemote;
                o.packages.loadRemote = function (data) {
                    packagesApplied.push(data.values[0].value);
                    orig(data);
                };
                o.client.value('c1');
                var v1 = loader.forName('version')[0];
                loader.resolve(v1, mkdata(['ver1']));
                this.assert("version chosen", 'ver1', o.version.value());
                var p1 = loader.forName('packages')[0];
                //client changes while packages (computed from ver1) is pending
                o.client.value('c2');
                loader.resolve(p1, mkdata(['pkg-from-ver1']));
                this.assert("transitive dependent response dropped", [], packagesApplied);
                this.assert("transitive dependent is no longer loading", false, o.packages.loading());
            },
            cascade_deepDependentReloadedWhenValueUnchanged_Test: function () {
                var loader = mkDeferredLoader();
                var control = mkChain(loader, ['client', 'version', 'packages'], {
                    version: ['client'],
                    packages: ['version']
                });
                var o = control.options;
                o.client.value('c1');
                loader.resolve(loader.forName('version')[0], mkdata(['ver1']));
                //packages in flight, client changes: version reloads, packages request is dropped
                o.client.value('c2');
                loader.resolve(loader.forName('version')[1], mkdata(['ver1']));
                //version value did not change, so the dropped packages load must be re-issued
                var packagesReqs = loader.forName('packages');
                this.assert("packages reloaded after upstream finished", 2, packagesReqs.length);
                loader.resolve(packagesReqs[1], mkdata(['pkg']));
                this.assert("packages applied", 'pkg', o.packages.value());
            },
            /**
             * Builds the region/client/version/packages chain with every option already selected,
             * then changes client while version (still holding the previous client's value) reloads.
             */
            cascade_clientChangedSetup: function () {
                var loader = mkDeferredLoader();
                var control = mkChain(loader, ['region', 'client', 'version', 'packages'], {
                    client: ['region'],
                    version: ['region', 'client'],
                    packages: ['region', 'client', 'version']
                });
                var o = control.options;
                o.region.value('/EMEA');
                loader.resolve(loader.forName('client')[0], mkdata(['/ALPHA']));
                var versionReqs = loader.forName('version');
                loader.resolve(versionReqs[versionReqs.length - 1], mkdata(['/ALPHA_R1']));
                var pkgReqs = loader.forName('packages');
                loader.resolve(pkgReqs[pkgReqs.length - 1], mkdata(['alpha-core']));
                var pkgCountBefore = loader.forName('packages').length;
                o.client.value('/BETA');
                var versionReqs2 = loader.forName('version');
                return {
                    loader: loader,
                    o: o,
                    pkgCountBefore: pkgCountBefore,
                    versionReq: versionReqs2[versionReqs2.length - 1]
                };
            },
            cascade_dependentDeferredWhileUpstreamLoading_Test: function () {
                var s = this.cascade_clientChangedSetup();
                this.assert("version reloaded with the new client", '/BETA', s.versionReq.params['client']);
                this.assert("packages not requested while version is loading", s.pkgCountBefore,
                    s.loader.forName('packages').length);
                this.assert("packages shows loading while waiting", true, s.o.packages.loading());
                s.loader.resolve(s.versionReq, mkdata(['/BETA_R7']));
                var pkgReqs = s.loader.forName('packages');
                this.assert("exactly one new packages request", s.pkgCountBefore + 1, pkgReqs.length);
                var last = pkgReqs[pkgReqs.length - 1];
                this.assert("packages requested with consistent upstream values",
                    {client: '/BETA', version: '/BETA_R7'},
                    {client: last.params['client'], version: last.params['version']});
                s.loader.resolve(last, mkdata(['beta-core']));
                this.assert("packages applied", 'beta-core', s.o.packages.value());
                this.assert("no error shown", false, !!s.o.packages.remoteError());
            },
            cascade_dependentReloadedWhenUpstreamValueUnchanged_Test: function () {
                var s = this.cascade_clientChangedSetup();
                //the new client offers the same version value, so version.value does not change
                s.loader.resolve(s.versionReq, mkdata(['/ALPHA_R1']));
                var pkgReqs = s.loader.forName('packages');
                this.assert("deferred packages request issued", s.pkgCountBefore + 1, pkgReqs.length);
                this.assert("packages requested with the new client", '/BETA', pkgReqs[pkgReqs.length - 1].params['client']);
            },
            cascade_dependentNotRequestedWhenUpstreamFails_Test: function () {
                var s = this.cascade_clientChangedSetup();
                s.loader.reject(s.versionReq, 'Server returned an error response: 500');
                this.assert("upstream error reported", true, !!s.o.version.remoteError());
                this.assert("packages not requested with outdated version", s.pkgCountBefore,
                    s.loader.forName('packages').length);
                this.assert("packages no longer loading", false, s.o.packages.loading());
                this.assert("no error shown on packages", false, !!s.o.packages.remoteError());
            },
            cascade_dependentNotRequestedWhenServerReportsUpstreamError_Test: function () {
                var s = this.cascade_clientChangedSetup();
                //the server reports a failing remote URL inside a successful response
                s.loader.resolve(s.versionReq, {err: {message: 'Not Found'}});
                this.assert("upstream error reported", true, !!s.o.version.remoteError());
                this.assert("packages not requested with outdated version", s.pkgCountBefore,
                    s.loader.forName('packages').length);
                this.assert("packages no longer loading", false, s.o.packages.loading());
            },
            cascade_deferredDependentClearedWhenRequiredValueRemoved_Test: function () {
                var loader = mkDeferredLoader();
                var control = mkChain(loader, ['a', 'b', 'c'], {c: ['a', 'b']});
                var o = control.options;
                o.a.value('a0');
                o.b.value('b1');
                loader.forName('c').forEach(function (r) {
                    loader.resolve(r, mkdata(['c1']));
                });
                control.loadRemoteOptionValues('a');
                o.b.value('b2');
                this.assert("c deferred while a loads", true, o.c.loading());
                var cCount = loader.forName('c').length;
                o.b.value('');
                this.assert("c no longer loading once a required value is unset", false, o.c.loading());
                loader.resolve(loader.forName('a')[loader.forName('a').length - 1], mkdata(['a1']));
                this.assert("c not requested with an unset required value", cCount, loader.forName('c').length);
                this.assert("c still not loading", false, o.c.loading());
            },
            cascade_deferralIsTransitive_Test: function () {
                var loader = mkDeferredLoader();
                var control = mkChain(loader, ['a', 'b', 'c'], {b: ['a'], c: ['b']});
                var o = control.options;
                control.loadRemoteOptionValues('a');
                control.loadRemoteOptionValues('b');
                var cCount = loader.forName('c').length;
                control.loadRemoteOptionValues('c');
                this.assert("c deferred while b is itself deferred", cCount, loader.forName('c').length);
                this.assert("c shows loading", true, o.c.loading());
                loader.reject(loader.forName('a')[loader.forName('a').length - 1], 'down');
                this.assert("b no longer loading after a failed", false, o.b.loading());
                this.assert("c no longer loading after a failed", false, o.c.loading());
                this.assert("c never requested", cCount, loader.forName('c').length);
            },
            abortsPreviousRequest_Test: function () {
                var aborted = 0;
                var loader = {
                    loadRemoteOptionValues: function () {
                        return {
                            abort: function () {
                                aborted++;
                            },
                            then: function () {
                            }
                        };
                    }
                };
                var control = mkChain(loader, ['a'], {});
                control.loadRemoteOptionValues('a');
                control.loadRemoteOptionValues('a');
                this.assert("first request aborted when superseded", 1, aborted);
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