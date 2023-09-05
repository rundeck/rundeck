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

//= require momentutil
//= require vendor/knockout.min
//= require vendor/knockout-mapping
//= require knockout-foreachprop
//= require workflow
//= require nodeFiltersKO
//= require adhocCommandKO
//= require executionStateKO
//= require executionLogOutputKO
//= require koBind


/*
 Manifest for "framework/adhoc.gsp" page
 */



function showError(message) {
    appendText($("error"), message);
    $("error").show();
}

/**
 * START run execution code
 */


var running = false;
function runStarted() {
    running = true;
}
function afterRun() {
    running = false;
    jQuery('.execRerun').show();
    jQuery('#runFormExec').trigger('focus')
}
function runError(msg) {
    jQuery('.errormessage').html(msg);
    jQuery('#runerror').collapse('show');
    jQuery('#runcontent').hide();
    onRunComplete();
}
function requestFailure(trans) {
    runError("Request failed: " + trans.statusText);
}
/**
 * Run the command
 * @param elem
 */
function runFormSubmit(elem) {
    if (running || !$F('runFormExec')) {
        return false;
    }
    if (!nodeFilter.filter() && !nodeFilter.filterName()) {
        //no node filter
        return false;
    }
    var data = jQuery('#' + elem + " :input").serialize();
    adhocCommand.running(true);
    runStarted();
    $('runcontent').loading('Starting Execution…');
    jQuery.ajax({
        type: 'POST',
        url: _genUrl(appLinks.scheduledExecutionRunAdhocInline, data),
        beforeSend: _createAjaxSendTokensHandler('adhoc_req_tokens'),
        success: function (data, status, xhr) {
            try {
                startRunFollow(data);
            } catch (e) {
                console.log(e);
                runError(e);
            }
        },
        error: function (data, jqxhr, err) {
            requestFailure(jqxhr);
        }
    }).done(_createAjaxReceiveTokensHandler('adhoc_req_tokens'));
    return false;
}
/**
 * Load content view to contain output
 * @param data
 */
function startRunFollow(data) {
    if (data.error) {
        runError(data.error);
    } else if (!data.id) {
        runError("Server response was invalid: " + data.toString());
    } else {
        $('runcontent').loading('Loading Output…');
        jQuery('#runcontent').load(_genUrl(appLinks.executionFollowFragment, {
            id: data.id,
            mode: 'tail'
        }), function (resp, status, jqxhr) {
            if (status == 'success') {
                /** Kick this event into Vue Land */
                window._rundeck.eventBus.emit('ko-adhoc-running', data)
                Element.show('runcontent');
                _initAffix();
                var nodeflowvm=continueRunFollow(data);
                jQuery('#runcontent .executionshow').each(function(index,el){
                    ko.applyBindings(nodeflowvm,el);
                })
            } else {
                requestFailure(jqxhr);
            }
        });
    }
}
/**
 * Start following the output
 * @param data
 */
function continueRunFollow(data) {
    var pageParams = loadJsonData('pageParams');
    var workflowData=[{exec:adhocCommand.commandString()}];
    var workflow = new RDWorkflow(workflowData,{});
    var followControl = new FollowControl(data.id, 'runcontent', {
        workflow:workflow,
        parentElement: 'commandPerform',
        cmdOutputErrorId: 'cmdoutputerror',
        outfileSizeId: 'outfilesize',
        extraParams: pageParams.disableMarkdown,
        smallIconUrl: pageParams.smallIconUrl,
        iconUrl: pageParams.iconUrl,
        lastlines: pageParams.lastlines,
        maxLastLines: pageParams.maxLastLines,
        killjobauth: pageParams.adhocKillAllowed,
        //showFinalLine: {value: false, changed: false},
        colStep: {value: false},
        colNode: {value: false},
        collapseCtx: {value: false, changed: false},
        groupOutput:{value:false},
        tailmode: true,
        taildelay: 1.5,
        browsemode: false,
        nodemode: false,
        //taildelay: 1,
        //truncateToTail: false,
        execData: {},
        appLinks: appLinks,
        // dobind: true
    });
    var nodeflowvm=new NodeFlowViewModel(
        workflow,
        null,
        null,
        null,
        {
            followControl: followControl,
            executionId: data.id,
            logoutput: new LogOutput({
                followControl: followControl,
                bindFollowControl: true,
                options: {
                    followmode: "tail",
                    showStep: false,
                    showNodeCol: false
                }
            })
        }
    );
    var flowState = new FlowState(data.id,null,{
        workflow:workflow,
        loadUrl:_genUrl(appLinks.executionAjaxExecState,{id:data.id}),
        outputUrl:null,
        selectedOutputStatusId:null,
        reloadInterval:1500
    });
    nodeflowvm.followFlowState(flowState);

    flowState.beginFollowing();
    /** Base the complete event on the flow view instead of the "log"
     * so it will kick if the legacy log viewer is disabled */
    nodeflowvm.completed.subscribe(onCompletedChange)
    var oldControl=adhocCommand.followControl;
    adhocCommand.followControl=followControl;
    if(oldControl){
        oldControl.stopFollowingOutput();
    }
    return nodeflowvm;
}

