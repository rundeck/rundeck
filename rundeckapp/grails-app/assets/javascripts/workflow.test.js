//= require workflow

RDWorkflow.test = function () {
    console.assert(RDWorkflow.paramsForContextId('2@node=a') === 'node=a');
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
    console.assert(RDWorkflow.parseContextId('1').length === 1, 'wrong length');
    console.assert(RDWorkflow.parseContextId('1/1').length === 2, 'wrong length');
    console.assert(RDWorkflow.parseContextId('1/1/1').length === 3, 'wrong length');
    console.assert(RDWorkflow.parseContextId('1/2/3')[0] ===  '1', 'wrong value');
    console.assert(RDWorkflow.parseContextId('1/2/3')[1] === '2', 'wrong value');
    console.assert(RDWorkflow.parseContextId('1/2/3')[2] === '3', 'wrong value');
    console.assert(RDWorkflow.parseContextId('1e@abc/2/3')[0] === '1e@abc', 'wrong value');
    console.assert(RDWorkflow.parseContextId('1/2e@asdf=xyz/3')[1] === '2e@asdf=xyz', 'wrong value');


    //clean context id
    console.assert(RDWorkflow.cleanContextId('1/2/3')==='1/2/3','wrong value');
    console.assert(RDWorkflow.cleanContextId('1e@abc/2/3')==='1/2/3','wrong value');
    console.assert(RDWorkflow.cleanContextId('1/2e@asdf=xyz/3')==='1/2/3','wrong value');
};

jQuery(function () {
    RDWorkflow.test();
});
