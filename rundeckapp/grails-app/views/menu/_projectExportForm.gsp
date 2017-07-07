%{--
  - Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

<div class="row">
    <div class="col-sm-10 col-sm-offset-1">
        <g:form style="display: inline;" controller="project" action="exportPrepare"
                class="form-horizontal"
                params="[project: (params.project ?: request.project)]"
                useToken="true">

            <div class="panel panel-primary" id="exportform">
                <div class="panel-heading">
                    <span class="h3"><g:message code="export.archive"/></span>
                </div>

                <div class="list-group">
                    <div class="list-group-item">
                        <div class="form-group">
                            <label class="control-label col-sm-2"><g:message code="project.prompt"/></label>

                            <div class="col-sm-10">
                                <span class="form-control-static"><g:enc>${params.project ?:
                                        request.project}</g:enc></span>
                            </div>
                        </div>

                        <div class="form-group">
                            <label class="control-label col-sm-2">Include</label>

                            <div class="col-sm-10">
                                <div class="checkbox">
                                    <label>
                                        <g:checkBox name="exportAll" value="true" checked="true"
                                                    class="export_all"/>
                                        <em>All</em>
                                    </label>
                                </div>

                            </div>
                        </div>

                        <div class="form-group">
                            <div class="col-sm-offset-2 col-sm-10 export_select_list">
                                <div class="checkbox">
                                    <label><g:checkBox name="exportJobs" value="true"/> Jobs</label>
                                </div>

                                <div class="checkbox">
                                    <label><g:checkBox name="exportExecutions" value="true"/> Executions</label>
                                </div>

                                <div class="checkbox">
                                    <label><g:checkBox name="exportConfigs" value="true"/> Configuration</label>
                                </div>

                                <div class="checkbox">
                                    <label><g:checkBox name="exportReadmes" value="true"/> Readme/Motd</label>
                                </div>

                                <auth:resourceAllowed
                                        action="${[AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN]}"
                                        any="true"
                                        context='application'
                                        type="project_acl"
                                        name="${params.project}">

                                    <div class="checkbox">
                                        <label><g:checkBox name="exportAcls" value="true"/> ACL Policies</label>
                                    </div>
                                </auth:resourceAllowed>
                                <auth:resourceAllowed
                                        action="${[AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN]}"
                                        any="true"
                                        context='application'
                                        type="project_acl"
                                        has="false"
                                        name="${params.project}">

                                    <div class="checkbox disabled text-muted">
                                        <i class="glyphicon glyphicon-ban-circle"></i> ACL Policies (Unauthorized)
                                    </div>

                                </auth:resourceAllowed>

                            </div>
                        </div>
                    </div>
                </div>


                <div class="panel-footer">
                    <button type="button" class="btn btn-default">
                        <g:message code="cancel"/>
                    </button>
                    <button type="submit" class="btn btn-success"><g:message code="button.Export.title"/></button>
                </div>
            </div>
        </g:form>
    </div>
</div>