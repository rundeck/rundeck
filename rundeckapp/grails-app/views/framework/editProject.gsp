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
   chooseProject.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: Dec 29, 2010 6:28:51 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <g:set var="rkey" value="${g.rkey()}"/>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="projectconfigure"/>
    <meta name="projtabtitle" content="${message(code:'configuration')}"/>
    <meta name="projconfigselected" content="edit-project"/>
    <title><g:message code="edit.configuration" /></title>

    <asset:javascript src="prototype/effects"/>
    <asset:javascript src="leavePageConfirm.js"/>
    <asset:javascript src="storageBrowseKO.js"/>
    <g:jsMessages code="page.unsaved.changes"/>
    <g:javascript>

    var confirm = new PageConfirm(message('page.unsaved.changes'));
    function init(){
        $$('input').each(function(elem){
            if(elem.type=='text'){
                elem.observe('keypress',noenter);
            }
        });
    }
    var _storageBrowseSelected=confirm.setNeedsConfirm;
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
    <g:form action="saveProject" method="post" useToken="true" onsubmit="return configControl.checkForm();" class="form">
    <div class="col-xs-12">
      <div class="card"  id="createform">
        <div class="card-header">
          <h3 class="card-title">
            <g:message code="domain.Project.edit.message" default="Configure Project"/>: <g:enc>${params.project ?: request.project}</g:enc>
            <g:link controller="framework" action="editProjectConfig"
              params="[project: params.project ?: request.project]"
              class="has_tooltip pull-right btn btn-sm"
              data-placement="bottom"
              title="${message(
                      code: 'page.admin.EditProjectConfigFile.title',
                      default: 'Advanced: Edit config file directly'
              )}">
              <!-- <g:icon name="file"/> -->
              <g:message code="page.admin.EditProjectConfigFile.button" default="Edit Configuration File"/>
            </g:link>
          </h3>
        </div>
        <div class="card-content">
          <g:render template="editProjectForm" model="${[editOnly:true,project: params.project ?: request.project]}"/>
        </div>
        <div class="card-footer">
          <g:submitButton name="cancel" value="${g.message(code:'button.action.Cancel',default:'Cancel')}" class="btn btn-default reset_page_confirm"/>
          <g:submitButton name="save" value="${g.message(code:'button.action.Save',default:'Save')}" class="btn btn-primary reset_page_confirm"/>
        </div>
      </div>
    </div>
    </g:form>
  </div>
  <g:render template="storageBrowseModalKO"/>
</div>
</body>
</html>
