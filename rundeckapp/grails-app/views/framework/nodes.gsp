<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<html>
<head>
    <g:set var="rkey" value="${g.rkey()}" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="nodes"/>
    <title><g:message code="gui.menu.Run"/></title>
    <g:javascript library="executionControl"/>
    <g:javascript library="yellowfade"/>
    <g:javascript library="pagehistory"/>
    <g:set var="defaultLastLines" value="${grailsApplication.config.rundeck.gui.execution.tail.lines.default}"/>
    <g:set var="maxLastLines" value="${grailsApplication.config.rundeck.gui.execution.tail.lines.max}"/>
    <script type="text/javascript">
        function showError(message) {
            $("error").innerHTML += message;
            $("error").show();
        }

        //set box filterselections
        var _setFilterSuccess=function(response,name){
            var data=eval("("+response.responseText+")"); // evaluate the JSON;
            if(data){
                var bfilters=data['filterpref'];
                //reload page
                document.location="${createLink(controller:'framework',action:'nodes')}"+(bfilters[name]?"?filterName="+encodeURIComponent(bfilters[name]):'');
            }
        };

        //method called by _nodeFilterInputs
        function _matchNodes(){

        }
        function _clearNodeFilters(){
            $$('.nfilteritem').each(Element.hide);
            $$('.filterAdd').each(Element.show);
            $$('.nfilteritem input').each(function(e){e.value='';});
            return false;
        }

        /*********
         *  remote editor
         *********/
        
        var remoteSite;
        var remoteEditStarted=false;
        var remoteEditExpect=false;
        var projectname;

        /**
         * Start remote editor for node with url
         * @param name node name
         * @param url url
         */
        function doRemoteEdit(name,project,url){
            _remoteEditClear();

            projectname=project;
            $('editNodeIdent').innerHTML=name;

            //create iframe for url
            var ifm = document.createElement('iframe');
            ifm.width="640px";
            ifm.height="480px";
            _remoteEditExpect(url);
            ifm.src=url;

            $('remoteEditTarget').appendChild(ifm);

            _remoteEditShow();
        }

        //setup functions

        /**
         * Begin listener for message protocol from origin url
         * @param originUrl
         */
        function _remoteEditExpect(originUrl){
            remoteEditExpect=true;
            remoteSite=originUrl;
            remoteEditStarted=false;

            Event.observe(window,'message', _rdeckNodeEditOnmessage);
        }

        /**
         * Stop listener for message protocol
         */
        function _remoteEditStop(){
            remoteEditExpect=false;
            remoteSite=null;
            remoteEditStarted=false;
            projectname=null;

            Event.stopObserving(window,'message', _rdeckNodeEditOnmessage);
        }

        /**
         * Clear/reset remote editor dom content
         */
        function _remoteEditClear(){
            _remoteEditStop();
            
            _clearTarget();

            shouldrefresh=false;

            $('editNodeIdent').innerHTML='';
            var errhold=$('remoteEditError');
            errhold.innerHTML="";
            errhold.hide();
        }

        /**
         * Show remote editor dom content
         */
        function _remoteEditShow(){
            $('remoteEditTarget').show();
            $('remoteEditholder').show();

            $('remoteEditResultHolder').hide();
            $('nodesTable').hide();
            $('${rkey}nodesfilterholder').hide();
        }

        /**
         * Hide remote editor dom content
         */
        function _remoteEditHide(){
            $('remoteEditholder').hide();
            $('remoteEditTarget').hide();

            $('remoteEditToolbar').show();
            $('nodesTable').show();
            $('${rkey}nodesfilterholder').show();
        }

        /**
         * Clear iframe holder
         */
        function _clearTarget(){
            $('remoteEditTarget').innerHTML="";
            $('remoteEditTarget').hide();

        }
        /**
         * Finish all node editor stuff, and hide it
         */
        function _remoteEditCompleted(){
            _remoteEditStop();
            _remoteEditHide();
            _remoteEditClear();
        }

        var shouldrefresh=false;
        /**
         * If necessary, reload the nodes page
         */
        function _remoteEditContinue(){
            if(shouldrefresh){
                document.location='${createLink(controller:"framework",action:"nodes")}';
            }else{
                _remoteEditCompleted();
            }
        }

        /**
         * Perform Ajax request to tell server to re-fetch the nodes data for the project
         */
        function _remoteEditDidSave(){
            if(projectname){
                new Ajax.Request('${createLink(controller:"framework",action:"reloadNodes")}.json',{
                    parameters:{project:projectname},
                    evalJSON:true,
                    onSuccess:function(req){
                        var data=req.responseJSON;
                        if(data.success){
                            shouldrefresh=true;
                        }
                    },
                    onFailure:function(e){
                        //console.log(e);
                    }
                });
            }
        }

        //protocol handler functions//


        /**
         * handler for :finished message
         * @param changed true if changes were saved
         */
        function _rdeckNodeEditFinished(changed){
            
            if(changed){
                $('remoteEditResultText').innerHTML="Node changes were saved successfully.";
                _remoteEditDidSave();
            }else{
                $('remoteEditResultText').innerHTML="Node changes were not saved.";
            }
            _remoteEditStop();
            _clearTarget();
            
            $('remoteEditToolbar').hide();
            $('remoteEditResultHolder').show();
        }

        /**
         * handler for error message
         * @param origin
         * @param msg
         */
        function _rdeckNodeEditError(origin,msg){
            _remoteEditStop();
            _clearTarget();
            
            var errhold=$('remoteEditError');
            errhold.innerHTML=(origin?origin+" reported an error: ":"")+msg;
            errhold.show();
        }

        /**
         * handler for :started message
         */
        function _rdeckNodeEditStarted(){
            remoteEditStarted=true;
        }

        var PROTOCOL='rundeck:node:edit';
        /**
         * onmessage handler
         * @param msg
         */
        function _rdeckNodeEditOnmessage(msg){
            if(!remoteEditExpect || !remoteSite || !remoteSite.startsWith(msg.origin+"/")){
                return;
            }
            var data=msg.data;
            if(!remoteEditStarted && PROTOCOL+':started'==data){
                _rdeckNodeEditStarted();
            }else if(PROTOCOL+':error'==data || data.startsWith(PROTOCOL+':error:')){
                var err=data.substring((PROTOCOL+':error').length);
                if(err.startsWith(":")){
                    err=err.substring(1);
                }
                _rdeckNodeEditError(msg.origin,err?err:"(No message)");
            }else if (remoteEditStarted){
                if(PROTOCOL+':finished:true'==data){
                    _rdeckNodeEditFinished(true);
                }else if(PROTOCOL+':finished:false'==data){
                    _rdeckNodeEditFinished(false);
                }else{
                    _rdeckNodeEditError(null,"Unexpected message received from ["+msg.origin+"]: "+data);
                }
            }
        }
        /**
         * END remote edit code
         */


        /**
         * START run execution code
         */
        <g:set var="jsdata" value="${query?.properties.findAll{it.key==~/^(node(In|Ex)clude.*|project)$/ &&it.value}}"/>

        var nodeFilterData_${rkey}=${jsdata.encodeAsJSON()};
        var nodespage=0;
        var pagingMax=20;
        function expandResultNodes(page,elem){
            if(!page){
                page=0;
            }
            nodespage=page;
            if(!elem){
                elem='nodelist';
            }
            var view=page==0?'table':'tableContent';
            _updateMatchedNodes(nodeFilterData_${rkey},elem,'${session.project}',false,{view:view,expanddetail:true,inlinepaging:true,page:page,max:pagingMax});
        }
        function _loadNextNodesPageTable(max,total,tbl,elem){
            if(!nodespage){
                nodespage=0;
            }
            var next=nodespage+1;
            if(total<0 || max*next<total){
                //create sibling of elem
                var div= new Element('tbody');
                $(tbl).insert({bottom:div});
                //total < 0 means load all remaining, so invert next page
                expandResultNodes(next* (total<0?-1:1),Element.identify(div));
            }
//            console.log("next: "+(max*(next+1))+", total: "+total);
            var loadCount = max*(next+1);
            if(loadCount>=total || total<0){
                //hide pager button area
                $(elem).hide();
            }else{
                //update moreCount
                $('moreCount').innerHTML=total-loadCount;
                if(total-loadCount<max){
                    $('nextPageButton').hide();
                }
            }
        }
        function disableRunBar(){
            $('runbox').down('input[type="text"]').disable();
            $('runbox').down('button').disabled=true;
        }
        function enableRunBar(){
            $('runbox').down('input[type="text"]').enable();
            $('runbox').down('button').disabled=false;
        }
        function collapseNodeView(){
//            $$('.obs_shownodes').each(Element.show);
            $$('.obs_shownodes').each(function(e){Expander.close(e,null);});

            $$('.nodeview').each(Element.hide);
            $$('.nodeviewsummary').each(Element.show);
        }
        function showNodeView(){
//            $$('.obs_shownodes').each(Element.hide);
            $$('.obs_shownodes').each(function(e){Expander.open(e,null);});
            $$('.nodeview').each(Element.show);
            $$('.nodeviewsummary').each(Element.hide);
        }
        function runStarted(){
            running=true;
            $$('.hiderun').each(Element.hide);
            $$('.showrun').each(Element.show);
            collapseNodeView();
        }
        function afterRun(){
            running=false;
            $$('.showafterrun').each(Element.show);
            $$('.hideafterrun').each(Element.hide);
        }
        function runError(msg){
            $('runerror').innerHTML=msg;
            $('runerror').show();
            $('runcontent').hide();
            onRunComplete();
        }
        function requestFailure(trans){
            runError("Request failed: "+trans.statusText);
        }
        var running=false;
        /**
         * Run the command
         * @param elem
         */
        function runFormSubmit(elem){
            if(running){
                return false;
            }
            var data = Form.serialize(elem);
            disableRunBar();
            runStarted();
            $('runcontent').loading('Starting Execution&hellip;');
            new Ajax.Request("${createLink(controller:'scheduledExecution',action:'runAdhocInline')}",{
                parameters:data,
                evalScripts:true,
                evalJSON:true,
                onSuccess: function(transport) {
                    var data =transport.responseJSON;
//                    alert("data: "+data);
                    try{
                    startRunFollow(data);
                    }catch(e){
                        runError(e);
                    }
                },
                onFailure:requestFailure
            });
            return false;
        }
        /**
         * Load content view to contain output
         * @param data
         */
        function startRunFollow(data){
            if(data.error){
                runError(data.error);
            }else if(!data.id){
                runError("Server response was invalid: "+data.toString());
            }else {
                $('runcontent').loading('Loading Output&hellip;');
                new Ajax.Updater('runcontent',"${createLink(controller:'execution',action:'followFragment')}",{
                parameters:{id:data.id,mode:'tail'},
                evalScripts:true,
                onComplete: function(transport) {
                    if (transport.request.success()) {
                        Element.show('runcontent');
                        try{
                        continueRunFollow(data);
                        }catch(e){
                            runError(e);
                        }
                    }
                },
                onFailure:requestFailure
            });
            }
        }
        /**
         * Start following the output
         * @param data
         */
        function continueRunFollow(data){
             var followControl = new FollowControl(data.id,'runcontent',{
                extraParams:"<%="true" == params.disableMarkdown ? '&disableMarkdown=true' : ''%>",
                iconUrl: "${resource(dir: 'images', file: 'icon')}",
                lastlines: ${params.lastlines ? params.lastlines : defaultLastLines},
                maxLastLines: ${maxLastLines},
                tailmode: true,
                 taildelay:1,
                execData: {node:"test"},
                appLinks:appLinks,
                onComplete:onRunComplete,
                dobind:true
            });
            followControl.beginFollowingOutput(data.id);
        }
        function onRunComplete(){
            enableRunBar();
            afterRun();
            loadHistory();
        }

        /**
         * START tag filter link code
         */
        function setTagFilter(value){
            if($('schedJobNodeIncludeTags').value){
                $('schedJobNodeIncludeTags').value+=","+value;
            }else{
                $('schedJobNodeIncludeTags').value=value;

            }
            $('${rkey}filter').down('form').submit();
        }

        /**
         * filter toggle
         */
        function filterToggle(evt) {
            ['${rkey}filter','${rkey}filterdispbtn'].each(Element.toggle);
            ['outsidefiltersave'].each($('${rkey}filter').visible()?Element.hide:Element.show);
        }
        function filterToggleSave(evt) {
            ['${rkey}filter','${rkey}fsave'].each(Element.show);
            ['${rkey}filterdispbtn','${rkey}fsavebtn'].each(Element.hide);
            ['outsidefiltersave'].each($('${rkey}filter').visible()?Element.hide:Element.show);
        }

        /** START history
         *
         */
        var histControl = new HistoryControl('histcontent',{compact:true,nofilters:true,recentFilter:'1d',projFilter:'${session.project}'});
        function loadHistory(){
            histControl.loadHistory();
        }
        var lastRunExec=0;
        /**
         * Handle embedded content updates
         */
        function _updateBoxInfo(name,data){
            if(name=='events' && data.lastDate){
                histControl.setHiliteSince(data.lastDate);
            }
            if (name == 'nowrunning' && data.lastExecId && data.lastExecId != lastRunExec) {
                lastRunExec = data.lastExecId;
                loadHistory();
            }
            if(name=='nodetable'){
                if(data.total && data.total!="0"){
                    enableRunBar();
                }else{
                    disableRunBar();
                }
            }
        }
        /**
         * now running
         */
        var savedcount=0;
        function _pageUpdateNowRunning(count){
            if(count!=savedcount){
                savedcount=count;
                loadHistory();
            }
        }

        var runupdate;
        function loadNowRunning(){
            runupdate=new Ajax.PeriodicalUpdater({ success:'nowrunning'},'${createLink(controller:"menu",action:"nowrunningFragment")}',{
                evalScripts:true,
                parameters:{projFilter:'${session.project}'},
                onFailure:function (response) {
                    showError("AJAX error: Now Running [" + runupdate.url + "]: " + response.status + " "
                                      + response.statusText);
                    runupdate.stop();
                }
            });
        }

        /**
         * START page init
         */

        function init() {
            loadNowRunning();
            $$('#runbox input').each(function(elem){
                if(elem.type=='text'){
                    elem.observe('keypress',function(evt){
                        if(!noenter(evt)){
                            runFormSubmit('runbox');
                            return false;
                        }else{
                            return true;
                        }
                    });
                }
            });
            $$('.obs_filtertoggle').each(function(e) {
                Event.observe(e, 'click', filterToggle);
            });
            $$('.obs_filtersave').each(function(e) {
                Event.observe(e, 'click', filterToggleSave);
            });
//            $$('.obs_shownodes').each(function(e){
//                Event.observe(e, 'click', showNodeView);
//            });

            $$('#${rkey}filter div.filter input').each(function(elem) {
                if (elem.type == 'text') {
                    elem.observe('keypress', function(evt) {
                        if (!noenter(evt)) {
                            $('nodefiltersubmit').click();
                            return false;
                        } else {
                            return true;
                        }
                    });
                }
            });
        }
        Event.observe(window,'load',init);

    </script>
    <style type="text/css">
        .detail_content{
            padding:4px 10px;
        }
        .filterSetButtons{
            width:200px;
            line-height:24px;
            margin-right:10px;
        }
        .filterSetButtons .button{
            white-space:nowrap;
        }

        .node_entry .project{

        }
        #remoteEditholder{
            margin: 0px 20px 0 20px;

        }
        #remoteEditholder iframe{
            border:0;
        }
        #remoteEditholder .toolbar{
            margin:4px;
        }
        .runbox input[type='text']{
            font-size: 150%;
            font-family: Monaco, 'Courier New', 'DejaVu Sans Mono', 'Bitstream Vera Sans Mono', monospace;
            font-family: Courier, monospace;
        }
        #runerror{
            color:red;
            margin:5px 20px;
        }

        #runcontent{
            overflow-x:auto;
            margin:10px 0;
        }
        div.header{
            padding:3px 10px;
            background: #eee;
            border-top: 1px solid #ddd;
            border-left: 1px solid #ddd;
            border-right: 1px solid #ddd;
            color: black;
            font-weight:bold;
        }
        #histcontent div.jobsreport{
            margin:0;
            padding:0;
        }
        #histcontent table.queryTable > tbody > tr > td{
            padding:0;
            margin:0;
        }
        #histcontent table{
            width:100%;
        }
        #nodesPaging{
            margin-top:5px;
        }
    </style>
