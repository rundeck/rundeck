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

class ProviderDescriptionSet {

    constructor(service, data) {
        const self = this;
        this.service = ko.observable(service);
        this.providers = ko.observableArray(
            data.map((plugin) => new ProviderDescription(Object.assign({service: service}, plugin)))
        );
        this.providerByName = (name) => ko.utils.arrayFirst(self.providers(), (provider) => provider.name() === name);
    }
}

class PluginServices {
    constructor(data) {
        const self = this;
        this.services = ko.observableArray(
            data.map((service) => new ProviderDescriptionSet(service.name, service.providers))
        );
        this.serviceByName = (name) => ko.utils.arrayFirst(self.services(), (service) => service.service() === name);

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
    self.provider = ko.observable(data.provider);
    self.config = ko.observable(data.config);
    self.uid = ko.observable(data.uid);
    self.loading = ko.observable(false);
    self.mode = ko.observable('edit');
    self.validationErrors = ko.observable(false);
    self.formId = data.formId;
    self.formPrefixes = data.formPrefixes;
    self.inputFieldPrefix = data.inputFieldPrefix;
    self.postLoadEditor = data.postLoadEditor;
    self.deleteCallback = data.deleteCallback;
    self.formDom = () => jQuery('#' + self.formId);

    self.urls = {
        edit    : appLinks.pluginPropertiesForm,
        view    : appLinks.pluginPropertiesPreview,
        validate: appLinks.pluginPropertiesValidateAjax,
    };

    self.loadPluginView = (url, service, name, params, data, report) => jQuery.ajax(
        {
            url        : _genUrl(
                url,
                jQuery.extend(
                    {
                        service: service,
                        name   : name
                    },
                    params
                )
            ),
            method     : 'POST',
            contentType: 'application/json',
            data       : JSON.stringify({config: data, report: report}),

        }
    );


    self.getFormData = () => jQueryFormData(self.formDom(), self.formPrefixes);
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
        if (!self.provider()) {
            return;
        }
        self.loading(true);
        return self.loadPluginView(
            self.urls[val],
            self.service(),
            self.provider(),
            {inputFieldPrefix: self.inputFieldPrefix},
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
            self.service(),
            self.provider(),
            {inputFieldPrefix: self.inputFieldPrefix},
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

    // self.provider.subscribe(self.loadActionSelect);
    self.init = () => {
        self.mode(self.hasConfigData() ? 'view' : 'edit');
        self.mode.subscribe((mode) => {
            self.loadModeView(mode);
        });
        self.provider.subscribe((val) => {
            if (val && self.mode()) {
                self.loadModeView(self.mode());
            }
        });
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
    self.fieldname = ko.observable(data.fieldname);
    self.fieldid = ko.observable(data.fieldid);
    self.fieldtype = ko.observable(data.fieldtype);
    self.idkey = ko.observable(data.idkey);
    self.util = ko.observable({});
    self.type = ko.observable(data.type);
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
                items         : ko.observableArray(),
                privInc       : 0,
                formPrefixes  : data.formPrefixes,
                inputPrefix   : data.inputPrefix,
                postLoadEditor: data.postLoadEditor,
                service       : data.service,

                init() {
                    ko.utils.arrayForEach(self.items(), function (d) {
                        d.init()
                    });
                },

                createItem(id, type, config) {
                    return new PluginEditor(
                        {
                            uid             : `cond_${id}`,
                            service         : self.service,
                            provider        : type,
                            config          : config,
                            formId          : `form_${id}`,
                            formPrefixes    : self.formPrefixes.map((val) => `${val}entry[cond_${id}].config.`),
                            inputFieldPrefix: `${self.inputPrefix}entry[cond_${id}].`,
                            postLoadEditor  : self.postLoadEditor,
                            deleteCallback  : self.removeItem
                        });
                },

                addType(obj, evt) {
                    const id = (++self.privInc);
                    let data = jQuery(evt.target).data();
                    let type = data['pluginType'];
                    if (!type) {
                        return;
                    }
                    const pluginEditor = self.createItem(id, type, {data: false, type: type, config: {}});
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
                let pluginEditor = self.createItem(
                    id,
                    val.type,
                    {
                        config: val.config,
                        data  : true,
                        report: {errors: error}
                    }
                );
                self.items.push(pluginEditor);
            });

        }
    }
}