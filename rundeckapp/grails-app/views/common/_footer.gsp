    <g:message code="main.app.name"/>
    &copy; Copyright 2014 <a href="http://simplifyops.com">
    <span style="color:red;">#Simplify</span>Ops</a>.

    All rights reserved.

    <g:link controller="menu" action="licenses">Licenses</g:link>
    <g:set var="buildIdent" value="${grailsApplication.metadata['build.ident']}"/>
    <g:set var="appId" value="${g.message(code: 'main.app.name')}"/>
    <span class="version"><g:enc>${buildIdent}</g:enc></span>
    <span class="rundeck-version-identity"  data-version-string="${enc(attr: buildIdent)}" data-app-id="${enc(attr: appId)}"></span>
