<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: Jan 15, 2010
  Time: 4:54:53 PM
  To change this template use File | Settings | File Templates.
--%>
<g:set var="views" value="${[table:'nodesTable',embed:'nodesEmbed','':'nodes',tableContent:'nodesTableContent']}"/>
<g:if test="${!params.view || params.view!='tableContent'}">
    <g:render template="/common/messages"/>
    <div class="allnodes">
</g:if>
    <div class="presentation clear">
    <g:render template="${views[params.view]?views[params.view]:views['']}" model="${[nodes:allnodes,totalexecs:totalexecs,jobs:jobs,params:params,total:total,allcount:allcount,page:page,max:max,nodeauthrun:nodeauthrun,tagsummary:tagsummary]}"/>
    </div>
<g:if test="${!params.view || params.view!='tableContent'}">
    </div>
    <g:render template="/common/boxinfo" model="${[name:'nodes',model:[title:'Nodes',total:total,linkUrl:createLink(controller:'framework',action:'nodes')]]}"/>
</g:if>