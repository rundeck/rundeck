<%@ page import="com.dtolabs.rundeck.core.dispatcher.DataContextUtils" %><g:set var="ukey" value="${g.rkey()}"/>
<table cellpadding="0" cellspacing="0" width="100%" id="nodesTable">

        <% def seen=false %>
        <g:each in="${nodes.keySet().sort()}" var="nodekey" status="i">
            <g:set var="nodedata" value="${nodes[nodekey]}"/>
            <g:set var="node" value="${nodedata.node}"/>
            <g:set var="executions" value="${nodedata.executions}"/>
            <g:set var="resources" value="${nodedata.resources}"/>
            <g:set var="resName" value="${node.nodename}"/>
            <g:set var="resHost" value="${node.hostname}"/>

            <tr class="${i%2==1?'alternateRow':''} node_entry ${nodedata.islocal?'server':''}">
                <td class="objIdent" colspan="3">
                    <g:if test="${expanddetail}">
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


                    <g:if test="${totalexecs && totalexecs[node.nodename]}">
                        (${totalexecs[node.nodename]})
                    </g:if>
                    <g:if test="${!session.project}">
                    <span class="project">
                        &bull; <span class="action textbtn" onclick="selectProject('${nodedata.project.name.encodeAsJavaScript()}');" title="Select this project">${nodedata.project.name}</span> 
                    </span>
                    </g:if>
                    
                    <g:if test="${node.tags}">
                        <span class="nodetags">
                            <g:each var="tag" in="${node.tags.sort()}">
                                <span class="tag">${tag}</span>
                            </g:each>
                        </span>
                    </g:if>
                    <span class="desc">
                        ${node.description?.encodeAsHTML()}
                    </span>
                    <g:if test="${!nodeauthrun[node.nodename]}">
                    <span class="desc" title="Not authorized to run on this node">
                        !
                    </span>
                    </g:if>

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

                    <g:if test="${expanddetail}">
                        %{--<g:link  controller="reports" action="index" params="${[nodeFilter:node.nodename]}" title="View History for Node ${node.nodename}">--}%
                            <!--&raquo; history-->
                        %{--</g:link>--}%
                        <div id="${ukey}node_detail_${i}" class="detail_content nodedetail" style="display:none">
                            <g:render template="nodeDetailsSimple" model="[node:node,key:ukey+'_'+node.nodename+'_key',projects:nodedata.projects]"/>
                        </div>
                    </g:if>
                    <g:else>
                        <g:render template="nodeTooltipView" model="[node:node,key:ukey+'_'+node.nodename+'_key',includeDescription:true]"/>
                    </g:else>
                </td>
                
            </tr>
            <g:if test="${executions}" >
                <tr class="${i%2==1?'alternateRow':''} node_execs ">
                    <td ></td>
                    <td colspan="${params.simple?'1':'3'}">
                        <div>
                            <g:render template="/menu/executions" model="${[executions:executions,jobs:jobs,small:true,upref:node.nodename+'_']}"/>
                        </div>
                    </td>
                </tr>
            </g:if>
        </g:each>
</table>
<g:javascript>
    if(typeof(initTooltipForElements)=='function'){
        initTooltipForElements('tr.node_entry span.node_ident');
    }
</g:javascript>

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