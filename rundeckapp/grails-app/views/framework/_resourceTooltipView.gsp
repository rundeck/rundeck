<div id="${key}_tooltip" style="display:none;" class="detailpopup nodedetail" >
    <span >
        <g:set var="resBase" value="${ resource.properties.'entity.order' ? resource.properties.'entity.order' : 'Resource'}"/>
        <g:img file="${'icon-small-'+resBase+'Object.png'}" width="16px" height="16px"/>
        ${resource.name}
    </span>
    <span  class="objdesc">
        ${resource.description}
    </span>
    <g:render template="resourceDetailsSimple" bean="${resource}" var="resource"/>
</div>