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

//= require knockout.min
//= require knockout-mapping
//= require ui/toggle

"use strict";

class ProviderDescription {
    constructor(data) {
        this.service = ko.observable(data.service);
        this.name = ko.observable(data.name);
        this.title = ko.observable(data.title);
    }
}

class ServiceDescriptionSet {

    constructor(service, data, labels) {
        const self = this;
        this.loaded = ko.observable(false);
        this.error = false;
        this.service = ko.observable(service);
        this.providers = ko.observableArray(
            data.map((plugin) => new ProviderDescription(Object.assign({service: service}, plugin)))
        );
        this.labels = ko.observable(labels);
        this.providerByName = (name) => ko.utils.arrayFirst(self.providers(), (provider) => provider.name() === name);
    }

    addProviders(providers) {
        let service = this.service();
        providers.forEach((plugin) => {
            this.providers.push(
                new ProviderDescription(Object.assign(
                    {service: service},
                    plugin
                )))
        });
    }
}

class PluginServices {
    constructor(data) {
        const self = this;
        this.services = ko.observableArray(
            data.map((service) => new ServiceDescriptionSet(service.name, service.providers, service.labels))
        );
        this.serviceByName = (name) => {
            if (!name) {
                return null;
            }
            let found = ko.utils.arrayFirst(this.services(), (service) => service.service() === name);
            if (!found) {
                found = new ServiceDescriptionSet(name, [], {});
                this.services.push(found);
                this.serviceByNameAsync(name);
            }
            return found;
        };
        this.serviceByNameAsync = (name) => {
            let cached = this.serviceByName(name);
            let dfd = jQuery.Deferred();
            if (cached && cached.loaded()) {
                dfd.resolve(cached);
            } else {
                jQuery.ajax(
                    {
                        url     : _genUrl(appLinks.pluginServiceDescriptions, {service: name}),
                        dataType: 'json',
                        success : (data) => {
                            let value = cached;
                            if (!value) {
                                value = new ServiceDescriptionSet(name, [], {});
                                this.services.push(value);
                            }
                            value.addProviders(data.descriptions);
                            value.labels(data.labels);
                            value.loaded(true);

                            dfd.resolve(value);
                        },
                        error   : (err) => {
                            dfd.reject(err);
                        }
                    }
                );
            }
            return dfd.promise();
        }

    }
}

/**
 * Manages a single plugin's configuration editor
 * @param data
 * @constructor
 */
