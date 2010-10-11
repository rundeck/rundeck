<%--
 Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 --%>
<%--
   onsuccess.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: May 17, 2010 6:30:25 PM
   $Id$
--%>
<%@ page contentType="text/html" %>
<html>
<head><title>Execution <g:message code="status.label.${execution.status=='true'?'succeed':'fail'}"/></title>
    <style type="text/css">
    span.jobname {
        font-weight: bold;
    }

    span.result {
        font-weight: bold;
        color: green;
    }

    span.result.fail {
        color: red;
    }

    span.date {
        font-style: italic;
    }

    div, ul {
        margin-top: 10px;
    }

    ul {
        list-style-type: none;
        margin-left: 0;
        padding-left: 0;
    }

    div.foot {
        padding: 5px;
        color: #888;
    }

    div.foot ul {
        display: inline;
        margin: 0;
        padding: 0;
    }

    div.foot li {
        display: inline;
        margin: 0;
    }

    div.content {
        background: white url(<g:resource file="/images/bg-bottom-1.png" absolute="true"/>) repeat-x bottom left;
        padding: 0 0 4px;
    }

    div.content > div {
        background: transparent url(<g:resource file="/images/bg-left-1.png"  absolute="true"/>) no-repeat top left;
        padding: 15px;
    }
    </style>
</head>
<body>
<div class="content">
    <div class="report">
        <g:if test="${execution}">
            <g:if test="${scheduledExecution}">
                <g:set var="jobtitle" value="${scheduledExecution.jobName}"/>
            </g:if>
            <g:elseif test="${execution?.adhocExecution}">
                <g:if test="${execution?.adhocExecution && execution.adhocScript}">
                    <g:set var="jobtitle" value="${execution.adhocScript}"/>
                </g:if>
                <g:elseif test="${execution?.adhocExecution}">
                    <g:set var="jobtitle" value="run"/>
                </g:elseif>
            </g:elseif>
            <g:else>
                <g:set var="jobtitle" value="${execution.ctxCommand}"/>
            </g:else>
            <span class="jobname ">${jobtitle.encodeAsHTML()}</span>:
            <span class="result ${execution?.status != 'true' ? 'fail' : ''}"><g:message code="status.label.${execution.status=='true'?'succeed':'fail'}"/></span>

            <g:if test="${execution }">
                - <g:link absolute="true" controller="execution" action="show" id="${execution.id}" title="View execution output">View Output &raquo;</g:link>
            </g:if>
            <div class="detail">
                <g:if test="${execution.dateCompleted}">

                    Time: 
                    <span class="date">
                        <g:formatDate date="${execution?.dateCompleted}" formatName="jobslist.date.format"/>
                    </span>
                </g:if>
                <g:if test="${execution.dateCompleted && execution.dateStarted}">
                    <span class="date">
                        (<g:timeDuration end="${execution?.dateCompleted}" start="${execution.dateStarted}"/>)
                    </span>
                </g:if>
            </div>
            <g:if test="${execution.user}">
            <div class="user">
              User: ${execution.user.encodeAsHTML()}
            </div>
            </g:if>
            <div class="link">
                <g:if test="${scheduledExecution}">
                    <g:link absolute="true" controller="scheduledExecution" action="show" id="${scheduledExecution.id}">"${scheduledExecution.jobName.encodeAsHTML()}" Job Detail &raquo;</g:link>
                </g:if>
            </div>

        </g:if>
    </div>
</div>
<div class="foot">
    Run Deck:
    <g:link absolute="true" controller="menu" action="jobs">Jobs &raquo;</g:link>
    <g:link absolute="true" controller="reports" action="index">Events &raquo;</g:link>
    <g:link absolute="true" controller="framework" action="nodes">Resources &raquo;</g:link>
</div>

</body>
</html>