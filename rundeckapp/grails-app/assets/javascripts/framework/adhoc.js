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
//= require knockout.min
//= require knockout-mapping
//= require knockout-foreachprop
//= require workflow
//= require historyKO
//= require nodeFiltersKO
//= require adhocCommandKO
//= require executionStateKO

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

    function disableRunBar(runnning) {
        var runbox = jQuery('#runbox');
        if (runnning) {
            runbox.find('button.runbutton').button('loading');
        }
    }
function enableRunBar() {
    var runbox = jQuery('#runbox');
    runbox.find('button.runbutton')
        .button('reset');
}
var running = false;
function runStarted() {
    running = true;
}
function afterRun() {
    running = false;
    jQuery('.execRerun').show();
    jQuery('#runFormExec').focus();
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
    disableRunBar(true);
    runStarted();
    $('runcontent').loading('Starting Execution…');
    jQuery.ajax({
        type: 'POST',
        url: _genUrl(appLinks.scheduledExecutionRunAdhocInline, data),
        beforeSend: _ajaxSendTokens.curry('adhoc_req_tokens'),
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
    }).success(_ajaxReceiveTokens.curry('adhoc_req_tokens'));
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
                Element.show('runcontent');
                _initAffix();
                var nodeflowvm=continueRunFollow(data);
                ko.applyBindings(nodeflowvm,jQuery('#runcontent .executionshow')[0]);
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
        viewoptionsCompleteId: 'viewoptionscomplete',
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
        onComplete: onRunComplete,
        dobind: true
    });
    var nodeflowvm=new NodeFlowViewModel(
        workflow,
        null,
        null,
        null,
        {followControl:followControl,executionId:data.id}
    );
    var flowState = new FlowState(data.id,null,{
        workflow:workflow,
        loadUrl:_genUrl(appLinks.executionAjaxExecState,{id:data.id}),
        outputUrl:null,
        selectedOutputStatusId:null,
        reloadInterval:1500
    });
    nodeflowvm.followFlowState(flowState);
    followControl.beginFollowingOutput(data.id);
    flowState.beginFollowing();
    var oldControl=adhocCommand.followControl;
    adhocCommand.followControl=followControl;
    if(oldControl){
        oldControl.stopFollowingOutput();
    }
    return nodeflowvm;
}
function onRunComplete() {
    adhocCommand.running(false);
    enableRunBar();
    afterRun();
}

var nodeFilter;
var adhocCommand;

/**
 * Handle embedded content updates
 */
function _updateBoxInfo(name, data) {
    if (data.total && data.total != "0" && !running) {
        enableRunBar();
        adhocCommand.canRun(true);
    } else if (!running) {
        disableRunBar(false);
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

    //history tabs binding
    var history = new History(appLinks.reportsEventsAjax, appLinks.menuNowrunningAjax);
    ko.applyBindings(history, document.getElementById('activity_section'));
    setupActivityLinks('activity_section', history);
    //if empty query, automatically load first activity_link
    if (pageParams.emptyQuery == 'true') {
        history.activateNowRunningTab();
    }

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
            maxShown: 100,
            emptyMode: 'blank',
            project: pageParams.project,
            nodesTitleSingular: message('Node'),
            nodesTitlePlural: message('Node.plural')
        }));

    ko.applyBindings(nodeFilter, document.getElementById('nodefilterViewArea'));
    ko.applyBindings(nodeFilter, document.getElementById('nodefiltersHidden'));

    adhocCommand = new AdhocCommand({commandString:pageParams.runCommand}, nodeFilter);
    ko.applyBindings(adhocCommand, document.getElementById('adhocInput'));

    //show selected named filter
    nodeFilter.filterName.subscribe(function (val) {
        if (val) {
            jQuery('a[data-node-filter-name]').removeClass('active');
            jQuery('a[data-node-filter-name=\'' + val + '\']').addClass('active');
        }
    });
    nodeFilter.total.subscribe(function(val){
        if (val && val != "0" && !running) {
            enableRunBar();
            adhocCommand.canRun(true);
        } else if (!running) {
            disableRunBar(false);
            adhocCommand.canRun(false);
        }
    });
    nodeSummary.reload();
    nodeFilter.updateMatchedNodes();
    jQuery('.act_adhoc_history_dropdown').click(function () {
        adhocCommand.loadRecentCommands();
    });
}
jQuery(document).ready(init);
