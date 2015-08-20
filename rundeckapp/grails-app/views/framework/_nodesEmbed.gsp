<g:if test="${params.declarenone && nodes.size()<1}">
    <span class="warn note">None</span>
</g:if>
<g:set var="max" value="${-1}"/>
<g:if test="${params.maxShown}">
    <g:set var="max" value="${params.int('maxShown')}"/>
</g:if>
<g:if test="${max>0 && nodes.size()>max}">
<a href="#embednodeset" class="textbtn textbtn-default " data-toggle="collapse">Show all <g:enc>${nodes.size()}</g:enc> Nodes
    <i class="auto-caret"></i>
</a>
</g:if>
<span id="embednodeset" class=" ${max > 0 && nodes.size() > max? 'collapse collapse-expandable':''} ansicolor-on matchednodes embed embed_clean">
        <% def i =0 %>
        <g:each in="${nodes.keySet().sort{a,b->a.compareTo(b)}}" var="nodename">

            <g:set var="nkey" value="${g.rkey()}"/>
            <g:set var="nodedata" value="${nodes[nodename]}"/>
            <g:set var="node" value="${nodedata.node}"/>
            <g:set var="executions" value="${nodedata.executions}"/>
            <g:set var="resources" value="${nodedata.resources}"/>
            <g:set var="resName" value="${node.nodename}"/>
            <g:set var="resHost" value="${node.hostname}"/>
            <g:set var="runnable" value="${null == nodeauthrun || nodeauthrun[node.nodename]}"/>
            <a id="${enc(attr:nkey)}_key"
                tabindex="0"
                role="button"
                  class="${i%2==1?'alternateRow':''} node_entry ${nodedata.islocal?'server':''} node_ident textbtn-default textbtn-plain ${nodeStatusColorCss(node:node)}"
                  data-toggle="popover"
                  data-placement="bottom"
                  data-trigger="focus"
                  data-popover-content-ref="#${nkey+'_key_tooltip'}"
                  data-popover-template-class="popover-wide"
                  style="${nodeStatusColorStyle(node:node)}"
                  data-node="${enc(attr:node.nodename)}"
                  data-key="${enc(attr:nkey)}">

                <g:nodeStatusColor node="${node}" icon="true"><g:nodeStatusIcon
                        node="${node}"
                ><i class="rdicon node ${runnable?'node-runnable':''} icon-small"></i></g:nodeStatusIcon></g:nodeStatusColor>&nbsp;${node.nodename}
            </a>

            <g:render template="nodeTooltipView" model="[node:node,key: nkey+'_key',islocal:nodedata.islocal,runnable:runnable, nodefilterLinkId: nodefilterLinkId?:'']"/>
            <% i++ %>
        </g:each>
        <g:javascript>
            fireWhenReady('embednodeset',function(){
                _initPopoverContentRef('#embednodeset');
            });

        </g:javascript>
</span>
