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
  Time: 5:24:57 PM
  To change this template use File | Settings | File Templates.
--%>
<span class="form-group">
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
            class="form-control input-sm"
        />

    </span>
</span>
