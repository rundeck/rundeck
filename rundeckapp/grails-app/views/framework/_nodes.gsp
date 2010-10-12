<g:set var="ukey" value="${g.rkey()}"/>
<table cellpadding="0" cellspacing="0" width="100%">

        <% def seen=false %>
        <g:each in="${nodes.keySet().sort()}" var="nodekey" status="i">
            <g:set var="nodedata" value="${nodes[nodekey]}"/>
            <g:set var="node" value="${nodedata.node}"/>
            <g:set var="executions" value="${nodedata.executions}"/>
            <g:set var="resources" value="${nodedata.resources}"/>
            <g:set var="resName" value="${node.nodename}"/>
            <g:set var="resHost" value="${node.hostname}"/>

            <tr class="${i%2==1?'alternateRow':''} node_entry ">
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
                            <g:each var="tag" in="${node.tags}">
                                <span class="tag">${tag}</span>
                            </g:each>
                        </span>
                    </g:if>
                    <span class="desc">
                        ${node.description}
                    </span>

                    <g:if test="${node.attributes?.editUrl}">
                        <a href="${node.attributes?.editUrl}" target="_new">Edit...</a>
                    </g:if>

                    <g:if test="${expanddetail}">
                        <g:link  controller="reports" action="index" params="${[nodeFilter:node.nodename]}" title="View Events for Node ${node.nodename}">
                            &raquo; events
                        </g:link>
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