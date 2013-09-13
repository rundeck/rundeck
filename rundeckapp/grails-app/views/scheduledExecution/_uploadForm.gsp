<g:if test="${flash.message || uploadError || flash.error || messages || errjobs || skipjobs || jobs || execerrors || execsuccess}">

    <g:if test="${flash.error || uploadError}">
        <div class="row">
            <div class="col-sm-12">
                <div class="alert alert-warning">${flash.error?flash.error.encodeAsHTML():uploadError.encodeAsHTML()}</div>
            </div>
        </div>
    </g:if>
    <g:if test="${flash.message}">
        <div class="row">
            <div class="col-sm-12">
                <div class="alert alert-info">${flash.message.encodeAsHTML()}</div>
            </div>
        </div>
    </g:if>

    <g:if test="${messages}">
        <div class="row">
            <div class="col-sm-12">
                <div class="alert alert-info">
                    <g:each var="msg" in="${messages}">
                        <div>${msg}</div>
                    </g:each>
                </div>
            </div>
        </div>
    </g:if>



    <g:if test="${execerrors}">
        <div class="row">
        <div class="col-sm-12">

        <div class="batchresset">
            <span class="prompt errors">${execerrors.size()} Job${execerrors.size()==1?' was':'s were'} not executed due to errors</span>

            <div class="presentation">
                <g:if test="${execerrors.size()>0}">
                    <table cellpadding="0" cellspacing="0" style="width: 700px;" class="jobsList">
                        <% def j=0 %>
                        <g:each in="${execerrors}" var="entry">
                            <g:set var="entrynum" value="${entry.entrynum}"/>
                            <tr class=" ${j%2==1?'alternateRow':'normalRow'}">
                                <td>
                                    #${entrynum}:
                                </td>
                                <td class="jobname" >
                                   ${entry.error}
                                </td>
                                <td class="errors">
                                    ${entry.message}
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
    <g:if test="${execsuccess}">
        <div class="row">
        <div class="col-sm-12">


                <div class="batchresset">
                    <span class="prompt info">${execsuccess.size()} Job Execution${execsuccess.size()==1?' was':'s were'} successfully queued</span>

            <div class="">
                <g:if test="${execsuccess.size()>0}">
                    <table cellpadding="0" cellspacing="0" style="width: 700px;" class="jobsList">
                        <% def j=0 %>
                        <g:each in="${execsuccess}" var="entry">
                            <g:set var="execution" value="${entry.execution}"/>
                            <g:set var="entrynum" value="${entry.entrynum}"/>
                            <g:set var="execid" value="${entry.id}"/>
                            <tr class=" ${j%2==1?'alternateRow':'normalRow'}">
                                <td>
                                    #${entrynum}:
                                </td>
                                <td class="jobname" >
                                    <g:if test="${execid}">
                                        <g:link controller="execution" action="show" id="${execid}">${execution.toString().encodeAsHTML()}</g:link >
                                    </g:if>
                                    <g:else>
                                        ${execution.toString().encodeAsHTML()}
                                    </g:else>
                                </td>
                                %{--<td class="jobdesc" style="">${execution.description.size()>100?execution.description.substring(0,100).encodeAsHTML():execution.description.encodeAsHTML()}</td>--}%
                                <g:hasErrors bean="${execution}">
                                    <td class="errors">
                                        <g:renderErrors bean="${execution}" as="list"/>
                                    </td>
                                </g:hasErrors>
                            </tr>
                        </g:each>
                    </table>
                </g:if>
            </div>
        </div>
        </div>
        </div>
    </g:if>
    <g:if test="${errjobs}">
        <div class="row">
        <div class="col-sm-12">
      <div class="batchresset">
               <span class="prompt errors">${errjobs.size()} Job${errjobs.size()==1?' was':'s were'} not processed due to errors</span>

            <div class="presentation">
                <g:if test="${errjobs.size()>0}">
                    <table cellpadding="0" cellspacing="0" style="width: 700px;" class="jobsList">
                        <% def j=0 %>
                        <g:each in="${errjobs}" var="entry">
                            <g:set var="scheduledExecution" value="${entry.scheduledExecution}"/>
                            <g:set var="entrynum" value="${entry.entrynum}"/>
                            <tr class=" ${j%2==1?'alternateRow':'normalRow'}">
                                <td>
                                    #${entrynum}:
                                </td>
                                <td class="jobname" >
                                    <g:if test="${scheduledExecution.id}">
                                        <g:link controller="scheduledExecution" action="show" id="${scheduledExecution.extid}">${scheduledExecution.jobName.encodeAsHTML()}</g:link >
                                    </g:if>
                                    <g:else>
                                        ${scheduledExecution.jobName.encodeAsHTML()}
                                    </g:else>
                                </td>
                                <td class="jobdesc" style="">${scheduledExecution.description?.size()>100?scheduledExecution.description.substring(0,100).encodeAsHTML():scheduledExecution.description?.encodeAsHTML()}</td>
                                        <td class="errors">
                                <g:hasErrors bean="${scheduledExecution}">
                                            <g:renderErrors bean="${scheduledExecution}" as="list"/>
                                </g:hasErrors>
                                            <g:if test="${entry.errmsg}">
                                                ${entry.errmsg.encodeAsHTML()}
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
                <span class="prompt info">${skipjobs.size()} <g:message code="domain.ScheduledExecution.title"/>${skipjobs.size()==1?' was':'s were'} skipped due to existing jobs with the same name</span>

            <div class="presentation">
                <g:if test="${skipjobs.size()>0}">
                    <table cellpadding="0" cellspacing="0" style="width: 700px;" class="jobsList">
                        <% def j=0 %>
                        <g:each in="${skipjobs}" var="entry">
                            <g:set var="scheduledExecution" value="${entry.scheduledExecution}"/>
                            <g:set var="entrynum" value="${entry.entrynum}"/>
                            <tr class=" ${j%2==1?'alternateRow':'normalRow'}">

                                <td>
                                    #${entrynum}:
                                </td>
                                <td class="jobname" >
                                    ${scheduledExecution.jobName.encodeAsHTML()}
                                </td>
                                <td class="jobdesc" style="">${scheduledExecution.description?.size()>100?scheduledExecution.description.substring(0,100).encodeAsHTML():scheduledExecution.description?.encodeAsHTML()}</td>
                                <td class="sepL">
                                    Existing:
                                </td>
                                <td class="jobname">
                                    <g:if test="${scheduledExecution.id}">
                                        <g:link controller="scheduledExecution" action="show" id="${scheduledExecution.extid}">${scheduledExecution.jobName.encodeAsHTML()}</g:link >
                                    </g:if>
                                </td>
                                <td class="jobdesc">
                                    ${scheduledExecution.origDescription.size()>100?scheduledExecution.origDescription.substring(0,100).encodeAsHTML():scheduledExecution.origDescription.encodeAsHTML()}
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
           <span class="text-info">${jobs.size()} <g:message code="domain.ScheduledExecution.title"/>${jobs.size()==1?' was':'s were'} successfully created/modified</span>

            <g:render template="/menu/jobslist" model="[jobslist:jobs,total:jobs.size()]"/>
        </div>
        </div>
        </div>
    </g:if>
