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
function initKoBind (sel, mapping,debug) {
    let doc = jQuery('body')
    if (sel) {
        doc = jQuery(sel)
    }
    if(debug){
        console.log(`${debug}: start binding for ${!sel?'body':sel}:`, mapping,doc)
    }
    doc.find('[data-ko-bind]').each(function (i, el) {
        let jqel = jQuery(el)
        const controller = jqel.data('koBind')
        if(mapping[controller] && jqel.data('koBoundController')){
            //already bound, skip
            if(debug){
                console.log(`${debug}: warning: ko-bind for ${controller} : another is already bound:`, el, jqel.data('koBoundController'))
            }
            return
        }
        const data = jqel.data()
        if (controller.match(/^[A-Z]/)) {
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
            jqel.data('koBoundController',obj)
            if(debug){
                console.log(`${debug}: (bind): ko-bind for new ${controller} `, el, obj)
            }
        } else if (
            controller.match(/^[a-z]/) &&
            mapping &&
            typeof (mapping[controller]) === 'object') {
            let ctrl = mapping && mapping[controller]
            ko.applyBindings(ctrl, el)

            jqel.data('koBoundController',ctrl)
            if(debug){
                console.log(`${debug}: (bind): ko-bind for mapped: ${controller}`, el, ctrl)
            }
        } else if(debug) {
            console.log(`${debug}: warning: ko-bind for ${controller} : controller not found.`, el)
        }
    })
}
