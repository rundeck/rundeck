<g:set var="wasfiltered" value="${paginateParams?.keySet().grep(~/.*Filter|groupPath$/)}"/>
<g:render template="workflowsFull" model="${[small:true,groupTree:groupTree,wasfiltered:wasfiltered?true:false,nextExecutions:nextExecutions,authMap:authMap,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true]}"/>
<g:set var="sectionTitle" value="${g.message(code:'domain.ScheduledExecution.title')+'s'}"/>
<g:render template="/common/boxinfo" model="${[name:'workflows',model:[title:sectionTitle,total:total,linkUrl:createLink(controller:'menu',action:'jobs',params:[project:params.project])]]}"/>
