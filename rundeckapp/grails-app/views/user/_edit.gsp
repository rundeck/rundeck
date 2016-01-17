<div class="form-group ${user.errors?.hasFieldErrors('login')?'has-error':''}">
    <label for="login" class="col-sm-2 control-label">
        <g:message code="domain.User.login.label"/>
    </label>

    <div class="col-sm-10">
        <g:if test="${newuser}">
            <div class="input-group">
                <span class="input-group-addon"><g:icon name="user"/></span>
                <g:textField name="login" value="${user.login}" class=" form-control"/>
            </div>
            <g:if test="${user.errors?.hasFieldErrors('login')}">
                <div class="help-block">
                    <g:fieldError field="login" bean="${user}"/>
                </div>
            </g:if>
            <g:hiddenField name="newuser" value="true"/>
        </g:if>
        <g:elseif test="${user.login}">
            <p class=" form-control-static">
                <g:icon name="user"/>
                <g:enc>${user.login}</g:enc>
            </p>
            <g:hiddenField name="login" value="${user.login}"/>
            <g:hiddenField name="newuser" value="false"/>
        </g:elseif>
    </div>
</div>

<div class="form-group ${user.errors?.hasFieldErrors('email')?'has-error':''}">
    <label for="email" class="col-sm-2 control-label">
        <g:message code="domain.User.email.label"/>
    </label>

    <div class="col-sm-10">
        <g:textField name="email" value="${user.email}" class=" form-control"/>
        <g:if test="${user.errors?.hasFieldErrors('email')}">
            <div class="help-block">
                <g:fieldError field="email" bean="${user}"/>
            </div>
        </g:if>
    </div>
</div>


<div class="form-group ${user.errors?.hasFieldErrors('firstName')?'has-error':''}">
    <label for="email" class="col-sm-2 control-label">
        <g:message code="domain.User.firstName.label"/>
    </label>

    <div class="col-sm-10">
        <g:textField name="firstName" value="${user.firstName}" class=" form-control"/>
        <g:if test="${user.errors?.hasFieldErrors('firstName')}">
            <div class="help-block">
                <g:fieldError field="firstName" bean="${user}"/>
            </div>
        </g:if>
    </div>
</div>

<div class="form-group ${user.errors?.hasFieldErrors('lastName')?'has-error':''}">
    <label for="email" class="col-sm-2 control-label">
        <g:message code="domain.User.lastName.label"/>
    </label>

    <div class="col-sm-10">
        <g:textField name="lastName" value="${user.lastName}" class=" form-control"/>
        <g:if test="${user.errors?.hasFieldErrors('lastName')}">
            <div class="help-block">
                <g:fieldError field="lastName" bean="${user}"/>
            </div>
        </g:if>
    </div>
</div>

