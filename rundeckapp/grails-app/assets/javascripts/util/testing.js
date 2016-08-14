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

var TestHarness = function (name,data) {
    var self = this;
    self.name=name;
    var failed=0;
    var total=0;
    var NDX=Math.floor((Math.random() * 1024));
    var ident='js_test_'+NDX;
    var curPrefix='';
    self.compare=function(expect,val){
        "use strict";
        if(typeof(expect)!=typeof(val)){
            return false;
        }
        if(expect==null && val!=null || expect!=null && val==null){
            return false;
        }
        //todo: array and object compare
        if(expect!=null && val!=null && jQuery.type(expect)=='array' && jQuery.type(val)=='array'){
            if(expect.length!=val.length){
                return false;
            }
            for(var i=0;i<expect.length;i++){
                if(!self.compare(expect[i],val[i])){
                    return false;
                }
            }
            return true;
        }else if (expect!=null && val!=null && typeof(expect)=='object'){
            if(Object.keys(expect).length!=Object.keys(val).length){
                return false;
            }
            for(var p in expect){
                if(!self.compare(expect[p],val[p])){

                    return false;
                }
            }
            for(var p in val){
                if(!self.compare(expect[p],val[p])){
                    return false;
                }
            }
            return true;
        }
        return expect===val;
    };
    self.error=function(message){
        "use strict";
        jQuery('#'+ident).append(jQuery('<div class="js-test-failure"></div>').append(jQuery('<span class="text-danger"></span>').text(message)));
    };
    self.ok=function(msg){
        "use strict";
        jQuery('#'+ident).append(jQuery('<div></div>').append(jQuery('<span class="text-success"></span>').text("OK: " + msg)));
    };
    self.assert = function (msg, expect, val) {
        total++;
        if(null==expect && null==val && typeof(msg)!='string'){
            expect=true;
            val=msg;
            msg='(assert)';
        }else if(null==val && typeof(expect)=='string' && typeof(msg)=='boolean'){
            val=msg;
            msg=expect;
            expect=true;
        }
        if (!self.compare(expect,val)) {
            failed++;
            var message = "FAIL: " +curPrefix+ msg + ": expected: " + JSON.stringify(expect) + ", was: " + JSON.stringify(val);
            self.error(message);
            try{
                throw new Error("assert failed: "+message);
            }catch(e){
                console.log(e,e.stack);
            }
        } else {
            self.ok(curPrefix+msg);
        }
    };
    self.log = function (msg, data) {
        jQuery('#'+ident).append(jQuery('<div></div>').append(jQuery('<span class="text-' +
            'info"></span>').text("LOG: " + msg)));
        if(data){
            jQuery('#'+ident).append(jQuery('<div></div>').append(jQuery('<span class="text-info"></span>').text(data)));
        }
    };

    self.testMatrix=function(name,dataset,tester){
        "use strict";

        dataset.forEach(function (t) {
            var val2 = tester(t[0]);
            self.assert(messageTemplate(name,[JSON.stringify(t[0]),JSON.stringify(t[1])]), t[1], val2);
        });
    };
    self.holder={};
    self.prepare=function(){
        "use strict";

        if (typeof(window.Messages) == 'object') {
            self.holder['Messages']= Messages;
            var t={};
            for(var p in Messages){
                t[p]=p;
            }
            window.Messages=t;
        }
    };
    self.restore=function(){
        "use strict";
        window.Messages=self.holder['Messages'];
        self.holder={};
    };


    self.testAll = function () {
        self.prepare();
        jQuery(document.body).append(jQuery('<div id="'+ident+'" class="collapse in"></div>'));
        var jQuery2 = jQuery('#'+ident);
        self.log("Start: "+self.name);
        for (var i in self) {
            if (i.endsWith('Test')) {
                try {
                    curPrefix= i + ': ';
                    self[i].call(self, i + ': ');
                }catch(e){
                    self.assert('caught error running test: '+e, 'ok', 'exception');
                    console.log("error",e,e.stack);
                }
            }
        }
        curPrefix='';
        if(failed>0){
            jQuery2.prepend(jQuery('<div></div>').append(jQuery('<span class="text-danger"></span>').text("FAIL: " + failed+"/"+total+" assertions failed")));
        }else{
            jQuery2.collapse('hide');
            jQuery(document.body).append('<div></div>')
                .append('<span class="btn btn-link text-success" data-toggle="collapse" data-target="#'+ident+'">OK: '+total+' Tests Passed</span>')
        }
        self.restore();
    };
    for(var val in data){
        self[val]=data[val].bind(self);
    }

    jQuery(function () {
        self.testAll();
    });
};

