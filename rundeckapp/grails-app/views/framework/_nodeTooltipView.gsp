<div id="${key}_tooltip" style="display:none;" class="detailpopup nodedetail" >
    <span >
        <img src="${resource(dir:'images',file:'icon-small-Node.png')}" alt="Node" width="16px" height="16px"/>
        ${node.nodename}
    </span>
    <span class="desc">
        ${node.description}
    </span>
    
    <g:render template="nodeDetailsSimple" bean="${node}" var="node"/>
</div>