%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%
 <%--
    _nodesTableContent.gsp

    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Jan 13, 2011 10:03:47 AM
 --%>
<%@ page import="com.dtolabs.rundeck.core.dispatcher.DataContextUtils" %><g:set var="ukey" value="${g.rkey()}"/>
<g:set var="cols" value="${colkeys ? colkeys.sort() :  []}"/>
<g:if test="${cols && cols!=['tags'] && page==0}">
    <tr>
        <th>Node</th>
        <g:each in="${cols.findAll{!it.startsWith('ui:')}}" var="colname">
            <th><g:enc>${colname}</g:enc></th>
        </g:each>
        <g:if test="${!cols}">
            <th>Tags</th>
            <th colspan="3" class="text-center">User @ Host</th>
        </g:if>
    </tr>
</g:if>
<% def seen=false %>
        <g:each in="${nodes.keySet().sort()}" var="nodekey" status="i">
            <g:set var="nodedata" value="${nodes[nodekey]}"/>
            <g:set var="node" value="${nodedata.node}"/>
            <g:set var="executions" value="${nodedata.executions}"/>
            <g:set var="resources" value="${nodedata.resources}"/>
            <g:set var="resName" value="${node.nodename}"/>
            <g:set var="runnable" value="${null == nodeauthrun || nodeauthrun[node.nodename]}"/>

            <tr class="${i%2==1?'alternateRow':''} node_entry ${nodedata.islocal?'server':''} hover-action-holder ansicolor-on">
                <td class="nodeident" title="${enc(attr:node.description)}" >
                    <g:if test="${expanddetail||params.expanddetail}">
                        <g:expander key="${ukey+'node_detail_'+i}" imgfirst="true">
                        <span class="node_ident ${nodeStatusColorCss(node:node)}"
                              style="${nodeStatusColorStyle(node:node)}"
                              id="${enc(attr:ukey)}_${enc(attr:node.nodename)}_key">
                            <g:nodeStatusColor node="${node}" icon="true"><g:nodeStatusIcon
                                    node="${node}"
                            ><i class="rdicon node ${runnable?'node-runnable':''} icon-small">

                                </i></g:nodeStatusIcon></g:nodeStatusColor>

                            ${resName}
                        </span>
                        </g:expander>
                    </g:if>
                    <g:else>
                        <span class="node_ident" id="${enc(attr:ukey)}_${enc(attr:node.nodename)}_key">
                            <i class="rdicon node ${runnable ? 'node-runnable' : ''} icon-small"></i>
                            <g:enc>${resName}</g:enc>
                        </span>
                    </g:else>
                    <tmpl:nodeFilterLink key="name" value="${resName}"
                                         linkicon="glyphicon glyphicon-circle-arrow-right"/>
                    <span class="nodedesc"></span>
                    <span class="text-muted ">
                        <g:nodeBadgeIcons node="${node}" css="badge"/>
                        ${node.description}
                    </span>
                </td>
                <g:each in="${cols.sort().findAll{!it.startsWith('ui:')}}" var="colname" status="coli">
                    <g:if test="${colname=='tags'}">
                        <td  title="Tags" class="nodetags" >
                            <g:if test="${node.tags}">
                                <span class="nodetags">
                                    <i class="glyphicon glyphicon-tags text-muted"></i>
                                    <g:each var="tag" in="${node.tags.sort().findAll{!it.startsWith('ui:')}}">
                                        <tmpl:nodeFilterLink key="tags" value="${tag}" linkclass="textbtn tag"/>
                                    </g:each>
                                </span>
                            </g:if>
                        </td>
                    </g:if>
                    <g:else>
                        <td >
                            <g:if test="${node.attributes[colname]}">
                                <span class="value">
                                    <g:enc>${node.attributes[colname]}</g:enc>
                                    <tmpl:nodeFilterLink key="${colname}" value="${node.attributes[colname]}"
                                                         linkicon="glyphicon glyphicon-search textbtn-saturated hover-action"
                                                         linkclass="textbtn textbtn-info"/>
                                </span>
                            </g:if>
                        </td>
                    </g:else>
                </g:each>

                <g:if test="${!cols}">
                    <td title="Tags" class="nodetags" >
                        <g:if test="${node.tags}">
                            <span class="nodetags">
                                <i class="glyphicon glyphicon-tags text-muted"></i>
                                <g:each var="tag" in="${node.tags.sort().findAll{!it.startsWith('ui:')}}">
                                    <tmpl:nodeFilterLink key="tags" value="${tag}" linkclass="textbtn tag"/>
                                </g:each>
                            </span>
                        </g:if>
                    </td>
                    <td class="username"  title="Username">
                        <g:if test="${node.username}">

                            <tmpl:nodeFilterLink key="username" value="${node['username']}"/>
                            <span class="atsign">@</span>
                        </g:if>
                    </td>
                    <td class="hostname"  title="Hostname">
                        <tmpl:nodeFilterLink key="hostname" value="${node['hostname']}"/>
                    </td>
                </g:if>
                <td>
                    <g:if test="${node.attributes?.remoteUrl}">
                        <g:set var="nodecontextdata" value="${DataContextUtils.nodeData(node)}"/>
                        <%
                            nodecontextdata.project=nodedata.project.name
                        %>
                        <g:set var="remoteUrl" value="${DataContextUtils.replaceDataReferences(node.attributes?.remoteUrl,[node:nodecontextdata])}" />
                        <span class="action " title="Edit this node via remote URL..." onclick='doRemoteEdit("${enc(js: node.nodename)}","${enc(js: nodedata.project.name)}","${enc(js: remoteUrl)}");'>Edit&hellip;</span>
                    </g:if>
                    <g:elseif test="${node.attributes?.editUrl}">
                        <g:set var="nodecontextdata" value="${DataContextUtils.nodeData(node)}"/>
                        <%
                            nodecontextdata.project=nodedata.project.name
                        %>
                        <g:set var="editUrl" value="${DataContextUtils.replaceDataReferences(node.attributes?.editUrl,[node:nodecontextdata])}" />
                        <a href="${enc(attr:editUrl)}" target="_blank" title="Opens a link to edit this node at a remote site.">Edit</a>
                    </g:elseif>

                </td>
            </tr>

            <g:if test="${expanddetail||params.expanddetail}">
                <tr id="${enc(attr:ukey+'node_detail_'+i)}" class="detail_content nodedetail ${nodedata.islocal ? 'server' : ''}" style="display:none">
                    <td colspan="${(4+cols.size())}">
                        <g:render template="nodeDetailsSimple" model="[runnable:runnable, useNamespace:true, linkAttrs: true, node:node,key:ukey+'_'+node.nodename+'_key',projects:nodedata.projects,exclude: cols?null:['username','hostname']]"/>
                    </td>
                </tr>
            </g:if>
            <g:else>
                <g:render template="nodeTooltipView" model="[runnable: runnable, node:node,key:ukey+'_'+node.nodename+'_key',includeDescription:true]"/>
            </g:else>
        </g:each>
