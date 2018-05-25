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
 * Useful for generated URLs; replace a path element of a href URL, by substituting values into placeholders.
 * Placeholders are in the form '<$>'.  The "messageTemplate" binding value can be a single value, which
 * will be used for the first $, or it can be an object with a 'value' property, possibly observable, containing an array
 * for the replacement values.
 *
 */
ko.bindingHandlers.urlPathParam = {
    init: function (element, valueAccessor, allBindings, viewModel, bindingContext) {

        var text=jQuery(element).attr('href');
        jQuery(element).data('ko-orig-href',text);
    },
    update:function(element, valueAccessor, allBindings, viewModel, bindingContext){
        var data=ko.utils.unwrapObservable(valueAccessor());
        var template=jQuery(element).data('ko-orig-href');
        var values=[];
        if(typeof(data)!='object'){
            values=[data];
        }else if(jQuery.isArray(data)){
            values=data;
        }else if(typeof(data)=='object'){
            values=ko.utils.unwrapObservable(data['value']);
            if(!jQuery.isArray(values)){
                values=[values];
            }
        }
        for(var i=0;i<values.length;i++){
            values[i] = ko.utils.unwrapObservable(values[i]);
        }
        var count=0;
        var text = template.replace(/%3C%24%3E/ig,function(match, offset, string){
            return values[count++];
        });
        element.setAttribute('href',text);
    }
};
