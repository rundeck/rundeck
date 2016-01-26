<g:if test="${params.declarenone}">
    <span data-bind="if: !total() || total()<1">
        <span class="warn note">None</span>
    </span>
</g:if>
<g:set var="max" value="${-1}"/>
<g:if test="${params.maxShown}">
    <g:set var="max" value="${params.int('maxShown')}"/>
</g:if>
<span data-bind="if: total()>100">
    <a href="#embednodeset" class="textbtn textbtn-default " data-toggle="collapse">Show all <span data-bind="text: total()"></span> Nodes
        <i class="auto-caret"></i>
    </a>
</span>
<span id="embednodeset" class=" ansicolor-on matchednodes embed embed_clean"
      data-bind="if: total()<=100"
>
    %{--<% def i =0 %>--}%
    <span data-bind="foreach: nodeSet().nodes">

        %{--<g:set var="nkey" value="${g.rkey()}"/>--}%
        %{--<g:set var="nodedata" value="${nodes[nodename]}"/>--}%
        %{--<g:set var="node" value="${nodedata.node}"/>--}%
        %{--<g:set var="resName" value="${node.nodename}"/>--}%
        %{--<g:set var="resHost" value="${node.hostname}"/>--}%
        %{--<g:set var="runnable" value="${null == nodeauthrun || nodeauthrun[node.nodename]}"/>--}%
        %{--class="${nodeStatusColorCss(node:node)}"--}%
        <a
           tabindex="0"
           role="button"
           class="node_entry  node_ident textbtn-default textbtn-plain "
           data-bind="css: {islocal: 'server'}, attr: { 'data-node': nodename }"
           data-toggle="popover"
           data-placement="bottom"
           data-trigger="focus"
           %{--data-popover-content-ref="#${nkey+'_key_tooltip'}"--}%
           data-popover-template-class="popover-wide"
           %{--style="${nodeStatusColorStyle(node:node)}"--}%
           data-node=""
           %{--data-key="${enc(attr:nkey)}"--}%
>

            %{--<g:nodeStatusColor node="${node}" icon="true"><g:nodeStatusIcon--}%
                    %{--node="${node}"--}%
            %{--><i class="rdicon node ${runnable?'node-runnable':''} icon-small"></i></g:nodeStatusIcon></g:nodeStatusColor>--}%

            <i class="rdicon node icon-small" data-bind="css: {authrun: 'node-runnable'}"></i>
            <span data-bind="text: nodename"></span>
        </a>

        %{--<g:render template="nodeTooltipView" model="[node:node,key: nkey+'_key',islocal:nodedata.islocal,runnable:runnable, nodefilterLinkId: nodefilterLinkId?:'']"/>--}%
        %{--<% i++ %>--}%
    </span>
    %{--<g:javascript>--}%
        %{--fireWhenReady('embednodeset',function(){--}%
            %{--_initPopoverContentRef('#embednodeset');--}%
        %{--});--}%

    %{--</g:javascript>--}%
</span>
