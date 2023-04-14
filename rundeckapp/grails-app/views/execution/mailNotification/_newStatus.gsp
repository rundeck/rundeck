%{--
- Copyright 2019 Rundeck, Inc. (https://www.rundeck.com)
-
- Licensed under the Apache License, Version 2.0 (the "License");
- you may not use this file except in compliance with the License.
- You may obtain a copy of the License at
-
- http://www.apache.org/licenses/LICENSE-2.0
-
- Unless required by applicable law or agreed to in writing, software
- distributed under the License is distributed on an "AS IS" BASIS,
- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
- See the License for the specific language governing permissions and
- limitations under the License.
--}%
<%--
   onsuccess.gsp

   Author: Jesse Marple
   Created: August 26th, 2019
   $Id$
--%>

<%@ page import="org.rundeck.app.data.providers.v1.execution.ReferencedExecutionDataProvider; rundeck.Execution" %>
<%@ page import="rundeck.ReferencedExecution" %>

<%
    request.setAttribute("IS_MAIL_RENDERING_REQUEST",Boolean.TRUE)
%>

<g:set var="referencedExecutionDataProvider" bean="${org.rundeck.app.data.providers.v1.execution.ReferencedExecutionDataProvider}"/>
<g:set var="execCount" value="${scheduledExecution.id ? Execution.countByScheduledExecutionAndDateCompletedIsNotNull(scheduledExecution) : 0}"/>
<g:set var="successcount" value="${scheduledExecution.id ? Execution.countByScheduledExecutionAndStatus(scheduledExecution, 'succeeded') : 0}"/>
<g:set var="refsuccesscount" value="${scheduledExecution.id ? referencedExecutionDataProvider.countByJobIdAndStatus(scheduledExecution.id, 'succeeded') : 0}"/>

<g:set var="refexecCount" value="${scheduledExecution.id ? referencedExecutionDataProvider.countByJobId(scheduledExecution.id) : 0}"/>

<g:set var="successrate" value="${(execCount + refexecCount) > 0 ? ((successcount+refsuccesscount) / (execCount+refexecCount)) : 0}"/>

<g:set var="status" value="${execution.statusSucceeded() ? 'succeed' : execution.cancelled?'warn': execution.customStatusString?'other':'fail'}" />

<g:set var="statusColor" value="${execution.statusSucceeded() ? 'green' : execution.cancelled?'yellow': execution.customStatusString?'blue':'red'}" />

<g:set var="execfailed" value="${execstate in ['failed','aborted']}" />

<html lang="en" xmlns:v="urn:schemas-microsoft-com:vml" xmlns:o="urn:schemas-microsoft-com:office:office">
<head>
  <meta charset="utf-8">
  <meta http-equiv="x-ua-compatible" content="ie=edge">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="x-apple-disable-message-reformatting">
  <title>Execution
    <g:message code="status.label.${execstate}" />
  </title>


  <style type="text/css">
    @import url('https://fonts.googleapis.com/css?family=Merriweather|Open+Sans');

    @media only screen {

      .col,
      td,
      th,
      div,
      p {
        font-family: -apple-system, system-ui, BlinkMacSystemFont, "Segoe UI", "Roboto", "Helvetica Neue", Arial, sans-serif;
      }

      .serif {
        font-family: 'Merriweather', Georgia, serif !important;
      }

      .sans-serif {
        font-family: 'Open Sans', Arial, sans-serif !important;
      }
    }

    #outlook a {
      padding: 0;
    }

    img {
      border: 0;
      line-height: 100%;
      vertical-align: middle;
    }

    .col {
      font-size: 16px;
      line-height: 26px;
      vertical-align: top;
    }

    @media only screen and (max-width: 730px) {
      .wrapper img {
        max-width: 100%;
      }

      u~div .wrapper {
        min-width: 100vw;
      }

      .container {
        width: 100% !important;
        -webkit-text-size-adjust: 100%;
      }
    }

    @media only screen and (max-width: 699px) {
      .d{
        display: block;
        margin-bottom: 20px;
      }
      .col {
        box-sizing: border-box;
        display: inline-block !important;
        line-height: 23px;
        width: 100% !important;
      }

      .col-sm-1 {
        max-width: 8.33333%;
      }

      .col-sm-2 {
        max-width: 16.66667%;
      }

      .col-sm-3 {
        max-width: 25%;
      }

      .col-sm-4 {
        max-width: 33.33333%;
      }

      .col-sm-5 {
        max-width: 41.66667%;
      }

      .col-sm-6 {
        max-width: 50%;
      }

      .col-sm-7 {
        max-width: 58.33333%;
      }

      .col-sm-8 {
        max-width: 66.66667%;
      }

      .col-sm-9 {
        max-width: 75%;
      }

      .col-sm-10 {
        max-width: 83.33333%;
      }

      .col-sm-11 {
        max-width: 91.66667%;
      }

      .col-sm-push-1 {
        margin-left: 8.33333%;
      }

      .col-sm-push-2 {
        margin-left: 16.66667%;
      }

      .col-sm-push-3 {
        margin-left: 25%;
      }

      .col-sm-push-4 {
        margin-left: 33.33333%;
      }

      .col-sm-push-5 {
        margin-left: 41.66667%;
      }

      .col-sm-push-6 {
        margin-left: 50%;
      }

      .col-sm-push-7 {
        margin-left: 58.33333%;
      }

      .col-sm-push-8 {
        margin-left: 66.66667%;
      }

      .col-sm-push-9 {
        margin-left: 75%;
      }

      .col-sm-push-10 {
        margin-left: 83.33333%;
      }

      .col-sm-push-11 {
        margin-left: 91.66667%;
      }

      .full-width-sm {
        display: table !important;
        width: 100% !important;
      }

      .stack-sm-first {
        display: table-header-group !important;
      }

      .stack-sm-last {
        display: table-footer-group !important;
      }

      .stack-sm-top {
        display: table-caption !important;
        max-width: 100%;
        padding-left: 0 !important;
      }

      .toggle-content {
        max-height: 0;
        overflow: auto;
        transition: max-height .4s linear;
        -webkit-transition: max-height .4s linear;
      }

      .toggle-trigger:hover+.toggle-content,
      .toggle-content:hover {
        max-height: 999px !important;
      }

      .show-sm {
        display: inherit !important;
        font-size: inherit !important;
        line-height: inherit !important;
        max-height: none !important;
      }

      .hide-sm {
        display: none !important;
      }

      .align-sm-center {
        display: table !important;
        float: none;
        margin-left: auto !important;
        margin-right: auto !important;
      }

      .align-sm-left {
        float: left;
      }

      .align-sm-right {
        float: right;
      }

      .text-sm-center {
        text-align: center !important;
      }

      .text-sm-left {
        text-align: left !important;
      }

      .text-sm-right {
        text-align: right !important;
      }

      .borderless-sm {
        border: none !important;
      }

      .nav-sm-vertical .nav-item {
        display: block;
      }

      .nav-sm-vertical .nav-item a {
        display: inline-block;
        padding: 4px 0 !important;
      }

      .spacer {
        height: 0;
      }

      .p-sm-0 {
        padding: 0 !important;
      }

      .p-sm-10 {
        padding: 10px !important;
      }

      .p-sm-20 {
        padding: 20px !important;
      }

      .p-sm-30 {
        padding: 30px !important;
      }

      .pt-sm-0 {
        padding-top: 0 !important;
      }

      .pt-sm-10 {
        padding-top: 10px !important;
      }

      .pt-sm-20 {
        padding-top: 20px !important;
      }

      .pt-sm-30 {
        padding-top: 30px !important;
      }

      .pr-sm-0 {
        padding-right: 0 !important;
      }

      .pr-sm-10 {
        padding-right: 10px !important;
      }

      .pr-sm-20 {
        padding-right: 20px !important;
      }

      .pr-sm-30 {
        padding-right: 30px !important;
      }

      .pb-sm-0 {
        padding-bottom: 0 !important;
      }

      .pb-sm-10 {
        padding-bottom: 10px !important;
      }

      .pb-sm-20 {
        padding-bottom: 20px !important;
      }

      .pb-sm-30 {
        padding-bottom: 30px !important;
      }

      .pl-sm-0 {
        padding-left: 0 !important;
      }

      .pl-sm-10 {
        padding-left: 10px !important;
      }

      .pl-sm-20 {
        padding-left: 20px !important;
      }

      .pl-sm-30 {
        padding-left: 30px !important;
      }

      .px-sm-0 {
        padding-right: 0 !important;
        padding-left: 0 !important;
      }

      .px-sm-10 {
        padding-right: 10px !important;
        padding-left: 10px !important;
      }

      .px-sm-20 {
        padding-right: 20px !important;
        padding-left: 20px !important;
      }

      .px-sm-30 {
        padding-right: 30px !important;
        padding-left: 30px !important;
      }

      .py-sm-0 {
        padding-top: 0 !important;
        padding-bottom: 0 !important;
      }

      .py-sm-10 {
        padding-top: 10px !important;
        padding-bottom: 10px !important;
      }

      .py-sm-20 {
        padding-top: 20px !important;
        padding-bottom: 20px !important;
      }

      .py-sm-30 {
        padding-top: 30px !important;
        padding-bottom: 30px !important;
      }
    }
  </style>

  <style>
    .code-wrapper {
      background-color: #e2e2e2;
      padding: 10px 15px;
    }
    .execution-status-warn{
      border-color: yellow;
    }
    .execution-status-succeed{
      border-color: green;
    }
    .execution-status-other{
      border-color: blue;
    }
    .execution-status-fail{
      border-color: red;
    }


    @media only screen and (max-width: 699px) {
    .job-link{
      display: block;
      margin-bottom:20px;
    }
     .mobile-button-stack{
       margin-bottom:20px;
     }
    }
  </style>