function PluginEditor(data) {
    const self = this;
    self.service = ko.observable(data.service);
    self.serviceDescription = ko.computed(() => pluginServices.serviceByName(self.service()));
    self.provider = ko.observable(data.provider);
    self.propname = ko.observable(data.propname);
    self.property = ko.observable(data.property);
    self.embedded = ko.observable(data.embedded);
    self.embeddedMode = ko.observable(data.embeddedMode);

    self.config = ko.observable(data.config);
    self.uid = ko.observable(data.uid);
    self.loading = ko.observable(false);
    self.mode = ko.observable(data.previewMode ? 'view' : 'edit');
    self.validationErrors = ko.observable(false);
    self.formId = data.formId;
    self.formPrefixes = data.formPrefixes;
    self.inputFieldPrefix = data.inputFieldPrefix;
    self.postLoadEditor = data.postLoadEditor;
    self.deleteCallback = data.deleteCallback;
    self.canDelete = data.canDelete !== null ? data.canDelete : true;
    self.canSelectProvider = data.canSelectProvider !== null ? data.canSelectProvider : true;
    self.formDom = () => jQuery('#' + self.formId);

    /**
     * indicator for the _type form field value
     */
    self.embeddedModeIndicator = ko.pureComputed(() => {
        if (self.embedded()) {
            if (self.embeddedMode() === 'type') {
                return 'embedded';
            } else if (self.embeddedMode() === 'plugin') {
                return 'embeddedPlugin';
            }
        }

    });

    self.urls = {
        edit    : appLinks.pluginPropertiesForm,
        view    : appLinks.pluginPropertiesPreview,
        validate: appLinks.pluginPropertiesValidateAjax,
    };
    self.queryParams = () => {
        if (self.embedded()) {
            if (self.embeddedMode() === 'type') {
                return {
                    service          : self.service(),
                    name             : self.provider(),
                    embeddedProperty : self.propname(),
                    hidePluginSummary: true,
                    inputFieldPrefix : self.inputFieldPrefix
                }
            } else if (self.embeddedMode() === 'plugin') {
                return {
                    service         : self.service(),
                    name            : self.provider(),
                    inputFieldPrefix: self.inputFieldPrefix
                    // hidePluginSummary: true
                }
            }
        } else {
            return {
                service         : self.service(),
                name            : self.provider(),
                inputFieldPrefix: self.inputFieldPrefix

            }
        }
    };

    self.loadPluginView = (url, params, data, report) => jQuery.ajax(
        {
            url        : _genUrl(
                url,
                jQuery.extend(
                    self.queryParams(),
                    params
                )
            ),
            method     : 'POST',
            contentType: 'application/json',
            data       : JSON.stringify({config: data, report: report}),
        }
    );


    self.getFormData = () => jQueryFormData(self.formDom(), self.formPrefixes, [self.inputFieldPrefix]);
    self.hasConfigData = () => self.config() && self.config().data;
    self.getConfigData = () => {
        if (self.config() !== null) {
            return self.config().data ? self.config().config : {};
        } else {
            //serialize form data
            return self.getFormData();
        }
    };
    self.getConfigReport = () => {
        if (self.config() !== null) {
            return self.config().data ? self.config().report : {};
        }
    };

    self.contentHtml = ko.observable();

    self.setModeEdit = () => self.mode('edit');
    self.isModeEdit = ko.pureComputed(() => self.mode() === 'edit');
    self.setModeView = () => self.mode('view');
    self.isModeView = ko.pureComputed(() => self.mode() === 'view');
    self.modeToggle = () => self.mode(self.mode() === 'view' ? 'edit' : 'view');
    self.loadModeView = (val) => {
        if (self.embeddedMode() === 'plugin' && !self.provider()) {
            return;
        } else if (self.embeddedMode() === 'type' && !self.propname()) {
            return;
        } else if (!self.embedded() && !self.provider()) {
            return;
        }

        self.loading(true);
        return self.loadPluginView(
            self.urls[val],
            {},
            self.getConfigData(),
            self.getConfigReport()
        ).success(function (data) {
            self.loading(false);
            self.contentHtml(data);
        });
    };
    self.save = () => {
        //validate changes
        let config = self.getFormData();
        self.loadPluginView(
            self.urls['validate'],
            {},
            config,
            {}
        ).success(function (json) {
            if (json.valid) {
                self.validationErrors(false);
                self.config({config, data: true, report: {}});
                self.mode('view');
            } else {
                self.validationErrors(true);
                //reload with error data
                self.config({config, data: true, report: json});
                self.loadModeView('edit');
            }
        });
    };
    self.delete = () => {
        self.provider(null);
        self.config(null);
        self.mode('edit');
        self.formDom().html(null);
        if (self.deleteCallback) {
            self.deleteCallback(self);
        }
    };

    self.contentHtml.subscribe((html) => {
        self.formDom().html(html);
        if (typeof(self.postLoadEditor) === 'function') {
            self.postLoadEditor(self.formDom());
        }
    });

    self.init = () => {
        let initMode;
        if (data.previewMode) {
            initMode = 'view';
        } else if (data.embedded) {
            initMode = 'edit';
        } else {
            initMode = self.hasConfigData() ? 'view' : 'edit';
        }
        self.mode(initMode);
        self.mode.subscribe((mode) => {
            self.loadModeView(mode);
        });
        if (!self.embedded() ||  self.embeddedMode() === 'plugin') {
            self.provider.subscribe((val) => {
                if (val && self.mode()) {
                    self.loadModeView(self.mode());
                }
            });
        }
        self.loadModeView(self.mode());
    }
}

/**
 * A single plugin configuration property
 * @param data
 * @constructor
 */
