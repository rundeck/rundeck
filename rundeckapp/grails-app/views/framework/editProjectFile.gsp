%{--
  - Copyright 2015 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -        http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%
<%--
   Author: Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
--%>
<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="projectconfigure"/>
    <meta name="projtabtitle" content="${message(code:filename=='readme.md'?'project.readme.title':filename=='motd.md'?'project.motd.title':'edit.project.file')}"/>
    <meta name="projconfigselected" content="${(filename=='readme.md'?'edit-readme':filename=='motd.md'?'edit-motd':'edit.project.file')}"/>
    <title><g:message code="edit.project.file" /></title>

    <asset:javascript src="prototype/effects"/>
    <asset:javascript src="leavePageConfirm.js"/>
    <g:jsMessages code="page.unsaved.changes"/>
    <g:javascript>

    function init(){
        $$('input').each(function(elem){
            if(elem.type=='text'){
                elem.observe('keypress',noenter);
            }
        });
        var confirm = new PageConfirm(message('page.unsaved.changes'));
        jQuery('.apply_ace').each(function () {
            _setupAceTextareaEditor(this,confirm.setNeetsConfirm);
        });
    }
    jQuery(init);
    </g:javascript>
</head>

<body>
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
                <!-- TODO: This message mentions 'home page'. Is that accurate? Should this be changed to Dashboard? -->
                <g:markdown><g:message code="project.file.${filename}.help.markdown" default="Enter markdown"/></g:markdown>
              </div>
              <textarea name="fileText" class="form-control code apply_ace" data-ace-autofocus='true' data-ace-session-mode="markdown" data-ace-height="500px" data-ace-control-soft-wrap="true">${fileText}</textarea>
            </div>
            <div class="card-footer">
              <g:submitButton name="cancel" value="${g.message(code: 'button.action.Cancel', default: 'Cancel')}" class="btn btn-default reset_page_confirm"/>
              <g:submitButton name="save" value="${g.message(code: 'button.action.Save', default: 'Save')}" class="btn btn-primary reset_page_confirm"/>
              <g:if test="${displayConfig?.contains('none')}">
                <span class="text-warning text-right">
                  <g:set var="authAdmin" value="${auth.resourceAllowedTest( action: AuthConstants.ACTION_ADMIN, type: "project", name: (params.project ?: request.project), context: "application" )}"/>
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
<!--[if (gt IE 8)|!(IE)]><!--> <asset:javascript src="ace-bundle.js"/><!--<![endif]-->
</body>
</html>
