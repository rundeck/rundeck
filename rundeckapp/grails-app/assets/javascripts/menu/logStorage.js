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
//= require knockout-mapping
//= require ko/binding-url-path-param
//= require ko/binding-message-template
//= require ko/binding-popover

function ReqSet(stats) {
    "use strict";
    var self = this;
    self.stats=stats;
    self.max = ko.observable(20);
    self.offset = ko.observable(0);
    self.total = ko.observable(0);
    self.contents = ko.observableArray([]);
    self.maxInt = ko.pureComputed(function () {
        return parseInt(self.max());
    });
    self.offsetInt = ko.pureComputed(function () {
        return parseInt(self.offset());
    });
    self.mapping={
        contents:{
            key: function (data) {
                return ko.utils.unwrapObservable(data.id);
            }
        }
    }
}
/*
 js for "menu/logStorage.gsp" page
 */
function StorageStats(data) {
    "use strict";
    var self = this;
    self.baseUrl = data.baseUrl;
    self.requestsUrl = data.requestsUrl;
    self.missingUrl = data.missingUrl;
    self.resumeUrl = data.resumeUrl;
    self.cleanupUrl = data.cleanupUrl;
    self.tokensName = data.tokensName;
    self.loaded = ko.observable(false);
    self.loading = ko.observable(false);
    self.enabled = ko.observable(false);
    self.queuedCount = ko.observable(0);
    self.totalCount = ko.observable(0);
    self.partialCount = ko.observable(0);
    self.retriesCount = ko.observable(0);
    self.queuedRequestCount = ko.observable(0);
    self.queuedRetriesCount = ko.observable(0);
    self.queuedIncompleteCount = ko.observable(0);
    self.running = ko.observable(0);
    self.succeededCount = ko.observable(0);
    self.failedCount = ko.observable(0);
    self.incompleteCount = ko.observable(0);
    self.missingCount = ko.observable(0);
    self.incompleteRequests = ko.observable(new ReqSet(self));
    self.missingRequests = ko.observable(new ReqSet(self));
    self.retryDelay = ko.observable();
    self.reloadTime = ko.observable(10000);

    //0,1,2, indicates first panel view state
    self.progressView = ko.observable(0);
    self.percent = ko.pureComputed(function () {
        var suc = self.succeededCount();
        var total = self.totalCount();
        var val = total > 0 ? 100.0 * (suc / total) : 0;
        return val.toFixed(2);
    });
    self.percentText = ko.pureComputed(function () {
        return self.percent() + "%";
    });
    self.toggleProgressView = function () {
        self.progressView((self.progressView() + 1) % 3);
    };
    self.ajaxAction=function(url,opts){
        return jQuery.ajax({
            url: _genUrl(url,opts),
            method:'POST',
            beforeSend: _createAjaxSendTokensHandler(self.tokensName)
        }).success(_createAjaxReceiveTokensHandler(self.tokensName));
    };
    self.resumeAllIncomplete = function () {
        return self.ajaxAction(self.resumeUrl,{}).success(function(data){
            if(data.status=='ok'){
                self.reload();
            }
        });
    };
    self.resumeSingleIncomplete = function (obj) {
        var id=obj;
        if(typeof(obj)=='object' && obj.id){
            id=ko.unwrap(obj.id);
        }else if(typeof(obj)=='string'){
            id=obj;
        }
        return self.ajaxAction(self.resumeUrl,{id:id}).success(function(data){
            if(data.status=='ok'){
                self.reload();
            }
            if(typeof(obj)=='object' && data.contents){
                ko.mapping.fromJS(data.contents,{},obj);
            }
        });
    };
    self.cleanupAllIncomplete = function () {
        return self.ajaxAction(self.cleanupUrl,{}).success(function(data){
            if(data.status=='ok'){
                self.reload();
            }
        });
    };
    self.cleanupSingleIncomplete = function (obj) {
        var id=obj;
        if(typeof(obj)=='object' && obj.id){
            id=ko.unwrap(obj.id);
        }else if(typeof(obj)=='string'){
            id=obj;
        }
        return self.ajaxAction(self.cleanupUrl,{id:id}).success(function(data){
            if(data.status=='ok'){
                self.reload();
            }
            if(typeof(obj)=='object'){
                //remove obj from parent
                self.incompleteRequests().contents.remove(obj );
                // self.incompleteRequests().contents.remove(function (item) { return item.id() ==id ; } );
            }
        });
    };
    self.loadIncomplete = function () {
        jQuery.ajax({
            url: _genUrl(self.requestsUrl, {
                max: self.incompleteRequests().maxInt(),
                offset: self.incompleteRequests().offsetInt()
            }),
            dataType: 'json',
            success: function (data) {
                ko.mapping.fromJS(data, {}, self);
            }
        });
    };
    self.incompletePages = ko.computed(function () {
        var arr = [1];
        var tot = self.incompleteRequests().total();
        while (tot > 0) {
            tot = tot - self.incompleteRequests().maxInt();
            arr.push(arr.length + 1)
        }
        return arr;
    });
    self.hasIncompletePageForward = function () {
        var requests = self.incompleteRequests();
        var offset = requests.offsetInt();
        return offset + requests.maxInt() < requests.total();
    };
    self.incompletePageForward = function () {
        var requests = self.incompleteRequests();
        var offset = requests.offsetInt();
        if (offset + requests.maxInt() < requests.total()) {
            requests.offset(offset + requests.maxInt());
            self.loadIncomplete();
        }
    };
    self.hasIncompletePageBackward = function () {
        var requests = self.incompleteRequests();
        var val = requests.offsetInt() - requests.maxInt();
        return val >= 0;
    };
    self.incompletePageBackward = function () {
        var requests = self.incompleteRequests();
        var val = requests.offsetInt() - requests.maxInt();
        requests.offset(val >= 0 ? val : 0);
        self.loadIncomplete();
    };
    self.loadMissing = function () {
        jQuery.ajax({
            url: _genUrl(self.missingUrl, {
                max: self.missingRequests().maxInt(),
                offset: self.missingRequests().offsetInt()
            }),
            dataType: 'json',
            success: function (data) {
                ko.mapping.fromJS(data, {}, self);
            }
        });
    };
    self._timer=null;
    self.reload = function () {
        self.loading(true);
        clearTimeout(self._timer);
        jQuery.ajax({
            url: self.baseUrl,
            dataType: 'json',
            success: function (data) {
                ko.mapping.fromJS(data, {}, self);
                self.loaded(true);
                self.loading(false);
                if(self.incompleteRequests().total()>0){
                    self.loadIncomplete();
                }
                self._timer=setTimeout(self.reload, self.reloadTime());
            }
        });
    };
}
StorageStats.init = function (opts) {
    "use strict";
    var storage = new StorageStats(opts);
    jQuery(function () {
        "use strict";
        ko.applyBindings(storage);
        storage.reload();
    });
    return storage;
};
