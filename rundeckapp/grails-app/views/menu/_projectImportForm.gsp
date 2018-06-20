%{--
  - Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>


<div class="row">
  <div class="col-xs-12">
    <g:form controller="project" action="importArchive" params="[project: params.project ?: request.project]" useToken="true" enctype="multipart/form-data" class="form">
      <div class="card" id="importform">
        <div class="card-header">
          <h3 class="card-title"><g:message code="import.archive"/></h3>
        </div>
        <div class="card-content">
          <div class="list-group list-group-tab-content">
            <div class="list-group-item">
              <div class="form-group">
                <label>
                  <g:message code="choose.a.rundeck.archive"/>
                  <input type="file" name="zipFile" class="form-control"/>
                </label>
                <p class="help-block">
                  <g:message code="archive.import.help"/>
                </p>
              </div>
            </div>
            <div class="list-group-item">
              <h4 class="list-group-item-heading"><g:message code="imported.jobs"/></h4>
              <div class="radio">
                <input type="radio" name="jobUuidOption" value="preserve" checked/>
                <label title="Original UUIDs will be preserved, conflicting UUIDs will be replaced">
                    <g:message code="project.archive.import.jobUuidOption.preserve.label"/>
                </label>
                <p class="help-block"><g:message code="project.archive.import.jobUuidOption.preserve.description"/></p>
              </div>
              <div class="radio">
                <input type="radio" name="jobUuidOption" value="remove"/>
                  <label title="New UUIDs will be generated for every imported Job">
                      <g:message code="project.archive.import.jobUuidOption.remove.label"/>
                  </label>
                  <p class="help-block"><g:message code="project.archive.import.jobUuidOption.remove.description"/></p>
                </div>
            </div>
            <div class="list-group-item">
                <h4 class="list-group-item-heading"><g:message code="Execution.plural"/></h4>
                <div class="radio">
                  <input type="radio" name="importExecutions" value="true" checked/>
                  <label title="All executions and reports will be imported">
                      <g:message code="archive.import.importExecutions.true.title"/>
                  </label>
                  <span class="help-block"><g:message code="archive.import.importExecutions.true.help"/></span>
                </div>
                <div class="radio">
                  <input type="radio" name="importExecutions" value="false"/>
                  <label title="No executions or reports will be imported">
                    <g:message code="archive.import.importExecutions.false.title"/>
                  </label>
                  <span class="help-block"><g:message code="archive.import.importExecutions.false.help"/></span>
                </div>
            </div>
            <div class="list-group-item">
                <h4 class="list-group-item-heading">Configuration</h4>
                <div class="radio">
                  <input type="radio" name="importConfig" value="true" checked/>
                  <label title="">
                    <g:message code="archive.import.importConfig.true.title"/>
                  </label>
                  <span class="help-block">
                      <g:message code="archive.import.importConfig.true.help"/>
                  </span>
                </div>
                <div class="radio">
                  <input type="radio" name="importConfig" value="false"/>
                  <label title="">
                    <g:message code="archive.import.importExecutions.false.title"/>
                  </label>
                  <span class="help-block">
                      <g:message code="archive.import.importConfig.false.help"/>
                  </span>
                </div>
            </div>
            <auth:resourceAllowed action="${[AuthConstants.ACTION_CREATE, AuthConstants.ACTION_ADMIN]}" any="true" context='application' type="project_acl" name="${params.project}">
              <div class="list-group-item">
                <h4 class="list-group-item-heading">ACL Policies</h4>
                  <div class="radio">
                    <input type="radio" name="importACL" value="true" checked/>
                    <label title="">
                      <g:message code="archive.import.importACL.true.title"/>
                    </label>
                    <span class="help-block">
                      <g:message code="archive.import.importACL.true.help"/>
                    </span>
                  </div>
                  <div class="radio">
                    <input type="radio" name="importACL" value="false"/>
                    <label title="">
                      <g:message code="archive.import.importExecutions.false.title"/>
                    </label>
                    <span class="help-block">
                      <g:message code="archive.import.importACL.false.help"/>
                    </span>
                  </div>
              </div>
            </auth:resourceAllowed>
            <auth:resourceAllowed action="${[AuthConstants.ACTION_CREATE, AuthConstants.ACTION_ADMIN]}" any="true" context='application' type="project_acl" has="false" name="${params.project}">
              <div class="list-group-item">
                <h4 class="list-group-item-heading">ACL Policies</h4>
                <span class="help-block">
                  <i class="glyphicon glyphicon-ban-circle"></i>
                  <g:message code="archive.import.importACL.unauthorized.help"/>
                </span>
              </div>
            </auth:resourceAllowed>
          </div>
        </div>
        <div class="card-footer">
          <div id="uploadFormButtons">
            <g:submitButton name="cancel" value="${g.message(code:'button.action.Cancel',default:'Cancel')}" class="btn btn-default"/>
            <g:actionSubmit action="importArchive" value="${g.message(code:'button.action.Import',default:'Import')}" id="uploadFormUpload" onclick="['uploadFormButtons','importUploadSpinner'].each(Element.toggle)" class="btn btn-primary"/>
          </div>
          <div id="importUploadSpinner" class="spinner block" style="display:none;">
            <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/>
            <g:message code="uploading.file"/>
          </div>
        </div>
      </div>
    </g:form>
  </div>
</div>