</head>

<body
  style="box-sizing:border-box;margin:0;padding:0;width:100%;word-break:break-word;-webkit-font-smoothing:antialiased;">
  <div class="content">
  <div id="layoutBody">
  <div style="display:none;font-size:0;line-height:0;">
    <!-- Add your preheader text here -->
  </div>
  <table class="wrapper" cellpadding="15" cellspacing="0" role="presentation" width="100%">
    <tr>
      <td align="center" bgcolor="#FFFFFF">
        <g:if test="${execution.executionState=='missed'}">
          <table class="container" cellpadding="0" cellspacing="0" role="presentation" width="700">
            <tr>
              <td class="col" colspan="2" width="310" style="padding: 0 10px;">
                <h3><g:message code="status.label.${execution.executionState}" /></h3>
                <div>Project: ${execution.project}</div>
                <div>Job: ${scheduledExecution.jobName}</div>
                <div>Missed Schedule Time: <g:enc>${execution.dateStarted}</g:enc></div>
              </td>
            </tr>
          </table>
        </g:if>
        <g:else>
        <table class="container" cellpadding="0" cellspacing="0" role="presentation" width="700">
          <tr>
            <td align="left" bgcolor="#FFFFFF">
              <!-- ADD ROWS HERE -->
          <tr>
            <td class="col" colspan="2" width="310" style="padding: 0 10px;">
              <div style="border-left:20px solid ${statusColor};padding-left:10px;" class="execution-status-${status}">
                <h3 style="margin: 0;">
                  <g:enc>${execution.project}</g:enc>
                </h3>
                <h1 style="margin: 10px 0 15px 0;">
                  <g:link controller="execution" action="show" id="${execution.id}" class="job-link"
                    absolute="${absolute ? 'true' : 'false'}"
                    params="${(followparams?.findAll { it.value }?:[:]) + [project: execution.project]}">
                    <g:if test="${scheduledExecution}">
                      <g:message code="scheduledExecution.identity"
                        args="[scheduledExecution.jobName, execution.id.toString()]" />
                      <%-- Greg, is it better to use a message here rather than a direct outputting of the string? --%>
                    </g:if>
                    <g:else>
                      <g:message code="execution.identity" args="[execution.id.toString()]" />
                    </g:else>
                  </g:link>

                  ${scheduledExecution.jobName}

                </h1>
                <h3 style="margin: 0;">
                  by <g:enc>${execution?.user}</g:enc>
                  <%-- What is the difference? --%>
                  <%-- by <g:username user="${execution.user}" /> --%>
                </h3>
                <h3 style="margin: 0;">
                  <g:if test="${execution.dateCompleted != null}">
                    <span
                      class="${execution.statusSucceeded() ? 'succeed' : execution.cancelled?'warn': execution.customStatusString?'other':'fail'}">

                      <g:if test="${execution.customStatusString}">
                        "${execution.customStatusString}"
                      </g:if>
                      <g:else>
                        <g:message code="status.label.${execution.executionState}" />
                        <g:if test="${execution.cancelled}">
                          <g:if test="${execution.abortedby}"> by
                            <g:username user="${execution.abortedby}" />
                          </g:if>
                        </g:if>
                      </g:else>
                    </span>
                    <span style="${wdgt.styleVisible(if: execution.dateCompleted != null )}">
                      <g:message code="after" default="after" />
                      <g:if test="${execution.dateCompleted}">
                        <g:relativeDate start="${execution.dateStarted}" end="${execution.dateCompleted}" />
                      </g:if>
                      <g:message code="at" default="at" />

                      <g:if test="${execution.dateCompleted}">
                        <g:relativeDate atDate="${execution.dateCompleted}" />
                      </g:if>
                    </span>

                  </g:if>

                </h3>
              </div>
            </td>
          </tr>
          <tr>
            <td class="spacer py-sm-20" height="20"></td>
          </tr>
          <g:if test="${scheduledExecution.description != null}">

            <tr>
              <td class="col" colspan="2" style="padding: 0 10px;">
                <div>
                  <a class="toggle-trigger" style="text-decoration: none;">
                    <button class="toggle-trigger"
                      style="background-color: #EEEEEE; margin: 0; padding: 0; display: block; width: 100%; text-align: left; border: none; outline: none; font-size: 15px;">
                      <table bgcolor="#EEEEEE" cellpadding="0" cellspacing="0" role="presentation" width="100%">
                        <tr>
                          <td style="padding: 20px;">Job Description</td>
                        </tr>
                      </table>
                    </button>
                  </a>
                  <div class="toggle-content">
                    <table cellpadding="0" cellspacing="0" width="100%" role="presentation"
                      style="border: 1px solid #EEEEEE;">
                      <tr>
                        <td style="padding: 20px;">
                          ${scheduledExecution.description}
                        </td>
                      </tr>
                    </table>
                  </div>
                </div>
              </td>
            </tr>
            <tr>
              <td class="spacer py-sm-20" height="20"></td>
            </tr>
          </g:if>
          <tr>
            <td class="col" colspan="1" style="padding: 0 10px;">
              <div style="background-color:#EEEEEE; border-radius: 3px; line-height: 100%; mso-padding-alt: 5px 50px 10px;text-align: center;">
                <g:link absolute="true" controller="execution"
                      class="mobile-button-stack"
                      params="[project: execution.project]"
                      action="show" id="${execution.id}" title="View execution output" style="color: #000000; display: block; font-size: 13px; padding: 10px 50px; text-decoration:
                      none;">View Output</g:link>
              </div>
            </td>
          <g:if test="${execstate!='running'}">
            <td class="col" colspan="1" style="padding: 0 10px;">
              <div style="background-color:#EEEEEE; border-radius: 3px; line-height: 100%; mso-padding-alt: 5px 50px 10px;text-align: center;">
                <g:link class="mobile-button-stack"
                    title="Download entire output file"
                    controller="execution"
                    action="downloadOutput"
                    absolute="true"
                    params="[project:execution.project]"
                    id="${execution.id}"
                    style="color: #000000; display: block; font-size: 13px; padding: 10px 50px; text-decoration:none;">
                    Download Output
                    <g:if test="${filesize}">(<g:enc>${filesize}</g:enc> bytes)</g:if>
                </g:link>
              </div>
            </td>
          </g:if>
          </tr>
      </td>
    </tr>
    <tr>
      <td colspan="3" style="padding: 0 10px;">
        <div class="spacer py-sm-10" style="line-height: 30px;">‌</div>
        <table cellpadding="0" cellspacing="0" role="presentation" width="100%">
          <tr>
            <g:if test="${null!=execution.dateStarted}">
              <td class="col pb-sm-30 borderless-sm" style="padding: 0 20px;">
                <h5 style="margin: 0 0 10px;">Started</h5>
                <div style="font-size: 30px; line-height: 100%;">
                  <g:relativeDate elapsed="${execution.dateStarted}" agoClass="timeago" />
                </div>
                <div style="font-size: 12px; line-height: 100%; margin-top:10px;">
                  <g:enc>${execution.dateStarted}</g:enc>
                </div>
              </td>
            </g:if>
            <g:else>
              <td class="col pb-sm-30 borderless-sm">
                <h5 style="margin: 0 0 10px;">Started</h5>
                <div style="font-size: 30px; line-height: 100%;">
                  Just Now
                </div>
              </td>
            </g:else>
            <g:if test="${null!=execution.dateCompleted}">
              <td class="col pb-sm-30 borderless-sm"
                style="border-left: 1px solid #EEEEEE; padding: 0 20px;">
                <h5 style="margin: 0 0 10px;">Finished</h5>
                <div style="font-size: 30px; line-height: 100%;">
                  <g:relativeDate elapsed="${execution.dateCompleted}" agoClass="timeago" />
                </div>
                <div style="font-size: 12px; line-height: 100%; margin-top:10px;">
                  <g:enc>${execution.dateCompleted}</g:enc>
                </div>
              </td>
            </g:if>
            <g:if test="${null!=execution.dateCompleted && null!=execution.dateStarted}">
              <td class="col borderless-sm" style="border-left: 1px solid #EEEEEE; padding: 0 20px;">
                <h5 style="margin: 0 0 10px;">Duration</h5>
                <div style="font-size: 30px; line-height: 100%;">
                  <g:relativeDate start="${execution.dateStarted}" end="${execution.dateCompleted}" />
                </div>
              </td>
            </g:if>
          </tr>
        </table>
        <div class="spacer py-sm-10" style="line-height: 30px;">‌</div>
      </td>
    </tr>
    <tr>
      <td class="divider" bgcolor="#FFFFFF" style="padding: 10px;" colspan="2">
        <div style="background: #d0d8de; height: 1px; line-height: 1px;">‌</div>
      </td>
    </tr>
    <tr>
      <td colspan="3" style="padding: 0 10px;">
        <div class="spacer py-sm-10" style="line-height: 30px;">‌</div>
        <table cellpadding="0" cellspacing="0" role="presentation" width="100%">

          <g:set var="avgduration" value="${scheduledExecution.getAverageDuration()}"/>

          <tr>
            <g:if test="${avgduration>0}">
              <td class="col pb-sm-30" width="184" style="padding: 0 20px;">
                <h5 style="margin: 0 0 10px;"><g:message code="average.duration"/></h5>
                <div style="font-size: 30px; line-height: 100%;">
                  <g:timeDuration time="${avgduration}"/>
                </div>
              </td>
            </g:if>

            <%-- <td class="col pb-sm-30 borderless-sm" width="190" style="border-left: 1px solid #EEEEEE; padding: 0 20px;">
              <h5 style="margin: 0 0 10px;">Actual Duration</h5>
              <div style="font-size: 30px; line-height: 100%;">???</div>
            </td> --%>
            <td class="col pb-sm-30 borderless-sm" width="190" style="border-left: 1px solid #EEEEEE; padding: 0 20px;">
              <h5 style="margin: 0 0 10px;">
                <g:message code="Execution.plural"/>
              </h5>
              <div style="font-size: 30px; line-height: 100%;">
                ${execCount}
              </div>
            </td>
            <td class="col borderless-sm" width="184" style="border-left: 1px solid #EEEEEE; padding: 0 20px;">
              <h5 style="margin: 0 0 10px;">Success Rate</h5>
              <div style="font-size: 30px; line-height: 100%;">
                <g:formatNumber number="${successrate}" type="percent"/>
              </div>
            </td>
          </tr>
        </table>
        <div class="spacer py-sm-10" style="line-height: 30px;">‌</div>
      </td>
    </tr>
    <g:if test="${nodestatus || execution?.failedNodeList}">
    <tr>
      <td class="col" colspan="2" style="padding: 0 10px;">
        <div>
          <a class="toggle-trigger" style="text-decoration: none;">
            <button class="toggle-trigger"
              style="background-color: #EEEEEE; margin: 0; padding: 0; display: block; width: 100%; text-align: left; border: none; outline: none; font-size: 15px;">
              <table bgcolor="#EEEEEE" cellpadding="0" cellspacing="0" role="presentation" width="100%">
                <tr>
                  <td style="padding: 20px;">Nodes</td>
                </tr>
              </table>
            </button>
          </a>
          <div class="toggle-content">
            <table cellpadding="0" cellspacing="0" width="100%" role="presentation" style="border: 1px solid #EEEEEE;">
              <tr>
                <td style="padding: 20px;">
                  <g:if test="${nodestatus }">
                          <g:set var="vals" value="${[nodestatus.succeeded,nodestatus.failed,nodestatus.total]}"/>
                          <g:set var="summary" value=""/>
                          <g:if test="${vals && vals.size()>2 && vals[2]!='0' && vals[2]!=0}">
                              <g:set var="a" value="${vals[0] instanceof String? Integer.parseInt(vals[0]):vals[0]}"/>
                              <g:set var="fai" value="${vals[1] instanceof String? Integer.parseInt(vals[1]):vals[1]}"/>
                              <g:set var="den" value="${vals[2] instanceof String? Integer.parseInt(vals[2]):vals[2]}"/>

                              <g:set var="sucperc" value="${(int)Math.floor((a/den)*100)}"/>
                              <g:set var="perc" value="${(int)Math.floor((fai/den)*100)}"/>
                              <g:if test="${null!=vals[0] && null!=vals[2]}">
                              <g:set var="sucsummary" value="${vals[0]+' of '+vals[2]}"/>
                              <g:set var="summary" value="${vals[1]+' of '+vals[2]}"/>
                              </g:if>
                          </g:if>
                          <g:else>
                              <g:set var="perc" value="${0}"/>
                          </g:else>
                          <g:if test="${vals && vals.size()>1 && vals[1]!='0' && vals[1]!=0}">
                              <g:enc>${vals[1]}</g:enc> failed
                          </g:if>
                          <g:else>
                              <g:enc>${vals[0]}</g:enc> ok
                          </g:else>
                          <g:if test="${perc>0}">
                          <g:render template="/common/progressBar" model="${[completePercent:(int)perc,title:'Failed nodes',className:'nodes failure',showpercent:false,innerContent:summary]}"/>
                          </g:if>
                  </g:if>
                  <g:if test="${execution?.failedNodeList}">
                          <g:set var="failednodes" value="${execution?.failedNodeList.split(',')}"/>
                          <g:if test="${!nodestatus}">
                          <g:enc>${failednodes.length}</g:enc> failed:
                          </g:if>
                          <div><g:enc>${execution?.failedNodeList}</g:enc></div>
                  </g:if>
                </td>
              </tr>
            </table>
          </div>
        </div>
      </td>
    </tr>
    <tr>
      <td class="spacer py-sm-20" height="20"></td>
    </tr>
    </g:if>
    <g:if test="${logOutput}">
    <tr>
      <td class="col" colspan="2" style="padding: 0 10px;">
        <div>
          <a class="toggle-trigger" style="text-decoration: none;">
            <button class="toggle-trigger"
              style="background-color: #EEEEEE; margin: 0; padding: 0; display: block; width: 100%; text-align: left; border: none; outline: none; font-size: 15px;">
              <table bgcolor="#EEEEEE" cellpadding="0" cellspacing="0" role="presentation" width="100%">
                <tr>
                  <td style="padding: 20px;">Log Output</td>
                </tr>
              </table>
            </button>
          </a>
          <div class="toggle-content">
            <table cellpadding="0" cellspacing="0" width="100%" role="presentation" style="border: 1px solid #EEEEEE;">
              <tr>
                <td style="padding: 20px;">
                  <pre>
                    <g:if test="${allowUnsanitized}">
                      ${enc(rawtext:logOutput)}
                    </g:if>
                    <g:else>
                      ${enc(sanitize:logOutput)}
                    </g:else>
                  </pre>
                </td>
              </tr>
            </table>
          </div>
        </div>
      </td>
    </tr>
    <tr>
      <td class="spacer py-sm-20" height="20"></td>
    </tr>
    </g:if>
    <!-- /// -->
    </td>
    </tr>
  </table>
        </g:else>
  </td>
  </tr>
  </table>
  </div>
  </div>
</body>

</html>
