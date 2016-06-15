//= require momentutil
//= require knockout.min
//= require knockout-mapping
//= require knockout-foreachprop
//= require menu/job-remote-options

/**
 * Selectable value with name/value pair
 * @param data
 * @constructor
 */
function OptionVal(data) {
    "use strict";

    var self = this;
    self.label = ko.observable(data.label);
    self.value = ko.observable(data.value);
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
function Option(data) {
    "use strict";

    var self = this;
    self.remoteLoadCallback = null;
    self.name = ko.observable(data.name);
    self.description = ko.observable(data.description);
    self.descriptionHtml = ko.observable(data.descriptionHtml);
    self.loading = ko.observable(false);
    self.required = ko.observable(data.required ? true : false);
    self.enforced = ko.observable(data.enforced ? true : false);
    self.fieldName = ko.observable(data.fieldName);
    self.hasError = ko.observable(data.hasError);
    self.hasRemote = ko.observable(data.hasRemote);
    self.fieldName = ko.observable(data.fieldName);
    self.fieldId = ko.observable(data.fieldId);
    self.fieldLabelId = ko.observable(data.fieldLabelId);
    self.optionDepsMet = ko.observable(data.optionDepsMet);
    self.secureInput = ko.observable(data.secureInput);
    self.multivalued = ko.observable(data.multivalued);
    self.delimiter = ko.observable(data.delimiter);
    self.value = ko.observable(data.value);
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

    self.setReloadCallback = function (func) {
        self.remoteLoadCallback = func;
    };

    self.reloadRemoteValues = function () {
        if (self.hasRemote() && self.remoteLoadCallback) {
            self.remoteLoadCallback(self.name());
        }
    };

    //set up multivaluelist if default/selected values
    self.evalMultivalueChange = function () {
        if(self.multiValueList().length>0) {
            //construct value string from selected multivalue options
            var str = '';
            var strs=[];

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
            }
            return false;
        };
        if (!self.enforced() && self.selectedMultiValues()) {
            //add any selectedMultiValues that are not in values list

            ko.utils.arrayForEach(self.selectedMultiValues(), function (val) {
                if (ko.utils.arrayIndexOf(self.values(), val) >= 0) {
                    return;
                }
                self.multiValueList.push(self.createMultivalueEntry({
                    label: '_new',
                    value: val,
                    selected: testselected(val),
                    editable: true,
                    multival: true
                }));
            });
        }
        if (self.values()) {
            ko.utils.arrayForEach(self.values(), function (val) {
                var selected = testselected(val);
                self.multiValueList.push(self.createMultivalueEntry({
                    label: '_new',
                    value: val,
                    selected: selected,
                    editable: false,
                    multival: true
                }));
            });
        }

        self.multiValueList.subscribe(self.evalMultivalueChange);

        if(self.hasRemote()){
            //when remote values are loaded, set the multivalue entries with them
            self.remoteValues.subscribe(function(newval){
                var temp=[];
                ko.utils.arrayForEach(newval, function (val) {
                    var selected = testselected(val.value());
                    temp.push(self.createMultivalueEntry({
                        label: val.label(),
                        value: val.value(),
                        selected: selected,
                        editable: false,
                        multival: true
                    }));
                });
                self.multiValueList(temp);
            });
        }
    }
    self.remoteError = ko.observable();

    self.selectedOptionValue = ko.observable(data.value);
    self.defaultStoragePath = ko.observable(data.defaultStoragePath);
    self.truncateDefaultValue = ko.computed(function () {
        var val = self.defaultValue();
        if (!val || val.length < 50) return val;
        return val.substring(0, 50);
    });
    self.setDefault = function () {
        self.value(self.defaultValue());
    };
    self.hasSingleEnforcedValue = ko.computed(function () {
        //labelsSet && 1==labelsSet.size() && optionSelect.enforced
        return self.enforced() && self.values() && self.values().length == 1;
    });
    self.singleEnforcedValue = ko.computed(function () {
        return self.values() ? self.values()[0] : null;
    });
    self.hasValue = ko.computed(function () {
        return self.value();
    });
    self.hasValues = ko.computed(function () {
        var values = self.values();
        return values && values.length > 0;
    });
    self.hasExtended = ko.computed(function () {
        //hasExtended: !optionSelect.secureInput && (values || optionSelect.values || optionSelect.multivalued)
        return !self.secureInput() && (self.hasValues() || self.multivalued() || self.hasRemote() && self.remoteValues().length > 0);
    });
    self.hasTextfield = ko.computed(function () {
        //!optionSelect.enforced && !optionSelect.multivalued || optionSelect.secureInput || !optionSelect.enforced &&
        // err
        return !self.enforced() && !self.multivalued() || self.secureInput() || !self.enforced() && self.hasError();
    });
    self.showDefaultButton = ko.computed(function () {
        //!optionSelect.enforced && !optionSelect.multivalued && !optionSelect.secureInput && optionSelect.defaultValue
        // && !(optionSelect.values.contains(optionSelect.defaultValue))
        return !self.enforced() && !self.multivalued() && !self.secureInput() && self.defaultValue()
            && !(self.values() && self.values().indexOf(self.defaultValue()) >= 0 ) &&
            self.value() != self.defaultValue();
    });
    self.selectOptions = ko.computed(function () {
        var arr = [];
        if (!self.enforced() && !self.multivalued()) {
            arr.push(new OptionVal({label: "-choose-", value: ''}));
        }
        var remotevalues = self.remoteValues();
        var localvalues = self.values();
        if (self.hasRemote()) {
            ko.utils.arrayForEach(remotevalues, function (val) {
                arr.push(val);
            });
        } else {
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
    self.multivalueOptions = ko.computed(function () {

    });

    self.selectedOptionValue.subscribe(function (newval) {
        if (newval && typeof(newval) == 'object' && typeof(newval.value) == 'function' && newval.value()) {
            self.value(newval.value());
        } else if (typeof(newval) == 'string') {
            self.value(newval);
        }
    });

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
            self.remoteError(null);
            var rvalues = [];
            if (data.selectedvalue) {
                self.value(data.selectedvalue);
                self.selectedOptionValue(data.selectedvalue);
            }
            var selval;
            ko.utils.arrayForEach(data.values, function (val) {
                var optval;
                if (typeof(val) == 'object') {
                    optval = new OptionVal({label: val.name, value: val.value});
                } else if (typeof(val) == 'string') {
                    optval = new OptionVal({label: val, value: val});
                }
                if (optval) {
                    rvalues.push(optval);
                }

                if (optval.value() == self.value()) {
                    selval = optval;
                }
            });
            self.remoteValues(rvalues);
        }
    };
    self.animateRemove = function (div) {
        jQuery(div).fadeTo('slow', 0, function () {
            jQuery(div).remove();
        });
    };
    self.animateAdd = function (div) {
        jQuery(div).hide().slideDown();
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