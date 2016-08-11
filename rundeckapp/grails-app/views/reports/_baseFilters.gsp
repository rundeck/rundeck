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
  Date: Aug 8, 2008
  Time: 5:23:27 PM
  To change this template use File | Settings | File Templates.
--%>
    <tr>
        <td><label for="jobFilter"><g:message code="jobquery.title.jobFilter"/></label>:</td>
        <td><g:textField name="jobFilter" value="${query.jobFilter}"/></td>
    </tr>
<g:if test="${query.jobIdFilter}">
    <tr>
    <td><label for="jobIdFilter"><g:message code="jobquery.title.jobIdFilter"/></label>:</td>
    <td><g:textField name="jobIdFilter" value="${query.jobIdFilter}"/></td>
    </tr>
</g:if>

    <tr>
<td><label for="userFilter"><g:message code="jobquery.title.userFilter"/></label>:</td>
<td><g:textField name="userFilter" value="${query.userFilter}"/></td>
    </tr>

<tr>
<td><label for="titleFilter"><g:message code="jobquery.title.titleFilter"/></label>:</td>
<td><g:textField name="titleFilter" value="${query.titleFilter}"/></td>
    </tr>
<tr>
    <td> <g:message code="jobquery.title.statFilter"/>:</td>
    <td>
        %{--<g:radio name="statFilter" value="succeed" checked="${query.statFilter=='succeed'}" id="statFilterTrue"/> <label for="statFilterTrue">Succeeded</label>--}%
        %{--<g:radio name="statFilter" value="fail" checked="${query.statFilter=='fail'}" id="statFilterFalse"/> <label for="statFilterFalse">Failed</label>--}%
        %{--<g:radio name="statFilter" value="" checked="${!query.statFilter}"  id="statFilter"/> <label for="statFilter">Either</label>--}%
        <g:select name="statFilter" from="${['succeed','fail','cancel']}" value="${query.statFilter}" noSelection="['':'Any']" valueMessagePrefix="status.label"/>
    </td>
</tr>

