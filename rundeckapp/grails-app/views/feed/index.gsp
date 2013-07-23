<%@ page import="rundeck.Execution; java.text.SimpleDateFormat; rundeck.ExecReport" contentType="text/xml;charset=UTF-8" %><%--
--%><g:set var="feedTitle" value="Reports Feed ${paginateParams?.size()>0?' (Filtered)':''}"/><%--
--%><g:set var="feedLink" value="${createLink(controller:'reports',action:'index',params:paginateParams)}"/><%--
--%><g:set var="feedDescription" value="Recent Reports ${paginateParams?.size()>0?' (Filtered)':''}"/><%--
--%><g:set var="items" value="${[]}"/><%--
--%><g:each in="${reports}" var="report"><%--
--%><g:set var="item" value="[:]"/><%--
--%><%
        item.title=(report.status=='succeed'?'SUCCESS':'FAILURE')
        if(report.jcExecId){
            item.link=createLink(controller:'execution', action:'show',params:[id:report.jcExecId], absolute: true)
        }
        if(report.reportId){
            item.title+=": "+ report.reportId
        }else {
            item.title+=": adhoc"
        }
        if(report.adhocExecution && report.adhocScript){
            item.title += " - " + report.adhocScript
        }else if(!report.reportId || report.reportId=='adhoc'){
            item.title += " - " + report.title
        }
        if(report.jcExecId){
            def exec = Execution.get(Long.parseLong(report.jcExecId))
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
