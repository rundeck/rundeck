<%@ page import="com.dtolabs.rundeck.core.dispatcher.DataContextUtils" %><g:set var="ukey" value="${g.rkey()}"/>
<g:if test="${params.declarenone && nodes.size()<1}">
    <span class="warn note">None</span>
</g:if>
<g:if test="${nodes && nodes.size()>0}">
<table cellpadding="0" cellspacing="0" width="100%" id="nodesTable">
    <tr>
        <th>Name</th>
        <th>Description</th>
        <th>Tags</th>
        <th>Username</th>
        <th>Hostname</th>
        <th></th>
    </tr>
        <% def seen=false %>
        <g:each in="${nodes.keySet().sort()}" var="nodekey" status="i">
            <g:set var="nodedata" value="${nodes[nodekey]}"/>
            <g:set var="node" value="${nodedata.node}"/>
            <g:set var="executions" value="${nodedata.executions}"/>
            <g:set var="resources" value="${nodedata.resources}"/>
            <g:set var="resName" value="${node.nodename}"/>
            <g:set var="resHost" value="${node.hostname}"/>

            <tr class="${i%2==1?'alternateRow':''} node_entry ${nodedata.islocal?'server':''}">
                <td class="objIdent" title="Name">
                    <g:if test="${expanddetail||params.expanddetail}">
                        <g:expander key="${ukey+'node_detail_'+i}" imgfirst="true">
                        <span class="node_ident" id="${ukey}_${node.nodename}_key">
                            <img src="${resource(dir:'images',file:'icon-small-Node.png')}" alt="Node" width="16px" height="16px"/>
                            ${resName}
                        </span>
                        </g:expander>
                    </g:if>
                    <g:else>
                        <span class="node_ident" id="${ukey}_${node.nodename}_key">
                            <img src="${resource(dir:'images',file:'icon-small-Node.png')}" alt="Node" width="16px" height="16px"/>
                            ${resName}
                        </span>
                    </g:else>

                    <g:if test="${!session.project}">
                    <span class="project">
                        &bull; <span class="action textbtn" onclick="selectProject('${nodedata.project.name.encodeAsJavaScript()}');" title="Select this project">${nodedata.project.name}</span> 
                    </span>
                    </g:if>
                    </td>
                <td class="desc"  title="Description">
                        <span class="desc">${node.description}</span>
                </td>
                <td  title="Tags">
                    <g:if test="${node.tags}">
                        <span class="nodetags">
                            <g:each var="tag" in="${node.tags}">
                                <g:link class="tag action" action="nodes" params="${[nodeIncludeTags:tag]}" title="Filter by tag: ${tag}">${tag}</g:link>
                                %{--<span class="action textbtn" onclick="setTagFilter('${tag.encodeAsJavaScript()}');" title="Add to existing filter">+</span>--}%
                            </g:each>
                        </span>
                    </g:if>
                </td>

                <td class="username"  title="Username">
                        ${node.username} <span class="atsign">@</span>
                </td>
                <td class="hostname"  title="Hostname">
                        ${node.hostname}
                </td>
                <td>
                    <g:if test="${node.attributes?.remoteUrl}">
                        <g:set var="nodecontextdata" value="${DataContextUtils.nodeData(node)}"/>
                        <%
                            nodecontextdata.project=nodedata.project.name
                        %>
                        <g:set var="remoteUrl" value="${DataContextUtils.replaceDataReferences(node.attributes?.remoteUrl,[node:nodecontextdata])}" />
                        <span class="action " title="Edit this node via remote URL..." onclick='doRemoteEdit("${node.nodename.encodeAsJavaScript()}","${nodedata.project.name.encodeAsJavaScript()}","${remoteUrl.encodeAsJavaScript()}");'>Edit&hellip;</span>
                    </g:if>
                    <g:elseif test="${node.attributes?.editUrl}">
                        <g:set var="nodecontextdata" value="${DataContextUtils.nodeData(node)}"/>
                        <%
                            nodecontextdata.project=nodedata.project.name
                        %>
                        <g:set var="editUrl" value="${DataContextUtils.replaceDataReferences(node.attributes?.editUrl,[node:nodecontextdata])}" />
                        <a href="${editUrl}" target="_blank" title="Opens a link to edit this node at a remote site.">Edit</a>
                    </g:elseif>

                </td>
            </tr>

            <g:if test="${expanddetail||params.expanddetail}">
                %{--<g:link  controller="reports" action="index" params="${[nodeFilter:node.nodename]}" title="View History for Node ${node.nodename}">--}%
                    <!--&raquo; history-->
                %{--</g:link>--}%
                <tr id="${ukey}node_detail_${i}" class="detail_content nodedetail" style="display:none">
                    <td colspan="4"><g:render template="nodeDetailsSimple" model="[node:node,key:ukey+'_'+node.nodename+'_key',projects:nodedata.projects,exclude:['type','username','hostname','tags']]"/></td>
                </tr>
            </g:if>
            <g:else>
                <g:render template="nodeTooltipView" model="[node:node,key:ukey+'_'+node.nodename+'_key',includeDescription:true]"/>
            </g:else>
        </g:each>
</table>

<g:javascript>
    if(typeof(initTooltipForElements)=='function'){
        initTooltipForElements('tr.node_entry span.node_ident');
    }
</g:javascript>
</g:if>
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