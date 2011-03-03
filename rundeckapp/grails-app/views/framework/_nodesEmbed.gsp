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
            <span class="${i%2==1?'alternateRow':''} node_entry ${nodedata.islocal?'server':''} node_ident obs_tooltip" id="${node.nodename}_key" >
                ${node.nodename.encodeAsHTML()}
            </span>
            <g:render template="nodeTooltipView" model="[node:node,key:node.nodename+'_key',islocal:nodedata.islocal]"/>
            <% i++ %>
        </g:each>
        <g:javascript>
            if(typeof(initTooltipForElements)=='function'){
                initTooltipForElements('.obs_tooltip');
            }
        </g:javascript>