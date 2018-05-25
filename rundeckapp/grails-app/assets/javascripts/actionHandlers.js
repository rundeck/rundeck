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

/**
 * Created by greg on 2/25/16.
 */
function ActionHandlers() {
    var self = this;
    self.handlers = {};
    self.registerHandler = function (key, func) {
        self.handlers[key] = func;
    };
    self.triggerHandler = function (evt,key, el) {
        if(self.handlers[key]!=null){
            if (el.is('a')) evt.preventDefault();
            self.handlers[key](el);
        }
    };

    /**
     * Register the handler for an action as a modal toggle
     * @param key
     */
    self.registerModalHandler=function(key,target,data){
        self.registerHandler(key,function(el){
           jQuery(target).modal(data);
        });
    };

    self.init = function () {
        jQuery(document.body).on('click', '.page_action', function (e) {

            var el = jQuery(this);
            var handler = el.data('action');
            self.triggerHandler(e, handler, el);
        });
    };

}
var PageActionHandlers;
jQuery(function () {
    PageActionHandlers = new ActionHandlers();
    PageActionHandlers.init();
});