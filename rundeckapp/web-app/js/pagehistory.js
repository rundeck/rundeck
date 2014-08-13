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


/** START history
 *
 */
var HistoryControl = Class.create({
    hiliteSince:0,
    histLoading : false,
    lastHistload : 0,
    histTimer : null,
    target:null,
    defaultParams:{compact:true,nofilters:true,recentFilter:'1d'},
    initialize:function(elem, params) {
        this.target = elem;
        if (params) {
            this.defaultParams = params;
        }
    },
    setHiliteSince:function(val){
        this.hiliteSince=val;
    },
    timedLoadHistory:function(xparams) {
        this.histTimer = null;
        this.loadHistory(xparams);
    },
    loadHistory:function(xparams) {
        var tNow = new Date().getTime();
        var obj = this;
        if (tNow - this.lastHistload < 3000 || this.histLoading || this.histTimer) {
            if (!this.histLoading && !this.histTimer && this.lastHistload) {
                var when = (this.lastHistload + 3000) - tNow;
                this.histTimer = setTimeout(function(){obj.timedLoadHistory(xparams);}, when);
            }
            return;
        } else {
            this.lastHistload = tNow;
            this.histLoading = true;
        }
        var params = {};//{projFilter:this.project}
        Object.extend(params, this.defaultParams);
        if (this.hiliteSince) {
            params.hiliteSince = this.hiliteSince;
        }
        if(xparams){
            Object.extend(params,xparams);
        }
        jQuery('#' + this.target).load(
            _genUrl(appLinks.reportsEventsFragment, params),
            function (response, status, xhr) {
                obj.histLoading = false;
                if (status == "error") {

                }else{
                    Element.show(obj.target);
                    obj.doHistoryHilite(obj.target);
                }
            }
        );
    },

    doHistoryHilite:function(param) {
        $(param).select('.newitem').each(function(e) {
            doyft(Element.identify(e));
        });
    }
});
