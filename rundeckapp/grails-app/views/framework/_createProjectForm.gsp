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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <g:set var="rkey" value="${g.rkey()}"/>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="nodes"/>
    <title><g:message code="domain.Project.choose.title" default="Create a Project"/></title>

    <asset:javascript src="prototype/effects"/>
    <asset:javascript src="storageBrowseKO.js"/>
    <g:javascript>

    function init(){
        $$('input').each(function(elem){
            if(elem.type=='text'){
                elem.observe('keypress',noenter);
            }
        });
    }
    jQuery(init);
    </g:javascript>
    <style type="text/css">
    #configs li {
        margin-top: 5px;
    }

    </style>
</head>

<body>
<g:set var="adminauth"
       value="${auth.resourceAllowedTest(type: 'resource', kind: 'project', action: ['create'], context: 'application')}"/>
<g:if test="${adminauth}">
  <div class="container-fluid">
    <div class="row">
      <div class="col-xs-12">
        <div class="card" id="createform">
          <g:form action="createProject" useToken="true" method="post" onsubmit="return configControl.checkForm();">
            <div class="card-header">
              <h4 class="card-title"><g:message code="domain.Project.create.message" default="Create a new Project"/></h4>
            </div>
            <div class="card-content">
              <tmpl:editProjectForm/>
            </div>
            <div class="card-footer">
              <g:submitButton name="create" value="${g.message(code: 'button.action.Create', default: 'Create')}" class="btn btn-default"/>
            </div>
          </div>
        </g:form>
      </div>
    </div>
    <g:render template="storageBrowseModalKO"/>
  </div>
</g:if>
<g:else>
    <div class="pageBody">
        <div class="error note"><g:message code="unauthorized.project.create"/></div>
    </div>
</g:else>
</body>
</html>
