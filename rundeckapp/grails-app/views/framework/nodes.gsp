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
            var isvis= $('${ukey}filter').visible();
            $$('.obs_filtertoggle').each(isvis?Element.hide:Element.show);
        }
        function filterToggleSave(evt) {
            ['${ukey}filter','${ukey}fsave'].each(Element.show);
            ['${ukey}filterdispbtn','${ukey}fsavebtn'].each(Element.hide);
        }


        /**
         * Handle embedded content updates
         */
        function _updateBoxInfo(name,data){
            if(name=='nodetable'){
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
            $$('.act_filtertoggle').each(function(e) {
                Event.observe(e, 'click', filterToggle);
            });

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
            <h4 class="match">
            <span class="obs_nodes_allcount">${total}</span> Node<span class="obs_nodes_allcount_plural">${1 != total ? 's' : ''}</span>
            </h4>
        </div>
            <g:if test="${session.project && run_authorized}">
                <g:form class="form form-inline"  action="adhoc" controller="framework" method="get">
                <div class=" form-inline clearfix" id="runbox">
                    <g:hiddenField name="project" value="${session.project}"/>
                    <g:render template="nodeFiltersHidden" model="${[params: params, query: query]}"/>
                    <div class=" col-sm-10">
                    <g:if test="${total!=null}">
                        <div class="input-group pull-right ">
                            <button class="btn btn-success ${total>0?'runbutton':'disabled '} ">
                                Run command on <span class="obs_nodes_allcount">${total}</span> Node<span
                                    class="obs_nodes_allcount_plural">${1 != total ? 's' : ''}</span> …
                                <span class="glyphicon glyphicon-play"></span>
                            </button>
                        </div>
                    </g:if>

                    </div>

                    <div class="hiderun" id="runerror" style="display:none"></div>
                </div>
                </g:form>
            </g:if>
        </div>
    </g:if>
<div class="row row-space">
<div  class="col-sm-12 nodeview clearfix">
    <g:set var="wasfiltered" value="${paginateParams?.keySet().grep(~/(?!proj).*Filter|groupPath|project$/)||(query && !query.nodeFilterIsEmpty())}"/>
    <g:set var="filtersOpen" value="${showFilter||params.createFilters||params.editFilters||params.saveFilter || filterErrors?true:false}"/>

        <g:if test="${!params.nofilters}">
            <div style=" ${wdgt.styleVisible(if:filtersOpen)}" id="${ukey}filter">
            <g:form action="nodes" controller="framework" class="form form-horizontal">
                <g:if test="${params.compact}">
                    <g:hiddenField name="compact" value="${params.compact}"/>
                </g:if>
                <div class="panel panel-default ">
                    <div class="panel-heading">
                    <span class="textbtn textbtn-info act_filtertoggle">
                        Filter Nodes
                        <b class="glyphicon glyphicon-chevron-down"></b>
                    </span>
                    </div>


                    <g:hiddenField name="max" value="${max}"/>
                    <g:hiddenField name="offset" value="${offset}"/>
                    <g:hiddenField name="exec" value="" class="execCommand"/>

                    <g:render template="/common/queryFilterManagerHorizontal"
                              model="${[rkey: ukey, filterName: filterName, filterset: filterset,
                                      filterLinks:true,
                                      deleteActionSubmit: 'deleteNodeFilter', storeActionSubmit: 'storeNodeFilter']}"/>

                    <div class="panel-body  obs_hide_filtermgr">
                        <g:render template="nodeFilterInputs" model="${[params:params,query:query]}"/>
                    </div>

                    <div class="panel-footer obs_hide_filtermgr text-right" >
                        <g:submitButton  name="Filter" onclick="return _submitNodeFilters();"
                                         id="nodefiltersubmit" value="Filter" class="btn btn-primary btn-sm"/>

                        <g:submitButton name="Clear" onclick="return _clearNodeFilters();" value="Clear"
                            class="btn btn-default btn-sm"
                        />
                    </div>
                </div>
            </g:form>
            </div>
        </g:if>



                <g:set var="adminauth"
                       value="${auth.resourceAllowedTest(kind:'node',action:[AuthConstants.ACTION_REFRESH])}"/>
                <g:if test="${adminauth}">
                    <g:if test="${selectedProject && selectedProject.shouldUpdateNodesResourceFile()}">
                        <span class="floatr"><g:link action="reloadNodes" params="${[project:selectedProject.name]}" class="action button" title="Click to update the resources.xml file from the source URL, for project ${selectedProject.name}" onclick="\$(this.parentNode).loading();">Update Nodes for project ${selectedProject.name}</g:link></span>
                    </g:if>
                </g:if>
                <g:if test="${!params.nofilters}">
                <div id="${ukey}nodesfilterholder" >

                    <div >
                        <span style="${!filtersOpen?'':'display:none;'} " id='${ukey}filterdispbtn' >
                            <span title="Click to modify filter" class="textbtn textbtn-default query  act_filtertoggle" >
                                <g:if test="${filterName}">
                                    <i class="glyphicon glyphicon-filter"></i>
                                    ${filterName.encodeAsHTML()}:
                                </g:if>
                                <g:render template="displayNodeFilters" model="${[displayParams:query]}"/>
                                <b class="glyphicon glyphicon-chevron-right"></b>
                            </span>
                        </span>


                        <g:if test="${filterset}">
                            <g:render template="/common/selectFilter" model="[filterLinks: true, filterset:filterset,filterName:filterName,prefName:'nodes',noSelection:filterName?'-All Nodes-':null]"/>

                        </g:if>

                        <g:if test="${params.formInput!='true' || filterName}">
                            <g:form action="nodes" style="display: inline">
                                <g:hiddenField name="formInput" value="true"/>
                                <button name="Clear" value="Clear" class="btn btn-default btn-sm" onclick="return _submitNodeFilters();">Show all nodes</button>
                            </g:form>
                        </g:if>
                    </div>

                </div>
                </g:if>



                <div class=" clear matchednodes " id="nodelist" >
                    <g:render template="allnodes" model="${[nodeview:'table', expanddetail: true,allnodes: allnodes, totalexecs: totalexecs, jobs: jobs, params: params, total: total, allcount: allcount, page: page, max: max, nodeauthrun: nodeauthrun, tagsummary: tagsummary]}" />
                </div>



</div>
</div>


</div>
<div id="loaderror"></div>
</body>
</html>
