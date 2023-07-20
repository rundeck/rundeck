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
<%@ page import="org.rundeck.app.data.providers.v1.execution.ReferencedExecutionDataProvider; rundeck.Execution" %>
<%@ page import="rundeck.ReferencedExecution; rundeck.Execution" %>
<g:set var="referencedExecutionDataProvider" bean="${org.rundeck.app.data.providers.v1.execution.ReferencedExecutionDataProvider}"/>
<g:set var="lastrun"
       value="${scheduledExecution.id ? Execution.findByJobUuidAndDateCompletedIsNotNull(scheduledExecution.uuid, [max: 1, sort: 'dateStarted', order: 'desc']) : null}"/>
<g:set var="reflastrun"
       value="${scheduledExecution.id ? referencedExecutionDataProvider.findByJobUuid(scheduledExecution.uuid) : null}"/>
<g:set var="successcount"
       value="${scheduledExecution.id ? Execution.countByJobUuidAndStatus(scheduledExecution.uuid, 'succeeded') : 0}"/>
<g:set var="refsuccesscount"
       value="${scheduledExecution.id ? referencedExecutionDataProvider.countByJobUuidAndStatus(scheduledExecution.uuid, 'succeeded') : 0}"/>
<g:set var="execCount"
       value="${scheduledExecution.id ? Execution.countByJobUuidAndDateCompletedIsNotNull(scheduledExecution.uuid) : 0}"/>
<g:set var="refexecCount"
       value="${scheduledExecution.id ? referencedExecutionDataProvider.countByJobUuid(scheduledExecution.uuid) : 0}"/>
<g:set var="successrate" value="${(execCount + refexecCount) > 0 ? ((successcount+refsuccesscount) / (execCount+refexecCount)) : 0}"/>
<g:render template="/scheduledExecution/showStats"
          model="[scheduledExecution: scheduledExecution, lastrun: lastrun ? lastrun : null, successrate: successrate,reflastrun: reflastrun ? reflastrun : null]"/>
