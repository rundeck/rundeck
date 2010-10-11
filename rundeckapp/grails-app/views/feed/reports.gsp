<%@ page contentType="text/xml;charset=UTF-8" %>
    <g:set var="feedTitle" value="${g.message(code:'main.app.name')} Reports Feed"/>
    <g:set var="feedLink" value="${createLink(controller:'menu',action:'reports',params:paginateParams)}"/>
    <g:set var="feedDescription" value="${'Recently Completed '+g.message(code:'main.app.name')+' '+g.message(code:'domain.ScheduledExecution.title')+'s'}"/>
    <g:set var="items" value="${[]}"/>
    <g:each in="${executions}" var="execution">
    <g:set var="scheduledExecution" value="${jobs[execution.scheduledExecution?.id.toString()]}"/>
        <g:set var="item" value="[:]"/>
    <%
        item.title=(execution.status=='true'?'SUCCESS':execution.cancelled?'KILLED':'FAILED')+": "+(scheduledExecution?scheduledExecution.jobName:'Execution '+execution.id)
        item.link=createLink(controller:'execution',action:'show',id:execution.id)
        item.templateName="/feed/reportItem"
        item.model=[execution:execution]
        item.date=execution.dateCompleted
        items<<item
    %>
    </g:each>
    <g:render template="/common/rssBase" model="[feedTitle:feedTitle,feedLink:feedLink,feedDescription:feedDescription,items:items]"/>