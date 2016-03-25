<%@ page import="rundeck.Execution; java.text.SimpleDateFormat; rundeck.ExecReport" contentType="text/xml;charset=UTF-8" %><%--
--%><g:set var="feedTitle" value="Rundeck History: ${paginateParams.projFilter}"/><%--
--%><g:set var="feedLink" value="${createLink(controller:'reports',action:'index',params:paginateParams)}"/><%--
--%><g:set var="feedDescription" value="${feedTitle}"/><%--
--%><g:set var="items" value="${[]}"/><%--
--%><g:each in="${reports}" var="report"><%--
--%><g:set var="item" value="[:]"/><%--
--%><%
        def exec
        if (report.jcExecId) {
            exec = Execution.get(Long.parseLong(report.jcExecId))
        }
        item.title=(exec?exec.executionState:report.status=='succeed'?'SUCCEEDED':'FAILED')
        if(report.jcExecId){
            item.link=createLink(controller:'execution', action:'show',params:[id:report.jcExecId], absolute: true)
        }
        if(report.reportId){
            item.title+=": "+ report.reportId
        }else {
            item.title+=": adhoc"
        }

        if(exec){
            if(exec.argString){
                item.title+=" "+exec.argString
            }
        }
        item.title+=" (" + g.formatDate(date:report.dateCompleted,formatName: 'jobslist.date.format')+")"

        item.templateName="/feed/jobreportItem"
        item.model=[report:report]
        item.date=report.dateCompleted
        items<<item
%></g:each><g:render template="/common/rssBase" model="[feedTitle:feedTitle,feedLink:feedLink,feedDescription:feedDescription,items:items]"/>
