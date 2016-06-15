var TestHarness = function (name,data) {
    var self = this;
    self.name=name;
    var failed=0;
    var total=0;
    self.compare=function(expect,val){
        "use strict";
        if(typeof(expect)!=typeof(val)){
            return false;
        }
        if(expect==null && val!=null || expect!=null && val==null){
            return false;
        }
        //todo: array and object compare
        if(expect!=null && val!=null && typeof(expect)=='object'){
            if(expect.length!=val.length){
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
        jQuery('#js_test').append(jQuery('<div></div>').append(jQuery('<span class="text-danger"></span>').text(message)));
    };
    self.ok=function(msg){
        "use strict";
        jQuery('#js_test').append(jQuery('<div></div>').append(jQuery('<span class="text-success"></span>').text("OK: " + msg)));
    };
    self.assert = function (msg, expect, val) {
        total++;
        if (!self.compare(expect,val)) {
            failed++;
            var message = "FAIL: " + msg + ": expected: " + JSON.stringify(expect) + ", was: " + JSON.stringify(val);
            self.error(message);
            try{
                throw new Error("assert failed: "+message);
            }catch(e){
                console.log(e,e.stack);
            }
        } else {
            self.ok(msg);
        }
    };
    self.log = function (msg, data) {
        jQuery('#js_test').append(jQuery('<div></div>').append(jQuery('<span class="text-' +
            'info"></span>').text("LOG: " + msg)));
        jQuery('#js_test').append(jQuery('<div></div>').append(jQuery('<span class="text-info"></span>').text(data)));
    };

    self.testMatrix=function(name,dataset,tester){
        "use strict";

        dataset.each(function (t) {
            var val2 = tester(t[0]);
            self.assert(messageTemplate(name,[JSON.stringify(t[0]),JSON.stringify(t[1])]), t[1], val2);
        });
    };


    self.testAll = function () {
        jQuery(document.body).append(jQuery('<div id="js_test" class="collapse in"></div>'));
        self.assert("Start: "+self.name, 1, 1);
        for (var i in self) {
            if (i.endsWith('Test')) {
                try {
                    self[i].call(self, i + ': ');
                }catch(e){
                    self.assert(i + ':', null, e);
                    console.log("error",e,e.stack);
                }
            }
        }
        if(failed>0){
            jQuery('#js_test').prepend(jQuery('<div></div>').append(jQuery('<span class="text-danger"></span>').text("FAIL: " + failed+"/"+total+" assertions failed")));
        }else{
            jQuery('#js_test').collapse('hide');
            jQuery(document.body).append('<div></div>')
                .append('<span class="btn btn-link text-success" data-toggle="collapse" data-target="#js_test">OK: '+total+' Tests Passed</span>')
        }
    };
    for(var val in data){
        self[val]=data[val].bind(self);
    }

    jQuery(function () {
        self.testAll();
    });
};

