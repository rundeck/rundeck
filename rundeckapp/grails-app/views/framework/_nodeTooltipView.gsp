<div id="${key.encodeAsHTML()}_tooltip" style="display:none;" class="detailpopup node_entry ${islocal?'server':''} tooltipcontent" >
    <span >
        <i class="rdicon node icon-small"></i>
        ${node.nodename.encodeAsHTML()}
    </span>
    <span class="nodedesc"></span>
    <div class="nodedetail">
    <g:render template="/framework/nodeDetailsSimple" bean="${node}" var="node"/>
    </div>
</div>
