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

<%@ page import="rundeck.Execution" %>
<g:set var="lastrun"
       value="${scheduledExecution.id ? Execution.findByScheduledExecutionAndDateCompletedIsNotNull(scheduledExecution, [max: 1, sort: 'dateStarted', order: 'desc']) : null}"/>
<g:set var="successcount"
       value="${scheduledExecution.id ? Execution.countByScheduledExecutionAndStatusInList(scheduledExecution, ['true','succeeded','scheduled']) : 0}"/>
<g:set var="execCount"
       value="${scheduledExecution.id ? Execution.countByScheduledExecution(scheduledExecution) : 0}"/>
<g:set var="successrate" value="${execCount > 0 ? (successcount / execCount) : 0}"/>
<g:render template="/scheduledExecution/showStats"
          model="[scheduledExecution: scheduledExecution, lastrun: lastrun ? lastrun : null, successrate: successrate]"/>
