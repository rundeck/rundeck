<%--
  Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

 --%>
 <%--
    _nodesTableContent.gsp

    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Jan 13, 2011 10:03:47 AM
 --%>
<%@ page import="com.dtolabs.rundeck.core.dispatcher.DataContextUtils" %><g:set var="ukey" value="${g.rkey()}"/>
<% def seen=false %>
        <g:each in="${nodes.keySet().sort()}" var="nodekey" status="i">
            <g:set var="nodedata" value="${nodes[nodekey]}"/>
            <g:set var="node" value="${nodedata.node}"/>
            <g:set var="executions" value="${nodedata.executions}"/>
            <g:set var="resources" value="${nodedata.resources}"/>
            <g:set var="resName" value="${node.nodename}"/>

            <tr class="${i%2==1?'alternateRow':''} node_entry ${nodedata.islocal?'server':''}">
                <td class="objIdent" title="Name">
                    <g:if test="${expanddetail||params.expanddetail}">
                        <g:expander key="${ukey+'node_detail_'+i}" imgfirst="true">
                        <span class="node_ident" id="${ukey}_${node.nodename}_key">
                            <img src="${resource(dir:'images',file:'icon-small-Node.png')}" alt="Node" width="16px" height="16px"/>
                            ${resName.encodeAsHTML()}
                        </span>
                        </g:expander>
                    </g:if>
                    <g:else>
                        <span class="node_ident" id="${ukey}_${node.nodename}_key">
                            <img src="${resource(dir:'images',file:'icon-small-Node.png')}" alt="Node" width="16px" height="16px"/>
                            ${resName.encodeAsHTML()}
                        </span>
                    </g:else>

                    <g:if test="${!session.project}">
                    <span class="project">
                        &bull; <span class="action textbtn" onclick="selectProject('${nodedata.project.name.encodeAsJavaScript()}');" title="Select this project">${nodedata.project.name.encodeAsHTML()}</span>
                    </span>
                    </g:if>
                    </td>
                <td class="desc"  title="Description">
                        <span class="desc">${node.description?.encodeAsHTML()}</span>
                </td>
                <td  title="Tags">
                    <g:if test="${node.tags}">
                        <span class="nodetags">
                            <g:each var="tag" in="${node.tags.sort()}">
                                <g:link class="tag action" action="nodes" params="${[nodeIncludeTags:tag]}" title="Filter by tag: ${tag.encodeAsHTML()}">${tag.encodeAsHTML()}</g:link>
                                %{--<span class="action textbtn" onclick="setTagFilter('${tag.encodeAsJavaScript()}');" title="Add to existing filter">+</span>--}%
                            </g:each>
                        </span>
                    </g:if>
                </td>

                <td class="username"  title="Username">
                    <g:if test="${node.username}">
                        ${node.username?.encodeAsHTML()} <span class="atsign">@</span>
                    </g:if>
                </td>
                <td class="hostname"  title="Hostname">
                        ${node.hostname?.encodeAsHTML()}
                    <g:if test="${null!=nodeauthrun && !nodeauthrun[node.nodename]}">
                        <span title="Not authorized to 'run' on this node">
                            <img src="${resource(dir: 'images', file: 'icon-tiny-warn.png')}" alt="Node" width="12px"
                                 height="12px"/>
                        </span>
                    </g:if>
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
                        <a href="${editUrl.encodeAsHTML()}" target="_blank" title="Opens a link to edit this node at a remote site.">Edit</a>
                    </g:elseif>

                </td>
            </tr>

            <g:if test="${expanddetail||params.expanddetail}">
                %{--<g:link  controller="reports" action="index" params="${[nodeFilter:node.nodename]}" title="View History for Node ${node.nodename}">--}%
                    <!--&raquo; history-->
                %{--</g:link>--}%
                <tr id="${ukey}node_detail_${i}" class="detail_content nodedetail" style="display:none">
                    <td colspan="6"><g:render template="nodeDetailsSimple" model="[node:node,key:ukey+'_'+node.nodename+'_key',projects:nodedata.projects,exclude:['username','hostname','tags']]"/></td>
                </tr>
            </g:if>
            <g:else>
                <g:render template="nodeTooltipView" model="[node:node,key:ukey+'_'+node.nodename+'_key',includeDescription:true]"/>
            </g:else>
        </g:each>