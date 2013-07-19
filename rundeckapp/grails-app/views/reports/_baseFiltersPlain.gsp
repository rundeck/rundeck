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
<span class="formItem">
    <label for="jobFilter"><g:message code="jobquery.title.jobFilter"/></label>:
    <span><g:textField name="jobFilter" value="${query.jobFilter}" autofocus="true"/></span>
</span>
<g:if test="${query.jobIdFilter}">
    <span class="formItem">
        <label for="jobIdFilter"><g:message code="jobquery.title.jobIdFilter"/></label>:
        <span><g:textField name="jobIdFilter" value="${query.jobIdFilter}"/></span>
    </span>
</g:if>

<span class="formItem">
    <label for="userFilter"><g:message code="jobquery.title.userFilter"/></label>:
    <span><g:textField name="userFilter" value="${query.userFilter}"/></span>
</span>
<span class="formItem">
    <label for="titleFilter"><g:message code="jobquery.title.titleFilter"/></label>:
    <span><g:textField name="titleFilter" value="${query.titleFilter}"/></span>
</span>
<span class="formItem">
    <span class="label"><g:message code="jobquery.title.statFilter"/>:</span>
    <span>
        %{--<g:radio name="statFilter" value="succeed" checked="${query.statFilter=='succeed'}" id="statFilterTrue"/> <label for="statFilterTrue">Succeeded</label>--}%
        %{--<g:radio name="statFilter" value="fail" checked="${query.statFilter=='fail'}" id="statFilterFalse"/> <label for="statFilterFalse">Failed</label>--}%
        %{--<g:radio name="statFilter" value="" checked="${!query.statFilter}"  id="statFilter"/> <label for="statFilter">Either</label>--}%
        <g:select name="statFilter" from="${['succeed', 'fail', 'cancel']}" value="${query.statFilter}"
                  noSelection="['': 'Any']" valueMessagePrefix="status.label"/>
    </span>
</span>
