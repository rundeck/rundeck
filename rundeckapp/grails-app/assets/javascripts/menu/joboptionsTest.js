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

jQuery(function () {
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

    new TestHarness("jobotionsTest.js", {
        baseOptionValueTest: function (pref) {
            "use strict";
            var opt = new OptionVal({});
            this.assert(pref + " option value", null, opt.value());
            this.assert(pref + " option value", null, opt.label());
            this.assert(pref + " option value", false, opt.selected());
            this.assert(pref + " option value", false, opt.editable());
            this.assert(pref + " option value", false, opt.multival());

            opt = new OptionVal({label: 'test', value: 'test2', selected: true, editable: true, multival: true});
            this.assert(pref + " option value", 'test', opt.label());
            this.assert(pref + " option value", 'test2', opt.value());
            this.assert(pref + " option value", true, opt.selected());
            this.assert(pref + " option value", true, opt.editable());
            this.assert(pref + " option value", true, opt.multival());
        },
        optionValueResultValueTest: function (pref) {
            "use strict";
            var opt = new OptionVal({value: "abc", selected: true});
            this.assert(pref + " option value", 'abc', opt.resultValue());
            opt.value('');
            this.assert(pref + " option value", '', opt.resultValue());
            opt.value('xxx');
            opt.selected(false);
            this.assert(pref + " option value", '', opt.resultValue());
            opt.value('');
            this.assert(pref + " option value", null, opt.resultValue());
            opt.value(null);
            this.assert(pref + " option value", null, opt.resultValue());
        },
        baseOption_truncateDefaultValue_Test: function (pref) {
            "use strict";
            var opt = mkopt();
            opt.defaultValue('short');
            this.assert(pref + " value", 'short', opt.truncateDefaultValue());
            opt.defaultValue('long 123456789012345678901234567890123456789012345-----');
            this.assert(pref + " value", 'long 123456789012345678901234567890123456789012345', opt.truncateDefaultValue());
            opt.defaultValue('');
            this.assert(pref + " value", '', opt.truncateDefaultValue());
            opt.defaultValue(null);
            this.assert(pref + " value", null, opt.truncateDefaultValue());
        },
        baseOption_hasSingleEnforcedValue_Test: function (pref) {
            "use strict";
            var opt = mkopt();
            opt.enforced(true);
            opt.values(['x']);
            this.assert(pref + " option", true, opt.hasSingleEnforcedValue());
            this.assert(pref + " option", 'x', opt.singleEnforcedValue());
            opt.enforced(false);
            this.assert(pref + " option", false, opt.hasSingleEnforcedValue());
            this.assert(pref + " option", null, opt.singleEnforcedValue());
            opt.enforced(true);
            opt.values(['x', 'z']);
            this.assert(pref + " option", false, opt.hasSingleEnforcedValue());
            this.assert(pref + " option", null, opt.singleEnforcedValue());
            opt.values(null);
            this.assert(pref + " option", false, opt.hasSingleEnforcedValue());
            this.assert(pref + " option", null, opt.singleEnforcedValue());
        },
        baseOption_hasValues_Test: function (pref) {
            "use strict";
            var self = this;
            var opt = mkopt();
            self.testMatrix(
                pref + " hasValues({0})",
                [
                    [null, false],
                    [[], false],
                    [['x'], true],
                    [['x', 'y'], true]
                ],
                function (val) {
                    opt.values(val);
                    return opt.hasValues();
                }
            );

        },
        baseOption_hasExtended_Test: function (pref) {
            "use strict";
            var self = this;
            var opt = mkopt();
            self.testMatrix(
                pref + " hasExtended({0})",
                [
                    [{secureInput: true}, false],
                    [{secureInput: false, values: ['a'], multivalued: false, hasRemote: false, remoteValues: []}, true],
                    [{secureInput: false, values: [], multivalued: true, hasRemote: false, remoteValues: []}, true],
                    [{secureInput: false, values: [], multivalued: false, hasRemote: true, remoteValues: []}, false],
                    [{secureInput: false, values: [], multivalued: false, hasRemote: true, remoteValues: ['x']}, true]
                ],
                function (val) {
                    opt.secureInput(val.secureInput);
                    opt.multivalued(val.multivalued);
                    opt.hasRemote(val.hasRemote);
                    opt.values(val.values);
                    opt.remoteValues(val.remoteValues);
                    return opt.hasExtended();
                }
            );

        },
        baseOption_hasTextfield_Test: function (pref) {
            "use strict";
            var self = this;
            var opt = mkopt();
            self.testMatrix(
                pref + " hasTextfield({0})",
                [
                    [{enforced: false, multivalued: false, secureInput: false, hasError: false}, true],
                    [{enforced: false, multivalued: true, secureInput: false, hasError: true}, true],
                    [{enforced: true, multivalued: false, secureInput: true, hasError: false}, true],

                    [{enforced: true, multivalued: true, secureInput: false, hasError: false}, false],
                    [{enforced: true, multivalued: false, secureInput: false, hasError: true}, false]
                ],
                function (val) {
                    opt.enforced(val.enforced);
                    opt.multivalued(val.multivalued);
                    opt.secureInput(val.secureInput);
                    opt.hasError(val.hasError);
                    return opt.hasTextfield();
                }
            );

        },
        baseOption_showDefaultButton_Test: function (pref) {
            "use strict";
            var self = this;
            var opt = mkopt();
            self.testMatrix(
                pref + " showDefaultButton({0})",
                [
                    [{
                        enforced: true,
                        multivalued: true,
                        secureInput: true,
                        defaultValue: null,
                        values: null,
                        value: 'x'
                    }, false],
                    [{
                        enforced: false,
                        multivalued: false,
                        secureInput: false,
                        defaultValue: 'x',
                        values: null,
                        value: 'x'
                    }, false],
                    [{
                        enforced: false,
                        multivalued: false,
                        secureInput: false,
                        defaultValue: 'x',
                        values: null,
                        value: 'Z'
                    }, true],
                    [{
                        enforced: false,
                        multivalued: false,
                        secureInput: false,
                        defaultValue: 'x',
                        values: ['x'],
                        value: 'Z'
                    }, false],
                    [{
                        enforced: false,
                        multivalued: false,
                        secureInput: false,
                        defaultValue: 'x',
                        values: ['Z'],
                        value: 'Z'
                    }, true],
                ],
                function (val) {
                    opt.enforced(val.enforced);
                    opt.multivalued(val.multivalued);
                    opt.secureInput(val.secureInput);
                    opt.defaultValue(val.defaultValue);
                    opt.values(val.values);
                    opt.value(val.value);
                    return opt.showDefaultButton();
                }
            );

        },
        baseOption_selectOptions_Test: function (pref) {
            "use strict";
            var self = this;
            var opt = mkopt();
            self.testMatrix(
                pref + " selectOptions({0})",
                [
                    [{enforced: false, multivalued: false, remoteValues: null, values: null, hasRemote: false}, ''],
                    [{enforced: false, multivalued: false, remoteValues: null, values: ['x'], hasRemote: false}, ',x'],
                    [{
                        enforced: false,
                        multivalued: false,
                        remoteValues: null,
                        values: ['x', 'y'],
                        hasRemote: false
                    }, ',x,y'],
                    [{
                        enforced: true,
                        multivalued: false,
                        remoteValues: null,
                        values: ['x', 'y'],
                        hasRemote: false
                    }, 'x,y'],
                    [{
                        enforced: false,
                        multivalued: true,
                        remoteValues: null,
                        values: ['x', 'y'],
                        hasRemote: false
                    }, 'x,y'],
                    [{
                        enforced: false,
                        multivalued: false,
                        remoteValues: [mkval('a')],
                        values: null,
                        hasRemote: false
                    }, ''],
                    [{
                        enforced: false,
                        multivalued: false,
                        remoteValues: [mkval('a'), mkval('b')],
                        values: null,
                        hasRemote: false
                    }, ''],
                    [{
                        enforced: true,
                        multivalued: false,
                        remoteValues: [mkval('a'), mkval('b')],
                        values: null,
                        hasRemote: false
                    }, ''],
                    [{
                        enforced: false,
                        multivalued: true,
                        remoteValues: [mkval('a'), mkval('b')],
                        values: null,
                        hasRemote: false
                    }, ''],
                    [{
                        enforced: false,
                        multivalued: false,
                        remoteValues: [mkval('a')],
                        values: null,
                        hasRemote: true
                    }, ',a'],
                    [{
                        enforced: false,
                        multivalued: false,
                        remoteValues: [mkval('a'), mkval('b')],
                        values: null,
                        hasRemote: true
                    }, ',a,b'],
                    [{
                        enforced: true,
                        multivalued: false,
                        remoteValues: [mkval('a'), mkval('b')],
                        values: null,
                        hasRemote: true
                    }, 'a,b'],
                    [{
                        enforced: false,
                        multivalued: true,
                        remoteValues: [mkval('a'), mkval('b')],
                        values: null,
                        hasRemote: true
                    }, 'a,b']

                ],
                function (val) {
                    opt.enforced(val.enforced);
                    opt.multivalued(val.multivalued);
                    opt.remoteValues(val.remoteValues);
                    opt.values(val.values);
                    opt.hasRemote(val.hasRemote);

                    var testValue = opt.selectOptions();

                    return ko.utils.arrayMap(testValue, function (val) {
                        return val.value();
                    }).join(",");
                }
            );

        },
        baseOption_selectedOptionValue_changes_value_Test: function (pref) {
            "use strict";
            var self = this;
            var opt = mkopt();
            self.testMatrix(
                pref + " selectedOptionValue({0})",
                [
                    ['', ''],
                    ['a', 'a'],
                    [mkval('a'), 'a'],
                    [mkval('a', 'Z'), 'a']

                ],
                function (val) {
                    opt.selectedOptionValue(val);

                    return opt.value();
                }
            );

        },
        baseOption_loadRemote_json_data_with_values_Test: function (pref) {
            "use strict";
            var self = this;
            self.testMatrix(
                pref + " loadRemote({0})",
                [
                    [{values:['x','y'],selectedvalue:'x'}, {value:'x',selectedOptionValue:'x',remoteValues:'x,y'}],
                    [{values:['x','y']}, {value:null,selectedOptionValue:null,remoteValues:'x,y'}],

                ],
                function (val) {
                    var opt = mkopt({value:null,selectedOptionValue:null});
                    opt.loadRemote(val);

                    return {
                        value:opt.value(),
                        selectedOptionValue:opt.selectedOptionValue(),
                        remoteValues:ko.utils.arrayMap(opt.remoteValues(),function(val){return val.value();}).join(',')
                    };
                }
            );

        },
        baseOption_loadRemote_json_data_with_error_Test: function (pref) {
            "use strict";
            var self = this;
            self.testMatrix(
                pref + " loadRemote({0})",
                [
                    [{err:{message:"blah"},srcUrl:"blee"}, {remoteError:{message:"blah",url:"blee"},remoteValues:''}],

                ],
                function (val) {
                    var opt = mkopt({value:null,selectedOptionValue:null});
                    opt.loadRemote(val);

                    return {
                        remoteError:opt.remoteError(),
                        remoteValues:ko.utils.arrayMap(opt.remoteValues(),function(val){return val.value();}).join(',')
                    };
                }
            );

        }
    });

});