function PluginProperty(data) {
    const self = this;
    self.project = ko.observable(data.project);
    self.name = ko.observable(data.name);
    self.service = ko.observable(data.service);
    self.provider = ko.observable(data.provider);
    self.title = ko.observable(data.title);
    self.fieldname = ko.observable(data.fieldname);
    self.fieldid = ko.observable(data.fieldid);
    self.fieldtype = ko.observable(data.fieldtype);
    self.idkey = ko.observable(data.idkey);
    self.util = ko.observable({});
    self.type = ko.observable(data.type);
    self.hasEmbeddedType = ko.observable(data.hasEmbeddedType);
    self.hasEmbeddedPluginType = ko.observable(data.hasEmbeddedPluginType);
    self.embeddedServiceName = ko.observable(data.embeddedServiceName);
    self.error = data.error;
    self.editor = null;
    self.listEditor = null;
    self.renderingOptions = ko.observable(data.renderingOptions || {});

    self.getField = () => jQuery('#' + self.fieldid());
    self.value = ko.observable(data.value);
    self.toggle = new UIToggle({value: false});
    self.getAssociatedProperty = () => {
        const associated = self.renderingOptions()['associatedProperty'];
        const idkey = self.idkey();
        if (associated && idkey && typeof(PluginSet) === 'object' && typeof(PluginSet[idkey]) === 'object') {
            return PluginSet[idkey][associated];
        }
        return null;
    };
    self.init = function () {
        if (self.type() === 'Embedded') {
            //load embedded editor
            const eid = self.idkey();
            let configVal = {data: false};
            let embeddedType = null;
            if (data.value && typeof(data.value) === 'object') {
                if (self.hasEmbeddedType()) {
                    configVal = {data: true, config: data.value};
                } else if (self.hasEmbeddedPluginType()) {
                    embeddedType = data.value.type;
                    configVal = {data: true, config: data.value.config};
                }
            }
            if (self.hasEmbeddedType()) {
                if (data.error && typeof(data.error) === 'object') {
                    configVal['data'] = true;
                    configVal['report'] = {errors: data.error};
                }
            } else if (self.hasEmbeddedPluginType()) {
                if (data.error && typeof(data.error) === 'object' && data.error['config']) {
                    configVal['data'] = true;
                    configVal['report'] = {errors: data.error['config']};
                }
            }
            self.editor = new PluginEditor(
                {
                    uid             : `eprop_${eid}`,
                    service         : self.hasEmbeddedType() ? self.service() : self.embeddedServiceName(),
                    provider        : self.hasEmbeddedType() ? self.provider() : embeddedType,
                    propname        : self.name(),
                    property        : self,
                    config          : configVal,
                    formId          : `form_eprop_${eid}`,
                    formPrefixes    : [`${self.fieldname()}.config.`, `orig.${self.fieldname()}.config.`],
                    inputFieldPrefix: `${self.fieldname()}.`,
                    previewMode     : data.previewMode,
                    embedded        : true,
                    embeddedMode    : self.hasEmbeddedType() ? 'type' : 'plugin',
                    // embeddedServiceName: self.embeddedServiceName(),
                    // embeddedProvider   : embeddedType,
                    // postLoadEditor  : self.postLoadEditor, //TODO:
                    // deleteCallback  : self.removeItem // TODO
                }
            );
            self.editor.init();
        } else if (self.type() === 'Options' && self.hasEmbeddedPluginType()) {
            const eid = self.idkey();
            let configVal = {data: false};
            let embeddedType = null;
            if (data.value && jQuery.isArray(data.value)) {
                configVal = {data: true, list: data.value};
            }
            if (data.error && jQuery.isArray(data.error)) {
                configVal['data'] = true;
                configVal['errors'] = data.error;
            }
            self.listEditor = new PluginListEditor(
                Object.assign(
                    {
                        idkey          : eid,
                        postLoadEditor : data.postLoadEditor,
                        service        : self.embeddedServiceName(),
                        propname       : self.name(),
                        property       : self,
                        formId         : `form_eprop_${eid}`,
                        formPrefixes   : [`${self.fieldname()}.`, `orig.${self.fieldname()}.`],
                        inputPrefix    : `${self.fieldname()}.`,
                        previewMode    : data.previewMode,
                        hasEmbeddedType: false
                        // postLoadEditor  : self.postLoadEditor, //TODO:
                        // deleteCallback  : self.removeItem // TODO
                    },
                    configVal
                )
            );
        } else if (self.type() === 'Options' && self.hasEmbeddedType()) {
            const eid = self.idkey();
            let configVal = {data: false};
            let embeddedType = null;
            console.log("Options:hasEmbeddedType, data value:", data.value);
            if (data.value && jQuery.isArray(data.value)) {
                configVal = {data: true, list: data.value};
            }
            if (data.error && jQuery.isArray(data.error)) {
                configVal['data'] = true;
                configVal['errors'] = data.error;
            }
            self.listEditor = new PluginListEditor(
                Object.assign(
                    {
                        idkey          : eid,
                        postLoadEditor : data.postLoadEditor,
                        service        : self.service(),
                        provider       : self.provider(),
                        propname       : self.name(),
                        property       : self,
                        formId         : `form_eprop_${eid}`,
                        formPrefixes   : [`${self.fieldname()}.`, `orig.${self.fieldname()}.`],
                        inputPrefix    : `${self.fieldname()}.`,
                        previewMode    : data.previewMode,
                        hasEmbeddedType: true
                        // postLoadEditor  : self.postLoadEditor, //TODO:
                        // deleteCallback  : self.removeItem // TODO
                    },
                    configVal
                )
            );
        }
    };
    self.init();
}