function onCompletedChange(val) {
    if (val == true)
        onRunComplete()
}
function onRunComplete() {
    adhocCommand.running(false);
    afterRun();
}

var nodeFilter;
var adhocCommand;

/**
 * Handle embedded content updates
 */
function _updateBoxInfo(name, data) {
    if (data.total && data.total != "0" && !running) {
        adhocCommand.canRun(true);
    } else if (!running) {
        adhocCommand.canRun(false);
    }
    if (null != data.total && typeof(nodeFilter) != 'undefined') {
        nodeFilter.total(data.total);
    }
    if (null != data.allcount) {
        if (typeof(nodeFilter) != 'undefined') {
            nodeFilter.allcount(data.allcount);
        }
    }
    if (null != data.filter) {
        if (typeof(nodeFilter) != 'undefined') {
            nodeFilter.filter(data.filter);
        }
    }
}

function closeOutputArea(){
    jQuery('#runcontent').hide();
    onRunComplete();
    if(adhocCommand.followControl) {
        adhocCommand.followControl.stopFollowingOutput();
    }
}


/**
 * START page init
 */
function init() {
    var pageParams = loadJsonData('pageParams');
    jQuery('body').on('click', '.nodefilterlink', function (evt) {
        evt.preventDefault();
        nodeFilter.selectNodeFilterLink(this);
    });
    jQuery('#nodesContent').on('click', '.closeoutput', function (evt) {
        evt.preventDefault();
        closeOutputArea();
    });
    $$('#runbox input').each(function (elem) {
        if (elem.type == 'text') {
            elem.observe('keypress', function (evt) {
                if (!noenter(evt)) {
                    runFormSubmit('runbox');
                    return false;
                } else {
                    return true;
                }
            });
        }
    });


    //setup node filters knockout bindings
    var filterParams = loadJsonData('filterParamsJSON');
    var nodeSummary = new NodeSummary({baseUrl:appLinks.frameworkNodes});
    nodeFilter = new NodeFilters(
        appLinks.frameworkAdhoc,
        appLinks.scheduledExecutionCreate,
        appLinks.frameworkNodes,
        jQuery.extend(filterParams, {
            nodeSummary:nodeSummary,
            view: 'embed',
            maxShown: filterParams.matchedNodesMaxCount,
            emptyMode: 'blank',
            project: pageParams.project,
            nodesTitleSingular: message('Node'),
            nodesTitlePlural: message('Node.plural')
        }));


    adhocCommand = new AdhocCommand({commandString:pageParams.runCommand}, nodeFilter);

    //show selected named filter
    nodeFilter.filterName.subscribe(function (val) {
        if (val) {
            jQuery('a[data-node-filter-name]').removeClass('active');
            jQuery('a[data-node-filter-name=\'' + val + '\']').addClass('active');
        }
    });
    nodeFilter.total.subscribe(function(val){
        if (val && val != "0" && !running) {
            adhocCommand.canRun(true);
        } else if (!running) {

            adhocCommand.canRun(false);
        }
    });
    nodeSummary.reload();
    nodeFilter.updateMatchedNodes();
    jQuery('.act_adhoc_history_dropdown').on('click',function () {
        adhocCommand.loadRecentCommands();
    });

    initKoBind(null, {nodeFilter: nodeFilter, adhocCommand: adhocCommand})
}
jQuery(document).ready(init);
