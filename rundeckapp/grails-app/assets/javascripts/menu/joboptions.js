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
}
function Option(data) {
    "use strict";

    var self = this;
    self.name = ko.observable(data.name);
    self.description = ko.observable(data.description);
    self.loading=ko.observable(false);
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
    self.value = ko.observable(data.value);
    self.values = ko.observableArray(data.values);
    self.remoteValues = ko.observableArray([]);
    self.remoteError = ko.observable();

    self.selectedOptionValue = ko.observable();
    self.defaultValue = ko.observable(data.defaultValue);
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
        return !self.secureInput() && (self.hasValues() || self.multivalued() || self.hasRemote() && self.remoteValues().length>0);
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

    self.selectedOptionValue.subscribe(function (newval) {
        if (newval && newval.value()) {
            self.value(newval.value());
        }
    });

    /**
     * Option values data loaded from remote JSON request
     * @param data
     */
    self.loadRemote=function(data){
        if(data.err && data.err.message){
            var err=data.err;
            if(err) {
                err.url = data.srcUrl;
            }
            self.remoteError(err);
            self.remoteValues([]);
        }else if (data.values) {
            self.remoteError(null);
            var rvalues = [];
            ko.utils.arrayForEach(data.values,function(val){
               if(typeof(val)=='object'){
                   rvalues.push(new OptionVal({label:val.name,value:val.value}));
               } else if(typeof(val)=='string'){
                   rvalues.push(new OptionVal({label:val,value:val}));
               }
            });
            self.remoteValues(rvalues);
        }
    };
}
function JobOptions(data) {
    "use strict";
    var self = this;
    self.options = ko.observableArray();
    self.remoteoptions=null;
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