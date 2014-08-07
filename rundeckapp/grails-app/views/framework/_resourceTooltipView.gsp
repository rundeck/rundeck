<div id="${enc(attr:key)}_tooltip" style="display:none;" class="detailpopup nodedetail" >
    <span >
        <g:set var="resBase" value="${ resource.properties.'entity.order' ? enc(attr:resource.properties.'entity.order') : 'Resource'}"/>
        <g:img file="${'icon-small-'+ enc(attr:resBase)+'Object.png'}" width="16px" height="16px"/>
        <g.enc>${resource.name}</g.enc>
    </span>
    <span  class="objdesc">
        <g.enc>${resource.description}</g.enc>
    </span>
    <g:render template="resourceDetailsSimple" bean="${resource}" var="resource"/>
</div>
