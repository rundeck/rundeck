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
 */

"use strict";

/**
 * bind via knockout
 */
function initKoBind(sel, mapping) {
    var doc = jQuery('body');
    if (sel) {
        doc = jQuery(sel);
    }
    doc.find('[data-ko-controller]').each(function (i, el) {
        var controller = jQuery(el).data('koController');
        var data = jQuery(el).data();
        var obj;
        if (/^[A-Z]/.match(controller) && typeof(eval(controller)) === 'function') {
            //create new instance
            obj = eval("new " + controller + "(data)");
            if (data['koControllerId']) {
                window[data['koControllerId']] = obj;
            }
            ko.applyBindings(obj, el);
        } else if (/^[a-z]/.match(controller) && mapping && typeof(mapping[controller]) === 'object') {

            ko.applyBindings(mapping[controller], el);
        }
    })
}