
<g:if test="${ jobgroups}">
    <g:render template="groupTree" model="${[small:params.compact?true:false,jobgroups:jobgroups,currentJobs:jobgroups['']?jobgroups['']:[],wasfiltered:wasfiltered?true:false,jobauthorizations:jobauthorizations,nowrunning:nowrunning,nextExecutions:nextExecutions,authMap:authMap,nowrunningtotal:nowrunningtotal,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true,jobsjscallback:jobsjscallback,runAuthRequired:runAuthRequired]}"/>
</g:if>