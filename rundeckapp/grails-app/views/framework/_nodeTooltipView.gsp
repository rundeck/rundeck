<div id="${key.encodeAsHTML()}_tooltip" style="display:none;" class="detailpopup nodedetail ${islocal?'server':''} tooltipcontent" >
    <span >
        <i class="rdicon node icon-small"></i>
        ${node.nodename.encodeAsHTML()}
    </span>
    <span class="desc">
        ${node.description?.encodeAsHTML()}
    </span>
    
    <g:render template="/framework/nodeDetailsSimple" bean="${node}" var="node"/>
</div>
