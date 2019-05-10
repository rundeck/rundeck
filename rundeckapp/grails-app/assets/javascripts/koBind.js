/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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


"use strict"

/**
 * bind via knockout, e.g. data-ko-bind="varName" or data-ko-bind="TypeName"
 */
function initKoBind (sel, mapping) {
    let doc = jQuery('body')
    if (sel) {
        doc = jQuery(sel)
    }
    doc.find('[data-ko-bind]').each(function (i, el) {
        const controller = jQuery(el).data('koBind')
        const data = jQuery(el).data()
        if (/^[A-Z]/.match(controller)) {
            const ctrl = eval(controller)
            if (typeof (ctrl) !== 'function') {
                return
            }
            //create new instance
            let obj = new ctrl(data)
            if (data['koBindVar']) {
                window[data['koBindVar']] = obj
            }
            ko.applyBindings(obj, el)
        } else if (
            /^[a-z]/.match(controller) &&
            mapping &&
            typeof (mapping[controller]) === 'object' ||
            typeof (window[controller]) === 'object') {
            ko.applyBindings(mapping && mapping[controller] || window[controller], el)
        }
    })
}
