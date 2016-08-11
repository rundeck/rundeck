
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

<g:if test="${ jobgroups}">
    <g:render template="groupTree" model="${[small:params.compact?true:false,jobgroups:jobgroups,currentJobs:jobgroups['']?jobgroups['']:[],wasfiltered:wasfiltered?true:false,jobauthorizations:jobauthorizations,nowrunning:nowrunning,nextExecutions:nextExecutions,authMap:authMap,nowrunningtotal:nowrunningtotal,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true,jobsjscallback:jobsjscallback,runAuthRequired:runAuthRequired]}"/>
</g:if>