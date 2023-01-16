%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%<%@ page import="rundeck.Execution; java.text.SimpleDateFormat; rundeck.ExecReport" contentType="text/xml;charset=UTF-8" %><%--
--%><g:set var="feedTitle" value="Rundeck History: ${paginateParams.projFilter}"/><%--
--%><g:set var="feedLink" value="${createLink(controller:'reports',action:'index',params:paginateParams)}"/><%--
--%><g:set var="feedDescription" value="${feedTitle}"/><%--
--%><g:set var="items" value="${[]}"/><%--
--%><g:each in="${reports}" var="report"><%--
--%><g:set var="item" value="[:]"/><%--
--%><%
        def exec
        if (report.executionId) {
            exec = Execution.get(report.executionId)
        }
        item.title=(exec?exec.executionState:report.status=='succeed'?'SUCCEEDED':'FAILED')
        if(report.executionId){
            item.link=createLink(controller:'execution', action:'show',params:[id:report.executionId,project:report.ctxProject], absolute: true)
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
