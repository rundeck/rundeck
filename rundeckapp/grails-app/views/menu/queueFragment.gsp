    <g:set var="wasfiltered" value="${paginateParams}"/>
    <g:form action="nowrunning">
        <table cellspacing="0" cellpadding="0" class="queryTable" width="100%">
            <tr>
                <td style="text-align:left;vertical-align:top; display:none;" id="qfilter">
                    
                </td>
                <td style="text-align:left;vertical-align:top;" id="qcontent">

                    <g:render template="executions" model="[executions:nowrunning,jobs:jobs,nowrunning:true,idprefix:'nowrun',small:true]"/>

                    <g:if test="${total && max && total.toInteger() > max.toInteger()}">
                        <span class="info note">Showing ${nowrunning.size()} of ${total}</span>
                    </g:if>
                </td>
            </tr>
        </table>
    </g:form>
<span class="paginate"><g:paginate action="nowrunning" total="${total}" max="${max}"/></span>

<g:render template="/common/boxinfo" model="${[name:params.fragmentName?params.fragmentName:'queue',model:[title:'Dispatcher Queue',total:total,linkUrl:createLink(controller:'menu',action:'nowrunning')]]}"/>
