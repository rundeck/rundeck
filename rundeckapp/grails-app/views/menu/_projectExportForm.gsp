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


<%@ page import="org.rundeck.core.auth.AuthConstants" %>
<script type="application/javascript">
    function select_all() {
        jQuery('.export_select_list input[type=checkbox]').prop('checked', true);
    }
    function select_none() {
        jQuery('.export_select_list input[type=checkbox]').val([]);
    }
    function deselect_one() {
        jQuery('.export_all').prop('checked', false);
    }
    jQuery(function () {
        jQuery('.obs_export_select_all').on('click', select_all);
        jQuery('.obs_export_select_none').on('click', select_none);
        jQuery('.export_select_list input[type=checkbox]').on('change', function () {
            if (!jQuery(this).prop('checked')) {
                deselect_one();
            }
        });
        jQuery('.export_all').on('change', function () {
            if (jQuery(this).prop('checked')) {
                select_all();
            }
        });
    });
</script>
<div>
  <g:form style="display: inline;" controller="project" action="exportPrepare" class="form-horizontal" params="[project: (params.project ?: request.project)]" useToken="true">
    <div class="card" id="exportform">
      <div class="card-content">
        <div class="list-group">
          <div class="list-group-item">
            <div class="form-group">
              <label class="control-label col-sm-2"><g:message code="project.prompt"/></label>
                <div class="col-sm-10">
                  <div class="form-control-static" style="margin-top:.4em;">
                    <g:enc>${params.project ?: request.project}</g:enc>
                  </div>
                </div>
            </div>
            <div class="form-group">
              <label class="control-label col-sm-2">Include</label>
              <div class="col-sm-10">
                <div class="checkbox">
                  <g:checkBox name="exportAll" value="true" checked="true"class="export_all"/>
                  <label for="exportAll">
                      <em>All</em>
                  </label>
                </div>
              </div>
            </div>
            <div class="form-group">
              <div class="col-sm-offset-2 col-sm-10 export_select_list">
                <div class="checkbox">
                  <g:checkBox name="exportJobs" value="true"/>
                  <label for="exportJobs">Jobs</label>
                </div>
                <div class="checkbox">
                  <g:checkBox name="exportExecutions" value="true"/>
                  <label for="exportExecutions">Executions</label>
                </div>
                <div class="checkbox">
                  <g:checkBox name="exportConfigs" value="true"/>
                  <label for="exportConfigs">Configuration</label>
                </div>
                <div class="checkbox">
                  <g:checkBox name="exportReadmes" value="true"/>
                  <label for="exportReadmes">Readme/Motd</label>
                </div>
                <auth:resourceAllowed action="${[AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN]}" any="true" context='application' type="project_acl" name="${params.project}">
                  <div class="checkbox">
                    <g:checkBox name="exportAcls" value="true"/>
                    <label for="exportAcls">ACL Policies</label>
                  </div>
                </auth:resourceAllowed>
                <auth:resourceAllowed action="${[AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN]}" any="true" context='application' type="project_acl" has="false" name="${params.project}">
                  <div class="checkbox disabled text-primary">
                    <i class="glyphicon glyphicon-ban-circle"></i> ACL Policies (Unauthorized)
                  </div>
                </auth:resourceAllowed>
                <auth:resourceAllowed action="${[AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN]}" context='application' type="project" name="${params.project}">
                <div class="checkbox">
                  <g:checkBox name="exportScm" value="true"/>
                  <label for="exportScm">SCM configuration</label>
                </div>
                </auth:resourceAllowed>
                <auth:resourceAllowed action="${[AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN]}" context='application' type="project" has="false" name="${params.project}">
                  <div class="checkbox disabled text-primary">
                    <i class="glyphicon glyphicon-ban-circle"></i> SCM Configuration (Unauthorized)
                  </div>
                </auth:resourceAllowed>
                <feature:enabled name="webhooks">
                <auth:resourceAllowed action="${[AuthConstants.ACTION_ADMIN]}" context='application' type="project" name="${params.project}">
                  <div class="checkbox">
                    <g:checkBox name="exportWebhooks" value="true"/>
                    <label for="exportScm">Webhooks</label>
                  </div>
                </auth:resourceAllowed>
                <auth:resourceAllowed action="${[AuthConstants.ACTION_ADMIN]}" context='application' type="project" name="${params.project}" has="false">
                  <div class="checkbox disabled text-primary">
                    <i class="glyphicon glyphicon-ban-circle"></i> Webhooks (Unauthorized)
                  </div>
                </auth:resourceAllowed>
                </feature:enabled>
                <auth:resourceAllowed action="${[AuthConstants.ACTION_ADMIN]}" context='application' type="project" name="${params.project}">
                  <div class="checkbox">
                    <g:checkBox name="exportScheduleDefinitions" value="true"/>
                    <label for="exportScm">Schedule Definitions</label>
                  </div>
                </auth:resourceAllowed>
                <auth:resourceAllowed action="${[AuthConstants.ACTION_ADMIN]}" context='application' type="project" name="${params.project}" has="false">
                  <div class="checkbox disabled text-primary">
                    <i class="glyphicon glyphicon-ban-circle"></i> Schedule Definitions (Unauthorized)
                  </div>
                </auth:resourceAllowed>
                <feature:enabled name="calendar">
                <auth:resourceAllowed action="${[AuthConstants.ACTION_ADMIN]}" context='application' type="project" name="${params.project}">
                  <div class="checkbox">
                    <g:checkBox name="exportCalendars" value="true"/>
                    <label for="exportCalendars">Calendars</label>
                  </div>
                </auth:resourceAllowed>
                <auth:resourceAllowed action="${[AuthConstants.ACTION_ADMIN]}" context='application' type="project" name="${params.project}" has="false">
                  <div class="checkbox disabled text-primary">
                    <i class="glyphicon glyphicon-ban-circle"></i> Calendars (Unauthorized)
                  </div>
                </auth:resourceAllowed>
                </feature:enabled>
              </div>
            </div>



            <div class="form-group">
              <label class="control-label col-sm-2">Referenced Jobs:</label>
              <div class="col-sm-10">
                  <div class="radio">
                    <g:radio name="stripJobRef" value="no" id="dontStrip" checked="checked" />
                    <label for="dontStrip">
                    <g:message code="export.jobref.strip.none"/>
                    </label>
                  </div>
                  <div class="radio">
                    <g:radio name="stripJobRef" value="name" id="stripName"/>
                    <label for="stripName">
                    <g:message code="export.jobref.strip.name"/>
                    </label>
                  </div>
                  <div class="radio">
                    <g:radio name="stripJobRef" value="uuid" id="stripUuid"/>
                    <label for="stripUuid">
                    <g:message code="export.jobref.strip.uuid"/>
                    </label>
                  </div>
              </div>
            </div>

          </div>
        </div>
      </div>
      <div class="card-footer">
        <g:submitButton name="cancel" value="${g.message(code:'button.action.Cancel',default:'Cancel')}" class="btn btn-default  btn-sm"/>
        <button type="submit" class="btn btn-primary btn-sm"><g:message code="export.archive"/> <g:icon name="download"/></button>
        <auth:resourceAllowed action="${[AuthConstants.ACTION_PROMOTE, AuthConstants.ACTION_ADMIN]}" context='application' type="project" name="${params.project}">
          <button type="button" data-toggle="modal" data-target="#exportModal" class="btn btn-info  btn-sm pull-right"><g:message code="export.another.instance"/></button>
        </auth:resourceAllowed>
      </div>
    </div>
    <!-- Generate Modal -->
    <div class="modal fade clearconfirm" id="exportModal" tabindex="-1" role="dialog" aria-labelledby="gentokenLabel" aria-hidden="true">
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
            <h4 class="modal-title" id="exportLabel">
              <g:message code="export.another.instance"/>
            </h4>
          </div>
          <div class="modal-body" id="userTokenGenerateForm">
            <div class="row">
              <div class="col-sm-12">
                <div class="form">
                  <div class="form-group">
                    <div class="col-sm-2 control-label">
                      <label for="url"><g:message code="export.another.instance.url"/></label>
                    </div>
                    <div class="col-sm-10">
                      <input type='text' name="url" value="" id="url" class="form-control"/>
                      <span class="help-block">
                          <g:message code="export.another.instance.url.help"/>
                      </span>
                    </div>
                  </div>
                  <div class="form-group">
                    <div class="col-sm-2 control-label">
                      <label for="apitoken"><g:message code="export.another.instance.token"/></label>
                    </div>
                    <div class="col-sm-10">
                      <input type='password' name="apitoken" value="" id="apitoken" class="form-control"/>
                      <span class="help-block">
                        <g:message code="export.another.instance.token.help"/>
                      </span>
                    </div>
                  </div>
                  <div class="form-group">
                    <div class="col-sm-2 control-label">
                      <label for="targetproject"><g:message code="export.another.instance.project"/></label>
                    </div>
                    <div class="col-sm-10">
                      <input type='text' name="targetproject" value="" id="targetproject" class="form-control"/>
                      <span class="help-block">
                        <g:message code="export.another.instance.project.help"/>
                      </span>
                    </div>
                  </div>
                  <div class="form-group">
                    <div class="col-sm-2 control-label">
                      <label for="preserveuuid"><g:message code="project.archive.import.jobUuidOption.preserve.label"/></label>
                    </div>
                    <div class="col-sm-10">
                      <input type='checkbox' name="preserveuuid" value="preserveuuid" id="preserveuuid" class="form-control"/>
                      <span class="help-block">
                        <g:message code="project.archive.import.jobUuidOption.preserve.description"/>
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="modal-footer">
              <g:actionSubmit action="exportInstancePrepare" class="small btn btn-primary" value="${message(code:'export.another.instance.go')}" />
          </div>
        </div><!-- /.modal-content -->
      </div><!-- /.modal-dialog -->
    </div><!-- /.modal -->
  </g:form>
</div>
