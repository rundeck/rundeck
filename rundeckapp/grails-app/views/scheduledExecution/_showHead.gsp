<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants; rundeck.ScheduledExecution" %>

<div class="col-sm-12 jobInfoSection">
<g:if test="${scheduledExecution.groupPath}">
    <section>
        <g:set var="parts" value="${scheduledExecution.groupPath.split('/')}"/>
        <g:each in="${parts}" var="part" status="i">
            <g:if test="${i != 0}">/</g:if>
            <g:set var="subgroup" value="${parts[0..i].join('/')}"/>
            <g:link controller="menu"
                    action="jobs"
                    class="secondary"
                    params="${[groupPath: subgroup, project: scheduledExecution.project]}"
                    title="${'View ' + g.message(code: 'domain.ScheduledExecution.title') + 's in this group'}"
                    absolute="${absolute ? 'true' : 'false'}">
                <g:if test="${i==0}"><g:if test="${!noimgs}"><b class="glyphicon glyphicon-folder-close"></b></g:if></g:if>
                <g:enc>${part}</g:enc></g:link>
        </g:each>
    </section>
</g:if>
<section class="${scheduledExecution.groupPath?'section-space':''}" id="jobInfo_">
    <span class=" h3">
        <g:link controller="scheduledExecution" action="show"
            class="primary"
            params="[project: scheduledExecution.project]"
                id="${scheduledExecution.extid}"
                absolute="${absolute ? 'true' : 'false'}">
            <i class="glyphicon glyphicon-book"></i>
            <g:enc>${scheduledExecution?.jobName}</g:enc></g:link>
    </span>

    <g:if test="${jobActionButtons}">
        <div class="btn-group">
            <button type="button" class="btn btn-sm btn-link btn-default dropdown-toggle"
                    data-toggle="dropdown"
                    aria-expanded="false">
                <i class="glyphicon glyphicon-list"></i>
                Action
                <span class="caret"></span>
            </button>
            <ul class="dropdown-menu" role="menu">
                <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_UPDATE)}">
                    <li>
                        <g:link controller="scheduledExecution" title="Edit or Delete this Job"
                                action="edit"
                                params="[project: scheduledExecution.project]"
                                id="${scheduledExecution.extid}" class="">
                            <i class="glyphicon glyphicon-edit"></i>
                            <g:message code="scheduledExecution.action.edit.button.label"/>
                        </g:link>
                    </li>
                    <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: [AuthConstants.ACTION_READ])}">
                        <g:if test="${auth.resourceAllowedTest(kind: 'job', action: AuthConstants.ACTION_CREATE, project: scheduledExecution.project)}">
                            <li>
                                <g:link controller="scheduledExecution" title="Duplicate Job"
                                        action="copy"
                                        params="[project: scheduledExecution.project]"
                                        id="${scheduledExecution.extid}" class="">
                                    <i class="glyphicon glyphicon-plus"></i>
                                    <g:message
                                            code="scheduledExecution.action.duplicate.button.label"/>
                                </g:link>
                            </li>
                        </g:if>
                        <auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_DELETE}" project="${scheduledExecution.project}">
                            <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_DELETE)}">
                                <li class="divider"></li>
                                <li>
                                    <a data-toggle="modal"
                                       href="#jobdelete"
                                       title="${g.message(code: 'delete.this.job')}">
                                        <b class="glyphicon glyphicon-remove-circle"></b>
                                        <g:message code="delete.action.label" />
                                    </a>
                                </li>
                            </g:if>
                        </auth:resourceAllowed>
                        <li class="divider"></li>
                        <li><g:link controller="scheduledExecution"
                                    title="${g.message(code: 'scheduledExecution.action.downloadformat.button.label', args: ['XML'])}"
                                    params="[project: scheduledExecution.project]"
                                    action="show"
                                    id="${scheduledExecution.extid}.xml">
                            <b class="glyphicon glyphicon-file"></b>
                            <g:message code="scheduledExecution.action.downloadformat.button.label"
                                       args="['XML']"/>
                        </g:link>
                        </li>
                        <li>
                            <g:link controller="scheduledExecution"
                                    title="${g.message(code: 'scheduledExecution.action.downloadformat.button.label', args: ['YAML'])}"
                                    params="[project: scheduledExecution.project]"
                                    action="show"
                                    id="${scheduledExecution.extid}.yaml">
                                <b class="glyphicon glyphicon-file"></b>
                                <g:message
                                        code="scheduledExecution.action.downloadformat.button.label"
                                        args="['YAML']"/>
                            </g:link>
                        </li>
                    </g:if>
                </g:if>
            </ul>
        </div>
        <auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_DELETE}"
                              project="${scheduledExecution.project ?: params.project ?: request.project}">
            <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_DELETE, project: scheduledExecution.project)}">
                <div class="modal" id="jobdelete" tabindex="-1" role="dialog"
                     aria-labelledby="deletejobtitle" aria-hidden="true">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <button type="button" class="close" data-dismiss="modal"
                                        aria-hidden="true">&times;</button>
                                <h4 class="modal-title" id="deletejobtitle">
                                    <g:message code="really.delete.this.job"/>
                                </h4>
                            </div>

                            <g:form controller="scheduledExecution" useToken="true" action="delete"
                                    method="post" class="form form-horizontal">
                                <div class="modal-body">
                                    <div class="form-group">
                                        <label class="col-sm-2 control-label">
                                            <g:message code="scheduledExecution.jobName.label" />
                                        </label>
                                        <div class="col-sm-10">
                                            <p class="form-control-static text-info"><g:enc>${scheduledExecution.jobName}</g:enc></p>
                                        </div>
                                    </div>

                                    <auth:resourceAllowed type="project"
                                                          name="${scheduledExecution.project}"
                                                          context="application"
                                                          action="${[AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN]}"
                                                          any="true">
                                        <div class="form-group">
                                            <div class="col-sm-10 col-sm-offset-2">
                                                <label class="checkbox">
                                                    <input type="checkbox" name="deleteExecutions"
                                                           value="true"/>
                                                    <g:message code="delete.all.executions.of.this.job"/>
                                                </label>
                                            </div>
                                        </div>
                                    </auth:resourceAllowed>

                                </div>

                                <div class="modal-footer">
                                    <g:hiddenField name="id" value="${scheduledExecution.extid}"/>
                                    <button type="submit" class="btn btn-default btn-sm "
                                            data-dismiss="modal">
                                        <g:message code="cancel" />
                                    </button>
                                    <input type="submit" value="${g.message(code:'Delete')}"
                                           class="btn btn-danger btn-sm"/>
                                </div>
                            </g:form>
                        </div>
                    </div>
                </div>
            </g:if>
        </auth:resourceAllowed>
    </g:if>
        <g:if test="${scheduledExecution.scheduled && nextExecution}">
            <span class="scheduletime">
                <i class="glyphicon glyphicon-time"></i>
                <g:set var="titleHint"
                       value="${remoteClusterNodeUUID ? g.message(code: "expecting.another.cluster.server.to.run") : ''}"/>
                <span title="${clusterUUID ? g.message(code: "expecting.another.cluster.server.to.run") : ''} at ${enc(attr:g.relativeDate(atDate:nextExecution))}">
                    <g:relativeDate elapsed="${nextExecution}"
                                    untilClass="timeuntil"/>
                </span>
            </span>
        </g:if>
        <g:elseif test="${scheduledExecution.scheduled && !nextExecution}">
            <span class="scheduletime willnotrun has_tooltip" data-toggle="tooltip"
                data-placement="auto left"
                  title="${g.message(code: 'job.schedule.will.never.fire')}">
                <i class="glyphicon glyphicon-time"></i>
                <span class="detail"><g:message code="never" /></span>
            </span>
        </g:elseif>

</section>
<section class="section-space">
        <g:render template="/scheduledExecution/description"
                  model="[description: scheduledExecution.description, textCss: 'h4 text-muted', mode: jobDescriptionMode ?: 'expanded', rkey: g.rkey()]"/>
</section>
</div>
