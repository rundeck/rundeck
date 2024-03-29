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
 */
ko.bindingHandlers.executeOnEnter = {
    init: function (element, valueAccessor, allBindings, viewModel, bindingContext) {
        var handler = allBindings.get('executeOnEnter');
        jQuery(element).on('keypress',function (event) {
            var keyCode = (event.which ? event.which : event.keyCode);
            if (keyCode === 13) {
                handler.call(bindingContext.$data,event);
                return false;
            }
            return true;
        });
    }
};
