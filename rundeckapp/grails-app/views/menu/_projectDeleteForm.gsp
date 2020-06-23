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

<div class="row">
  <div class="col-xs-12">
    <div class="card">
      <div class="card-header">
        <h3 class="card-title"><g:message code="delete.project" /></h3>
      </div>
      <div class="card-content" style="padding: 2em 1em;">
        <g:if test="${flash.error}">
          <div class="alert alert-warning">
            <g:enc>${flash.error}</g:enc>
          </div>
        </g:if>
        <a class="btn btn-danger btn-lg" data-toggle="modal" href="#deleteProjectModal">
          <g:message code="delete.this.project.button" />
          <i class="glyphicon glyphicon-remove"></i>
        </a>
        <div class="modal fade" id="deleteProjectModal" role="dialog" aria-labelledby="deleteProjectModalLabel" aria-hidden="true">
          <g:form  controller="project" action="delete" params="[project: (params.project ?: request.project)]" useToken="true">
            <div class="modal-dialog">
              <div class="modal-content">
                <div class="modal-header">
                  <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                  <h4 class="modal-title" id="deleteProjectModalLabel"><g:message code="delete.project" /></h4>
                </div>
                <div class="modal-body">
                  <span class="text-danger h4"><g:message code="really.delete.this.project" /></span>
                </div>
                <div class="modal-body">
                  <div class="form-group">
                    <label class="control-label col-sm-2"><g:message code="project.prompt" /></label>
                      <div class="col-sm-10">
                        <span class="form-control-static"><g:enc>${params.project ?: request.project}</g:enc></span>
                      </div>
                  </div>
                </div>
                <div class="modal-footer">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><g:message code="no" /></button>
                  <button type="submit" class="btn btn-danger"><g:message code="delete.project.now.button" /></button>
                </div>
              </div><!-- /.modal-content -->
            </div><!-- /.modal-dialog -->
          </g:form>
        </div>
      </div>
    </div>
  </div>
</div>
