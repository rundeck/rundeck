<%@ page contentType="text/xml;charset=UTF-8" %><%--
--%><g:set var="feedTitle" value="${g.message(code:'domain.ScheduledExecution.title')+' Reports Feed '+(paginateParams?.size()>0?' (Filtered)':'')}"/><%--
--%><g:set var="feedLink" value="${createLink(controller:'reports',action:'jobs',params:paginateParams)}"/><%--
--%><g:set var="feedDescription" value="Recently Completed ${g.message(code:'domain.ScheduledExecution.title')}s ${paginateParams?.size()>0?' (Filtered)':''}"/><%--
--%><g:set var="items" value="${[]}"/><%--
--%><g:each in="${reports}" var="report"><%--
--%><g:set var="item" value="[:]"/><%--
--%><%
        item.title=(report.status=='succeed'?'SUCCESS':'FAILURE')
        if(report.jcExecId){
            item.link=createLink(controller:'execution', action:'show',params:[id:report.jcExecId])
        }else{
            item.link=createLink(controller:'reports',action:'jobs',params:paginateParams)
        }
        if(report.jcJobId){
            item.title+=": "+report.title
        }else if(report.adhocExecution && report.adhocScript){
            if(report.adhocScript.size()>30){
                item.title+=": "+report.adhocScript.substring(0,30)
            }else{
                item.title+=": "+report.adhocScript
            }
        }else if(report.adhocExecution){
            item.title+=": run"
        }else if (report.jcExecId){
            item.title+=": "+report.ctxCommand
        }
        item.templateName="/feed/jobreportItem"
        item.model=[report:report]
        item.date=report.dateCompleted
        items<<item
    %><%--
--%></g:each><%--
--%><g:render template="/common/rssBase" model="[feedTitle:feedTitle,feedLink:feedLink,feedDescription:feedDescription,items:items]"/>