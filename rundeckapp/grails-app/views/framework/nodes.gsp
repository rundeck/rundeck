<html>
<head>
    <g:set var="rkey" value="${g.rkey()}" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="nodes"/>
    <meta name="selectedMenu" content="Nodes"/>
    <title>Nodes</title>
    <script type="text/javascript">
        function showError(message) {
            $("error").innerHTML += message;
            $("error").show();
        }

        //set box filterselections
        function setFilterz(name,value){
            if(!value){
                value="!";
            }
            var str=name+"="+value;
            console.log("setFilterz: "+str);
            var x = setFilter(name,value,_setFilterSuccess);
            console.log("called: "+x);
            console.log("typeof: "+typeof(setFilter));
        }

        //set box filterselections
        function _setFilterSuccess(response,name){
            console.log("callback");
            var data=eval("("+response.responseText+")"); // evaluate the JSON;
            if(data){
                var bfilters=data['filterpref'];
                //reload page
                document.location="${createLink(controller:'framework',action:'nodes')}"+(bfilters[name]?"?filterName="+bfilters[name]:'');
            }
        }

        //set box filterselections
        function setFiltern(name,value){
            if(!value){
                value="!";
            }
            var str=name+"="+value;
            alert("setFilter: "+str);
            new Ajax.Request("${createLink(controller:'user',action:'addFilterPref')}",{parameters:{filterpref:str}, evalJSON:true,onSuccess:function(response){
                _setFilterSuccess(response,name);
            }});
        }

        /*********
         *  remote editor
         *********/
        
        var remoteSite;
        var remoteEditStarted=false;
        var remoteEditExpect=false;

        /**
         * Start remote editor for node with url
         * @param name node name
         * @param url url
         */
        function doRemoteEdit(name,url){
            _remoteEditClear();

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

            Event.stopObserving(window,'message', _rdeckNodeEditOnmessage);
        }

        /**
         * Clear/reset remote editor dom content
         */
        function _remoteEditClear(){
            _remoteEditStop();
            
            _clearTarget();

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

        //protocol handler functions


        /**
         * handler for :finished message
         * @param changed true if changes were saved
         */
        function _rdeckNodeEditFinished(changed){
            _remoteEditStop();
            _clearTarget();
            
            if(changed){
                $('remoteEditResultText').innerHTML="Node changes were saved successfully.";
            }else{
                $('remoteEditResultText').innerHTML="Node changes were not saved.";
            }
            
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
                <span class="prompt action" onclick="['${rkey}filter','${rkey}filterdispbtn'].each(Element.toggle); if (${isCompact}) { $('${rkey}nodescontent').toggle(); }">
                    Filter
                    <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure-open.png')}" width="12px" height="12px"/>
                </span>

                <g:render template="/common/queryFilterManager" model="${[filterName:filterName,filterset:filterset,deleteActionSubmit:'deleteNodeFilter',storeActionSubmit:'storeNodeFilter']}"/>
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

                <g:ifUserInAnyRoles roles="admin,nodes_admin">
                    <g:if test="${selectedProject && selectedProject.shouldUpdateNodesResourceFile()}">
                        <span class="floatr"><g:link action="reloadNodes" params="${[project:selectedProject.name]}" class="action button" title="Click to update the resources.xml file from the source URL, for project ${selectedProject.name}" onclick="\$(this.parentNode).loading();">Update Nodes for project ${selectedProject.name}</g:link></span>
                    </g:if>
                </g:ifUserInAnyRoles>
                <g:if test="${!params.nofilters}">
                <div style="margin-bottom: 5px;" id="${rkey}nodesfilterholder">
                    <g:if test="${wasfiltered}">

                        <g:if test="${!params.compact}">
                            <span class="prompt">${total} Nodes</span>
                            matching filter:
                        </g:if>


                        <g:if test="${filterset}">
                            <g:render template="/common/selectFilter" model="[noSelection:'-All-',filterset:filterset,filterName:filterName,prefName:'nodes']"/>
                        </g:if>
                        
                        <g:if test="${!filterName}">
                            <span class="prompt action" onclick="['${rkey}filter','${rkey}filterdispbtn','${rkey}fsave','${rkey}fsavebtn'].each(Element.toggle);if(${isCompact}){$('${rkey}nodescontent').toggle();}" id="${rkey}fsavebtn" title="Click to save this filter with a name">
                                save this filter&hellip;
                            </span>
                        </g:if>

                        <div style="padding:5px 0;margin:5px 0;${!filtersOpen?'':'display:none;'} " id='${rkey}filterdispbtn' >
                            <span title="Click to modify filter" class="info textbtn query action" onclick="['${rkey}filter','${rkey}filterdispbtn'].each(Element.toggle);if(${isCompact}){$('${rkey}nodescontent').toggle();}" >
                                <g:render template="displayNodeFilters" model="${[displayParams:query]}"/>
                                <img src="${resource(dir:'images',file:'icon-tiny-disclosure.png')}" width="12px" height="12px"/></span>
                        </div>
                        
                    </g:if>
                    <g:else>
                        <span class="prompt">Nodes (${total})</span>
                        <span class="prompt action" onclick="['${rkey}filter','${rkey}filterdispbtn'].each(Element.toggle);if(${isCompact}){$('${rkey}nodescontent').toggle();}" id="${rkey}filterdispbtn"  style="${!filtersOpen?'':'display:none;'}">
                            Filter
                            <img src="${resource(dir:'images',file:'icon-tiny-disclosure.png')}" width="12px" height="12px"/></span>
                        <g:if test="${filterset}">
                            <span style="margin-left:10px;">
                                <span class="info note">Filter:</span>
                                <g:render template="/common/selectFilter" model="[noSelection:'-All-',filterset:filterset,filterName:filterName,prefName:'nodes']"/>
                            </span>
                        </g:if>
                    </g:else>
                </div>
                </g:if>

                <div class="jobsReport clear">
                    <g:if test="${allnodes}">
                            <g:render template="nodes" model="${[nodes:allnodes,totalexecs:totalexecs,jobs:jobs,params:params,expanddetail:true]}"/>

                    </g:if>
                </div>

                 </td>

                </tr>
            </table>
</div>
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