</head>
<body>

<div class="pageBody">
    <span class="prompt">Now running <span class="nowrunningcount">(0)</span></span>
    <div id="nowrunning">
        <span class="note empty">No running Jobs</span>
    </div>

    <div id="error" class="error message" style="display:none;"></div>
</div>
<g:if test="${session.user && User.findByLogin(session.user)?.nodefilters}">
    <g:set var="filterset" value="${User.findByLogin(session.user)?.nodefilters}"/>
</g:if>
<div id="nodesContent">
    <g:set var="run_authorized" value="${auth.adhocAllowedTest( action:AuthConstants.ACTION_RUN)}"/>
    <g:set var="run_enabled" value="${run_authorized }"/>

    <g:if test="${session.project }">
        <div class="runbox" id="runbox">
        %{--<g:form action="execAndForget" controller="scheduledExecution" method="post" style="display:inline" onsubmit="return runFormSubmit(this);">--}%
            Command:
            <g:img file="icon-small-shell.png" width="16px" height="16px"/>
            <g:if test="${run_enabled}">
                <g:hiddenField name="project" value="${session.project}"/>
                <g:hiddenField name="doNodedispatch" value="true"/>
                <g:hiddenField name="nodeKeepgoing" value="true"/>
                <g:hiddenField name="nodeThreadcount" value="1"/>
                <g:hiddenField name="description" value=""/>

                <g:hiddenField name="workflow.commands[0].adhocExecution" value="true"/>
                <g:hiddenField name="workflow.threadcount" value="1"/>
                <g:hiddenField name="workflow.keepgoing" value="false"/>
                <g:hiddenField name="workflow.project" value="${session.project}"/>
                <g:render template="nodeFiltersHidden" model="${[params:params,query:query]}"/>
            </g:if>
            <g:if test="${run_enabled}">
                <g:textField name="workflow.commands[0].adhocRemoteString" size="80" placeholder="Enter a shell command" autofocus="true" />
            </g:if>
            <g:else>
                <input type="text" name="workflow.commands[0].adhocRemoteString" size="80" placeholder="Enter a shell command" autofocus="true" disabled/>
            </g:else>
        %{--</g:form>--}%
            <g:if test="${run_authorized}">
                <button onclick="runFormSubmit('runbox');" ${run_enabled?'':'disabled'}>Run</button>
            </g:if>
            <g:else>
                <span class="button disabled" title="You are not authorized to run ad-hoc jobs">Run</span>
            </g:else>

            <div class="hiderun" id="runerror" style="display:none"></div>
        </div>
        <div class="runbox nodesummary ">
            <g:expander classnames="button obs_shownodes" key="${rkey}nodeForm" open="true">
            <span class="match">${total} Node${1 != total ? 's' : ''}</span>
            </g:expander>
            <g:if test="${null!=allcount}">
                (of ${allcount})
            </g:if>
            <span class="type">
            <g:if test="${!filterName}">
                matching filter input
            </g:if>
            <g:else>
                matching filter '${filterName}'
            </g:else>
            </span>
        </div>
    </g:if>
