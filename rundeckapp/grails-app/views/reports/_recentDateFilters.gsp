<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: Aug 8, 2008
  Time: 5:24:57 PM
  To change this template use File | Settings | File Templates.
--%>
<tr>
    <td> <g:message code="jobquery.title.recentFilter"/>:</td>
    <td class="radiolist">
        <%
            def recentfilts=[[value:'1d',label:g.recentDescription(value:'1d')],[value:'1w',label:g.recentDescription(value:'1w')],[value:'1m',label:g.recentDescription(value:'1m')]]

            if(params.recentFilter && !['1d','1w','1m','-'].contains(params.recentFilter)){
                recentfilts<<[value:params.recentFilter,label:g.recentDescription(value:params.recentFilter)]
            }

            recentfilts<<[value:'-',label:'Other...']

        %>
        <g:select name="recentFilter" from="${recentfilts}" value="${params.recentFilter}" noSelection="['':'Any Time']" optionKey="value" optionValue="label"
            onchange="if(this.value=='-'){Element.show('extDateFilters')}else{Element.hide('extDateFilters')};"
        />
        %{--<div>--}%
        %{--<g:radio name="recentFilter" value="1d" checked="${params.recentFilter=='1d'}" id="recentFilter1D"/> <label for="recentFilter1D">Day</label>--}%
        %{--</div>--}%
        %{--<div>--}%
        %{--<g:radio name="recentFilter" value="1w" checked="${params.recentFilter=='1w'}" id="recentFilter1W"/> <label for="recentFilter1W">Week</label>--}%
        %{--</div>--}%
        %{--<div>--}%
        %{--<g:radio name="recentFilter" value="1m" checked="${params.recentFilter=='1m'}" id="recentFilter1M"/> <label for="recentFilter1M">Month</label>--}%
        %{--</div>--}%
        %{--<g:if test="${params.recentFilter && !['1d','1w','1m','-'].contains(params.recentFilter)}">--}%
            %{--<div>--}%
            %{--<g:radio name="recentFilter" value="${params.recentFilter}" checked="${true}"  id="recentFilterX"/> <label for="recentFilterX" class="additional"><g:recentDescription value="${params.recentFilter}"/></label>--}%
            %{--</div>--}%
        %{--</g:if>--}%

        %{--<div>--}%
        %{--<g:radio name="recentFilter" value="" checked="${!params.recentFilter}"  id="recentFilter"/> <label for="recentFilter">Any Time</label>--}%
        %{--</div>--}%
        %{--<div>--}%
        %{--<g:radio name="recentFilter" value="-" checked="${params.recentFilter=='-'}"  id="recentFilterOther" onchange="Element.show('extDateFilters');Expander.sync('extDateFiltersToggler','extDateFilters');"/> <label for="recentFilterOther">Other</label>--}%
        %{--</div>--}%
    </td>
</tr>