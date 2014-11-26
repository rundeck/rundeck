<div id="${enc(attr:key)}_tooltip" style="display:none;" class="detailpopup node_entry ${islocal?'server':''} tooltipcontent node_filter_link_holder"
     data-node-filter-link-id="${enc(attr:nodefilterLinkId?:'')}" >
    <span >
        <i class="rdicon node ${runnable ? 'node-runnable' : ''} icon-small"></i>
        <g:enc>${node.nodename}</g:enc>
    </span>
    <tmpl:nodeFilterLink key="name" value="${node.nodename}"
                         linkicon="glyphicon glyphicon-circle-arrow-right"/>
    <span class="nodedesc"></span>
    <div class="nodedetail">
    <g:render template="/framework/nodeDetailsSimple" bean="${node}" var="node"/>
    </div>
</div>
