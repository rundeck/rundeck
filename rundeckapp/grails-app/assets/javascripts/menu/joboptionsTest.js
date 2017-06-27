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
            value: null
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
            this.assert(" option value", null, opt.value());
            this.assert(" option value", null, opt.label());
            this.assert(" option value", false, opt.selected());
            this.assert(" option value", false, opt.editable());
            this.assert(" option value", false, opt.multival());

            opt = new OptionVal({label: 'test', value: 'test2', selected: true, editable: true, multival: true});
            this.assert(" option value", 'test', opt.label());
            this.assert(" option value", 'test2', opt.value());
            this.assert(" option value", true, opt.selected());
            this.assert(" option value", true, opt.editable());
            this.assert(" option value", true, opt.multival());
        },
        resultValueTest: function (pref) {
            "use strict";
            var opt = new OptionVal({value: "abc", selected: true});
            this.assert(" option value", 'abc', opt.resultValue());
            opt.value('');
            this.assert(" option value", '', opt.resultValue());
            opt.value('xxx');
            opt.selected(false);
            this.assert(" option value", '', opt.resultValue());
            opt.value('');
            this.assert(" option value", null, opt.resultValue());
            opt.value(null);
            this.assert(" option value", null, opt.resultValue());
        },
        truncateDefaultValue_Test: function (pref) {
            "use strict";
            var opt = mkopt();
            opt.defaultValue('short');
            this.assert(" value", 'short', opt.truncateDefaultValue());
            opt.defaultValue('long 123456789012345678901234567890123456789012345-----');
            this.assert(" value", 'long 123456789012345678901234567890123456789012345', opt.truncateDefaultValue());
            opt.defaultValue('');
            this.assert(" value", '', opt.truncateDefaultValue());
            opt.defaultValue(null);
            this.assert(" value", null, opt.truncateDefaultValue());
        },
        hasSingleEnforcedValue_Test: function (pref) {
            "use strict";
            var opt = mkopt();
            opt.enforced(true);
            opt.values(['x']);
            this.assert(" option", true, opt.hasSingleEnforcedValue());
            this.assert(" option", 'x', opt.singleEnforcedValue());
            opt.enforced(false);
            this.assert(" option", false, opt.hasSingleEnforcedValue());
            this.assert(" option", null, opt.singleEnforcedValue());
            opt.enforced(true);
            opt.values(['x', 'z']);
            this.assert(" option", false, opt.hasSingleEnforcedValue());
            this.assert(" option", null, opt.singleEnforcedValue());
            opt.values(null);
            this.assert(" option", false, opt.hasSingleEnforcedValue());
            this.assert(" option", null, opt.singleEnforcedValue());
        },
        hasValues_Test: function (pref) {
            "use strict";
            var self = this;
            var opt = mkopt();
            self.testMatrix(
                " hasValues({0})",
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
        hasExtended_Test: function (pref) {
            "use strict";
            var self = this;
            var opt = mkopt();
            self.testMatrix(
                " hasExtended({0})",
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
        hasTextfield_Test: function (pref) {
            "use strict";
            var self = this;
            var opt = mkopt();
            self.testMatrix(
                " hasTextfield({0})",
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
        showDefaultButton_Test: function (pref) {
            "use strict";
            var self = this;
            var opt = mkopt();
            self.testMatrix(
                " showDefaultButton({0})",
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
        selectOptions_Test: function (pref) {
            "use strict";
            var self = this;
            var opt = mkopt();
            self.testMatrix(
                " selectOptions({0})",
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
        selectedOptionValue_changes_value_Test: function (pref) {
            "use strict";
            var self = this;
            var opt = mkopt();
            self.testMatrix(
                " selectedOptionValue({0})",
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
        loadRemote_json_data_with_values_Test: function (pref) {
            "use strict";
            var self = this;
            self.testMatrix(
                " loadRemote({0})",
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
        loadRemote_json_data_with_object_values_selected_Test: function (pref) {
            "use strict";
            var self = this;
            self.testMatrix(
                " {2}: loadRemoteValues({0})",
                [
                    [
                        {values: [{value: 'x', name: 'X'}, {value: 'y', name: 'Y'}], selectedvalue: 'x'},
                        {value: 'x', selectedOptionValue: 'x', remoteValues: 'x,y'}
                    ],
                    [
                        {
                            values: [{value: 'x', name: 'X', selected: true}, {value: 'y', name: 'Y'}],
                            selectedvalue: 'x'
                        },
                        {value: 'x', selectedOptionValue: 'x', remoteValues: 'x,y'}
                    ],
                    [
                        {
                            values: [{value: 'x', name: 'X', selected: false}, {value: 'y', name: 'Y', selected: true}],
                            selectedvalue: 'x'
                        },
                        {value: 'y', selectedOptionValue: 'y', remoteValues: 'x,y'}
                    ],
                    [
                        {
                            values: [{value: 'x', name: 'X', selected: false}, {value: 'y', name: 'Y', selected: true}]
                        },
                        {value: 'y', selectedOptionValue: 'y', remoteValues: 'x,y'}
                    ],
                    [
                        {
                            values: [{value: 'x', name: 'X', selected: true}, {value: 'y', name: 'Y', selected: false}]
                        },
                        {value: 'x', selectedOptionValue: 'x', remoteValues: 'x,y'}
                    ]
                ],
                function (val) {
                    var opt = mkopt({value: null, selectedOptionValue: null});
                    opt.loadRemoteValues(val.values, val.selectedvalue);

                    return {
                        value: opt.value(),
                        selectedOptionValue: opt.selectedOptionValue(),
                        remoteValues: ko.utils.arrayMap(opt.remoteValues(), function (val) {
                            return val.value();
                        }).join(',')
                    };
                }
            );
        },
        loadRemote_json_data_with_init_values_Test: function (pref) {
            "use strict";
            var self = this;
            self.testMatrix(
                " loadRemoteValues({0})",
                [
                    [
                        {values: [{value: 'x', name: 'X'}, {value: 'y', name: 'Y'}], selectedvalue: 'x', init: 'x'},
                        {value: 'x', selectedOptionValue: 'x', remoteValues: 'x,y', initvalue: 'x'}
                    ],
                    [
                        {values: [{value: 'x', name: 'X'}, {value: 'y', name: 'Y'}], selectedvalue: 'x', init: 'y'},
                        {value: 'y', selectedOptionValue: 'y', remoteValues: 'x,y', initvalue: 'y'}
                    ],
                    [
                        {
                            values: [{value: 'x', name: 'X'}, {value: 'y', name: 'Y', selected: true}],
                            selectedvalue: 'x',
                            init: 'y'
                        },
                        {value: 'y', selectedOptionValue: 'y', remoteValues: 'x,y', initvalue: 'y'}
                    ],
                    [
                        {
                            values: [{value: 'x', name: 'X', selected: true}, {value: 'y', name: 'Y', selected: false}],
                            selectedvalue: 'x',
                            init: 'y'
                        },
                        {value: 'y', selectedOptionValue: 'y', remoteValues: 'x,y', initvalue: 'y'}
                    ]

                ],
                function (val) {
                    var opt = mkopt({value: val.init, selectedOptionValue: null});
                    opt.loadRemoteValues(val.values, val.selectedvalue);

                    return {
                        value: opt.value(),
                        selectedOptionValue: opt.selectedOptionValue(),
                        remoteValues: ko.utils.arrayMap(opt.remoteValues(), function (val) {
                            return val.value();
                        }).join(','),
                        initvalue: opt.initvalue()
                    };
                }
            );
        },
        loadRemote_json_data_twice_with_initvalue_Test: function (pref) {
            "use strict";
            var self = this;
            self.testMatrix(
                " loadRemoteValues({0})",
                [
                    [

                        {
                            loaded: [
                                {
                                    values: [
                                        {value: 'x', name: 'X'},
                                        {value: 'y', name: 'Y'}
                                    ],
                                    selectedvalue: 'x'
                                },
                                {
                                    values: [
                                        {value: 'a', name: 'A', selected: true},
                                        {value: 'x', name: 'X'},
                                        {value: 'z', name: 'Y'}
                                    ],
                                    selectedvalue: 'a'
                                }
                            ],
                            init: 'x'
                        },

                        [
                            {value: 'x', selectedOptionValue: 'x', remoteValues: 'x,y', initvalue: 'x', useinit: false},
                            {
                                value: 'a',
                                selectedOptionValue: 'a',
                                remoteValues: 'a,x,z',
                                initvalue: 'x',
                                useinit: false
                            }
                        ]
                    ]
                ],
                function (val) {
                    var opt = mkopt({value: val.init, selectedOptionValue: null});
                    opt.loadRemoteValues(val.loaded[0].values, val.loaded[0].selectedvalue);

                    var result = [{
                        value: opt.value(),
                        selectedOptionValue: opt.selectedOptionValue(),
                        remoteValues: ko.utils.arrayMap(opt.remoteValues(), function (val) {
                            return val.value();
                        }).join(','),
                        initvalue: opt.initvalue(),
                        useinit: opt.useinit()
                    }];
                    opt.loadRemoteValues(val.loaded[1].values, val.loaded[1].selectedvalue);

                    result.push({
                        value: opt.value(),
                        selectedOptionValue: opt.selectedOptionValue(),
                        remoteValues: ko.utils.arrayMap(opt.remoteValues(), function (val) {
                            return val.value();
                        }).join(','),
                        initvalue: opt.initvalue(),
                        useinit: opt.useinit()
                    });
                    return result;
                }
            );
        },

        loadRemote_json_data_with_multiple_selected_Test: function (pref) {
            "use strict";
            var self = this;
            self.testMatrix(
                " loadRemoteValues({0})",
                [
                    [ //init value
                        {
                            init: 'z',
                            values: [
                                {value: 'x', name: 'X', selected: true},
                                {value: 'y', name: 'Y'},
                                {value: 'z', name: 'Z'}
                            ],
                            selectedvalue: 'y'
                        },
                        {value: 'z', selectedOptionValue: 'z'}
                    ],
                    [ //no init value
                        {
                            values: [
                                {value: 'x', name: 'X', selected: true},
                                {value: 'y', name: 'Y'},
                                {value: 'z', name: 'Z'}
                            ],
                            selectedvalue: 'y'
                        },
                        {value: 'x', selectedOptionValue: 'x'}
                    ],
                    [ //no remote selected value
                        {
                            values: [
                                {value: 'x', name: 'X'},
                                {value: 'y', name: 'Y'},
                                {value: 'z', name: 'Z'}
                            ],
                            selectedvalue: 'y'
                        },
                        {value: 'y', selectedOptionValue: 'y'}
                    ]
                ],
                function (val) {
                    var opt = mkopt({value: val.init, selectedOptionValue: null});
                    opt.loadRemoteValues(val.values, val.selectedvalue);

                    return {
                        value: opt.value(),
                        selectedOptionValue: opt.selectedOptionValue()
                    };
                }
            );
        },
        loadRemote_json_data_with_error_Test: function (pref) {
            "use strict";
            var self = this;
            self.testMatrix(
                " loadRemote({0})",
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

        },
        multivalued_option_has_default_values_selectedTest: function (pref) {
            "use strict";
            var self = this;
            self.testMatrix(
                " selectedMultiValues of {0}",
                [
                    [{multivalued:true,defaultMultiValues:['a','b'],selectedMultiValues:[]},
                        ['a','b']],
                    [{multivalued:true,defaultMultiValues:['a','b'],selectedMultiValues:['x','y']},
                        ['x','y']]
                ],
                function (val) {
                    var opt = mkopt(val);

                    return opt.selectedMultiValues();
                }
            );

        }
    });

});