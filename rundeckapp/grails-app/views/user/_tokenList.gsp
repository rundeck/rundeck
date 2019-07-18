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


<g:set var="ukey" value="${g.rkey()}"/>
<table width="100%" class="table table-condensed table-striped apitokentable">
    <thead>
    <tr>
        <th class="table-header">
            <g:message code="token"/>
        </th>
        <th class="table-header" colspan="2">
            <g:message code="expiration"/>
        </th>
        <th class="table-header">
            <g:message code="token.username"/>
        </th>
        <th class="table-header">
            <g:message code="roles"/>
        </th>
        <th class="table-header">
        </th>
    </tr>
    </thead>
    <tbody id="apiTokenTableBody">
    <g:each in="${tokenList}" var="token">
        <g:render template="token" model="${[user: user, token: token, flashToken: flashToken]}"/>
    </g:each>
    </tbody>
</table>