</g:if>
<div class="row">
<div class="col-sm-10 col-sm-offset-1">
<div class="panel panel-primary">
    <g:form method="post" action="upload" enctype="multipart/form-data" class="form" role="form">
        <div class="panel-heading">
            <span class="h4">
                Upload <g:message code="domain.ScheduledExecution.title"/> Definition
                to project <b>${session.project.encodeAsHTML()}</b>
            </span>
        </div>
        <div class="panel-body">
        <g:hiddenField name="project" value="${session.project}"/>
        <div class="form-group">
        %{--<g:if test="${!didupload}">--}%
            <label for="xmlBatch">Select a <g:message
                code="domain.ScheduledExecution.title"/>s definition file.</label>
        %{--</g:if>--}%
        %{--<g:else>--}%
            %{--<label for="xmlBatch">--}%
                %{--<span class="textbtn textbtn-default"--}%
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
                <label class="radio-inline">
                <g:radio name="fileformat" value="xml"  checked="${params.fileformat?params.fileformat=='xml':true}"/>
                XML format</label>
                <label class="radio-inline">
                <g:radio name="fileformat" value="yaml"  checked="${params.fileformat?params.fileformat=='yaml':false}"/>
                YAML format</label>

            </div>

            <div class="form-group">
                <div class="control-label text-form-label">
                    When a <g:message
                            code="domain.ScheduledExecution.title"/> with the same name already exists:
                </div>


                <div class="radio">
                    <g:radio name="dupeOption" value="update" id="dupeOption1" checked="${params.dupeOption?params.dupeOption=='update':true}"/>
                    <label for="dupeOption1"><em>Update</em> the existing <g:message code="domain.ScheduledExecution.title"/></label>
                </div>

                <div class="radio">
                    <g:radio name="dupeOption" value="skip" id="dupeOption2" checked="${params.dupeOption=='skip'}"/>
                    <label for="dupeOption2"><em>Skip</em> the uploaded <g:message code="domain.ScheduledExecution.title"/></label>
                </div>

                <div class="radio">
                    <g:radio name="dupeOption" value="create" id="dupeOption3"  checked="${params.dupeOption=='create'}"/>
                    <label for="dupeOption3">Always <em>Create</em> a new <g:message code="domain.ScheduledExecution.title"/></label>
                </div>
            </div>

        <div class="form-group">
            <div class="control-label text-form-label">Imported Jobs:</div>
            <div class="radio">
                 <label title="Original UUIDs will be preserved, conflicting UUIDs will be replaced">
                    <input type="radio" name="createUUIDOption" value="preserve" checked/>
                    <g:message code="project.archive.import.jobUUIDBehavior.preserve.label"/>
                </label>
                <div class="help-block"><g:message
                        code="project.archive.import.jobUUIDBehavior.preserve.description"/></div>
            </div>

            <div class="radio">
                <label title="New UUIDs will be generated for every imported Job">
                    <input type="radio" name="createUUIDOption" value="remove"/>
                    <g:message code="project.archive.import.jobUUIDBehavior.remove.label"/>
                </label>
                <div class="help-block"><g:message
                        code="project.archive.import.jobUUIDBehavior.remove.description"/></div>
            </div>
        </div>
</div>
    <div class="panel-footer">

            <div id="uploadFormButtons">
                <g:actionSubmit id="createFormCancelButton" value="Cancel" class="btn btn-default"/>
                <g:actionSubmit action="upload" value="Upload" id="uploadFormUpload"
                                onclick="['uploadFormButtons','schedUploadSpinner'].each(Element.toggle)"
                                class="btn btn-primary"/>
            </div>

            <div id="schedUploadSpinner" class="spinner block" style="display:none;">
                <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/>
                Uploading File...
            </div>
    </div>
    </g:form>
</div>
</div>
</div>
