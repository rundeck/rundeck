<%@ page contentType="text/xml;charset=UTF-8" %><%--
--%><g:set var="feedTitle" value="Reports Feed ${paginateParams?.size()>0?' (Filtered)':''}"/><%--
--%><g:set var="feedLink" value="${createLink(controller:'reports',action:'index',params:paginateParams)}"/><%--
--%><g:set var="feedDescription" value="Recent Reports ${paginateParams?.size()>0?' (Filtered)':''}"/><%--
--%><g:set var="items" value="${[]}"/><%--
--%><g:each in="${reports}" var="report"><%--
--%><g:set var="item" value="[:]"/><%--
--%><%
        item.title=report.title
        if(report instanceof ExecReport){
            item.title=(report.status=='succeed'?'SUCCESS':'FAILURE')
            if(report.jcExecId || report.jcJobId){
                if(report.jcJobId){
                    item.link = createLink(controller: 'execution', action: 'show', params: [id: report.jcExecId], absolute:true)
                }else if (report.jcExecId){
                    item.link=createLink(controller:'execution', action:'show',params:[id:report.jcExecId], absolute: true)
                }
            }
            if(report.reportId){
                item.title+=": "+ report.reportId
            }else {
                item.title+=": adhoc"
            }
            if (report.jcExecId || report.jcJobId) {
                item.title+=" - "+report.title
            }else if(report.adhocExecution && report.adhocScript){
                item.title += " - " + report.adhocScript
            }else{
                item.title += " - " + report.title
            }
        }else{
            item.link=createLink(controller:'reports',action:'index',id:report.id,params:paginateParams, absolute: true)
        }

        item.templateName="/feed/jobreportItem"
        item.model=[report:report]
        item.date=report.dateCompleted
        items<<item
%></g:each><g:render template="/common/rssBase" model="[feedTitle:feedTitle,feedLink:feedLink,feedDescription:feedDescription,items:items]"/>