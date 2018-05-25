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
/*
 * Useful for i18n messages; replace the text content of an element, by substituting values into placeholders.
 * Placeholders are in the form '{0}','{1}', etc.  The "messageTemplate" binding value can be a single value, which
 * will be used for {0}, or it can be an object with a 'value' property, possibly observable, containing an array
 * for the replacement values.
 * If a binding "messageTemplatePluralize: true" is set, then the template text is treated as a singular and a plural
 * version of the same text, separated by "|" character.  If the first bound data value is "1", then singular form
 * is used, otherwise the plural form is used.
 *
 */
ko.bindingHandlers.messageTemplate = {
    init: function (element, valueAccessor, allBindings, viewModel, bindingContext) {

        var text=jQuery(element).text();
        jQuery(element).data('ko-message-template',text);
        return { 'controlsDescendantBindings': true };
    },
    update:function(element, valueAccessor, allBindings, viewModel, bindingContext){
        var pluralize=allBindings.get('messageTemplatePluralize');
        var data=ko.utils.unwrapObservable(valueAccessor());
        var template=jQuery(element).data('ko-message-template');
        
        var text = messageTemplate(template,data,pluralize);
        ko.utils.setTextContent(element, text);
    }
};
