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

var VersionIdentityTest = function () {
    var self = this;
    var assert = function (msg, expect, val) {
        if (expect != val) {
            jQuery(document.body).append(jQuery('<div></div>').append(jQuery('<span class="text-danger"></span>').text("FAIL: " + msg + ": expected: " + expect + ", was: " + val)));
        } else {
            jQuery(document.body).append(jQuery('<div></div>').append(jQuery('<span class="text-success"></span>').text("OK: " + msg)));
        }
    }
    function mkvid(vers){
          return new VersionIdentity({versionString:vers});
    }
    self.basicTest = function (pref) {
        assert(pref + "success", 1, 1);
        var data = mkvid('0').data();
        assert(pref + "major", 0, data.major);
        assert(pref + "minor", 0, data.minor);
        assert(pref + "point", 0, data.point);
        assert(pref + "release", 1, data.release);
        assert(pref + "tag", '', data.tag);
    };
    self.fullTest = function (pref) {
        var data = mkvid('2.3.4-5-SNAPSHOT').data();
        assert(pref + "major", 2, data.major);
        assert(pref + "minor", 3, data.minor);
        assert(pref + "point", 4, data.point);
        assert(pref + "release", 5, data.release);
        assert(pref + "tag", 'SNAPSHOT', data.tag);
        assert(pref + "version", '2.3.4-5-SNAPSHOT', data.version);
    };
    self.noReleaseTest = function (pref) {
        var data = mkvid('2.3.4-SNAPSHOT').data();
        assert(pref + "major", 2, data.major);
        assert(pref + "minor", 3, data.minor);
        assert(pref + "point", 4, data.point);
        assert(pref + "release", 1, data.release);
        assert(pref + "tag", 'SNAPSHOT', data.tag);
    };
    self.noTagTest = function (pref) {
        var data = mkvid('2.3.4-5').data();
        assert(pref + "major", 2, data.major);
        assert(pref + "minor", 3, data.minor);
        assert(pref + "point", 4, data.point);
        assert(pref + "release", 5, data.release);
        assert(pref + "tag", '', data.tag);
    };
    self.multiTest = function (pref) {
        var data = mkvid('2.3.4-SNAPSHOT (other-data)').data();
        assert(pref + "major", 2, data.major);
        assert(pref + "minor", 3, data.minor);
        assert(pref + "point", 4, data.point);
        assert(pref + "release", 1, data.release);
        assert(pref + "tag", 'SNAPSHOT', data.tag);
        assert(pref + "version", '2.3.4-SNAPSHOT', data.version);
    };
    self.testAll = function () {
        assert("Start tests", 1, 1);
        for (var i in self) {
            if (i.endsWith('Test')) {
                self[i].call(self, i + ': ');
            }
        }
    };
};
jQuery(function () {
    new VersionIdentityTest().testAll();
});
