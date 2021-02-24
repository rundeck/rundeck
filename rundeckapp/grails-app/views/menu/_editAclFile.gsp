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


<div class="card" id="createform">
    <div class="card-header">
        <h3 class="card-title">
            <g:if test="${backHref}">
                <a href="${backHref}" class="btn btn-xs btn-simple"
                   title="${titleText ?: g.message(code: 'navigation.back.title')}"><g:icon name="arrow-left"/></a>
            </g:if>
            ${title}
            <div class="buttons pull-right">
              <div class="dropdown">
                  <bs:dropdownToggle id="aclHelp" css="btn btn-default btn-sm">Help</bs:dropdownToggle>

                  <bs:dropdown labelId="aclHelp" css="dropdown-menu-right">
                      <bs:menuitem
                              target="_new"
                              icon="arrow-right"
                              iconAfter="true"
                              href="https://docs.rundeck.com/docs/administration/security/authorization.html"
                              code="link.title.access.control.policy"/>
                      <bs:menuitem
                              target="_new"
                              icon="arrow-right"
                              iconAfter="true"
                              href="https://docs.rundeck.com/docs/administration/security/authorization.html#example"
                              code="link.title.acl.policy.format"/>
                  </bs:dropdown>
              </div>
            </div>
        </h3>
    </div>

    <div class="card-content">

        <g:if test="${createField}">
            <div class="form-group ${input?.errors?.hasFieldErrors(createField)?'has-error':''}">
                <label class="control-label  col-sm-2">
                    ${createFieldLabel}
                </label>

                <div class="col-sm-10">
                    <input class="form-control" type="text" name="${createField}" value="${createFieldValue}"/>
                    <g:hiddenField name="create" value="true"/>
                    <g:if test="${createFieldHelpCode}">
                        <div class="help-block">
                          <g:message code="${createFieldHelpCode}"/>
                        </div>
                    </g:if>
                    <g:if test="${input?.errors?.hasFieldErrors(createField)}">

                        <div class="help-block">
                            <g:fieldError field="${createField}" bean="${input}"/>
                        </div>
                    </g:if>
                </div>
            </div>
        </g:if>

        <g:if test="${primaryLabel && primaryValue}">
            <div class="form-group">
                <label class="control-label  col-sm-2">
                    ${primaryLabel}
                </label>

                <div class="col-sm-10">
                    <p class="form-control-static text-info">${primaryValue}</p>
                </div>
            </div>
        </g:if>
        <g:if test="${secondaryLabel && secondaryValue}">
            <div class="form-group">
                <label class="control-label  col-sm-2">
                    ${secondaryLabel}
                </label>

                <div class="col-sm-10">
                    <p class="form-control-static text-info">${secondaryValue}</p>
                </div>
            </div>
        </g:if>
        <div class="form-group ${input?.errors?.hasFieldErrors('fileText') ? 'has-error' : ''}">
            <div class="col-sm-12" id="acl_text_editor_content">
                <textarea
                        name="fileText"
                        class="form-control code apply_ace"
                        data-ace-autofocus='true'
                        data-ace-session-mode="yaml"
                        data-ace-height="500px"
                        data-ace-control-soft-wrap="true">${fileText}</textarea>
                <g:if test="${input?.errors?.hasFieldErrors('fileText')}">

                    <div class="help-block">
                        <g:fieldError field="fileText" bean="${input}"/>
                    </div>
                </g:if>
            </div>
        </div>
        <g:if test="${validation && !validation.valid}">
            <h3>
                <i class="glyphicon glyphicon-warning-sign text-warning has_tooltip"
                   title="${message(code: "aclpolicy.format.validation.failed")}"></i>
                <g:message code="error.validation.failed.title"/>
            </h3>


            <g:render template="aclValidationReport"
                      model="${[validation: validation, documentPrefix: validationDocumentPrefix]}"/>

        </g:if>
    </div>


    <div class="card-footer">
        <g:submitButton name="cancel" value="${g.message(code: 'button.action.Cancel', default: 'Cancel')}"
                        class="btn btn-default reset_page_confirm"/>
        <g:submitButton name="save" value="${g.message(code: 'button.action.Save', default: 'Save')}"
                        class="btn btn-cta reset_page_confirm"/>
    </div>
</div>
