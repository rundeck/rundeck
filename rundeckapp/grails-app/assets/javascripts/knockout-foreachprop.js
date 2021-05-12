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

//= require vendor/knockout.min
/*
 source: http://stackoverflow.com/questions/14838135/how-to-use-knockout-to-iterate-over-an-object-not-array
 */
/**
 *
 * binding: foreachprop:
 * can also define "unsorted:" binding to not sort keys.
 * <pre><code>
 *      &lt;span data-bind="foreachprop: execution().jobArguments"&gt;
 &lt;span data-bind="text: key"&gt;&lt;/span&gt;:
 &lt;span data-bind="text: value" class="optvalue"&gt;&lt;/span&gt;
 &lt;/span&gt;
 </code></pre>
 */
ko.bindingHandlers.foreachprop = {
    transformObject: function (obj,unsorted) {
        var properties = [];
        for (var key in obj) {
            if (obj.hasOwnProperty(key)) {
                properties.push({ key: key, value: obj[key] });
            }
        }
        if(!unsorted) {
            properties.sort(function (a, b) {
                return a.key.localeCompare(b.key);
            });
        }
        return ko.observableArray(properties);
    },
    init: function (element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
        var unsorted = allBindingsAccessor.get('unsorted');
        var value = ko.utils.unwrapObservable(valueAccessor()),
            properties = ko.bindingHandlers.foreachprop.transformObject(value,unsorted);
        var childBindingContext = bindingContext.createChildContext(
            bindingContext.$rawData,
            null // Optionally, pass a string here as an alias for the data item in descendant contexts
        );
        jQuery(element).data('childBindingContext', childBindingContext);
        ko.applyBindingsToNode(element, { foreach: properties }, childBindingContext);
        return { controlsDescendantBindings: true };
    },
    update:function(element, valueAccessor, allBindingsAccessor, viewModel, bindingContext){
        "use strict";
        var unsorted = allBindingsAccessor.get('unsorted');
        var value = ko.utils.unwrapObservable(valueAccessor()),
            properties = ko.bindingHandlers.foreachprop.transformObject(value,unsorted);
        var childBindingContext=jQuery(element).data('childBindingContext');
        ko.bindingHandlers['foreach'].update(element, properties, allBindingsAccessor, viewModel, childBindingContext);
        return { controlsDescendantBindings: true };

    }
};
