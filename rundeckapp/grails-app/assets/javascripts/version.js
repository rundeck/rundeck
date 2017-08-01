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
var RundeckVersion = function (data) {
    var self = this;
    self.versionString = data['versionString'];
    self.versionData = {};
    self.versionDate = data['versionDate'];
    self.colorIdentity = data && data.colorIdentity ? data.colorIdentity : 'minorPoint';
    self.nameIdentity = data && data.nameIdentity ? data.nameIdentity : 'majorMinor';
    self.iconIdentity = data && data.iconIdentity ? data.iconIdentity : 'minorPoint';
    self.appId = data && data['appId'] ? data['appId'] : 'Rundeck';
    self.serverName = data && data['serverName'] ? data['serverName'] : null;
    self.csscolors = [
        'BlueViolet',
        'CadetBlue',
        'Chocolate',
        'CornflowerBlue',
        'Crimson',
        'DodgerBlue',
        'FireBrick',
        'ForestGreen',
        'Fuchsia',
        'Goldenrod',
        'HotPink',
        'Indigo',
        'LimeGreen',
        'Magenta',
        'Maroon',
        'MidnightBlue',
        'Navy',
        'Olive',
        'OrangeRed',
        'Purple',
        'RoyalBlue',
        'SaddleBrown',
        'SeaGreen',
        'Sienna',
        'SlateBlue',
        'SteelBlue',
        'Teal',
        'Tomato',
        'Violet'
    ];

    self.glyphicons = [
        'bell',
        'book',
        'briefcase',
        'bullhorn',
        'camera',
        'cutlery',
        'flag',
        'flash',
        'gift',
        'globe',
        'headphones',
        'leaf',
        'music',
        'paperclip',
        'phone',
        'plane',
        'pushpin',
        'tower',
        'glass',
        'knight',
        'tent',
        'apple',
        'lamp',
        'piggy-bank',
        'grain',
        'sunglasses'
    ];

    self.names = [
        'Americano',
        'Cafe Au Lait',
        'Cafe Bonbon',
        'Cafecito',
        'Cafe Cubano',
        'Caffe Latte',
        'Cafe Mocha',
        'Cappuccino',
        'Caramel Latte',
        'Coconut Latte',
        'Con Panna',
        'Doppio Espresso',
        'Dry Cappuccino',
        'Espresso Breve',
        'Eye Opener',
        'Hammerhead',
        'Macchiato',
        'Pumpkin Spice Latte',
        'Ristretto',
        'Solo Espresso',
        'Toffee Latte',
        'Turkish Coffee',
        'Vanilla Latte',
        'Viennese Espresso'
    ];
    self.splitVersion = function (versionString) {
        var partsa = String(versionString).split(' ');
        var version = partsa.length > 1 ? partsa[0] : versionString;
        var parts = String(version).split('-');
        var vparts = parts[0].split('\.');
        var data = {version: version};
        if (vparts.length > 0) {
            data['major'] = parseInt(vparts[0]);
        } else {
            data['major'] = 0;
        }
        if (vparts.length > 1) {
            data['minor'] = parseInt(vparts[1]);
        } else {
            data['minor'] = 0;
        }
        data['majorMinor'] = (data.major * 10) + data.minor;
        if (vparts.length > 2) {
            data['point'] = parseInt(vparts[2]);
        } else {
            data['point'] = 0;
        }
        data['minorPoint'] = (data.minor * 20) + data.point;
        var release = 1;
        var tag = '';
        if (parts.length > 1 && /^\d+$/.test(parts[1])) {
            release = parseInt(parts[1]);
            tag = parts.length > 2 ? parts.slice(2).join('-') : '';
        } else if (parts.length > 1) {
            tag = parts.slice(1).join('-');
        }

        data['tag'] = tag;
        data['release'] = release;
        data['pointRelease'] = data.point * 20 + release;
        data['minorPointRelease'] = (data.minor * 100) + data.point * 20 + release;
        data['full'] = data.major * 100 + data.minor * 20 + data.point;
        return data;
    };
    self.splitUUID = function (versionString) {
        var partsa = String(versionString).split('-');
        var apart = partsa.length > 0 ? partsa[0].substring(0, 2) : versionString;
        var data = {uuid:versionString};
        for (var i = 0; i < partsa.length; i++) {
            data['uuid' + i] = parseInt(partsa[i].substring(0, 2), 16);
            data['hexuuid' + i] = partsa[i];
        }
        var partsb = partsa.join('');
        var sixes = [];
        for (var j = 0; (j + 1) * 6 < partsb.length; j++) {
            data['6let' + i] = partsb.substring(j * 6, (j + 1) * 6);
            sixes.push(partsb.substring(j * 6, (j + 1) * 6));
        }
        data['sixes'] = sixes;
        return data;
    };
    self.inList = function (list, val) {
        return list[val % list.length];
    };
    self.colorForVersion = function (val) {
        return self.inList(self.csscolors, val);
    };
    self.nameForVersion = function (val) {
        return self.inList(self.names, val);
    };
    self.iconForVersion = function (val) {
        return self.inList(self.glyphicons, val);
    };
    self.data = function () {
        return self.versionData;
    };
    self.color = function () {
        return self.colorForVersion(self.versionData[self.colorIdentity]);
    };
    self.name = function () {
        return self.nameForVersion(self.versionData[self.nameIdentity]);
    };
    self.icon = function () {
        return self.iconForVersion(self.versionData[self.iconIdentity]);
    };
    self.text = function () {
        var sep = ' ';
        return [self.name(), self.color(), self.icon()].join(sep).toLowerCase().replace(/[^a-z]/g, sep);
    };

    if (self.versionString) {
        self.versionData = self.splitVersion(self.versionString);
    } else if (data.serverUuid) {
        self.versionData = self.splitUUID(data.serverUuid);
    }
};