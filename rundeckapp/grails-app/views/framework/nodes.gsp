<%@ page import="rundeck.User; com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<html>
<head>
    <g:set var="ukey" value="${g.rkey()}" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="nodes"/>
    <title><g:message code="gui.menu.Nodes"/> - ${session.project.encodeAsHTML()}</title>
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
        function _submitNodeFilters(){
            $$('.execCommand').each(function(e){e.setValue($F('runFormExec'));});
            return true;
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
            $('${ukey}nodesfilterholder').hide();
        }

        /**
         * Hide remote editor dom content
         */
        function _remoteEditHide(){
            $('remoteEditholder').hide();
            $('remoteEditTarget').hide();

            $('remoteEditToolbar').show();
            $('nodesTable').show();
            $('${ukey}nodesfilterholder').show();
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

        var nodeFilterData_${ukey}=${jsdata.encodeAsJSON()};
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
            _updateMatchedNodes(nodeFilterData_${ukey},elem,'${session.project}',false,{view:view,expanddetail:true,inlinepaging:true,page:page,max:pagingMax});
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
        var _runBtnHtml;
        function disableRunBar(){
            if($('runbox')){
                $('runbox').down('input[type="text"]').disable();
                if ($('runbox').down('button.runbutton')) {
                    $('runbox').down('button.runbutton').disabled = true;
                    $('runbox').down('button.runbutton').addClassName('disabled');
                    _runBtnHtml= $('runbox').down('button.runbutton').innerHTML;
                    $('runbox').down('button.runbutton').innerHTML="Running…";
                }
            }
        }
        function enableRunBar(){
            if ($('runbox')) {
                $('runbox').down('input[type="text"]').enable();
                if($('runbox').down('button.runbutton')){
                    $('runbox').down('button.runbutton').disabled=false;
                    $('runbox').down('button.runbutton').removeClassName('disabled');
                    $('runbox').down('button.runbutton').innerHTML = 'Run <span class="glyphicon glyphicon-play"></span>';
                }
            }
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
            $('runFormExec').focus();
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
            if(running || !$F('runFormExec')){
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
                        console.log(e);
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
//                        try{
                        continueRunFollow(data);
//                        }catch(e){
//                            console.log(e,e);
//                            runError(e);
//                        }
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
                 parentElement: 'commandPerform',
                 viewoptionsCompleteId: 'viewoptionscomplete',
                 cmdOutputErrorId: 'cmdoutputerror',
                 outfileSizeId: 'outfilesize',
                extraParams:"<%="true" == params.disableMarkdown ? '&disableMarkdown=true' : ''%>",
                smallIconUrl: "${resource(dir: 'images', file: 'icon-small')}",
                iconUrl: "${resource(dir: 'images', file: 'icon-small')}",
                lastlines: ${params.lastlines ? params.lastlines : defaultLastLines},
                maxLastLines: ${maxLastLines},
                 showFinalLine: {value: false, changed: false},
                 colStep:{value:false},
                tailmode: true,
                 taildelay:1,
                 truncateToTail:true,
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
            $('${ukey}filter').down('form').submit();
        }

        /**
         * filter toggle
         */
        function filterToggle(evt) {
            ['${ukey}filter','${ukey}filterdispbtn'].each(Element.toggle);
            ['outsidefiltersave'].each($('${ukey}filter').visible()?Element.hide:Element.show);
        }
        function filterToggleSave(evt) {
            ['${ukey}filter','${ukey}fsave'].each(Element.show);
            ['${ukey}filterdispbtn','${ukey}fsavebtn'].each(Element.hide);
            ['outsidefiltersave'].each($('${ukey}filter').visible()?Element.hide:Element.show);
        }


        /**
         * Handle embedded content updates
         */
        function _updateBoxInfo(name,data){
            if(name=='nodetable'){
                if(data.total && data.total!="0"){
                    enableRunBar();
                }else{
                    disableRunBar();
                }
                if(null !=data.total){
                    $$('.obs_nodes_page_total').each(function(e){
                        e.innerHTML=data.total;
                    });
                }
                if(null!=data.allcount){
                    $$('.obs_nodes_allcount').each(function (e) {
                        e.innerHTML = data.allcount;
                    });
                    $$('.obs_nodes_allcount_plural').each(function (e) {
                        e.innerHTML = data.allcount==1?'':'s';
                    });
                }
            }
        }



        /**
         * START page init
         */

        function init() {
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

            $$('#${ukey}filter div.filter input').each(function(elem) {
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
            expandResultNodes();
        }
        jQuery(document).ready(init);

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

        .commandcontent{
            margin:0;
        }

        table.execoutput {
            font-size: 100%;
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

<g:if test="${session.user && User.findByLogin(session.user)?.nodefilters}">
    <g:set var="filterset" value="${User.findByLogin(session.user)?.nodefilters}"/>
</g:if>
<div id="nodesContent">
    <g:set var="run_authorized" value="${auth.adhocAllowedTest( action:AuthConstants.ACTION_RUN)}"/>


    <g:render template="/common/messages"/>
    <div id="error" class="error message" style="display:none;"></div>
    <g:if test="${session.project}">
        <div>
        <div class="row">
        <div class="col-sm-2">
            <g:if test="${run_authorized}">
                <g:expander classnames="obs_shownodes" key="${ukey}nodeForm" open="true">
                    <span class="h4 match"><span class="obs_nodes_allcount">${total}</span> Node<span class="obs_nodes_allcount_plural">${1 != total ? 's' : ''}</span>
                    </span>
                </g:expander>
            </g:if>
            <g:else>
                <h4 class="match"><span class="obs_nodes_allcount">${total}</span> Node<span class="obs_nodes_allcount_plural">${1 != total ? 's' : ''}</span>
                </h4>
            </g:else>
        </div>
            <g:if test="${session.project && run_authorized}">
                <div class=" form-inline clearfix" id="runbox">
                    <g:hiddenField name="project" value="${session.project}"/>
                    <g:render template="nodeFiltersHidden" model="${[params: params, query: query]}"/>
                    <div class=" col-sm-10">
                        <div class="input-group">
                            <g:textField name="exec" size="50" placeholder="Enter a shell command"
                                         value="${runCommand}"
                                         id="runFormExec"
                                        class="form-control"
                                         autofocus="true"/>

                            <span class="input-group-btn">
                                <button class="btn btn-default has_tooltip" type="button"
                                        title="Node Dispatch Settings"
                                        data-placement="left"
                                        data-container="body"
                                        data-toggle="collapse" data-target="#runconfig">
                                    <i class="glyphicon glyphicon-cog"></i>
                                </button>

                                <button class="btn btn-success runbutton " onclick="runFormSubmit('runbox');">
                                    Run <span class="glyphicon glyphicon-play"></span>
                                </button>
                            </span>
                        </div>
                        <div class="collapse well well-sm " id="runconfig">
                            <div class="row">
                            <div class="col-sm-12">
                                <div class="form-group text-muted ">Node Dispatch Settings: </div>
                                <div class="form-group has_tooltip" title="Maximum number of parallel threads to use"
                                     data-placement="bottom">
                                    Thread count
                                </div>
                                <div class="form-group">
                                    <input min="1" type="number" name="nodeThreadcount" id="runNodeThreadcount"
                                           size="2"
                                           placeholder="Maximum threadcount for nodes" value="1"
                                           class="form-control  input-sm"/>
                                </div>

                                <div class="form-group">On node failure:</div>
                                <div class="radio">
                                    <label class="has_tooltip" title="Continue to execute on other nodes" data-placement="bottom">
                                        <input type="radio" name="nodeKeepgoing"
                                               value="true"
                                            checked
                                        /> <strong>Continue</strong>
                                    </label>
                                </div>

                                <div class="radio">
                                    <label class="has_tooltip" title="Do not execute on any other nodes"
                                           data-placement="bottom">
                                        <input type="radio" name="nodeKeepgoing"
                                               value="false"
                                               /> <strong>Stop</strong>
                                    </label>
                                </div>
                                <div class="pull-right">
                                    <button class="close " data-toggle="collapse" data-target="#runconfig">&times;</button>
                                </div>
                            </div>
                            </div>
                        </div>
                    </div>

                    <div class="hiderun" id="runerror" style="display:none"></div>
                </div>
            </g:if>
        </div>
    </g:if>
<div class="row row-space">
<div id="${ukey}nodeForm" class="col-sm-12 nodeview clearfix">
    <g:set var="wasfiltered" value="${paginateParams?.keySet().grep(~/(?!proj).*Filter|groupPath|project$/)||(query && !query.nodeFilterIsEmpty())}"/>
    <g:set var="filtersOpen" value="${params.createFilters||params.editFilters||params.saveFilter || filterErrors?true:false}"/>

<table cellspacing="0" cellpadding="0" width="100%">
        <tr>
        <g:if test="${!params.nofilters}">
        <td style=" ${wdgt.styleVisible(if:filtersOpen)}" id="${ukey}filter">
            <g:form action="nodes" controller="framework" class="form">
                <g:if test="${params.compact}">
                    <g:hiddenField name="compact" value="${params.compact}"/>
                </g:if>
                <span class="textbtn textbtn-default obs_filtertoggle" >
                    Filter
                    <b class="glyphicon glyphicon-chevron-down"></b>
                </span>

                <g:render template="/common/queryFilterManager" model="${[rkey:ukey,filterName:filterName,filterset:filterset,deleteActionSubmit:'deleteNodeFilter',storeActionSubmit:'storeNodeFilter']}"/>
                <div class="presentation filter">

                    <g:hiddenField name="max" value="${max}"/>
                    <g:hiddenField name="offset" value="${offset}"/>
                    <table class="simpleForm">
                        <g:render template="nodeFilterInputs" model="${[params:params,query:query]}"/>
                    </table>
                    <g:hiddenField name="exec" value="" class="execCommand"/>
                    <div>
                        <div class=" " style="text-align:right;">
                            <g:submitButton  name="Filter" onclick="return _submitNodeFilters();"
                                             id="nodefiltersubmit" value="Filter" class="btn btn-primary btn-sm"/>

                            <g:submitButton name="Clear" onclick="return _clearNodeFilters();" value="Clear"
                                class="btn btn-default btn-sm"
                            />
                        </div>
                    </div>
                </div>
            </g:form>
        </td>
            </g:if>
            <td style="text-align:left;vertical-align:top;" id="${ukey}nodescontent">


                <g:set var="adminauth"
                       value="${auth.resourceAllowedTest(kind:'node',action:[AuthConstants.ACTION_REFRESH])}"/>
                <g:if test="${adminauth}">
                    <g:if test="${selectedProject && selectedProject.shouldUpdateNodesResourceFile()}">
                        <span class="floatr"><g:link action="reloadNodes" params="${[project:selectedProject.name]}" class="action button" title="Click to update the resources.xml file from the source URL, for project ${selectedProject.name}" onclick="\$(this.parentNode).loading();">Update Nodes for project ${selectedProject.name}</g:link></span>
                    </g:if>
                </g:if>
                <g:if test="${!params.nofilters}">
                <div id="${ukey}nodesfilterholder" >
                    %{--<g:if test="${wasfiltered}">--}%


                        <div >
                            <span style="${!filtersOpen?'':'display:none;'} " id='${ukey}filterdispbtn' >
                            <span title="Click to modify filter" class="textbtn textbtn-default query  obs_filtertoggle" >
                                <g:render template="displayNodeFilters" model="${[displayParams:query]}"/>
                                <b class="glyphicon glyphicon-chevron-right"></b>
                            </span>
                            </span>


                        <g:if test="${!filterName}">
                            <span class="textbtn textbtn-info textbtn-on-hover obs_filtersave" title="Click to save this filter with a name" id="outsidefiltersave">
                                save this filter&hellip;
                            </span>
                        </g:if>

                        <g:if test="${filterset}">
                            <g:render template="/common/selectFilter" model="[filterset:filterset,filterName:filterName,prefName:'nodes',noSelection:filterName?'-Server Node-':null]"/>
                        </g:if>
                        <g:if test="${params.formInput}">
                            <g:form action="nodes" style="display: inline">
                                <g:hiddenField name="formInput" value="true"/>
                                <g:hiddenField name="exec" value="" class="execCommand"/>
                                <button name="Clear" value="Clear" class="btn btn-default btn-sm" onclick="return _submitNodeFilters();">Show all nodes</button>
                            </g:form>
                        </g:if>
                        </div>


                </div>
                </g:if>



                <div class=" clear matchednodes " id="nodelist" >
                    <span class="btn btn-default receiver" onclick="expandResultNodes();">Show ${total} Node${1 != total ? 's' : ''}...</span>
                </div>

                 </td>

                </tr>
            </table>

</div>
</div>


    <div id="runcontent" class="clearfix nodes_run_content" style="display: none"></div>

    <g:if test="${run_authorized}">

    <h4 class="text-muted"><g:message code="page.section.Activity"/></h4>

    <div class="row">
    <div class="col-sm-12">
        <g:render template="/reports/activityLinks" model="[filter: [
                jobIdFilter: 'null',
                userFilter: session.user,
                projFilter: session.project
        ]]"/>
    </div>
    </div>
    </g:if>

</div>
<div id="loaderror"></div>
</body>
</html>
