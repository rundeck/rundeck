<div id="${key.encodeAsHTML()}_tooltip" style="display:none;" class="detailpopup nodedetail ${islocal?'server':''} tooltipcontent" >
    <span >
        <img src="${resource(dir:'images',file:'icon-small-Node.png')}" alt="Node" width="16px" height="16px"/>
        ${node.nodename.encodeAsHTML()}
    </span>
    <span class="desc">
        ${node.description?.encodeAsHTML()}
    </span>
    
    <g:render template="/framework/nodeDetailsSimple" bean="${node}" var="node"/>
</div>