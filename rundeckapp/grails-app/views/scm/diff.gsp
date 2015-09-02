<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 8/27/15
  Time: 3:54 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="configure"/>
    <meta name="layout" content="base"/>
    <title><g:appTitle/> - <g:message code="scmController.page.diff.title" args="[params.project]"/></title>

</head>

<body>

<div class="row">
    <div class="col-sm-12">
        <g:render template="/common/messages"/>
    </div>
</div>

<div class="row">
    <div class="col-sm-12">
        <div class="list-group">
            <div class="list-group-item">
                <h4 class="list-group-item-heading"><g:message code="scmController.page.diff.description"/></h4>
            </div>

            <div class="list-group-item">
                <div class="row">
                    <g:render template="/scheduledExecution/showHead" model="[scheduledExecution: job]"/>
                </div>
            </div>

            <div class="list-group-item">
                <g:set var="jobstatus" value="${scmStatus?.get(job.extid)}"/>
                <g:render template="/scm/statusBadge" model="[
                        status: jobstatus?.synchState?.toString(),
                        text  : '',
                        commit: jobstatus?.commit,
                ]"/>
                <g:if test="${scmExportRenamedPath}">
                    <div>
                        <span class="has_tooltip text-muted" title="Original repo path">
                            <g:icon name="file"/>
                            ${scmExportRenamedPath}
                        </span>
                    </div>
                </g:if>
                <span class="has_tooltip" title="Repo file path">
                    <g:if test="${scmExportRenamedPath}">
                        <g:icon name="arrow-right"/>
                    </g:if>

                    <g:icon name="file"/>
                    ${scmFilePaths[job.extid]}
                </span>
            </div>
            <g:if test="${jobstatus?.commit}">
                <div class="list-group-item">
                    <g:expander key="commitInfo">Previous Commit</g:expander>
                    <table class="table table-bordered table-condensed table-striped " id="commitInfo"
                           style="display:none">
                        <g:set var="map" value="${jobstatus?.commit.asMap()}"/>
                        <g:each in="${map.keySet().sort()}" var="key">
                            <tr>
                                <td>${key}</td>
                                <td>${map[key]}</td>
                            </tr>
                        </g:each>
                    </table>
                </div>
            </g:if>


            <g:if test="${diffResult.oldNotFound}">

                <div class="list-group-item">
                    <div class="list-group-item-text text-info">
                        <g:message code="not.added.to.scm"/>
                    </div>
                </div>
            </g:if>
            <g:elseif test="${!diffResult.modified}">

                <div class="list-group-item">
                    <div class="list-group-item-text text-muted">
                        <g:message code="no.changes"/>
                    </div>
                </div>
            </g:elseif>
            <g:elseif test="${diffResult.content}">

                <div id="difftext"
                     class="list-group-item scriptContent expanded apply_ace"
                     data-ace-session-mode="diff">${diffResult.content}</div>
            </g:elseif>
            <g:if test="${diffResult.modified || diffResult.oldNotFound}">
                <g:link action="commit" controller="scm"
                        class="list-group-item ${diffResult.oldNotFound ? 'list-group-item-success' :
                                'list-group-item-info'}"
                        params="[project: params.project, jobIds: job.extid]">

                    <i class="glyphicon glyphicon-circle-arrow-right"></i>
                    <g:if test="${diffResult.oldNotFound}">
                        Commit new File
                    </g:if>
                    <g:else>
                        <g:message code="button.Commit.Changes.title"/>
                    </g:else>
                </g:link>
            </g:if>
        </div>
    </div>
</div>

<!--[if (gt IE 8)|!(IE)]><!--> <g:javascript library="ace/ace"/><!--<![endif]-->
<g:javascript>
    fireWhenReady('difftext', function (z) {
        jQuery('.apply_ace').each(function () {
            _applyAce(this, '400px');
        });
    });
</g:javascript>
</body>
</html>