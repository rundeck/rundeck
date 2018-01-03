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
  --}%
<%--
   onsuccess.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: May 17, 2010 6:30:25 PM
   $Id$
--%>
<%@ page import="rundeck.Execution" contentType="text/html" %>
<%
    request.setAttribute("IS_MAIL_RENDERING_REQUEST",Boolean.TRUE)
%>
<html>
<head><title>Execution <g:message code="status.label.${execstate}"/></title>
    <style type="text/css">
    span.jobname {
        font-weight: bold;
    }

    span.result {
        font-weight: bold;
    }

    span.result.fail {
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

    .grouplabel a {
        background: transparent url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAPCAYAAADtc08vAAAACXBIWXMAAAsTAAALEwEAmpwYAAACdUlEQVQokY2Tv0sbcRjGP5c7TdQgVmPERoItGBxUlA6Ko6VDhzh2sf+BpUs7uQvFv6E41q1zQehgg1oEA/4WJNHUcMfdeWdqyd33vneXTikUFHynZ3g+L88L76PyuOlZWlp6USwW3+Xz+cLh4eHefaa+5eXll+vr658WFxdfZTKZpysrK282NjY+7+zsnF9eXsZxHLc2NzevgMGhoaHnQFIBWF1d/VAsFt9nMpl8Nptla2tLHxgY6BgZGcmkUilc10XXdRzHwXEcCVxns9ns2traV212drZ3amrq7eTkZL7ZbOK6LmNjY8O2bbO/v4+UkmQySW9vL6Ojo4yPj3ckEolnzWaTwcHBijY9Pb0yPz8/Xa/XOTs7I4oi0uk0/f39TExMkEqliOOYIAjwfR/f9wnDEMuyME3zp9ZoNF4DlMtlZmZmGB4eBkAI8Q+QUiKlJAgCpJTEcYxlWcHBwcGVVigUvliWNZVMJsnlcnie95/5Pq0oCo7j3BiGoWue591YlkVPTw9SSoQQD4JtraoqruvWgUbC8zzXsiy6u7vxfZ8gCBBCIIS4V7cXOY5TAeLE3d3drW3bdHV14Xneg6AQgjAMCcMQ3/cxTfMcQLNt+7bRaLQ6OzuVdoL7YkdRRKvVQtM0dF3n4uJiG0A7PT29m5ubE6qqptr3t8EoigBQVRUhBIZhYNt2tVwufymVSt8BtEql8ieOYw9IhWFIq9VCURSklOi6jmmav3Rd36/VaqWTk5Ptvb29I+B3+/814DadTjeCIHhSq9UwDOO6Xq+Xq9Xqj6Ojo+3d3d1j4PahlikACwsLH3O5XF+1Wv1WKpWOAfeRLeUveujI8l0o/+MAAAAASUVORK5CYII=") top left no-repeat;
        padding-left: 18px;
    }

    .jobInfo .jobIcon.jobok {
        background: transparent url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAABDQAAAQ0BROAatAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAACUSURBVDjLY/j//z8DNjznXp8oEB+J3ObysfBIvAYudfg0XwLi/55rjf+HbnH8jssQvJphBuAzBK/muff6/3utM8VrCE7N8+9P+r/7xab/zcdL/vtvsMJpCF7NB1/t/N93pgGvIXg1wwzAZwjIgCPIfkbWjGwAzBDkMAFFMVUMoMwLFAciVaKRKgmJKkmZKpmJ1OwMACFquSGbMQYsAAAAAElFTkSuQmCC") top left no-repeat;
        padding-left: 18px;
    }

    .jobInfo .jobIcon.jobrunning {
        background: transparent url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAABDQAAAQ0BROAatAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAADVSURBVDjLrZNBCsIwEEV7Ce/gtbyBew/gQQLtovQGXdaVK12YdCUI2oo2RW0tUuGbWaRgdSJCF0M2eY/8mYkHwPtWUsqRqSSKokscx2Pungtem4IQAmEYNpzECVuBS8LCaZpClyWUOYMgYCUsfKtr6PaJ473BIc9ZCQufHy3muwLT5QZ5zUuc8Gx7wmSxckpIkNiGUWZ6toWtwEooDvXENpZGPIigi6CU+hnB9/33CP0m9iUW3mfZB8yOkSTXqurGyMHORSJJoTWkOTl4+FUe5DP9+51f1mwg28gP3wsAAAAASUVORK5CYII=") top left no-repeat;
        padding-left: 18px;
    }

    .jobInfo .jobIcon.jobwarn {
        background: transparent url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAABDQAAAQ0BROAatAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAACSSURBVDjLY/j//z8DVnw6VxSIjzxfJvLx/WYTDVzq8Gm+BMT/H85l+P9sicB3XIbg1QwzAJ8heDX/P533/+E8JryG4NZ8tuj//9uz/7/aE/D/0QJOnIbg1fz/3uL/bw7F4zUEr2aYAfgMARlwBNnPyJqRDYAZghwmoCimigEUeoHiQKRKNFIlIVElKVMlM5GYnQFK8OAbsIEBoAAAAABJRU5ErkJggg==") top left no-repeat;
        padding-left: 18px;
    }

    .jobInfo .jobIcon.joberror {
        background: transparent url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAABDQAAAQ0BROAatAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAACQSURBVDjLY/j//z8DVtyYKwrER55LiHx8b2eigUsdPs2XgPj/Q6CSZ8IC33EZglczzAB8huDV/L8p7/9DJia8huDW3Fr0///K2f9fBwb+f8TJidMQvJr/b1jy/01CAl5D8GqGGYDPEJABR5D9jKwZ2QCYIchhAopiqhhAoRcoDkSqRCNVEhJVkjJVMhOJ2RkAZYCcQZjmAs8AAAAASUVORK5CYII=") top left no-repeat;
        padding-left: 18px;
    }

    .jobInfoPart .partContent {
        background: transparent url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAcAAAAMCAYAAACulacQAAAACXBIWXMAAAsTAAALEwEAmpwYAAAA2ElEQVQY02MoLMyfJSDAL8AAAcwMyODUqeM/FyyYd4SLi0sIQwFQ8u2fP7//b9+++aaMjLQqVJgFTB49evjjly+f/4EU7Nu3+5WOjrYVXAFQ8gNQ8v+bN69+g+gzZ079MDc3C4TpBEu+ePHsHxD/BSr6e/Xqpf9xcTG1DAcP7oNLPn36+A9U8h9QshosCRT4D5T4DaKBDvwONNYfbCzQER9BukASQBe/ADrIEu4goORbkLErViy9DvSKCopXgMb+nDFj6mFgIAhiBEJubtYMQUEBfiiXCTn0AJtFlss26M18AAAAAElFTkSuQmCC") top left no-repeat;
        padding-left: 9px;
    }

    .wfitem.jobtype {
        background: transparent url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAQAAAC1+jfqAAAAjUlEQVQoz2NgAIL7Am73Bf+LoUDR/4r/kyaBZBlu8FrvlP5veFpsJgKKzBSdI3xF6T9Ygf8l8f8Gh4P1RCQRUFPyP5NIpyhEgSjQONHvIt/FfsKgyE+JnyqGou1iSAowoIjRiFEgil+BYIcyPgX8xpodyVPBCoSxKuC1ma/AAAHG/9GViPyX+W9lB5EFAP9Q0j0gWD5FAAAAAElFTkSuQmCC") top left no-repeat;
        padding-left: 18px;
    }

    .wfitem.plugintype {
        background: transparent url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAAegAAAHoBlQypfwAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAEBSURBVDiNrZMhcsMwFERfrBldSMRAMEBQJDcJaIiASAp8gRablRjqEqYhMtcZIuISu3VtuUlnuuxLf3f2a/UP4zhSQgjhBJynsjHGfJT6DiWBEMILcF0dX4wxr+ve6kkywHW623ewJOec6boOAGstUsqiky+BNdl7T9/3ACilcM4VRapHZIC+7/Hek3PejCPquj4Bb3vkGSklYoxorRFCAByHYbhVfEdF13VF8tLJ/C4TzpsU/ooKaObCWotSardZKYW1dnnUiLZtb8Mw3IGjEAKtNTFGUkobciGJ9wpgiuQCIKXEOffDyW8x/t9HKokUsNmHTQrLcR6Riw4WTp5a50+zQaPPZJVLUQAAAABJRU5ErkJggg==") top left no-repeat;
        padding-left: 18px;

    }
    .wfitem.exectype {
        background: transparent url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAAUQAAAFEBjheh4wAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAD4SURBVDiNpZOxSgNhEIS/XS/mMLFROQ5fQCtBfAVtFEF8B8UmTQobX8JWHyKNXUDxGdJF0uRqJWm00AtjkQuSGBL/c2CqZWeHYccABy6Bc2AHMBZDQBdoAfcAV0BeDEKYF7u0SyxP2AboW6Mmu1gTK8ECfQeMXo7drOOdBDuJl0QwBQPIABGb7LouH6ayx02xX/mLg+xHYMLdSP6ayvNt2WkcILBqsmZdPkjlz1viIMCBHVblvUT+ksjOll6dEogA2Kug23d09wFfCgmR3xmEMfPgczOICqW5sIcN7Kg6d6anT3T8JvjnKzvjVo1KuB8BrYiikpSs8zcLZrWZP+PxIQAAAABJRU5ErkJggg==") top left no-repeat;
        padding-left: 18px;

    }
    a.filelink{
        background: transparent url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA0AAAAQCAYAAADNo/U5AAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAAsSAAALEgHS3X78AAABL0lEQVQoz32RIW+EQBCFIdWksv0HFSTF9vegOUPSXHAn24pLVR1/AYNHopBIJKbB9TiS3hboe5sdwlGoeJljZ755b/cs27bvwjB8jaLoHfVIBUFw9H3/DXpxXfcJMxZ0Y6plOY7j1XXdKaXGpmkGaMT3WFXVkOf5CF0AHwQQ6LEsyy8MDtAFw0qUZZlC74cLkeBjCXXGgaBU7VQURQ/om3047icIWzU0jycR0zQd4zjuWZMk+cT87QS1bTtBcxFEkgGOhE5wetAQH0IgU68ksRG34/wE8bIANDSXxCbEu08QDjeheWwDeVdO0GDqEtTLePc/ToTYXIN4xuWr8cRpri3obAZ6E3Gp3kDn1XhrTnLOOYE8XLA1r9RviX28XqtfD//wPX7soD30/I/Y33H+F6RCSeI84+IgAAAAAElFTkSuQmCC") top left no-repeat;
        padding-left: 18px;
    }
    .jobName{
        font-weight:bold;
        margin-left:5px;
    }
    .jobInfoPart{
        margin-left:5px;
    }
    .jobDesc{
        color: #777;
    }
    span.prompt {
        font-weight: bold;
    }
    div.presentation{
        margin: 5px 0 5px 10px;
    }
    table.executioninfo tr > td:first-child,table.simpleForm tr > td:first-child, div.jobstats table tr > td:first-child {
        text-align: right;
        color: gray;
        white-space: nowrap;
    }
        table.simpleForm td{
            padding:2px;
            vertical-align:top;
        }
        /* progress bar */

div.progressContainer{
    width:406px;
    background: #ddd;
    height:14px;
    border:1px solid #aaa;
    position:relative;
    margin:0;
}
    div.progressContainer div{
        margin:0;
    }

div.progressContainer.small{
    width:106px;
}
div.progressContainer.indefinite{
    background: #eee;
    border:1px solid #ccc;
}
div.progressBar{
    width:0px;
    background:#88f;
    height:12px;
    color:black;
    text-shadow: 1px 1px 1px #fff;
    font-size:9px;
    line-height:9px;
    padding:1px 3px;
    border-right: 1px solid #999;
    margin:0;
}
div.progressContainer.indefinite div.progressBar{
    background:none;
    width:100%;
    height:12px;
    color:#555;
    text-shadow: 1px 1px 1px #fff;
    font-size:9px;
    line-height:9px;
    padding:1px 3px;
    border:0;
}
div.progressContainer.nodes{
    border: 0;
}
div.progressBar.nodes{
    background:#ccf;
    border-right:2px solid #aaf;
}
div.progressBar.nodes.failure{
    background:#fcc;
    border-right:2px solid #faa;
}
div.progressBar.full,div.progressBar.full.nodes{
}
div.progressBar.empty, div.progressBar.empty.failure{
    background:none;
    border-right: 0;
}
div.progressContainer.empty.nodes{
    border-right: 0;
}
div.progressContainer div.progressContent{
    padding-top:2px;
    padding-left: 5px;
    position: absolute;
    left:0;
    right:0;
    top:0;
    width: 100%;
}
    </style>
</head>
<body>
<g:set var="execfailed" value="${execstate in ['failed','aborted']}"/>
<div class="content">
    <div class="report">
        <g:render template="/scheduledExecution/showExecutionHead" model="[scheduledExecution:scheduledExecution,execution:execution,noimgs:true,absolute:true]"/>

        <div class="presentation">
            &bull; <span class="result ${execfailed ? 'fail' : ''}"><g:message code="status.label.${execstate}"/></span>
            <g:if test="${execution.customStatusString}">
                "${execution.customStatusString}"
            </g:if>
            <g:if test="${execution.dateCompleted && execution.dateStarted}">
            <span class="date">
                after <g:timeDuration end="${execution?.dateCompleted}" start="${execution.dateStarted}"/>
            </span>
            </g:if>
            <g:if test="${execstate=='aborted'}">
                by <em><g:enc>${execution.abortedby}</g:enc></em>
            </g:if>
            - <g:link absolute="true" controller="execution"
                      params="[project: execution.project]"
                      action="show" id="${execution.id}" title="View execution output">View Output &raquo;</g:link>
        </div>
        <g:if test="${execstate!='running'}">
        <div class="presentation">
            &bull; <g:link class="filelink"
                title="Download entire output file"
                controller="execution"
                action="downloadOutput"
                absolute="true"
                params="[project:execution.project]"
                id="${execution.id}">
                Download Output
            </g:link>
             <g:if test="${filesize}">(<g:enc>${filesize}</g:enc> bytes)</g:if>
        </div>
        </g:if>

        <span class="prompt">
<g:if test="${jobref}">
    Referenced Job "${jobref}"
</g:if>
            Execution</span>
        <div class="presentation">
            <table>
                <tr>
                    <td>

            <table class="executionInfo">
                <tr>
                    <td>User:</td>
                    <td><g:enc>${execution?.user}</g:enc></td>
                </tr>
                <g:if test="${null!=execution.dateCompleted && null!=execution.dateStarted}">

                    <tr>
                        <td>Time:</td>
                        <td><g:relativeDate start="${execution.dateStarted}" end="${execution.dateCompleted}" /></td>
                    </tr>
                </g:if>
                <g:if test="${null!=execution.dateStarted}">
                <tr>
                    <td>Started:</td>
                    <td>
                        <g:relativeDate elapsed="${execution.dateStarted}" agoClass="timeago"/>
                    </td>
                    <td><span class="timeabs"><g:enc>${execution.dateStarted}</g:enc></span></td>
                </tr>
                </g:if>
                <g:else>
                    <td>Started:</td>
                    <td>Just Now</td>
                </g:else>

            <g:if test="${null!=execution.dateCompleted}">
                    <tr>
                        <td>Finished:</td>
                        <td>
                            <g:relativeDate elapsed="${execution.dateCompleted}" agoClass="timeago"/>
                        </td>
                        <td><span class="timeabs"><g:enc>${execution.dateCompleted}</g:enc></span></td>
                    </tr>
                </g:if>
            </table>

                    </td>
                    <g:if test="${scheduledExecution}">
                        <td style="vertical-align:top;" class="toolbar small">
                            <g:render template="/scheduledExecution/renderJobStats"
                                      model="${[scheduledExecution: scheduledExecution]}"/>
                        </td>
                    </g:if>
                </tr>
            </table>
        </div>
        <span class="prompt">Details</span>
        <div class="presentation" id="schedExDetails${scheduledExecution?.id}">
            <g:render template="/execution/execDetails" model="[showEdit:false, execdata:execution,noimgs:true,nomatchednodes:true]"/>
        </div>
        %{--<g:set var="nodestatus" value="${[succeeded:20,failed:0,total:20]}"/>--}%
        <g:if test="${nodestatus || execution?.failedNodeList}">
            <span class="prompt">Nodes</span>
            <div class="presentation">
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
            </div>
        </g:if>
    </div>
</div>
<div class="foot">
    <g:appTitle/> :
    <g:enc>${execution.project}</g:enc>
    <g:link absolute="true" controller="framework" params="[project: execution.project]" action="nodes"><g:message code="gui.menu.Nodes"/> &raquo;</g:link>
    <g:link absolute="true" controller="menu" params="[project: execution.project]" action="jobs"><g:message code="gui.menu.Workflows"/> &raquo;</g:link>
    <g:link absolute="true" controller="reports" params="[project: execution.project]" action="index"><g:message code="gui.menu.Events"/> &raquo;</g:link>
</div>

</body>
</html>
