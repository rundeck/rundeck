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

<div class="container-fluid">
  <g:render template="/common/messages"/>
  <g:if test="${ messages || errjobs || skipjobs || jobs || execerrors || execsuccess}">
    <g:if test="${messages}">
        <div class="row">
            <div class="col-sm-12">
                <div class="alert alert-info">
                    <g:each var="msg" in="${messages}">
                        <div><g:enc>${msg}</g:enc></div>
                    </g:each>
                </div>
            </div>
        </div>
    </g:if>
    <g:if test="${errjobs}">
      <div class="row">
        <div class="col-sm-12">
          <div class="alert alert-danger">
            <span class="prompt errors"><g:enc>${errjobs.size()}</g:enc> Job${errjobs.size()==1?' was':'s were'} not processed due to errors</span>
            <div class="presentation">
              <g:if test="${errjobs.size()>0}">
                <table cellpadding="0" cellspacing="0" style="width: 700px;" class="jobsList">
                  <% def j=0 %>
                  <g:each in="${errjobs}" var="entry">
                    <g:set var="scheduledExecution" value="${entry.scheduledExecution}"/>
                    <g:set var="entrynum" value="${entry.entrynum}"/>
                    <tr class=" ${j%2==1?'alternateRow':'normalRow'}">
                      <td>#<g:enc>${entrynum}</g:enc>:</td>
                      <td class="jobname" >
                        <g:if test="${scheduledExecution.id}">
                          <g:link controller="scheduledExecution" action="show" id="${scheduledExecution.extid}"><g:enc>${scheduledExecution.jobName}</g:enc></g:link >
                        </g:if>
                        <g:else>
                          <g:enc>${scheduledExecution.jobName}</g:enc>
                        </g:else>
                      </td>
                      <td class="jobdesc" style=""><g:enc>${scheduledExecution.description?.size()>100?scheduledExecution.description.substring(0,100):scheduledExecution.description}</g:enc></td>
                      <td class="errors">
                        <g:hasErrors bean="${scheduledExecution}">
                          <g:renderErrors bean="${scheduledExecution}" as="list"/>
                        </g:hasErrors>
                        <g:if test="${entry.errmsg}">
                          <g:enc>${entry.errmsg}</g:enc>
                        </g:if>
                      </td>
                    </tr>
                  </g:each>
                </table>
              </g:if>
            </div>
          </div>
        </div>
      </div>
    </g:if>
    <g:if test="${skipjobs}">
      <div class="row">
        <div class="col-sm-12">
          <div class="batchresset">
            <span class="prompt info"><g:enc>${skipjobs.size()}</g:enc> <g:message code="domain.ScheduledExecution.title"/><g:enc>${skipjobs.size()==1?' was':'s were'}</g:enc> skipped due to existing jobs with the same name</span>
            <div class="presentation">
              <g:if test="${skipjobs.size()>0}">
                <table cellpadding="0" cellspacing="0" style="width: 700px;" class="jobsList">
                  <% def j=0 %>
                  <g:each in="${skipjobs}" var="entry">
                    <g:set var="scheduledExecution" value="${entry.scheduledExecution}"/>
                    <g:set var="entrynum" value="${entry.entrynum}"/>
                    <tr class=" ${j%2==1?'alternateRow':'normalRow'}">
                      <td>#<g:enc>${entrynum}</g:enc>:</td>
                      <td class="jobname" >
                          <g:enc>${scheduledExecution.jobName}</g:enc>
                      </td>
                      <td class="jobdesc" style=""><g:enc>${scheduledExecution.description?.size()>100?scheduledExecution.description.substring(0,100):scheduledExecution.description}</g:enc></td>
                      <td class="sepL">
                          Existing:
                      </td>
                      <td class="jobname">
                          <g:if test="${scheduledExecution.id}">
                              <g:link controller="scheduledExecution" action="show" id="${scheduledExecution.extid}"><g:enc>${scheduledExecution.jobName}</g:enc></g:link >
                          </g:if>
                      </td>
                      <td class="jobdesc">
                          <g:enc>${scheduledExecution.origDescription.size()>100?scheduledExecution.origDescription.substring(0,100):scheduledExecution.origDescription}</g:enc>
                      </td>
                    </tr>
                  </g:each>
                </table>
              </g:if>
            </div>
          </div>
        </div>
      </div>
    </g:if>
    <g:if test="${jobs}">
      <div class="row">
        <div class="col-sm-12">
          <div class="batchresset">
            <span class="text-info"><g:enc>${jobs.size()}</g:enc> <g:message code="domain.ScheduledExecution.title"/><g:enc>${jobs.size()==1?' was':'s were'}</g:enc> successfully created/modified</span>
            <g:render template="/menu/jobslist" model="[jobslist:jobs,total:jobs.size(), headers: false, showIcon:true]"/>
          </div>
        </div>
      </div>
    </g:if>
  </g:if>
  <div class="row">
    <div class="col-xs-12">
      <div class="card">
        <g:uploadForm method="POST"
                      useToken="true"
                      controller="scheduledExecution" action="upload"
                      params="[project:params.project]" class="form" role="form">
          <div class="card-header">
            <h3 class="card-title">
              Upload <g:message code="domain.ScheduledExecution.title"/> Definition to project <b><g:enc>${params.project ?: request.project}</g:enc></b>
            </h3>
          </div>
          <div class="card-content">
            <g:hiddenField name="project" value="${params.project ?: request.project}"/>
            <div class="form-group">
            %{--<g:if test="${!didupload}">--}%
                <label for="xmlBatch">Select a <g:message code="domain.ScheduledExecution.title"/>s definition file.</label>
            %{--</g:if>--}%
            %{--<g:else>--}%
                %{--<label for="xmlBatch">--}%
                    %{--<span class="btn btn-default"--}%
                          %{--onclick="toggleDisclosure('uploadFormDiv', 'uploadFormDiv-toggle', '${resource(dir:"images",file:"icon-tiny-disclosure.png")}', '${resource(dir:"images",file:"icon-tiny-disclosure-open.png")}')">--}%
                        %{--Upload File--}%
                        %{--<img src="${resource(dir: 'images', file: 'icon-tiny-disclosure' + (errjobs?.size() > 0 ? '-open' : '') + '.png')}"--}%
                             %{--id="uploadFormDiv-toggle"/>--}%
                    %{--</span>--}%
                %{--</label>--}%
            %{--</g:else>--}%
                <input type="file" name="xmlBatch" id="xmlBatch" class="form-control"/>
            </div>
            <div class="form-group">
              <div class="radio">
                <g:radio name="fileformat" value="xml"  checked="${params.fileformat?params.fileformat=='xml':true}" id="fileformat_xml"/>
                <label for="fileformat_xml">XML format</label>
              </div>
              <div class="radio">
                <g:radio name="fileformat" value="yaml"  checked="${params.fileformat?params.fileformat=='yaml':false}" id="fileformat_yaml"/>
                <label class="radio-inline" for="fileformat_yaml">YAML format</label>
              </div>
            </div>
            <div class="form-group">
              <div class="control-label text-form-label">
                When a <g:message code="domain.ScheduledExecution.title"/> with the same name already exists:
              </div>
              <div class="radio">
                <g:radio name="dupeOption" value="update" checked="${params.dupeOption?params.dupeOption=='update':true}" id="dupeOption_update"/>
                <label for="dupeOption_update">
                  <em>Update</em> the existing <g:message code="domain.ScheduledExecution.title"/>
                </label>
              </div>
              <div class="radio">
                <g:radio name="dupeOption" value="skip"  checked="${params.dupeOption=='skip'}" id="dupeOption_skip"/>
                <label for="dupeOption_skip"><em>Skip</em> the uploaded <g:message code="domain.ScheduledExecution.title"/></label>
              </div>
              <div class="radio">
                <g:radio name="dupeOption" value="create" checked="${params.dupeOption=='create'}" id="dupeOption_create"/>
                <label for="dupeOption_create">Always <em>Create</em> a new <g:message code="domain.ScheduledExecution.title"/></label>
              </div>
            </div>
            <div class="form-group">
              <div class="control-label text-form-label">Imported Jobs:</div>
              <div class="radio">
                <input type="radio" name="uuidOption" value="preserve" checked id="uuidOption_preserve"/>
                <label for="uuidOption_preserve"><g:message code="project.archive.import.jobUuidOption.preserve.label"/></label>
                <div class="help-block"><g:message code="project.archive.import.jobUuidOption.preserve.description"/></div>
              </div>
              <div class="radio">
                <input type="radio" name="uuidOption" value="remove" id="uuidOption_remove"/>
                <label for="uuidOption_remove"><g:message code="project.archive.import.jobUuidOption.remove.label"/></label>
                <div class="help-block"><g:message code="project.archive.import.jobUuidOption.remove.description"/></div>
              </div>
            </div>
            <div class="form-group">
              <div class="control-label text-form-label">Validate Referenced Jobs:</div>
              <div class="radio">
                <input type="radio" name="validateJobref" value="false" checked id="validateJobref_false"/>
                <label for="validateJobref_false"><g:message code="archive.import.importJobRef.false.title"/></label>
                <div class="help-block"><g:message code="archive.import.importJobRef.false.help"/></div>
              </div>
              <div class="radio">
                <input type="radio" name="validateJobref" value="true" id="validateJobref_true"/>
                <label for="validateJobref_true"><g:message code="archive.import.importJobRef.true.title"/></label>
                <div class="help-block"><g:message code="archive.import.importJobRef.true.help"/></div>
              </div>
            </div>
            <feature:enabled name="calendar">
              <div class="form-group">
                <div class="control-label text-form-label"><g:message code="archive.import.importJobCalendars.true.title"/>:</div>
                <div class="radio">
                  <input type="radio" name="validateCalendarRef" value="false" checked id="validateCalendarRef_false"/>
                  <label for="validateCalendarRef_false"><g:message code="archive.import.importJobCalendars.false.title"/></label>
                  <div class="help-block"><g:message code="archive.import.importJobCalendars.false.help"/></div>
                </div>
                <div class="radio">
                  <input type="radio" name="validateCalendarRef" value="true" id="validateCalendaRref_true"/>
                  <label for="validateCalendaRref_true"><g:message code="archive.import.importJobCalendars.true.title"/></label>
                  <div class="help-block"><g:message code="archive.import.importJobCalendars.true.help"/></div>
                </div>
              </div>
            </feature:enabled>

            </div>
          <div class="card-footer">
            <div id="uploadFormButtons">
              <g:actionSubmit id="createFormCancelButton" value="${g.message(code:'button.action.Cancel',default:'Cancel')}" class="btn btn-default"/>
              <g:submitButton name="Upload" id="uploadFormUpload"
                              value="${g.message(code:'button.action.Upload',default:'Upload')}"
                              onclick="['uploadFormButtons','schedUploadSpinner'].each(Element.toggle)"
                              class="btn btn-primary"/>
            </div>
            <div id="schedUploadSpinner" class="spinner block" style="display:none;">
              <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/>
              Uploading File...
            </div>
          </div>
        </g:uploadForm>
      </div>
    </div>
  </div>
</div>
