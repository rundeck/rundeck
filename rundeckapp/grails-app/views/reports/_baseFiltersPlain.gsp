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
<div class="form-group">
    <label for="jobIdFilter" class="sr-only"><g:message code="jobquery.title.jobFilter"/></label>
    <g:textField name="jobFilter" value="${query.jobFilter}" autofocus="true" class="form-control input-sm"
                 placeholder="${g.message(code:'jobquery.title.jobFilter')}"/>
</div>
<g:if test="${query.jobIdFilter}">
    <div class="form-group">
        <label for="jobIdFilter" class="sr-only"><g:message code="jobquery.title.jobIdFilter"/></label>
        <g:textField name="jobIdFilter" value="${query.jobIdFilter}" class="form-control input-sm"
                           placeholder="${g.message(code: 'jobquery.title.jobIdFilter')}"/>
    </div>
</g:if>

<div class="form-group">
    <label for="userFilter" class="sr-only"><g:message code="jobquery.title.userFilter"/></label>
    <g:textField name="userFilter" value="${query.userFilter}" class="form-control input-sm"
                 placeholder="${g.message(code: 'jobquery.title.userFilter')}"/>
</div>
<div class="form-group">
    <label for="titleFilter" class="sr-only"><g:message code="jobquery.title.titleFilter"/></label>
    <g:textField name="titleFilter" value="${query.titleFilter}" class="form-control input-sm"
                 placeholder="${g.message(code: 'jobquery.title.titleFilter')}"/>
</div>
<div class="form-group">
    <label for="statFilter" class="sr-only"><g:message code="jobquery.title.statFilter"/></label>
    <g:select name="statFilter" from="${['succeed', 'fail', 'cancel']}" value="${query.statFilter}"
              noSelection="['': 'Any']" valueMessagePrefix="status.label" class="form-control input-sm"/>
</div>
