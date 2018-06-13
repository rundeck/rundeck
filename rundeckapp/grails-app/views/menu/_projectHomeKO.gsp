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

%{--Template for project details--}%
<div data-bind="attr: { 'data-project': projectName }, ">
    <!-- ko if: project.showMotd() -->
    <div class="row">
      <div class="col-xs-12">
        <div class="card">
          <div class="card-content">
            <span data-bind="html: project.readme().motdHTML()"></span>
          </div>
        </div>
      </div>
    </div>
    <!-- /ko -->
    <div class="row">
        <div class="col-xs-12">
          <div class="card">
            <div class="card-content">
              <div class="row">
                <div class="col-xs-12 col-md-8">
                  <a href="${g.createLink(action: 'projectHome', controller: 'menu', params: [project: '<$>'])}"
                     data-bind="urlPathParam: projectName"
                     class="h2">
                      <span data-bind="if: project && project.label">
                          <span data-bind="text: project.label"></span>
                      </span>
                      <span data-bind="ifnot: project && project.label">
                          <span data-bind="text: projectName"></span>
                      </span>
                  </a>
                </div>
                <div class="col-xs-12 col-md-4">
                  <div data-bind="if: project">

                          <div class="pull-right">
                              <span data-bind="if: !project.loaded()">
                                  <g:img class="loading-spinner" file="spinner-gray.gif" width="24px" height="24px"/>
                              </span>
                              <span data-bind="if: project.auth().admin">
                                  <a href="${g.createLink(controller: "framework", action: "editProject", params: [project: '<$>'])}"
                                     data-bind="urlPathParam: projectName"
                                     class="btn btn-default btn-sm">
                                      <g:message code="gui.menu.Admin"/>
                                  </a>
                              </span>

                              <div class="btn-group " data-bind="if: project.auth().jobCreate">

                                  <button type="button" class="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown">
                                      <g:message code="create.job.button.label"/>
                                      <span class="caret"></span>
                                  </button>
                                  <ul class="dropdown-menu pull-right" role="menu">
                                      <li><a href="${g.createLink(
                                              controller: "scheduledExecution",
                                              action: "create",
                                              params: [project: '<$>']
                                      )}"
                                             data-bind="urlPathParam: projectName">
                                          <i class="glyphicon glyphicon-plus"></i>
                                          <g:message code="new.job.button.label"/>

                                      </a>
                                      </li>
                                      <li class="divider">
                                      </li>
                                      <li>
                                          <a href="${g.createLink(
                                                  controller: "scheduledExecution",
                                                  action: "upload",
                                                  params: [project: '<$>']
                                          )}"
                                             data-bind="urlPathParam: projectName"
                                             class="">
                                              <i class="glyphicon glyphicon-upload"></i>
                                              <g:message code="upload.definition.button.label"/>
                                          </a>
                                      </li>
                                  </ul>
                              </div>
                          </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="card" data-bind="if: project.description">
            <div class="card-content">
                <span class="text-primary" data-bind="text: project.description"></span>
            </div>
          </div>
          <div class="row">
            <div class="col-xs-12 col-md-6">
              <div class="card">
                <div class="card-content">
                      <span data-bind="if: project">
                          <a
                                  class="h4"
                                  data-bind="css: { 'text-primary': project.execCount()<1 }, urlPathParam: projectName "
                                  href="${g.createLink(controller: "reports", action: "index", params: [project: '<$>'])}">
                              <span class="summary-count "
                                    data-bind="css: { 'text-primary': project.execCount()<1, 'text-info':project.execCount()>0 } ">
                                  <span data-bind="text: project.loaded()?project.execCount():''"></span>
                                  <span data-bind="if: !project.loaded()">...</span>
                              </span>
                              <span data-bind="messageTemplate: project.execCount(), messageTemplatePluralize: true">
                                  <g:message code="Execution"/>|<g:message code="Execution.plural"/>
                              </span>
                              <g:message code="page.home.duration.in.the.last.day"/></a>

                          <span data-bind="if: project.failedCount()>0">
                              <a data-bind="urlPathParam: projectName "
                                 class="text-warning"
                                 href="${g.createLink(
                                         controller: "reports",
                                         action: "index",
                                         params: [project: '<$>', statFilter: 'fail']
                                 )}">
                                  <span data-bind="messageTemplate: project.failedCount()">
                                      <g:message code="page.home.project.executions.0.failed.parenthetical"/>
                                  </span>
                              </a>
                          </span>

                          <div>
                              <div data-bind="if: project.userCount()>0">
                                  <g:message code="by"/>
                                  <span class="text-info" data-bind="text: project.userCount()">
                                  </span>

                                  <span data-bind="messageTemplate: project.userCount(),messageTemplatePluralize:true">
                                      <g:message code="user"/>:|<g:message code="user.plural"/>:
                                  </span>

                                  <span data-bind="text: project.userSummary().join(', ')">

                                  </span>
                              </div>
                          </div>
                      </span>
                </div>
              </div>
            </div>
          </div>


        </div>



    </div>

    <div data-bind="if: project">
        <div class="row row-space"
             data-bind="if: project.showMessage() ">
            <div class="col-sm-12">
                <!-- ko if: project.showReadme() -->
                <div class="card">
                  <div class="card-content">
                    <span data-bind="html: project.readme().readmeHTML()"></span>
                  </div>
                </div>
                <!-- /ko -->
            </div>
        </div>
    </div>
</div>
