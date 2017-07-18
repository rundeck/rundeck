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

//= require momentutil
//= require knockout.min
//= require knockout-mapping
//= require knockout-foreachprop
//= require menu/job-remote-options
//= require ko/binding-popover
//= require ko/binding-datetimepicker

/**
 * Selectable value with name/value pair
 * @param data
 * @constructor
 */
function OptionVal(data) {
    "use strict";

    var self = this;
    self.label = ko.observable(data.label || null);
    self.value = ko.observable(data.value || null);
    self.selected = ko.observable(data.selected ? true : false);
    self.editable = ko.observable(data.editable ? true : false);
    self.multival = ko.observable(data.multival ? true : false);
    self.resultValue = ko.computed(function () {
        var sel = self.selected();
        var val = self.value();
        if (sel && val) {
            return val;
        }
        return sel || val ? "" : null;
    });
}
var _option_uid=0;
function Option(data) {
    "use strict";

    var self = this;
    self.remoteLoadCallback = null;
    self.name = ko.observable(data.name);
    self.uid = ko.observable(data.uid||(++_option_uid+'_opt'));
    self.description = ko.observable(data.description);
    self.descriptionHtml = ko.observable(data.descriptionHtml);
    self.loading = ko.observable(false);
    self.required = ko.observable(data.required ? true : false);
    self.enforced = ko.observable(data.enforced ? true : false);
    self.isDate = ko.observable(data.isDate ? true : false);
    self.dateFormat = ko.observable(data.dateFormat);
    /**
     * Type: used for file upload
     */
    self.optionType = ko.observable(data.optionType);
    self.fieldName = ko.observable(data.fieldName);
    self.hasError = ko.observable(data.hasError);
    self.hasRemote = ko.observable(data.hasRemote);
    self.fieldName = ko.observable(data.fieldName);
    self.fieldId = ko.observable(data.fieldId);
    self.fieldLabelId = ko.observable(data.fieldLabelId);
    self.optionDepsMet = ko.observable(data.optionDepsMet);
    self.secureInput = ko.observable(data.secureInput);
    self.multivalued = ko.observable(data.multivalued);
    self.multivalueAllSelected = ko.observable(data.multivalueAllSelected ? true : false);
    self.delimiter = ko.observable(data.delimiter);
    self.value = ko.observable(data.value);
    self.initvalue = ko.observable(data.value);
    self.useinit = ko.observable(data.value ? true : false);
    /**
     * static list of values to choose from
     */
    self.values = ko.observableArray(data.values);
    self.defaultValue = ko.observable(data.defaultValue);
    /**
     * list of values already selected
     */
    self.selectedMultiValues = ko.observableArray(data.selectedMultiValues);
    /**
     * list of values chosen as default for multivalued
     */
    self.defaultMultiValues = ko.observableArray(data.defaultMultiValues);
    /**
     * list of all multivalue strings to choose from
     */
    self.multiValueList = ko.observableArray(data.multiValueList);

    function emptyValue(val) {
        return (!val || val === '');
    }

    self.setReloadCallback = function (func) {
        self.remoteLoadCallback = func;
    };

    self.reloadRemoteValues = function () {
        if (self.hasRemote() && self.remoteLoadCallback) {
            self.remoteLoadCallback(self.name());
        } else {
            return true;
        }
    };

    //set up multivaluelist if default/selected values
    self.evalMultivalueChange = function () {
        if (self.multiValueList().length > 0) {
            //construct value string from selected multivalue options
            var str = '';
            var strs = [];

            var selected = ko.utils.arrayFilter(self.multiValueList(), function (val) {
                return val.selected() && val.value();
            });
            ko.utils.arrayForEach(selected, function (val) {
                strs.push(val.value());
            });
            self.value(strs.join(self.delimiter()));
            self.selectedMultiValues(strs);
        }
    };
    self.createMultivalueEntry = function (obj) {
        var optionVal = new OptionVal(obj);
        optionVal.resultValue.subscribe(function (newval) {
            if (newval == null) {
                //remove from parent
                self.multiValueList.remove(optionVal);
            } else {
                self.evalMultivalueChange();
            }
        });
        return optionVal;
    };
    self.remoteValues = ko.observableArray([]);
    if (self.multivalued()) {

        var testselected = function (val) {
            if (self.selectedMultiValues() && self.selectedMultiValues().length > 0) {
                return ko.utils.arrayIndexOf(self.selectedMultiValues(), val) >= 0;
            } else if (self.defaultMultiValues() && self.defaultMultiValues().length > 0) {
                return ko.utils.arrayIndexOf(self.defaultMultiValues(), val) >= 0;
            } else if (self.value()) {
                return self.value() == val;
            } else if (self.defaultValue()) {
                return self.defaultValue() == val;
            } else if (self.multivalueAllSelected()) {
                return true;
            }
            return false;
        };
        if(self.selectedMultiValues().length<1 && self.defaultMultiValues().length>0){
            //automatically select the default values
            self.selectedMultiValues(self.defaultMultiValues());
        }
        var addedExtras = false;
        var addExtraSelected = function (selected) {
            if (!self.enforced() && selected) {
                //add any selectedMultiValues that are not in values list

                ko.utils.arrayForEach(selected, function (val) {
                    if (self.values() != null && ko.utils.arrayIndexOf(self.values(), val) >= 0) {
                        return;
                    }

                    var found = ko.utils.arrayFirst(self.multiValueList(), function (oval) {
                        return oval.value() == val;
                    });
                    if (found) {
                        return;
                    }
                    self.multiValueList.unshift(self.createMultivalueEntry({
                        label: val,
                        value: val,
                        selected: !addedExtras,
                        editable: true,
                        multival: true
                    }));
                });
                addedExtras = true;
            }
        };


        if (self.hasRemote()) {
            //when remote values are loaded, set the multivalue entries with them
            self.remoteValues.subscribe(function (newval) {
                var temp = [];
                if(!self.enforced()) {
                    //preserve the editable values
                    temp = ko.utils.arrayFilter(self.multiValueList(),function (val) {
                        return val.editable();
                    });
                }
                ko.utils.arrayForEach(newval, function (val) {
                    var selected = testselected(val.value());
                    var hasselected = self.selectedMultiValues() && self.selectedMultiValues().length > 0;
                    var found = ko.utils.arrayFirst(self.multiValueList(),function (oval) {
                        return oval.value()==val.value() && oval.editable();
                    });
                    if(found){
                        found.label(val.label());
                        found.editable(false);
                        found.selected(true);
                        temp.push(found);
                    }else {
                        temp.push(self.createMultivalueEntry({
                            label: val.label(),
                            value: val.value(),
                            selected: selected || (!hasselected && val.selected()),
                            editable: false,
                            multival: true
                        }));
                    }
                });
                var multiselected=self.selectedMultiValues();
                self.multiValueList(temp);
                addExtraSelected(multiselected);
            });
        } else {
            addExtraSelected(self.selectedMultiValues());

            if (self.values() != null) {
                ko.utils.arrayForEach(self.values(), function (val) {
                    var selected = testselected(val);
                    self.multiValueList.push(self.createMultivalueEntry({
                        label: val,
                        value: val,
                        selected: selected,
                        editable: false,
                        multival: true
                    }));
                });
            }
        }
        self.multiValueList.subscribe(self.evalMultivalueChange);
    } else if (self.enforced() && self.values().length == 1 && emptyValue(self.value())) {
        //auto-set the value to only allowed value
        self.value(self.defaultValue() || self.values()[0]);
    }else if (!self.enforced() && emptyValue(self.value()) && !emptyValue(self.defaultValue())) {
        //auto-set the value to only allowed value
        self.value(self.defaultValue());
    }
    self.remoteError = ko.observable();

    self.selectedOptionValue = ko.observable(self.value());
    self.defaultStoragePath = ko.observable(data.defaultStoragePath);
    self.dateFormatErr = ko.computed(function () {
        if (!self.isDate() || !self.value() || !self.dateFormat()) {
            return false;
        }
        try {
            var m = moment(self.value(), self.dateFormat(), true);
            return !m.isValid();
        } catch (e) {
            return true;
        }
    });
    self.truncateDefaultValue = ko.computed(function () {
        var val = self.defaultValue();
        if (!val || val.length < 50) return val;
        return val.substring(0, 50);
    });
    self.setDefault = function () {
        self.value(self.defaultValue());
    };
    self.hasSingleEnforcedValue = ko.computed(function () {
        return self.enforced()
            && self.values() != null
            && self.values().length == 1;
    });
    self.singleEnforcedValue = ko.computed(function () {
        return self.hasSingleEnforcedValue() ? self.values()[0] : null;
    });
    self.hasValue = ko.computed(function () {
        return self.value();
    });
    self.hasValues = ko.computed(function () {
        var values = self.values();
        return values != null && values.length > 0;
    });
    self.hasExtended = ko.computed(function () {
        return !self.secureInput()
            && (
                self.hasValues()
                || self.multivalued()
                || self.hasRemote() && self.remoteValues().length > 0
            );
    });
    self.hasTextfield = ko.computed(function () {
        return !self.enforced()
            && (
                !self.multivalued()
                || self.hasError()
            )
            || self.secureInput();
    });
    self.showDefaultButton = ko.computed(function () {
        return !self.enforced()
            && !self.multivalued()
            && !self.secureInput()
            && self.defaultValue()
            && !(
                self.values() != null
                && self.values().indexOf(self.defaultValue()) >= 0
            )
            && self.value() != self.defaultValue();
    });

    self.isFileType=ko.computed(function () {
        return self.optionType() == 'file';
    });
    /**
     * Return the array of option objects to use for displaying the Select input for this option
     */
    self.selectOptions = ko.computed(function () {
        var arr = [];
        if (!self.enforced() && !self.multivalued()) {
            arr.push(new OptionVal({label: message('option.select.choose.text'), value: ''}));
        }
        var remotevalues = self.remoteValues();
        var localvalues = self.values();
        if (self.hasRemote() && remotevalues != null) {
            ko.utils.arrayForEach(remotevalues, function (val) {
                arr.push(val);
            });
        } else if (localvalues != null) {
            ko.utils.arrayForEach(localvalues, function (val) {
                arr.push(new OptionVal({label: val, value: val}));
            });
        }
        return arr;
    });
    self.newMultivalueEntry = function () {
        var arr = self.multiValueList;
        arr.unshift(self.createMultivalueEntry({
            label: '_new',
            value: '',
            selected: true,
            editable: true,
            multival: true
        }));
    };
    self.multivalueFieldKeydown = function (obj,evt) {
        var enterKey = !noenter(evt);
        if (enterKey) {
            self.newMultivalueEntry()
        }
        return !enterKey;
    };

    /**
     * When select box chooses an option value, set the value()
     */
    self.selectedOptionValue.subscribe(function (newval) {
        if (newval && typeof(newval) == 'object' && typeof(newval.value) == 'function' && newval.value()) {
            self.value(newval.value());
        } else if (typeof(newval) == 'string') {
            self.value(newval);
        }
    });

    self.loadRemoteValues = function (values, selvalue) {
        self.remoteError(null);
        var tvalues = [];
        if (self.useinit() && self.initvalue()) {
            tvalues[1] = (self.initvalue());
            self.useinit(false);
        }
        if (selvalue && tvalues.indexOf(selvalue) < 0) {
            tvalues[0] = selvalue;
        }
        var rvalues = [];
        var tselected = -1;
        var remoteselected;
        ko.utils.arrayForEach(values, function (val) {
            var optval;
            if (typeof(val) === 'object') {
                if (!remoteselected && val.selected) {
                    remoteselected = val.value;
                }
                optval = new OptionVal({label: val.name, value: val.value, selected: val.selected});
            } else if (typeof(val) === 'string') {
                optval = new OptionVal({label: val, value: val});
            }
            if (optval) {
                rvalues.push(optval);
                if (tvalues.length > 0 && tvalues.indexOf(optval.value()) > tselected) {
                    tselected = tvalues.indexOf(optval.value());
                }
            }
        });
        //choose value to select, by preference:
        //1: init value
        //2: remote "selected" value
        //3: input "selected" value

        var touse = tselected === 1 ? tvalues[tselected] :( remoteselected || (tselected >= 0 ? tvalues[tselected] : null));

        //triggers refresh of "selectOptions" populating select box
        self.remoteValues(rvalues);

        if ((touse) && !self.multivalued()) {
            //choose correct value
            self.selectedOptionValue(touse);
        }
    };
    /**
     * Option values data loaded from remote JSON request
     * @param data
     */
    self.loadRemote = function (data) {
        if (data.err && data.err.message) {
            var err = data.err;
            if (err) {
                err.url = data.srcUrl;
            }
            self.remoteError(err);
            self.remoteValues([]);
        } else if (data.values) {
            self.loadRemoteValues(data.values, data.selectedvalue);
        }
    };
    self.animateRemove = function (div) {
        jQuery(div).show().slideUp('fast', 0, function () {
            jQuery(div).remove();
        });
    };
    self.animateAdd = function (div) {
        jQuery(div).hide().slideDown('fast',function(){
            jQuery(div).find('input[type=text]').focus();
        });
    };
}
function JobOptions(data) {
    "use strict";
    var self = this;
    self.options = ko.observableArray();
    self.remoteoptions = null;
    self.mapping = {
        options: {
            key: function (data) {
                return ko.utils.unwrapObservable(data.name);
            },
            create: function (options) {
                return new Option(options.data);
            }
        }
    };

    ko.mapping.fromJS(data, self.mapping, self);
}