<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title>%{--
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

<g:appTitle/> - <g:message code="userController.page.edit.title"/></title>

</head>

<body>
<div class="content">
<div id="layoutBody">
  <div class="container-fluid">
    <div class="row">
        <div class="col-xs-12">
          <div class="card">
            <g:form action="update" class="form form-horizontal" useToken="true">
              <div class="card-header">
                  <h3 class="card-title"><g:message code="userController.page.edit.title"/></h3>
              </div>
              <div class="card-content">
                <g:render template="/common/messages"/>
                <tmpl:edit user="${user}"/>
              </div>
              <div class="card-footer">
                <hr>
                <g:actionSubmit id="editFormCancelButton" value="${g.message(code:'button.action.Cancel',default:'Cancel')}" class="btn btn-default"/>
                <g:submitButton name="Update" value="${g.message(code:'button.action.Update',default:'Update')}" class="btn btn-cta"/>
              </div>
            </g:form>
          </div>
        </div>
    </div>
  </div>
</div>
</div>
</body>
</html>
