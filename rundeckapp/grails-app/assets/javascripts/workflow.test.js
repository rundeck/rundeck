//= require workflow

RDWorkflow.assertObjEq = function (arr1,arr2) {
    "use strict";
    console.assert(arr1.length==arr2.length,arr1,arr2);
    if(arr1.length!=arr2.length){
        return false;
    }
    for(var prop in arr1){
        if(arr1.hasOwnProperty(prop)){
            console.assert(arr1[prop]==arr2[prop],arr1[prop],arr2[prop],"prop "+prop);
        }
    }
    return true;
};
RDWorkflow.assertArrayEq = function (arr1,arr2) {
    "use strict";

    console.assert(arr1.length==arr2.length,arr1,arr2);
    if(arr1.length!=arr2.length){
        return false;
    }
    for(var i=0;i<arr1.length;i++){
        console.assert(arr1[i]==arr2[i],arr1[i],arr2[i]);
    }
    return true;
};
RDWorkflow.test = function () {

    RDWorkflow.assertObjEq(RDWorkflow.unescape('abc/123','\\',['\\','/'],['/']),{text:'abc',bchar:'/',rest:'123'});
    RDWorkflow.assertObjEq(RDWorkflow.unescape('abc/123/456','\\',['\\','/'],['/']),{text:'abc',bchar:'/',rest:'123/456'});
    RDWorkflow.assertObjEq(RDWorkflow.unescape('a\\/bc/123/456','\\',['\\','/'],['/']), {text:'a/bc',bchar:'/',rest:'123/456'});

    RDWorkflow.assertArrayEq(RDWorkflow.splitEscaped('a\\/bc/123/456','/') , ['a/bc','123','456']);
    RDWorkflow.assertArrayEq(RDWorkflow.splitEscaped('a\\/b@c/1,2=3/4\\\\56','/') , ['a/b@c','1,2=3','4\\56']);



    console.assert(RDWorkflow.paramsForContextId('2@node=a') === 'node=a');
    console.assert(RDWorkflow.paramsForContextId('2@node\\=a') === 'node=a');
    console.assert(RDWorkflow.stepNumberForContextId('2@node=a') === 2);
    console.assert(RDWorkflow.workflowIndexForContextId('2@node=a') === 1);
    console.assert(RDWorkflow.isErrorhandlerForContextId('2@node=a') === false);
    console.assert(RDWorkflow.paramsForContextId('2') === null);
    console.assert(RDWorkflow.stepNumberForContextId('2') === 2);
    console.assert(RDWorkflow.workflowIndexForContextId('2') === 1);
    console.assert(RDWorkflow.isErrorhandlerForContextId('2') === false);
    console.assert(RDWorkflow.paramsForContextId('2e') === null);
    console.assert(RDWorkflow.stepNumberForContextId('2e') === 2);
    console.assert(RDWorkflow.workflowIndexForContextId('2e') === 1);
    console.assert(RDWorkflow.isErrorhandlerForContextId('2e') === true);
    console.assert(RDWorkflow.paramsForContextId('2e@blah=c') === 'blah=c');
    console.assert(RDWorkflow.stepNumberForContextId('2e@blah=c') === 2);
    console.assert(RDWorkflow.workflowIndexForContextId('2e@blah=c') === 1);
    console.assert(RDWorkflow.isErrorhandlerForContextId('2e@blah=c') === true);
    console.assert(RDWorkflow.stepNumberForContextId('1e@blah=c') === 1);
    console.assert(RDWorkflow.workflowIndexForContextId('1e@blah=c') === 0);
    console.assert(RDWorkflow.isErrorhandlerForContextId('1e@blah=c') === true);

    //parse context id
    RDWorkflow.assertArrayEq(RDWorkflow.parseContextId('1'),['1']);
    RDWorkflow.assertArrayEq(RDWorkflow.parseContextId('1/1'),['1','1']);
    RDWorkflow.assertArrayEq(RDWorkflow.parseContextId('1/1/1'),['1','1','1']);
    RDWorkflow.assertArrayEq(RDWorkflow.parseContextId('1/2/3'),['1','2','3']);
    RDWorkflow.assertArrayEq(RDWorkflow.parseContextId('1e@abc/2/3'),['1e@abc','2','3']);
    RDWorkflow.assertArrayEq(RDWorkflow.parseContextId('1/2e@asdf=xyz/3'),['1','2e@asdf=xyz','3']);
    RDWorkflow.assertArrayEq(RDWorkflow.parseContextId('2@node=crub\\/dub-1/1'),['2@node=crub/dub-1','1']);


    //clean context id
    console.assert(RDWorkflow.cleanContextId('1/2/3')==='1/2/3','wrong value');
    console.assert(RDWorkflow.cleanContextId('1e@abc/2/3')==='1/2/3','wrong value');
    console.assert(RDWorkflow.cleanContextId('1/2e@asdf=xyz/3')==='1/2/3','wrong value');

    //render string, with descriptions
    var wf1=new RDWorkflow([{"type":"example-node-step","nodeStep":true,"configuration":{"example":"whatever"}}],{
        nodeSteppluginDescriptions:{
            "example-node-step":{
                "title":"blah"
            }
        },
        wfSteppluginDescriptions:{}
    });
    console.assert(wf1.renderContextString("1") === "blah");

    var wf2=new RDWorkflow([{"type":"example-node-step","nodeStep":false,"configuration":{"example":"whatever"}}],{
        nodeSteppluginDescriptions:{},
        wfSteppluginDescriptions:{
            "example-node-step":{
                "title":"blah"
            }
        }
    });
    console.assert(wf2.renderContextString("1") === "blah");

    //render string, missing descriptions
    var wf3=new RDWorkflow([{"type":"example-node-step","nodeStep":true,"configuration":{"example":"whatever"}}],{
        nodeSteppluginDescriptions:{},
        wfSteppluginDescriptions:{}
    });
    console.assert(wf3.renderContextString("1") === "Plugin example-node-step");

    var wf4=new RDWorkflow([{"type":"example-node-step","nodeStep":false,"configuration":{"example":"whatever"}}],{
        nodeSteppluginDescriptions:{},
        wfSteppluginDescriptions:{}
    });
    console.assert(wf4.renderContextString("1") === "Plugin example-node-step");

};

jQuery(function () {
    RDWorkflow.test();
});
