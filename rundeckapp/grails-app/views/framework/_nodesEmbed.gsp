<g:if test="${params.declarenone && nodes.size()<1}">
    <span class="warn note">None</span>
</g:if>
        <% def i =0 %>
        <g:each in="${nodes.keySet().sort{a,b->a.compareTo(b)}}" var="nodename">

            <g:set var="nodedata" value="${nodes[nodename]}"/>
            <g:set var="node" value="${nodedata.node}"/>
            <g:set var="executions" value="${nodedata.executions}"/>
            <g:set var="resources" value="${nodedata.resources}"/>
            <g:set var="resName" value="${node.nodename}"/>
            <g:set var="resHost" value="${node.hostname}"/>
            <div class="${i%2==1?'alternateRow':''} node_entry " style="white-space:nowrap">
                <span class="node_ident" id="${node.nodename}_key">
                    <img src="${resource(dir:'images',file:'icon-small-Node.png')}" alt="Node" width="16px" height="16px"/>
                    ${node.nodename}
                </span>
                <g:render template="nodeTooltipView" model="[node:node,key:node.nodename+'_key']"/>
            </div>
            <% i++ %>
        </g:each>
        <g:javascript>
            if(typeof(initTooltipForElements)=='function'){
                initTooltipForElements('div.node_entry span.node_ident');
            }
        </g:javascript>