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

<g:appTitle/> - <g:message code="userController.page.create.title" /></title>

</head>

<body>


<div class="row">
    <div class="col-sm-offset-1 col-sm-10">
        <g:form action="store" class="form form-horizontal" useToken="true">
            <div class="panel panel-primary">
                <div class="panel-heading">
                    <h3 class="panel-title"><g:message code="userController.page.create.title" /></h3>
                </div>

                <div class="panel-body">
                    <g:render template="/common/messages"/>
                    <tmpl:edit user="${user}"/>
                </div>

                <div class="panel-footer">

                    <g:actionSubmit id="editFormCancelButton" value="Cancel" class="btn btn-default"/>
                    <g:submitButton name="Create" class="btn btn-primary"/>

                </div>
            </div>
        </g:form>
    </div>
</div>
</body>
</html>


