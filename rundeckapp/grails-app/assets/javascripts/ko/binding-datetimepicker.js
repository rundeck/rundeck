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

//= require knockout.min
jQuery(function () {
    ko.bindingHandlers.datetimepicker = {
        init: function (element, valueAccessor, allBindings, viewModel, bindingContext) {
            var opts = {useCurrent: true, sideBySide: true};
            var val = valueAccessor();
            if (ko.isObservable(val)) {
                val = ko.unwrap(val);
            }
            if (typeof(val) == 'string') {
                opts.defaultDate = val;
            }
            var dateFormat = allBindings.get('dateFormat');
            if (typeof(dateFormat) == 'string') {
                opts.format = dateFormat;
            }
            if (ko.isObservable(dateFormat)) {
                dateFormat = ko.unwrap(dateFormat);
            }
            if (typeof(dateFormat) == 'string') {
                opts.format = dateFormat;
            }
            if (opts.defaultDate) {
                try {
                    var m = moment(opts.defaultDate, opts.format, true);
                    if (!m.isValid()) {
                        opts.defaultDate = null;
                    } else {
                        //use a moment obj, due to datetimepicker bug https://github.com/Eonasdan/bootstrap-datetimepicker/issues/1704
                        opts.defaultDate = m;
                    }
                } catch (e) {
                    opts.defaultDate = null;
                }
            }
            // var locale = ko.unwrap(allBindings.get('locale'))
            //locale requires moment locale
            jQuery(element).datetimepicker(opts);

            ko.utils.domNodeDisposal.addDisposeCallback(element, function () {
                jQuery(element).datetimepicker("destroy");
            });
            //when a user changes the date, update the view model
            ko.utils.registerEventHandler(element, "dp.change", function (event) {
                var value = valueAccessor();
                if (ko.isObservable(value)) {
                    // value(event.date);
                    // console.log(event.date);
                    if (event.date) {
                        value(moment(event.date).format(dateFormat));
                    } else {
                        value(null);
                    }
                }
            });
        },
        update: function (element, valueAccessor, allBindings, viewModel, bindingContext) {
            "use strict";
            var widget = jQuery(element).data("datepicker");
            //when the view model is updated, update the widget
            if (widget) {
                widget.date = ko.utils.unwrapObservable(valueAccessor());
                if (widget.date) {
                    widget.setValue();
                }
            }
        }
    };
});