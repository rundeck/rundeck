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

<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 9/7/17
  Time: 10:36 AM
--%>

<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="projectconfigure"/>
    <meta name="projtabtitle" content="${message(code: 'edit.nodes.file')}"/>
    <title><g:message code="edit.nodes.file"/></title>

    <asset:javascript src="prototype/effects"/>
    <asset:javascript src="leavePageConfirm.js"/>
    <g:jsMessages code="page.unsaved.changes"/>
    <g:javascript>

        function init() {
            $$('input').each(function (elem) {
                if (elem.type == 'text') {
                    elem.observe('keypress', noenter);
                }
            });
            var confirm = new PageConfirm(message('page.unsaved.changes'));
            jQuery('.apply_ace').each(function () {
                _setupAceTextareaEditor(this, confirm.setNeetsConfirm);
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
    <div class="col-xs-12">
      <div class="card">
        <g:form action="saveProjectNodeSourceFile" method="post"
                params="${[project: params.project, index: index]}"
                useToken="true"
                class="form-horizontal">
            <div class="">
                <div class="" id="createform">
                    <div class="card-header">
                      <h3 class="card-title">
                        <g:message code="edit.nodes.file"/>
                      </h3>
                      <p class="category">${sourceDesc}</p>
                    </div>

                    <div class="card-content">

                        <div class="form-group">
                            <label class="control-label col-sm-2">
                                <g:message code="project.node.file.source.label"/>
                            </label>

                            <div class="col-sm-10">
                                <p class="form-control-static">
                                    ${index}.
                                    <g:if test="${providerDesc}">
                                        <g:render template="/framework/renderPluginDesc" model="${[
                                                serviceName: 'ResourceModelSource',
                                                description: providerDesc,
                                        ]}"/>
                                    </g:if>
                                </p>
                            </div>
                        </div>


                        <div class="form-group">
                            <label class="control-label  col-sm-2">
                                <g:message code="file.display.format.label"/>
                            </label>

                            <div class="col-sm-10">
                                <p class="form-control-static"><code>${fileFormat}</code></p>
                            </div>
                        </div>
                        <g:if test="${sourceDesc}">
                        <div class="form-group">
                            <label class="control-label  col-sm-2">
                                <g:message code="project.node.file.source.description.label" />
                            </label>

                            <div class="col-sm-10">
                                <p class="form-control-static text-info">${sourceDesc}</p>
                            </div>
                        </div>
                        </g:if>
                        <textarea
                                name="fileText"
                                class="form-control code apply_ace"
                                data-ace-autofocus='true'
                                data-ace-session-mode="${fileFormat}"
                                data-ace-height="500px"
                                data-ace-control-syntax="${fileFormat ? 'false' : 'true'}"
                                data-ace-control-soft-wrap="true">${fileText}</textarea>
                        <g:if test="${saveError}">
                            <h3><g:message code="project.nodes.edit.save.error.message" /></h3>
                            <div class="text-warning">${saveError}</div>
                        </g:if>
                        <g:if test="${fileEmpty}">
                            <div class="text-warning"><g:message code="project.nodes.edit.empty.description" /></div>
                        </g:if>
                    </div>


                    <div class="card-footer">
                        <g:submitButton name="cancel" value="${g.message(code: 'button.action.Cancel', default: 'Cancel')}" class="btn btn-default reset_page_confirm"/>
                        <g:submitButton name="save" value="${g.message(code: 'button.action.Save', default: 'Save')}" class="btn btn-primary reset_page_confirm"/>
                    </div>
                </div>
            </div>
        </g:form>
      </div>
    </div>

  </div>

</div>
<!--[if (gt IE 8)|!(IE)]><!--> <asset:javascript src="ace-bundle.js"/><!--<![endif]-->
</body>
</html>
