/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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
function PageConfirm(message,opts){
    var self=this;
    self.message=message;
    self.needConfirm = false;

    self.setNeedsConfirm=function() {
        self.needConfirm = true;
        if(opts && typeof(opts.setNeedsConfirm)==='function'){
            opts.setNeedsConfirm()
        }
    };

    self.clearNeedConfirm=function() {
        self.needConfirm = false;
        if(opts && typeof(opts.clearNeedConfirm)==='function'){
            opts.clearNeedConfirm()
        }
    };
    self.watchConfirm=function(input){
        jQuery(input).on("change", self.setNeedsConfirm);
    };
    self.opts=opts

    jQuery(function () {
        if(!opts || !opts.skipbehavior) {
            let orig = window.onbeforeunload
            window.onbeforeunload = function (ev) {
                if (self.needConfirm) {
                    return self.message;
                }
                if (typeof (orig) === 'function') {
                    return orig(ev)
                }
            };
        }
        jQuery(document.body).find(':input').on("change", self.setNeedsConfirm);
        jQuery(document.body).find(':input').on("keydown", self.setNeedsConfirm);
        jQuery(document.body).find('.reset_page_confirm').on("click", self.clearNeedConfirm);
    });
}