/**
 * A list of plugins of the same service
 */
class PluginListEditor {
    constructor(data) {
        const self = this;
        Object.assign(
            this,
            {
                items             : ko.observableArray(),
                idkey             : data.idkey || 'cond',
                formId            : data.formId || 'form',
                privInc           : 0,
                formPrefixes      : data.formPrefixes,
                inputPrefix       : data.inputPrefix,
                postLoadEditor    : data.postLoadEditor,
                service           : data.service,
                provider          : data.provider,
                propname          : data.propname,
                property          : data.property,
                previewMode       : data.previewMode,
                hasEmbeddedType   : ko.observable(data.hasEmbeddedType),
                serviceDescription: pluginServices.serviceByName(data.service),
                mode              : ko.observable(data.previewMode ? 'view' : 'edit'),
                isModeEdit        : ko.pureComputed(() => self.mode() === 'edit'),

                init() {
                    ko.utils.arrayForEach(self.items(), function (d) {
                        d.init()
                    });
                },

                createItem(id, type, config, isNew) {
                    return new PluginEditor(
                        {
                            uid             : `${this.idkey}_${id}`,
                            service         : self.service,
                            provider        : type,
                            propname        : self.propname,
                            config          : config,
                            formId          : `${this.formId}_item_${id}`,
                            formPrefixes    : self.formPrefixes.map((val) => `${val}entry[${this.idkey}_${id}].config.`),
                            inputFieldPrefix: `${self.inputPrefix}entry[${this.idkey}_${id}].`,
                            postLoadEditor  : self.postLoadEditor,
                            deleteCallback  : self.removeItem,
                            embedded        : true,
                            embeddedMode    : self.hasEmbeddedType() ? 'type' : 'plugin',
                            previewMode     : !isNew
                        });
                },

                /**
                 * Add an item with a selected provider type, for plugin types
                 * @param obj
                 * @param evt
                 */
                addType(obj, evt) {
                    const id = (++self.privInc);
                    let data = jQuery(evt.target).data();
                    let type = data['pluginType'];
                    if (!type) {
                        return;
                    }
                    const pluginEditor = self.createItem(id, type, {data: false, type: type, config: {}}, true);
                    self.items.push(pluginEditor);
                    pluginEditor.init();
                },

                /**
                 * Add an item, for embedded types
                 * @param obj
                 * @param evt
                 */
                addItem(obj, evt) {
                    const id = (++self.privInc);
                    let data = jQuery(evt.target).data();
                    const pluginEditor = self.createItem(
                        id,
                        self.provider,
                        {data: false, type: self.provider, config: {}},
                        true
                    );
                    self.items.push(pluginEditor);
                    pluginEditor.init();
                },

                removeItem(item) {
                    self.items.splice(self.items.indexOf(item), 1);
                }
            }
        );

        if (data && data.data) {
            let errors = data.errors;
            data.list.forEach(function (val, n) {
                let id = (++self.privInc);
                let error = errors && errors.length > n ? errors[n] : null;
                let type = self.hasEmbeddedType() ? self.provider : val.type;
                let config = self.hasEmbeddedType() ? val : val.config;
                let pluginEditor = self.createItem(
                    id,
                    type,
                    {
                        config: config,
                        type  : type,
                        data  : true,
                        report: {errors: error}
                    }
                );
                self.items.push(pluginEditor);
                pluginEditor.init();
            });

        }
    }
}