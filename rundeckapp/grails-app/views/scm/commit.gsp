<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 4/30/15
  Time: 3:29 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="configure"/>
    <meta name="layout" content="base"/>
    <title><g:appTitle/> - <g:message code="scmController.page.commit.title" args="[params.project]" /></title>

</head>

<body>

<div class="row">
    <div class="col-sm-12">
        <g:render template="/common/messages"/>
    </div>
</div>
<div class="row">
    <div class="col-sm-12 col-md-10 col-md-offset-1 col-lg-8 col-lg-offset-2">
        <g:form action="saveCommit"
                params="${[project:params.project]}"
                useToken="true"
                method="post" class="form form-horizontal">
            <div class="panel panel-primary" id="createform">
                <div class="panel-heading">
                    <span class="h3">
                        <g:message code="scmController.page.commit.description" default="SCM Export: Commit Changes"/>
                    </span>
                </div>
                <div class="list-group">
                    <div class="list-group-item">
                        <div class="row">
                            <div class="col-sm-12">
                                <div class="help-block">
                                    <g:message code="select.jobs.to.export" />
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="list-group-item">
                        <div class="form-group">
                        <g:each in="${jobs}" var="job">

                            <div class="checkbox col-sm-12">
                                <label>
                                    <g:set var="jobstatus" value="${scmStatus?.get(job.extid)}"/>
                                    <g:if test="${jobstatus?.synchState?.toString()!='CLEAN'}">

                                        <g:checkBox name="jobIds" value="${job.extid}" checked="${selected?.contains(job.extid)}"/>
                                    </g:if>
                                    <g:else>
                                    </g:else>

                                    <g:render template="statusIcon" model="[iscommit:true,status:jobstatus?.synchState?.toString(),notext:true,text:'',commit  : jobstatus?.commit]"/>
                                    <g:render template="statusIcon" model="[iscommit:true,status:jobstatus?.synchState?.toString(),noicon:true,text:job.jobName,commit  : jobstatus?.commit]"/>

                                    <span class="text-muted">
                                        - ${job.groupPath}
                                    </span>


                                </label>
                                <g:link action="diff" class="btn btn-xs btn-info" params="${[project:params.project,jobId:job.extid]}">
                                    <g:message code="button.View.Diff.title" />
                                </g:link>
                            </div>
                            <g:if test="${filesMap[job.extid]}">
                                <div class="col-sm-11 col-sm-offset-1">
                                    <span class="text-muted">
                                        <span class="glyphicon glyphicon-file"></span>
                                        ${filesMap[job.extid]}
                                    </span>
                                </div>
                            </g:if>

                        </g:each>
                        </div>
                        <g:if test="${jobs.size()>1}">
                            <div class=" row row-spacing">
                                <div class=" col-sm-12">
                                    <span class="textbtn textbtn-default" onclick="jQuery('input[name=jobIds]').prop('checked',true)">
                                        <g:message code="select.all" />
                                    </span>
                                &bull;
                                    <span class="textbtn textbtn-default" onclick="jQuery('input[name=jobIds]').prop('checked',false)">
                                        <g:message code="select.none" />
                                    </span>
                                </div>
                            </div>
                        </g:if>
                    </div>
                    <div class="list-group-item">
                        <g:each in="${properties}" var="prop">

                            <g:if test="${!prop.scope || prop.scope.isProjectLevel() || prop.scope.isUnspecified()}">
                                <g:render
                                        template="/framework/pluginConfigPropertyFormField"
                                        model="${[prop:prop,
                                                  prefix:'test',
                                                  error:nodeexecreport?.errors && isSelected ?nodeexecreport?.errors[prop.name]:null,
                                                  values: null,
                                                  fieldname:'commit.'+prop.name,
                                                  origfieldname:'orig.'+prop.name
                                        ]}"/>
                            </g:if>
                        </g:each>
                    </div>
                </div>

                <div class="panel-footer">
                    <g:submitButton name="create" value="${g.message(code: 'button.Commit.title', default: 'Commit')}"
                                    class="btn btn-default"/>
                </div>
            </div>
        </g:form>
    </div>
</div>
</body>
</html>