<div class="pageBody">

<g:render template="/common/messages"/>
<div id="${rkey}nodeForm" class="nodeview">
    <g:set var="wasfiltered" value="${paginateParams?.keySet().grep(~/(?!proj).*Filter|groupPath|project$/)||(query && !query.nodeFilterIsEmpty())}"/>
    <g:set var="filtersOpen" value="${params.createFilters||params.editFilters||params.saveFilter || filterErrors?true:false}"/>

<table cellspacing="0" cellpadding="0" class="queryTable" width="100%">
        <tr>
        <g:if test="${!params.nofilters}">
        <td style="text-align:left;vertical-align:top; width:400px; ${wdgt.styleVisible(if:filtersOpen)}" id="${rkey}filter">
            <g:form action="nodes" controller="framework" >
                <g:if test="${params.compact}">
                    <g:hiddenField name="compact" value="${params.compact}"/>
                </g:if>
                <span class="prompt action obs_filtertoggle" >
                    Filter
                    <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure-open.png')}" width="12px" height="12px"/>
                </span>

                <g:render template="/common/queryFilterManager" model="${[rkey:rkey,filterName:filterName,filterset:filterset,deleteActionSubmit:'deleteNodeFilter',storeActionSubmit:'storeNodeFilter']}"/>
                <div class="presentation filter">

                    <g:hiddenField name="max" value="${max}"/>
                    <g:hiddenField name="offset" value="${offset}"/>
                    <table class="simpleForm">
                        <g:render template="nodeFilterInputs" model="${[params:params,query:query]}"/>
                    </table>
                    <div>

                        <div class=" " style="text-align:right;">
                            <g:submitButton  name="Filter" id="nodefiltersubmit" value="Filter"/>

                            <g:submitButton name="Clear" onclick="return _clearNodeFilters();" value="Clear"/>
                        </div>
                    </div>
                </div>
                </g:form>
        </td>
            </g:if>
            <td style="text-align:left;vertical-align:top;" id="${rkey}nodescontent">


                <g:set var="adminauth"
                       value="${auth.resourceAllowedTest(kind:'node',action:[AuthConstants.ACTION_REFRESH])}"/>
                <g:if test="${adminauth}">
                    <g:if test="${selectedProject && selectedProject.shouldUpdateNodesResourceFile()}">
                        <span class="floatr"><g:link action="reloadNodes" params="${[project:selectedProject.name]}" class="action button" title="Click to update the resources.xml file from the source URL, for project ${selectedProject.name}" onclick="\$(this.parentNode).loading();">Update Nodes for project ${selectedProject.name}</g:link></span>
                    </g:if>
                </g:if>
                <g:if test="${!params.nofilters}">
                <div style="margin: 10px 0 5px 0;" id="${rkey}nodesfilterholder" >
                    %{--<g:if test="${wasfiltered}">--}%


                        <div style="margin:5px 0; padding:5px 0;">
                            <span style="padding:5px 0;margin:5px 0;${!filtersOpen?'':'display:none;'} " id='${rkey}filterdispbtn' >
                            <span title="Click to modify filter" class="info textbtn query action obs_filtertoggle" >
                                <g:render template="displayNodeFilters" model="${[displayParams:query]}"/>
                                <img src="${resource(dir:'images',file:'icon-tiny-disclosure.png')}" width="12px" height="12px"/></span>
                            </span>


                        <g:if test="${!filterName}">
                            <span class="prompt action obs_filtersave" title="Click to save this filter with a name" id="outsidefiltersave">
                                save this filter&hellip;
                            </span>
                        </g:if>

                        <g:if test="${filterset}">
                            <g:render template="/common/selectFilter" model="[filterset:filterset,filterName:filterName,prefName:'nodes',noSelection:filterName?'-Server Node-':null]"/>
                        </g:if>
                        <g:if test="${!params.Clear && !params.formInput}">
                            <g:form action="nodes" style="display: inline">
                                <g:hiddenField name="formInput" value="true"/>
                                <button name="Clear" value="Clear">Show all nodes</button>
                            </g:form>
                        </g:if>
                        </div>

                    %{--</g:if>--}%
                    %{--<g:else>
                        <span class="prompt action" onclick="['${rkey}filter','${rkey}filterdispbtn','runbox'].each(Element.toggle);if(${isCompact}){$('${rkey}nodescontent').toggle();}" id="${rkey}filterdispbtn"  style="${!filtersOpen?'':'display:none;'}">
                            Filter
                            <img src="${resource(dir:'images',file:'icon-tiny-disclosure.png')}" width="12px" height="12px"/></span>
                        <g:if test="${filterset}">
                            <span style="margin-left:10px;">
                                <span class="info note">Filter:</span>
                                <g:render template="/common/selectFilter" model="[filterset:filterset,filterName:filterName,prefName:'nodes']"/>
                            </span>
                        </g:if>
                    </g:else>--}%
                </div>
                </g:if>

                %{--<div class="nodesummary clear nodeview">--}%
                    %{--<span class="match">${total}/${allcount} Node${1 != allcount ? 's' : ''}</span>--}%
                    %{--<span class="type">--}%
                    %{--<g:if test="${!filterName}">--}%
                        %{--matching filter input--}%
                    %{--</g:if>--}%
                    %{--<g:else>--}%
                        %{--matching saved filter--}%
                    %{--</g:else>--}%
                    %{--</span>--}%
                %{--</div>--}%

                <div class=" clear matchednodes " id="nodelist" >
                    <span class="button action receiver" onclick="expandResultNodes();">Show ${total} Node${1 != total ? 's' : ''}...</span>
                    %{--<g:render template="nodes" model="${[nodes:allnodes,totalexecs:totalexecs,jobs:jobs,params:params,expanddetail:true]}"/>--}%
                    <g:javascript>

                        fireWhenReady('nodelist',expandResultNodes);
                    </g:javascript>

                </div>

                 </td>

                </tr>
            </table>
</div>
<div id="runcontent"></div>

    </div>
    <div class="runbox">History</div>
    <div class="pageBody">
        <div id="histcontent"></div>
        <g:javascript>
            fireWhenReady('histcontent',loadHistory);
        </g:javascript>
    </div>
</div>
<div id="loaderror"></div>
</body>
</html>