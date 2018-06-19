%{--
  - Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
 <%--
    jobDetailFragment.gsp

    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Jan 7, 2011 2:21:36 PM
 --%>

<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<!--Display job details-->
<div >

    <g:set var="authViewDefinition" value="${auth.jobAllowedTest(
        job: scheduledExecution,
        any: true,
        action: [AuthConstants.ACTION_READ]
    )}"/>
    <g:if test="${authViewDefinition}">
        <g:render template="/execution/execDetails" model="[execdata:scheduledExecution,knockout:true]"/>
    </g:if>
    <g:render template="/scheduledExecution/renderJobStats" model="${[scheduledExecution: scheduledExecution]}"/>
</div>
