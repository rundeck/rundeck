<html>
<head>
    <g:set var="rkey" value="${g.rkey()}" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="nodes"/>
    <title>Nodes</title>
    <g:javascript library="executionControl"/>
    <script type="text/javascript">
        function showError(message) {
            $("error").innerHTML += message;
            $("error").show();
        }

        //set box filterselections
        function _setFilterSuccess(response,name){
            console.log("callback");
            var data=eval("("+response.responseText+")"); // evaluate the JSON;
            if(data){
                var bfilters=data['filterpref'];
                //reload page
                document.location="${createLink(controller:'framework',action:'nodes')}"+(bfilters[name]?"?filterName="+encodeURIComponent(bfilters[name]):'');
            }
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
                        console.log(e);
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
         * START run
         */
        <g:set var="jsdata" value="${query?.properties.findAll{it.key==~/^(node(In|Ex)clude.*|project)$/ &&it.value}}"/>

        var nodeFilterData_${rkey}=${jsdata.encodeAsJSON()};

        function expandResultNodes(){
            _updateMatchedNodes(nodeFilterData_${rkey},'nodelist','${query.project}',false,{view:'table',expanddetail:true});
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

        }
        function runFormSubmit(elem){
            var data = Form.serialize(elem);

            disableRunBar();
            collapseNodeView();

            new Ajax.Request("${createLink(controller:'scheduledExecution',action:'runAdhocInline')}",{
                parameters:data,
                evalScripts:true,
                evalJSON:true,
                onSuccess: function(transport) {
                    var data =transport.responseJSON;
//                    alert("data: "+data);
                    startRunFollow(data);
                },
                onComplete: function(transport){
//                    alert("complet: "+transport.responseText);
                }
            });
            return false;
        }
        function startRunFollow(data){
            //execute run with the given node set.
            if(data.id){
            //after callback, load execution output in runcontent section
                new Ajax.Updater('runcontent',"${createLink(controller:'execution',action:'followFragment')}",{
                parameters:{id:data.id},
                evalScripts:true,
                onComplete: function(transport) {
                    if (transport.request.success()) {
                        Element.show('runcontent');
                        continueRunFollow(data);
                    }
                },
            });
            }
        }
        function continueRunFollow(data){
            try{
             var followControl = new FollowControl(data.id,'runcontent',{
                extraParams:"<%="true" == params.disableMarkdown ? '&disableMarkdown=true' : ''%>",
                iconUrl: "${resource(dir: 'images', file: 'icon')}",
                lastlines: ${params.lastlines ? params.lastlines : 20},
                tailmode: true,
                browsemode: ${followmode == 'browse'},
                nodemode: ${followmode == 'node'},
                execData: {node:"test"},
                 appLinks:appLinks,
                 onComplete:onRunComplete,
            });
            followControl.beginFollowingOutput(data.id);
            }catch(e){
                console.log(e.stack);
            }
        }
        function onRunComplete(){
            enableRunBar();
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
        .runbox{
            background: #ddd;
            margin-bottom: 10px;
            padding:5px;
        }
        .runbox input[type='text']{
            font-size: 150%;
            font-family: Monaco, 'Courier New', 'DejaVu Sans Mono', 'Bitstream Vera Sans Mono', monospace;
            font-family: Courier, monospace;
        }

    </style>
</head>
<body>

<g:if test="${session.user && User.findByLogin(session.user)?.nodefilters}">
    <g:set var="filterset" value="${User.findByLogin(session.user)?.nodefilters}"/>
</g:if>
<div id="nodesContent">

<div class="pageBody solo">
<g:render template="/common/messages"/>
<div id="${rkey}nodeForm">
    <g:set var="isCompact" value="${params.compact?true:false}"/>
    <g:set var="wasfiltered" value="${paginateParams?.keySet().grep(~/(?!proj).*Filter|groupPath|project$/)||(query && !query.nodeFilterIsEmpty())}"/>
    <g:set var="filtersOpen" value="${params.createFilters||params.editFilters||params.saveFilter || filterErrors?true:false}"/>
<table cellspacing="0" cellpadding="0" class="queryTable" width="100%">
        <tr>
        <g:if test="${!params.nofilters}">
        <td style="text-align:left;vertical-align:top; width:400px; ${wdgt.styleVisible(if:filtersOpen)}" id="${rkey}filter" >
            <g:form action="nodes" controller="framework" >
                <g:if test="${params.compact}">
                    <g:hiddenField name="compact" value="${params.compact}"/>
                </g:if>
                <span class="prompt action" onclick="['${rkey}filter','${rkey}filterdispbtn','runbox'].each(Element.toggle); if (${isCompact}) { $('${rkey}nodescontent').toggle(); }">
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
                            <g:submitButton  name="Filter" />

                            <g:submitButton name="Clear" />
                        </div>
                    </div>
                </div>
                </g:form>
        </td>
            </g:if>
            <td style="text-align:left;vertical-align:top;" id="${rkey}nodescontent">

                        <g:if test="${session.project && total>0}">
                            <div class="runbox" id="runbox">
                            <g:form action="execAndForget" controller="scheduledExecution" method="post" style="display:inline" onsubmit="runFormSubmit(this);">
                                Command:
                                <g:img file="icon-small-shell.png" width="16px" height="16px"/>
                                <g:hiddenField name="project" value="${session.project}"/>
                                <g:hiddenField name="doNodedispatch" value="true"/>
                                <g:if test="${!wasfiltered}">
                                    <g:hiddenField name="nodeIncludeName" value=".*"/>
                                </g:if>
                                <g:hiddenField name="nodeKeepgoing" value="true"/>
                                <g:hiddenField name="nodeThreadcount" value="1"/>
                                <g:hiddenField name="description" value=""/>

                                <g:hiddenField name="workflow.commands[0].adhocExecution" value="true"/>
                                <g:hiddenField name="workflow.threadcount" value="1"/>
                                <g:hiddenField name="workflow.keepgoing" value="false"/>
                                <g:hiddenField name="workflow.project" value="${session.project}"/>

                                <g:textField name="workflow.commands[0].adhocRemoteString" size="80" placeholder="Enter a shell command" autofocus="true" />
                                <g:render template="nodeFiltersHidden" model="${[params:params,query:query]}"/>
                                <g:if test="${auth.allowedTest(job:[jobName:'adhoc_run', groupPath:'ui'], action:UserAuth.WF_RUN)}">
                                <!--<input type="submit" value="Run"/>-->
                                </g:if>
                                <g:else>
                                    <span class="button disabled" title="You are not authorized to run ad-hoc jobs">Run</span>
                                </g:else>
                            </g:form>
                                <button onclick="runFormSubmit('runbox');">Run</button>
                            </div>
                        </g:if>
                <g:ifUserInAnyRoles roles="admin,nodes_admin">
                    <g:if test="${selectedProject && selectedProject.shouldUpdateNodesResourceFile()}">
                        <span class="floatr"><g:link action="reloadNodes" params="${[project:selectedProject.name]}" class="action button" title="Click to update the resources.xml file from the source URL, for project ${selectedProject.name}" onclick="\$(this.parentNode).loading();">Update Nodes for project ${selectedProject.name}</g:link></span>
                    </g:if>
                </g:ifUserInAnyRoles>
                <g:if test="${!params.nofilters}">
                <div style="margin-bottom: 5px;" id="${rkey}nodesfilterholder">
                    <g:if test="${wasfiltered}">


                        <div style="margin:5px 0; padding:5px 0;">
                            <span style="padding:5px 0;margin:5px 0;${!filtersOpen?'':'display:none;'} " id='${rkey}filterdispbtn' >
                            <span title="Click to modify filter" class="info textbtn query action" onclick="['${rkey}filter','${rkey}filterdispbtn','runbox'].each(Element.toggle);if(${isCompact}){$('${rkey}nodescontent').toggle();}" >
                                <g:render template="displayNodeFilters" model="${[displayParams:query]}"/>
                                <img src="${resource(dir:'images',file:'icon-tiny-disclosure.png')}" width="12px" height="12px"/></span>
                            </span>


                        <g:if test="${!filterName}">
                            <span class="prompt action" onclick="['${rkey}filter','${rkey}filterdispbtn','${rkey}fsave','${rkey}fsavebtn'].each(Element.toggle);if(${isCompact}){$('${rkey}nodescontent').toggle();}" id="${rkey}fsavebtn" title="Click to save this filter with a name">
                                save this filter&hellip;
                            </span>
                        </g:if>

                        <g:if test="${filterset}">
                            <g:render template="/common/selectFilter" model="[filterset:filterset,filterName:filterName,prefName:'nodes',noSelection:filterName?'-Server Node-':null]"/>
                        </g:if>
                        </div>

                    </g:if>
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

                <div class="nodesummary clear">
                    <span class="match">${total}/${allcount} Node${1 != allcount ? 's' : ''}</span>
                    <span class="type">
                    <g:if test="${!filterName}">
                        matching filter input
                    </g:if>
                    <g:else>
                        matching saved filter
                    </g:else>
                    </span>
                </div>

                <g:if test="${tagsummary}">
                    <div class="presentation clear" >
                    <g:set var="hidetop" value="${tagsummary.findAll {it.value>1}.size()>30}"/>
                    <g:if test="${hidetop}">
                    <span class="action button receiver" title="Show tag demographics" onclick="Element.show('tagdemo');Element.hide(this);">Show ${tagsummary.size()} tags&hellip;</span>
                    </g:if>
                    <span id="tagdemo" style="${wdgt.styleVisible(unless:hidetop)}">
                        <span class="desc">${tagsummary.size()} tags:</span>
                    <g:set var="singletag" value="${[]}"/>
                    <g:each var="tag" in="${tagsummary.sort{a,b->a.value>b.value?-1:a.value<b.value?1:a.key<=>b.key}.keySet()}">
                        <g:if test="${tagsummary[tag]>1 || tagsummary.size()<=30}">
                        <span class="summary"><g:link class=" action" action="nodes" params="${[nodeIncludeTags:tag]}" title="Filter by tag: ${tag}">${tag}</g:link>:${tagsummary[tag]}</span>
                        </g:if>
                        <g:else>
                            %{singletag<<tag}%
                        </g:else>
                    </g:each>
                    <g:if test="${singletag}">
                        <span class="action button receiver" title="See all tags" onclick="Element.show('singletags');Element.hide(this);">Show All&hellip;</span>
                        <span style="display:none" id="singletags">
                            <g:each var="tag" in="${singletag}">
                                <span class="summary"><g:link class=" action" action="nodes" params="${[nodeIncludeTags:tag]}" title="Filter by tag: ${tag}">${tag}</g:link>:${tagsummary[tag]}</span>
                            </g:each>
                        </span>
                    </g:if>
                    </span>
                    </div>
                </g:if>
                <div class="presentation clear matchednodes" id="nodelist" >
                    <span class="button action receiver" onclick="expandResultNodes();">Show ${total} Node${1 != total ? 's' : ''}...</span>
                    %{--<g:render template="nodes" model="${[nodes:allnodes,totalexecs:totalexecs,jobs:jobs,params:params,expanddetail:true]}"/>--}%
                    <g:if test="${total<=30}">
                        <g:javascript>

                            fireWhenReady('nodelist',expandResultNodes);
                        </g:javascript>
                    </g:if>

                </div>

                 </td>

                </tr>
            </table>
</div>
<div id="runcontent"></div>
<g:javascript>

$$('#${rkey}nodeForm input').each(function(elem){
    if(elem.type=='text'){
        elem.observe('keypress',noenter);
    }
});
</g:javascript>

    </div>
</div>
<div id="loaderror"></div>
</body>
</html>