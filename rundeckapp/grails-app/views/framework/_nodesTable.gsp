<g:if test="${params.declarenone && nodes.size()<1}">
    <span class="warn note">None</span>
</g:if>
<g:if test="${nodes && nodes.size()>0}">
    <div class="presentation clear">
    <g:render template="tagsummary" model="${[tagsummary:tagsummary,link:[action:'nodes',controller:'framework',param:'nodeIncludeTags']]}"/>
    </div>
    <table cellpadding="0" cellspacing="0" width="100%" id="nodesTable">
        <g:if test="${!page || page=='0'}">
            <tr>
                <th>Name</th>
                <th>Description</th>
                <th>Tags</th>
                <th>Username</th>
                <th>Hostname</th>
                <th></th>
            </tr>
        </g:if>
        <g:render template="nodesTableContent" model="${[nodes:nodes,params:params,total:total,allcount:allcount,page:page,max:max,nodeauthrun:nodeauthrun]}"/>
    </table>
    <g:if test="${page==0 && (page+1*max<total)}">
        <div id="nodesPaging">
            %{--${nodes.size()}/${allcount} (page ${page} in ${max})--}%
            <g:set var="remainCount" value="${total - (page+1*max)}"/>
            <g:if test="${remainCount>max}">
            <span id="nextPageButton" class="action button" onclick="_loadNextNodesPageTable(${max},${total},'nodesTable','nodesPaging');" title="Load next ${max} nodes...">Next ${max}&hellip;</span>
            </g:if>
            <span class="action button" onclick="_loadNextNodesPageTable(${max},-1,'nodesTable','nodesPaging');" title="Load all remaining nodes...">Load all <span id="moreCount">${remainCount}</span>&hellip;</span>
        </div>
    </g:if>

    <g:javascript>
        if(typeof(initTooltipForElements)=='function'){
            initTooltipForElements('tr.node_entry span.node_ident');
        }
    </g:javascript>
</g:if>
<g:render template="/common/boxinfo" model="${[name:'nodetable',model:[total:nodes?.size()]]}"/>

<div id="remoteEditholder" style="display:none" class="popout">
    <span id="remoteEditHeader">
            <span class="welcomeMessage">Edit node: <g:img file="icon-small-Node.png" width="16px" height="16px"/> <span id="editNodeIdent"></span></span>
    </span>
    <span class="toolbar" id="remoteEditToolbar">
        <span class="action " onclick="_remoteEditCompleted();" title="Close the remote edit box and discard any changes"><g:img file="icon-tiny-removex-gray.png" /> Close remote editing</span>
    </span>
    <div id="remoteEditResultHolder" class="info message" style="display:none">
        <span id="remoteEditResultText" class="info message" >
        </span>
        <span class="action " onclick="_remoteEditContinue();"> Continue&hellip;</span>
    </div>
    <div id="remoteEditError" class="error note" style="display:none">
    </div>
    <div id="remoteEditTarget" >

    </div>
</div>