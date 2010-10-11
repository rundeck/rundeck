<table cellpadding="0" cellspacing="0" width="100%">

        <% def seen=false %>
        <% def i =0 %>
        <g:each in="${nodes}" var="node">
            <g:set var="resName" value="${node.nodename}"/>
            <g:set var="resHost" value="${node.hostname}"/>

            <tr class="${i%2==1?'alternateRow':''} node_entry ">
                <td class="objIdent" colspan="3">
                <span class="node_ident" id="${node.nodename}_key">
                    <img src="${resource(dir:'images',file:'icon-small-Node.png')}" alt="Node" width="16px" height="16px"/>
                    ${resName}
                </span>
                    <g:if test="${totalexecs[node.nodename]}">
                        (${totalexecs[node.nodename]})
                    </g:if>
                    
                    <span  class="objdesc">
                        ${resHost}
                    </span>
                    <g:render template="nodeTooltipView" model="[node:node,key:node.nodename+'_key']"/>
                </td>
                
            </tr>
            <% i++ %>
        </g:each>
</table>

<g:javascript>
    if(typeof(initTooltipForElements)=='function'){
        initTooltipForElements('tr.node_entry span.node_ident');
    }
</g:javascript>