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

%{--<span class="formItem">--}%
    %{--<label for="textFilter"><g:message code="jobquery.title.textFilter"/></label>:--}%
    %{--<span><g:textField name="textFilter" value="${query.textFilter}"/></span>--}%
%{--</span>--}%
<div class="row">
  <div class="col-xs-12 col-sm-4">
    <div class="form-group">
        <label for="jobIdFilter" class="sr-only"><g:message code="jobquery.title.jobFilter"/></label>
        <g:textField name="jobFilter" value="${query.jobFilter}" autofocus="true" class="form-control"
                     placeholder="${g.message(code:'jobquery.title.jobFilter')}"/>
    </div>

    <g:if test="${query.jobIdFilter}">
        <div class="form-group">
            <label for="jobIdFilter" class="sr-only"><g:message code="jobquery.title.jobIdFilter"/></label>
            <g:textField name="jobIdFilter" value="${query.jobIdFilter}" class="form-control"
                               placeholder="${g.message(code: 'jobquery.title.jobIdFilter')}"/>
        </div>
    </g:if>
  </div>
  <div class="col-xs-12 col-sm-4">
    <div class="form-group">
        <label for="userFilter" class="sr-only"><g:message code="jobquery.title.userFilter"/></label>
        <g:textField name="userFilter" value="${query.userFilter}" class="form-control" placeholder="${g.message(code: 'jobquery.title.userFilter')}"/>
    </div>
  </div>
  <div class="col-xs-12 col-sm-4">
    <div class="form-group">
        <label for="execnodeFilter" class="sr-only"><g:message code="jobquery.title.filter"/></label>
        <g:textField name="execnodeFilter" value="${query.execnodeFilter}" class="form-control" placeholder="${g.message(code: 'jobquery.title.filter')}"/>
    </div>

  </div>
</div>
<div class="row">
  <div class="col-xs-12 col-sm-4">
    <div class="form-group">
        <label for="titleFilter" class="sr-only"><g:message code="jobquery.title.titleFilter"/></label>
        <g:textField name="titleFilter" value="${query.titleFilter}" class="form-control" placeholder="${g.message(code: 'jobquery.title.titleFilter')}"/>
    </div>
  </div>
      <div class="col-xs-12 col-sm-4">
        <div class="form-group">
            <label for="statFilter" class="sr-only"><g:message code="jobquery.title.statFilter"/></label>
            <g:select name="statFilter" from="${['succeed', 'fail', 'cancel']}" value="${query.statFilter}"
                      noSelection="['': 'Any']" valueMessagePrefix="status.label" class="form-control"/>
        </div>
      </div>
      <div class="col-xs-12 col-sm-4">
        <div class="form-group">
          <label for="recentFilter" class="sr-only"><g:message code="jobquery.title.recentFilter"/></label>
          <span class="radiolist">
              <%
                  def recentfilts = [[value: '1d', label: g.recentDescription(value: '1d')], [value: '1w', label: g.recentDescription(value: '1w')], [value: '1m', label: g.recentDescription(value: '1m')]]

                  if (query.recentFilter && !['1d', '1w', '1m', '-'].contains(query.recentFilter)) {
                      recentfilts << [value: query.recentFilter.encodeAsHTML(), label: g.recentDescription(value: query.recentFilter)]
                  }

                  recentfilts << [value: '-', label: 'Other...']

              %>
              <g:select name="recentFilter" from="${recentfilts}" value="${query.recentFilter?.encodeAsHTML()}"
                        noSelection="['': 'Any Time']" optionKey="value" optionValue="label"
                        onchange="if(this.value=='-'){Element.show('extDateFilters')}else{Element.hide('extDateFilters')};"
                  class="form-control"/>
          </span>
        </div>
      </div>
</div>
