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
<div class="container" data-bind="attr: { 'data-project': projectName }, ">
    <div class="row">
        <div class="col-sm-6 col-md-4">
            <a href="${g.createLink(action: 'projectHome', controller: 'menu', params: [project: '<$>'])}"
               data-bind="urlPathParam: projectName"
               class="h1">
                <i class="glyphicon glyphicon-tasks"></i>
                <span data-bind="text: projectName"></span>
            </a>

            <span data-bind="if: project">
                <span class="text-muted" data-bind="text: project.description"></span>
            </span>
        </div>

        <div class="clearfix visible-sm"></div>

        <div class="col-sm-6 col-md-4">
            <span data-bind="if: project">
                <a
                        class="h4"
                        data-bind="css: { 'text-muted': project.execCount()<1 }, urlPathParam: projectName "
                        href="${g.createLink(controller: "reports", action: "index", params: [project: '<$>'])}">
                    <span class="summary-count "
                          data-bind="css: { 'text-muted': project.execCount()<1, 'text-info':project.execCount()>0 } ">
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


        <div class="clearfix visible-xs visible-sm"></div>

        <div data-bind="if: project">

            <div class="col-sm-12 col-md-4">
                <div class="pull-right">
                    <span data-bind="if: !project.loaded()">
                        <g:img file="spinner-gray.gif" width="24px" height="24px"/>
                    </span>
                    <span data-bind="if: project.auth().admin">
                        <a href="${g.createLink(controller: "menu", action: "admin", params: [project: '<$>'])}"
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

    <div data-bind="if: project">
        <div class="row row-space"
             data-bind="if: project.readme && (project.readme().readmeHTML || project.readme().motdHTML)">
            <div class="col-sm-12">
                <!-- ko if: project.readme().motdHTML() -->
                <div class="well well autoshow">

                    <span data-bind="if: project.auth().admin">
                        <a href="${g.createLink(
                                controller: "framework",
                                action: "editProjectFile",
                                params: [project: '<$>', filename: 'motd.md']
                        )}"
                           data-bind="urlPathParam: projectName"
                           class="btn btn-link btn-sm pull-right autohide">
                            <g:icon name="pencil"/>
                            <g:message code="button.Edit.label" />
                        </a>
                    </span>
                    <span data-bind="html: project.readme().motdHTML()"></span>
                </div>
                <!-- /ko -->

                <!-- ko if: project.readme().readmeHTML() -->
                <div class="well well well-nobg autoshow">
                <span data-bind="if: project.auth().admin">
                    <a href="${g.createLink(
                            controller: "framework",
                            action: "editProjectFile",
                            params: [project: '<$>', filename: 'readme.md']
                    )}"
                       data-bind="urlPathParam: projectName"
                       class="btn btn-link btn-sm pull-right autohide">
                        <g:icon name="pencil"/>
                        <g:message code="button.Edit.label" />
                    </a>
                </span>
                    <span data-bind="html: project.readme().readmeHTML()"></span>
                </div>
                <!-- /ko -->

            </div>
        </div>
    </div>
</div>