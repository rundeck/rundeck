
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
 * hook to initialize custom bootstrap popover mechanism
 * &ltdiv data-bind="bootstrapPopover: true, bootstrapPopoverContentRef: '#elemid' &gt;
 * or
 * &ltdiv data-bind="bootstrapPopover: true, bootstrapPopoverContentFor: '#elemid' &gt;
 */
ko.bindingHandlers.bootstrapPopover = {
    init: function (element, valueAccessor, allBindings, viewModel, bindingContext) {
        if(allBindings.get('bootstrapPopoverContentRef')){
            _initPopoverContentRef(null,{element:element,contentRef:ko.unwrap(allBindings.get('bootstrapPopoverContentRef')),
                onShown:function(){_initPopoverMousedownCatch('body','._mousedown_popup_allowed',function (e) {
                    jQuery(element).popover("hide")
                })}}
                );
            ko.utils.domNodeDisposal.addDisposeCallback(element, function () {
                jQuery(element).popover("destroy")
            })
        }else if (allBindings.get('bootstrapPopoverContentFor')) {
            _initPopoverContentFor(null,{element:element,target:ko.unwrap(allBindings.get('bootstrapPopoverContentFor'))});
        }
    }
};
