<span class="entry" id="objectSelect${i}">
    <g:set var="resName" value="${resource.resourceName}"/>
    <g:set var="resType" value="${resource.resourceType}"/>
    <g:set var="resBase" value="Resource"/>
    <g:set var="resDesc" value=""/>
    <span class="objIdent">
        <img src="${resource(dir:'images',file:'icon-small-'+resBase+'Object.png')}"
             alt="${resBase}"  width="16px" height="16px"/>&nbsp;${resName} [${resType}]
    </span>
    <span  class="objdesc">
        ${resDesc}
    </span>
    <span>
        <g:link controller="reports" action="index" params="${[objFilter:resName,typeFilter:resType]}" title="Events for ${resName}[${resType}]">
            <img src="${resource(dir:'images',file:'icon-tiny-Reportcenter.png')}" width="12px" height="12px"/>
        </g:link>
    </span>
</span>