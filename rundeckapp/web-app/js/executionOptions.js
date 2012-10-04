/*
 * Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var ExecutionOptions = {
    multiVarCheckboxChangeHandler : function(optname,evt) {
        var check = Event.element(evt);
        if ("" == $(check).value && !$(check).checked) {
            //remove it
            var div = $(check.parentNode);
            $(div).parentNode.removeChild(div);
        }
    },
    multiVarCheckboxChangeWarningHandler: function(optname,evt) {
        var check = Event.element(evt);
        //show warning text if all checkboxes unchecked
        var parent = $(check).up('div.optionmultiarea');
        var test = true;
        $(parent).select("input[type='checkbox']").each(function(e) {
            if ($(e).checked) {
                test = false;
            }
        });
        if (test) {
            $$('#' + optname + '_state span.reqwarning').each(Element.show);
        }else{
            $$('#' + optname + '_state span.reqwarning').each(Element.hide);
        }
    },
     multiVarInputChangeHandler : function(check, evt) {
        var input = Event.element(evt);
        $(check).value = $(input).value;
    },
     multiVarInputKeydownHandler : function(check, evt) {
        var input = Event.element(evt);
        if (noenter(evt)) {
            $(check).value = $(input).value;
            return true;
        } else {
            return false;
        }
    },
    addMultivarValue: function(name, inputarea, value,handler) {
        var div = new Element('div');
        div.addClassName('optionvaluemulti');
        div.setStyle({'opacity':'0'});

        var divwrap = new Element('div');
        divwrap.addClassName('varinput');

        var inpu = new Element('input');
        inpu.setAttribute("type", "checkbox");
        inpu.setAttribute("name", "extra.option." + name);
        inpu.setAttribute("checked", "true");
        inpu.setAttribute("value", null != value ? value : "");
        Event.observe(inpu, 'change', ExecutionOptions.multiVarCheckboxChangeWarningHandler.curry(name));
        Event.observe(inpu, 'change', ExecutionOptions.multiVarCheckboxChangeHandler.curry(name));
        if(handler){
            handler(name, inpu);
        }

        var inpu2 = new Element('input');
        inpu2.setAttribute("type", "text");
        inpu2.setAttribute("name", "_temp");
        inpu2.setAttribute("placeholder", "Enter value");
        if (null != value) {
            inpu2.setAttribute("value", value);
        }
        Event.observe(inpu2, 'change', ExecutionOptions.multiVarInputChangeHandler.curry(inpu));
        Event.observe(inpu2, 'keydown', ExecutionOptions.multiVarInputKeydownHandler.curry(inpu));
        if (handler) {
            handler(name,inpu2);
        }

        $(divwrap).appendChild(inpu);
        $(divwrap).appendChild(inpu2);
        $(div).appendChild(divwrap);
        $(inputarea).insert({top:div});
        $$('#' + name + '_state span.reqwarning').each(Element.hide);
        Try.these(
            function() {
                Effect.Appear(div, {duration:0.5});
            },
            function() {
                $(div).show();
                $(div).highlight();
            }
            );

        $(inpu2).focus();
    }
};