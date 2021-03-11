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
  Date: 9/6/17
  Time: 9:39 AM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <g:set var="rkey" value="${g.rkey()}"/>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="projectconfigure"/>
    <meta name="projtabtitle" content="${message(code:'project.node.sources.title')}"/>
    <title>Project Nodes</title>

    <asset:javascript src="prototype/effects"/>
    <asset:javascript src="resourceModelConfig.js"/>
    <asset:javascript src="leavePageConfirm.js"/>
    <asset:javascript src="storageBrowseKO.js"/>
    <g:jsMessages code="page.unsaved.changes"/>
    <g:javascript>

    var configControl;
    var confirm = new PageConfirm(message('page.unsaved.changes'));
    function init(){
        configControl=new ResourceModelConfigControl('${enc(js:prefixKey)}',confirm.setNeedsConfirm);
        configControl.pageInit();
        jQuery('input').each(function(elem){
            if(elem.type=='text'){
                elem.observe('keypress',noenter);
            }
        });
        jQuery('.apply_ace').each(function () {
            _setupAceTextareaEditor(this, confirm.setNeetsConfirm);
        });
        }
    var _storageBrowseSelected=confirm.setNeedsConfirm;
    jQuery(init);
    </g:javascript>
</head>

<body>
<div class="content">
<div id="layoutBody">
  <div class="container-fluid">
    <div class="row">
      <div class="col-sm-12">
          <g:render template="/common/messages"/>
      </div>
    </div>
    <div class="row">
      <g:form action="saveProjectNodeSources" method="post" useToken="true" onsubmit="return configControl.checkForm();" class="form">
        <div class="col-xs-12">
          <div class="card"  id="createform">
            <div class="card-header">
              <h3 class="card-title">
                <g:message code="project.node.sources.title" default="Node Sources"/>: <g:enc>${params.project ?: request.project}</g:enc>
              </h3>
            </div>
            <div class="card-content">
              <div class="list-group">
                <g:hiddenField name="project" value="${project}"/>
                  <%--Render project configuration settings for 'resourceModelSource'--%>
                  <div class="list-group-item">
                  <g:render template="projectConfigurableForm"
                            model="${[extraConfigSet: extraConfig?.values(),
                                      category      : 'resourceModelSource',
                                      categoryPrefix     : 'extra.category.resourceModelSource.',
                                      titleCode     : 'project.configuration.extra.category.resourceModelSource.title',
                                      helpCode      : 'project.configuration.extra.category.resourceModelSource.description'
                            ]}"/>
                    </div>
                  <g:if test="${resourceModelConfigDescriptions}">
                    <div class="list-group-item">
                      <div class="help-block">
                        <g:message code="domain.Project.edit.ResourceModelSource.explanation" />
                      </div>
                      <div class="alert alert-warning" id="errors" style="display:none;"></div>
                      <ol id="configs" >
                        <g:if test="${configs}">
                          <g:each var="config" in="${configs}" status="n">
                            <li>
                              <div class="inpageconfig">
                                <g:set var="desc" value="${resourceModelConfigDescriptions.find {it.name==config.type}}"/>
                                <g:if test="${!desc}">
                                  <span class="warn note invalidProvider">Invalid Resource Model Source configuration: Provider not found: <g:enc>${config.type}</g:enc></span>
                                </g:if>
                                <g:render template="viewResourceModelConfig" model="${[prefix: prefixKey+'.'+(n+1)+'.', values: config.props, includeFormFields: true, description: desc, saved:true,type:config.type]}"/>
                                  <g:set var="writeableSource" value="${writeableSources.find { it.index == (n+1) }}"/>
                              </div>
                            </li>
                          </g:each>
                        </g:if>
                      </ol>

                        <div>
                            <a class="btn btn-cta btn-sm" data-toggle="modal" href="#sourcepickermodal">
                          <g:message code="project.resource.model.add.source.button.title" />
                          <i class="glyphicon glyphicon-plus"></i>
                            </a>
                      </div>
                        <g:render template="/common/modal" model="${[modalid  : 'sourcepickermodal',
                                                                     modalsize: 'modal-lg',
                                                                     title    : message(code: "framework.service.ResourceModelSource.add.title"),
                                                                     buttons  : []]}">
                            <div>
                                <div class="help-block">
                                    <g:message code="framework.service.ResourceModelSource.add.modal.description" />
                                </div>
                              <div class="list-group">
                                  <g:each in="${resourceModelConfigDescriptions}" var="description">
                                      <a onclick="configControl.addConfig('${enc(js: description.name)}');
                                      return false;"
                                         data-dismiss="modal"
                                         href="#"
                                         class="list-group-item">
                                          <i class="glyphicon glyphicon-plus text-muted"></i>
                                          <strong>

                                              <g:render template="/framework/renderPluginDesc" model="${[
                                                      serviceName    : 'ResourceModelSource',
                                                      description    : description,
                                                      showPluginIcon : true,
                                                      hideDescription: true
                                              ]}"/>
                                          </strong>
                                          <span class="help-block"><g:enc>${description.description}</g:enc></span>
                                      </a>
                                  </g:each>
                              </div>
                          </div>
                        </g:render>
                    </div>

                  </g:if>
              </div>
            </div>
                    <div class="card-footer">
                        <g:submitButton name="cancel" value="${g.message(code:'button.action.Cancel',default:'Cancel')}" class="btn btn-default reset_page_confirm"/>
                        <g:submitButton name="save" value="${g.message(code:'button.action.Save',default:'Save')}" class="btn btn-cta reset_page_confirm"/>
                    </div>
                </div>
            </div>
        </g:form>
    </div>

    <g:render template="storageBrowseModalKO"/>

    <div class="modal" id="deletenodesource" tabindex="-1" role="dialog" aria-labelledby="deletenodesourcetitle"
         aria-hidden="true">
        <g:form useToken="true" action="deleteProjectNodesource">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                        <h4 class="modal-title" id="deletenodesourcetitle">
                            Delete Node Source
                        </h4>
                    </div>

                    <div class="modal-body" >
                        Delete the Node Source?
                        <g:hiddenField id="deleteIndex" name="index" value=""/>
                        <g:hiddenField id="deleteProject" name="project" value="${project}"/>
                    </div>

                    <div class="modal-footer">
                        <button type="button" class="btn btn-sm btn-default" data-dismiss="modal"><g:message code="cancel" /></button>
                        <button type="submit" class="btn btn-sm btn-success obs-deletenodesource-confirm">
                            Delete
                        </button>
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
