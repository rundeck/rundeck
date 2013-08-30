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

            if (params.recentFilter && !['1d', '1w', '1m', '-'].contains(params.recentFilter)) {
                recentfilts << [value: params.recentFilter, label: g.recentDescription(value: params.recentFilter)]
            }

            recentfilts << [value: '-', label: 'Other...']

        %>
        <g:select name="recentFilter" from="${recentfilts}" value="${params.recentFilter}"
                  noSelection="['': 'Any Time']" optionKey="value" optionValue="label"
                  onchange="if(this.value=='-'){Element.show('extDateFilters')}else{Element.hide('extDateFilters')};"
            class="form-control input-sm"
        />

    </span>
</span>
