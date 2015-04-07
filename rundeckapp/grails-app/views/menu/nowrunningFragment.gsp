<g:set var="rkey" value="${g.rkey()}" />
<g:render template="runningExecutions" model="[executions:nowrunning,jobs:jobs,nowrunning:true,idprefix:rkey,emptyText:'']"/>
<g:if test="${total>max}">
    <tr>
    <td colspan="5">
    <span class="paginate nowrunning"><g:paginate class="pagination-sm pagination-embed" action="nowrunning" total="${total}"  max="${max}"/></span>
        <g:if test="${total && max && total.toInteger() > max.toInteger()}">
            <span class="text-muted">Showing <g:enc>${nowrunning.size()} of ${total}</g:enc></span>
        </g:if>
    </td>
    </tr>
</g:if>
<g:render template="/common/boxinfo" model="${[name:'nowrunning',model:[title:'Now Running',total:total,lastExecId:lastExecId,count:nowrunning.size()]]}"/>
