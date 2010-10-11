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
<td><label for="projFilter"><g:message code="jobquery.title.projFilter"/></label>:</td>
<td><g:textField name="projFilter" value="${query.projFilter}"/></td>
</tr>
    <tr>
<td><label for="objFilter"><g:message code="jobquery.title.objFilter"/></label>:</td>
<td><g:textField name="objFilter" value="${query.objFilter}"/></td>
    </tr>


    <tr>
<td><label for="userFilter"><g:message code="jobquery.title.userFilter"/></label>:</td>
<td><g:textField name="userFilter" value="${query.userFilter}"/></td>
    </tr>
    <tr>
<td><label for="nodeFilter"><g:message code="jobquery.title.nodeFilter"/></label>:</td>
<td><g:textField name="nodeFilter" value="${query.nodeFilter}"/></td>
    </tr>
    <tr>
<td><label for="tagsFilter"><g:message code="jobquery.title.tagsFilter"/></label>:</td>
<td><g:textField name="tagsFilter" value="${query.tagsFilter}"/></td>
    </tr>
<tr>
    <tr>
<td><label for="reportIdFilter"><g:message code="jobquery.title.reportIdFilter"/></label>:</td>
<td><g:textField name="reportIdFilter" value="${query.reportIdFilter}"/></td>
    </tr>
<tr>
<td><label for="messageFilter"><g:message code="jobquery.title.messageFilter"/></label>:</td>
<td><g:textField name="messageFilter" value="${query.messageFilter}"/></td>
    </tr>
<tr>
    <td> <g:message code="jobquery.title.statFilter"/>:</td>
    <td>
        %{--<g:radio name="statFilter" value="succeed" checked="${query.statFilter=='succeed'}" id="statFilterTrue"/> <label for="statFilterTrue">Succeeded</label>--}%
        %{--<g:radio name="statFilter" value="fail" checked="${query.statFilter=='fail'}" id="statFilterFalse"/> <label for="statFilterFalse">Failed</label>--}%
        %{--<g:radio name="statFilter" value="" checked="${!query.statFilter}"  id="statFilter"/> <label for="statFilter">Either</label>--}%
        <g:select name="statFilter" from="${['succeed','fail']}" value="${query.statFilter}" noSelection="['':'Any']" valueMessagePrefix="status.label"/>
    </td>
</tr>

