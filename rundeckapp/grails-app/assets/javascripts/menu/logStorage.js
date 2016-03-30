//= require knockout.min
//= require knockout-mapping

/*
 js for "menu/logStorage.gsp" page
 */
function StorageStats(baseUrl) {
    "use strict";
    var self = this;
    self.baseUrl=baseUrl;
    self.loaded = ko.observable(false);
    self.loading = ko.observable(false);
    self.enabled = ko.observable(false);
    self.queuedCount = ko.observable(0);
    self.totalCount = ko.observable(0);
    self.succeededCount = ko.observable(0);
    self.failedCount = ko.observable(0);
    self.incompleteCount = ko.observable(0);
    self.missingCount = ko.observable(0);
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
    self.reload = function () {
        self.loading(true);
        jQuery.ajax({
            url: self.baseUrl,
            dataType: 'json',
            success: function (data) {
                ko.mapping.fromJS(data, {}, self);
                self.loaded(true);
                self.loading(false);
                setTimeout(self.reload, self.reloadTime());
            }
        });
    };
}
StorageStats.init=function(url){
    "use strict";
    jQuery(function () {
        "use strict";
        var storage = new StorageStats(url);
        ko.applyBindings(storage);
        storage.reload();
    });
};