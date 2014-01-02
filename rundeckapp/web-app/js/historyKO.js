/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */


function Report(data) {
    var self = this;
    self.dateCompleted = ko.observable();
    self.dateStarted = ko.observable();
    self.execution = ko.observable();
    self.executionHref = ko.observable();

    self.duration = ko.computed(function () {
        return MomentUtil.duration(ko.utils.unwrapObservable(self.dateStarted()), ko.utils.unwrapObservable(self.dateCompleted()));
    });
    self.durationSimple = ko.computed(function () {
        return MomentUtil.formatDurationSimple(self.duration());
    });
    self.durationHumanize = ko.computed(function () {
        return MomentUtil.formatDurationHumanize(self.duration());
    });
    self.startTimeFormat = function (format) {
        return MomentUtil.formatTime(self.dateStarted(), format);
    };
    self.endTimeSimple = ko.computed(function () {
        return MomentUtil.formatTimeSimple(self.dateCompleted());
    });
    self.endTimeFormat = function (format) {
        var value = self.dateCompleted();
        return MomentUtil.formatTime(value, format);
    };
    self.nodeFailCount = ko.computed(function () {
        var ncount = ko.utils.unwrapObservable(self.node);
        var ns = ncount.split('/');
        if (ns.length == 3) {
            return parseInt(ns[1]);
        }
    });

    self.nodeSucceedCount = ko.computed(function () {
        var ncount = ko.utils.unwrapObservable(self.node);
        var ns = ncount.split('/');
        if (ns.length == 3) {
            return parseInt(ns[0]);
        }
    });
    ko.mapping.fromJS(data, {}, self);
}
function History(ajaxHistoryLink) {
    var self = this;
    self.ajaxHistoryLink = ajaxHistoryLink;
    self.reports = ko.observableArray([]);
    self.href = ko.observable();
    self.selected = ko.observable(false);
    self.max = ko.observable(20);
    self.total = ko.observable(0);
    self.offset = ko.observable(0);
    self.params = ko.observable();
    self.count = ko.computed(function () {
        return self.reports().length + self.offset() * self.max();
    });
    self.pageCount=ko.computed(function(){
        return totalPageCount(self.max(),self.total());
    });
    self.pages=ko.computed(function(){
        var total = self.total();
        var offset = self.offset();
        var max = self.max();
        var href = self.href();
        if (total < 1 || !href) {
            return '';
        }
        var pages = [];
        //remove offset/max params from href
        href=href.replace(/[&\?](offset|max)=\d+/ig,'');

        foreachPage(offset, max, total, {maxSteps:10}, function (pg) {
            var a;
            var url = _genUrl(href, {offset: pg.offset, max: pg.max});
            var label = pg.prevPage ? 'Previous' : pg.nextPage ? 'Next' : pg.skipped? '…' : (pg.page);
            pages.push(ko.utils.extend({url:url,label:label},pg));
        });

        return pages;
    });
    self.visitPage=function(page){
        loadHistoryLink(self,self.ajaxHistoryLink,page.url);
    };
}

var binding = {
    'reports': {
        key: function (data) {
            return ko.utils.unwrapObservable(data.id);
        },
        create: function (options) {
            return new Report(options.data);
        }
    }
};
function loadHistoryLink(history, ajaxBaseUrl, href) {
    var params = href.substring(href.indexOf('?'));
    var url = ajaxBaseUrl + params;

    jQuery.getJSON(url, function (data) {
        history.selected(true);
        ko.mapping.fromJS(Object.extend(data, { href: href, params: params }), binding, history);
    });
}
