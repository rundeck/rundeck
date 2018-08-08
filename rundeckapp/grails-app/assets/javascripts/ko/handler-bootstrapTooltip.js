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

/**
 * Initializes bootstrap tooltip on the dom element. Usage: &lt;div data-bind="bootstrapTooltip: true" title="blah" &gt;
 * tip: if the title of the element is bound to an observable, pass the same one as the binding, like
 * &lt;div data-bind="bootstrapTooltip: mytooltipObservable" title="blah" &gt;, to trigger updates when it changes.
 * @type {{init: ko.bindingHandlers.bootstrapTooltip.init}}
 */
ko.bindingHandlers.bootstrapTooltip = {
    init: function (element, valueAccessor, allBindings, viewModel, bindingContext) {
        "use strict";
        jQuery(element).tooltip({});

        ko.utils.domNodeDisposal.addDisposeCallback(element, function() {
            jQuery(element).tooltip("destroy");
        });
    },
    update: function (element, valueAccessor, allBindings, viewModel, bindingContext) {
        "use strict";
        var val = valueAccessor();
        if(ko.isObservable(val)){
            val = ko.unwrap(val);
            jQuery(element).tooltip('destroy');
            jQuery(element).data('original-title',null);
            jQuery(element).tooltip({});
        }
    }
};
