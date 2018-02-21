/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

"use strict";

//= require ko/extend-doubleBind

function MapEntry(data) {
    var self = this;
    self.key = ko.observable(data.key);
    self.value = ko.observable(data.value);
    self.map = data.map;
    self.index = ko.computed(function () {
        return self.map.entries.indexOf(self);
    });
    self.keyFieldName = ko.computed(function () {
        return self.map.inputPrefix() + self.index() + '.' + self.map.keyFieldIndicator();
    });
    self.valueFieldName = ko.computed(function () {
        return self.map.inputPrefix() + self.index() + '.' + self.map.valueFieldIndicator();
    });
    self.key.subscribe(self.map.kvchangewatch.bind(false));
    self.value.subscribe(self.map.kvchangewatch.bind(true));
}

function MultiMap(data) {
    var self = this;
    self.inputPrefix = ko.observable(data.inputPrefix || '');
    self.keyFieldIndicator = ko.observable(data.keyFieldIndicator || 'key');
    self.valueFieldIndicator = ko.observable(data.valueFieldIndicator || 'value');
    self.entries = ko.observableArray().extend({deferred: true});
    self.value = data.value || ko.observable();
    self.external = data.value;
    self.kvchange = ko.observable(0).extend({rateLimit: 500});
    self.kvchangewatch = function (x, v) {
        if (x || v) {
            self.kvchange(self.kvchange() + 1);
        }
    };

    self.mapping = {
        entries: {
            key: function (data) {
                return ko.utils.unwrapObservable(data.key);
            },
            create: function (options) {
                return new MapEntry(options.data);
            }
        }
    };
    self.load = function (data) {
        var arr = [];
        for (var prop in data) {
            if (data.hasOwnProperty(prop)) {
                arr.push({key: prop, value: data[prop], map: self});
            }
        }
        self.entries.removeAll();
        ko.mapping.fromJS({entries: arr}, self.mapping, self);
    };
    self.export = function () {
        return self.exportValues(self.entries());
    };
    self.exportValues = function (values) {
        var obj = {};
        ko.utils.arrayForEach(values, function (e) {
            if (e.key()) {
                obj[e.key()] = e.value() || '';
            }
        });
        return obj;
    };
    self.newEntry = function () {
        self.entries.push(new MapEntry({map: self}));
    };
    self.delete = function (entry) {
        self.entries.remove(entry);
    };
    if (data.data) {
        self.load(data.data);
    } else if (self.external) {
        self.load(self.value());
    }
    self.exportedObj = ko.computed({
        read: function () {
            var watch = self.kvchange();
            return self.exportValues(self.entries());
        },
        write: function (val) {
            self.load(val);
        }
    });
    self.importedObj = ko.observable();


    if (self.external) {
        //if using an externally provided observable, set up a two-way binding that won't repeat
        self.exportedObj.extend({doubleBind: self.value});
    } else {
        //else simply set the observable value
        self.exportedObj.subscribe(function (val) {
            self.value(val);
        });
    }


}