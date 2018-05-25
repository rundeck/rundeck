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
/**
 * hook to initialize custom bootstrap popover mechanism
 * &ltdiv data-bind="bootstrapPopover: true, bootstrapPopoverContentRef: '#elemid' &gt;
 * or
 * &ltdiv data-bind="bootstrapPopover: true, bootstrapPopoverContentFor: '#elemid' &gt;
 */
ko.bindingHandlers.bootstrapPopover = {
    init: function (element, valueAccessor, allBindings, viewModel, bindingContext) {
        if(allBindings.get('bootstrapPopoverContentRef')){
            _initPopoverContentRef(null,{element:element,contentRef:ko.unwrap(allBindings.get('bootstrapPopoverContentRef'))});
        }else if (allBindings.get('bootstrapPopoverContentFor')) {
            _initPopoverContentFor(null,{element:element,target:ko.unwrap(allBindings.get('bootstrapPopoverContentFor'))});
        }
    }
};

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
