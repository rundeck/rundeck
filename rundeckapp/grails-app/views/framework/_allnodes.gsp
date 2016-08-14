%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%

<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: Jan 15, 2010
  Time: 4:54:53 PM
  To change this template use File | Settings | File Templates.
--%>
<g:set var="views" value="${[table:'nodesTable',embed:'nodesEmbed','':'nodesEmbed',tableContent:'nodesTableContent']}"/>
<g:set var="nodeview" value="${nodeview?:params.view?:''}"/>
<g:if test="${!nodeview || nodeview!='tableContent'}">
    <g:render template="/common/messages"/>
</g:if>
    <g:render template="${views[nodeview]?:views['']}" model="${[nodes:allnodes,totalexecs:totalexecs,jobs:jobs,params:params,total:total,allcount:allcount,page:page,max:max,nodeauthrun:nodeauthrun,tagsummary:tagsummary]}"/>
<g:if test="${!nodeview || nodeview!='tableContent'}">
    <g:render template="/common/boxinfo" model="${[name:'nodes',model:[title:'Nodes',total:total,allcount:allcount?:total,filter:query?.asFilter(),linkUrl:createLink(controller:'framework',action:'nodes',params:[project:params.project])]]}"/>
</g:if>
