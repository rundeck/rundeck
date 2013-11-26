<g:if test="${params.declarenone && nodes.size()<1}">
    <span class="warn note">None</span>
</g:if>
        <% def i =0 %>
        <g:each in="${nodes.keySet().sort{a,b->a.compareTo(b)}}" var="nodename">

            <g:set var="nkey" value="${g.rkey()}"/>
            <g:set var="nodedata" value="${nodes[nodename]}"/>
            <g:set var="node" value="${nodedata.node}"/>
            <g:set var="executions" value="${nodedata.executions}"/>
            <g:set var="resources" value="${nodedata.resources}"/>
            <g:set var="resName" value="${node.nodename}"/>
            <g:set var="resHost" value="${node.hostname}"/>
            <span class="${i%2==1?'alternateRow':''} node_entry ${nodedata.islocal?'server':''} node_ident obs_clicktip action textbtn"
                  id="${nkey}_key" data-node="${node.nodename.encodeAsHTML()}" data-key="${nkey}">
                <i class="rdicon node icon-small"></i>
                ${node.nodename.encodeAsHTML()}
            </span>
            <g:render template="nodeTooltipView" model="[node:node,key: nkey+'_key',islocal:nodedata.islocal]"/>
            <% i++ %>
        </g:each>
        <g:javascript>
                        if(typeof(initClicktipForElements)=='function'){
                            initClicktipForElements('.obs_clicktip');
                        }

        </g:javascript>
