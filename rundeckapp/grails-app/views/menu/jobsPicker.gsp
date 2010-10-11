
<g:if test="${ groupTree}">
    <g:render template="groupTree" model="${[small:params.compact?true:false,groupTree:groupTree.subs,currentJobs:groupTree['jobs']?groupTree['jobs']:[],wasfiltered:wasfiltered?true:false,nowrunning:nowrunning,nextExecutions:nextExecutions,authMap:authMap,nowrunningtotal:nowrunningtotal,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true,jobsjscallback:jobsjscallback]}"/>
</g:if>