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

var NodeFiltersTest = function () {
    var self = this;
    var failed=0;
    var total=0;
    var assert = function (msg, expect, val) {
        total++;
        if (expect != val) {
            failed++;
            jQuery(document.body).append(jQuery('<div></div>').append(jQuery('<span class="text-danger"></span>').text("FAIL: " + msg + ": expected: " + expect + ", was: " + val)));
        } else {
            jQuery(document.body).append(jQuery('<div></div>').append(jQuery('<span class="text-success"></span>').text("OK: " + msg)));
        }
    };
    function mknf(data){
        return new NodeFilters('', '', '', data);
    }

    self.singlePageBasicTest=function(pref){
        var nf=mknf({});
        nf.pagingMax(5);
        nf.page(0);
        nf.allcount(5);
        nf.total(5);
        assert(pref+'pageRemaining',0,nf.pageRemaining());
        assert(pref+'hasMoreNodes',false,nf.hasMoreNodes());
        assert(pref+'hasMultiplePages',false,nf.hasMultiplePages());
    };
    self.twoPageTest=function(pref){
        var nf=mknf({});
        nf.pagingMax(5);
        nf.page(0);
        nf.allcount(6);
        nf.total(5);
        assert(pref+'basic',1,nf.pageRemaining());
        assert(pref+'hasMoreNodes',true,nf.hasMoreNodes());
        assert(pref+'hasMultiplePages',false,nf.hasMultiplePages());
    };
    self.multiPageTest=function(pref){
        var nf=mknf({});
        nf.pagingMax(5);
        nf.page(0);
        nf.allcount(20);
        nf.total(5);
        assert(pref+'basic',15,nf.pageRemaining());
        assert(pref+'hasMoreNodes',true,nf.hasMoreNodes());
        assert(pref+'hasMultiplePages',true,nf.hasMultiplePages());
    };

    self.defaultNodesTitleTest=function(pref){
        var nf=mknf({});
        nf.allcount(5);
        assert(pref+'plural','Nodes',nf.nodesTitle());
        nf.allcount(1);
        assert(pref+'singular','Node',nf.nodesTitle());
    };
    self.l18nNodesTitleTest=function(pref){
        var nf=mknf({nodesTitleSingular:'singular',nodesTitlePlural:'plural'});
        nf.allcount(5);
        assert(pref+'plural','plural',nf.nodesTitle());
        nf.allcount(1);
        assert(pref+'singular','singular',nf.nodesTitle());
    };

    self.testAll = function () {
        assert("Start: nodeFiltersKOTest.js", 1, 1);
        for (var i in self) {
            if (i.endsWith('Test')) {
                self[i].call(self, i + ': ');
            }
        }
        if(failed>0){

            jQuery(document.body).prepend(jQuery('<div></div>').append(jQuery('<span class="text-danger"></span>').text("FAIL: " + failed+"/"+total+" assertions failed")));
        }
    };
};
jQuery(function () {
    new NodeFiltersTest().testAll();
});
