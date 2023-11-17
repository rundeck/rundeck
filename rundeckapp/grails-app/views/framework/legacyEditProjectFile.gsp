<%--
  Created by IntelliJ IDEA.
  User: chrismcg
  Date: 11/14/23
  Time: 12:19 PM
--%>

<div class="content">
    <div id="layoutBody">
        <div class="title">
            <span class="text-h3">
                <g:if test="${filename?.equals('readme.md')}">
                    <i class="fas fa-file-alt"></i> ${g.message(code:"edit.readme")}
                </g:if>
                <g:else>
                    <i class="fas fa-comment-alt"></i> ${g.message(code:"edit.message.of.the.day")}
                </g:else>
            </span>
        </div>
        <div class="container-fluid">
            <div class="row">
                <div class="col-sm-12">
                    <g:render template="/common/messages"/>
                </div>
            </div>
            <div class="row">
                <g:form action="saveProjectFile" method="post" params="${[project:params.project]}" useToken="true" onsubmit="" class="form">
                    <g:hiddenField name="filename" value="${filename}"/>
                    <div class="col-xs-12">
                        <div class="card"  id="createform">
                            <div class="card-header">
                                <h3 class="card-title">
                                    <g:message code="project.file.${filename}.edit.message" default="Edit {0} for project {1}" args="${[filename,params.project ?: request.project]}"/>
                                </h3>
                            </div>
                            <div class="card-content">
                                <div class="help-block">
                                    <details class="details-reset more-info">
                                        <summary>
                                            <g:message code="project.file.${filename}.help.markdown.summary" default="Enter markdown"/>
                                            <span class="more-indicator-verbiage more-info-icon"><g:icon name="chevron-right"/></span>
                                            <span class="less-indicator-verbiage more-info-icon"><g:icon name="chevron-down"/></span>
                                        </summary>
                                        <g:markdown><g:message code="project.file.${filename}.help.markdown" default="Enter markdown"/></g:markdown>
                                    </details>
                                </div>
                                <textarea name="fileText" class="form-control code apply_ace" data-ace-autofocus='true' data-ace-session-mode="markdown" data-ace-height="500px" data-ace-control-soft-wrap="true">${fileText}</textarea>
                            </div>
                            <div class="card-footer">
                                <g:submitButton name="cancel" value="${g.message(code: 'button.action.Cancel', default: 'Cancel')}" class="btn btn-default reset_page_confirm"/>
                                <g:submitButton name="save" value="${g.message(code: 'button.action.Save', default: 'Save')}" class="btn btn-cta reset_page_confirm"/>
                                <g:if test="${displayConfig?.contains('none')}">
                                    <span class="text-warning text-right">
                                        <g:set var="authAdmin" value="${auth.resourceAllowedTest( action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN], type: AuthConstants.TYPE_PROJECT, name: (params.project ?: request.project), context: AuthConstants.CTX_APPLICATION )}"/>
                                        <g:if test="${authAdmin}">
                                            <g:message code="project.edit.readme.warning.not.displayed.admin.message" />
                                            <g:link controller="framework" action="editProject" params="[project: params.project]">
                                                <g:message code="project.configuration" />
                                            </g:link>
                                        </g:if>
                                        <g:else>
                                            <g:message code="project.edit.readme.warning.not.displayed.nonadmin.message" />
                                        </g:else>
                                    </span>
                                </g:if>
                            </div>
                        </div>
                    </div>
                </g:form>
            </div>
        </div>
    </div>
</div>
<!--[if (gt IE 8)|!(IE)]><!--> <asset:javascript src="ace-bundle.js"/><!--<![endif]-->