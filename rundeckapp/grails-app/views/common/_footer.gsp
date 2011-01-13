    <g:message code="main.app.name"/>
    <span class="num">${grailsApplication.metadata['app.version']}</span>
    &copy; Copyright 2010 <a href="http://dtolabs.com"><g:img file="dto-logo-small.png" width="16px" height="16px"/> DTO Labs</a>.

    All rights reserved.
    
    <span class="num">${grailsApplication.metadata['build.ident']}</span>
<g:if test="${grails.util.Environment.current==grails.util.Environment.DEVELOPMENT}">
    <g:timerSummary/>
</g:if>
