    <g:message code="main.app.name"/>
    <span class="num">${grailsApplication.metadata['app.version']}</span>
    &copy; Copyright 2013 <a href="http://simplifyops.com">
    <span style="color:red;">#Simplify</span>Ops</a>.

    All rights reserved.

    <g:link controller="menu" action="licenses">Licenses</g:link>

    <span class="num">${grailsApplication.metadata['build.ident']}</span>
<g:if test="${grails.util.Environment.current==grails.util.Environment.DEVELOPMENT}">
    <g:timerSummary/>
</g:if>
