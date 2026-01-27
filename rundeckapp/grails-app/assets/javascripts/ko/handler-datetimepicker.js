
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
 *
 */

jQuery(function () {
    // fallback format to match UI placeholder when dateFormat is not provided
    var DEFAULT_DATE_FORMAT = 'MM/DD/YYYY hh:mm a';

    ko.bindingHandlers.datetimepicker = {
        init: function (element, valueAccessor, allBindings, viewModel, bindingContext) {
            var opts = {useCurrent: true, sideBySide: true};
            var val = valueAccessor();
            if (ko.isObservable(val)) {
                val = ko.unwrap(val);
            }
            if (typeof(val) === 'string') {
                opts.defaultDate = val;
            }
            var dateFormat = allBindings.get('dateFormat');
            if (ko.isObservable(dateFormat)) {
                dateFormat = ko.unwrap(dateFormat);
            }
            // trim whitespace if string, otherwise use default format
            var effectiveFormat;
            if (typeof dateFormat === 'string') {
                var trimmedFormat = dateFormat.trim();
                effectiveFormat = trimmedFormat ? trimmedFormat : DEFAULT_DATE_FORMAT;
            } else {
                effectiveFormat = DEFAULT_DATE_FORMAT;
            }
            opts.format = effectiveFormat;
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
                var picker = jQuery(element).data("DateTimePicker");
                if (picker) {
                    picker.destroy();
                }
            });
            //when a user changes the date, update the view model
            ko.utils.registerEventHandler(element, "dp.change", function (event) {
                var value = valueAccessor();
                if (ko.isObservable(value)) {
                    value(event.date ? moment(event.date).format(effectiveFormat) : null);
                }
            });
        },
        update: function (element, valueAccessor, allBindings, viewModel, bindingContext) {
            "use strict";
            var widget = jQuery(element).data("DateTimePicker");
            //when the view model is updated, update the widget
            if (widget) {
                var koDate = ko.utils.unwrapObservable(valueAccessor());
                widget.date(koDate);
            }
        }
    };
});