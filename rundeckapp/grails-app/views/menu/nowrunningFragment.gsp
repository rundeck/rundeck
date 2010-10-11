<g:set var="rkey" value="${g.rkey()}" />
<g:render template="executions" model="[executions:nowrunning,jobs:jobs,nowrunning:true,idprefix:rkey]"/>
    <g:if test="${total && max && total.toInteger() > max.toInteger()}">
        <span class="info note">Showing ${nowrunning.size()} of ${total}</span>
    </g:if>
<span class="paginate"><g:paginate action="nowrunning" total="${total}"  max="${max}"/></span>

<g:render template="/common/boxinfo" model="${[name:'nowrunning',model:[title:'Now Running',total:total]]}"/>