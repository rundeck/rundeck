<g:if test="${flash.message || errors || messages || errjobs || skipjobs || jobs || execerrors || execsuccess}">
<div class="pageBody">

    <g:if test="${flash.message || errors}">
        <div class="error note">${flash.message?flash.message.encodeAsHTML():errors.encodeAsHTML()}</div>
    </g:if>

    <g:if test="${messages}">
        <div class="sepT presentation">
            <div class="info note">
            <g:each var="msg" in="${messages}">
                <div>${msg}</div>
            </g:each>
            </div>
        </div>
    </g:if>



    <g:if test="${execerrors}">
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
    </g:if>
    <g:if test="${execsuccess}">
        <div class="batchresset">
            <span class="prompt info">${execsuccess.size()} Job Execution${execsuccess.size()==1?' was':'s were'} successfully queued</span>

            <div class="presentation">
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
    </g:if>
    <g:if test="${errjobs}">
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
    </g:if>


    <g:if test="${skipjobs}">
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
    </g:if>


    <g:if test="${jobs}">
        <div class="batchresset">
            <span class="info prompt">${jobs.size()} <g:message code="domain.ScheduledExecution.title"/>${jobs.size()==1?' was':'s were'} successfully created/modified</span>

            <g:render template="/menu/jobslist" model="[jobslist:jobs,total:jobs.size()]"/>
        </div>
    </g:if>
</div>
</g:if>
<div style="margin-top:10px;" class="pageBody form">
    <g:form method="post" action="upload" enctype="multipart/form-data">
        <g:if test="${!didupload}">
        <span class="prompt">Upload File</span>
        </g:if>
        <g:else>
            <span class="prompt action" onclick="toggleDisclosure('uploadFormDiv','uploadFormDiv-toggle','${resource(dir:"images",file:"icon-tiny-disclosure.png")}','${resource(dir:"images",file:"icon-tiny-disclosure-open.png")}')">
                Upload File
                <img src="${resource(dir:'images',file:'icon-tiny-disclosure'+(errjobs?.size()>0?'-open':'')+'.png')}" id="uploadFormDiv-toggle"/>
            </span>
        </g:else>
        <div id="uploadFormDiv" style="${(didupload && errjobs?.size()<1)?'display:none':''}">
            <div class="presentation">
                <p><span class="info note">Choose the XML/YAML formatted <g:message code="domain.ScheduledExecution.title"/>s definition file.</span></p>
                <input type="file" name="xmlBatch"/>
            </div>
            <span class="prompt">File format:</span>
            <div class="presentation">
                <div>
                    <label>
                    <g:radio name="fileformat" value="xml"  checked="${params.fileformat?params.fileformat=='xml':true}"/>
                    XML</label>
                </div>
                <div>
                    <label>
                    <g:radio name="fileformat" value="yaml"  checked="${params.fileformat?params.fileformat=='yaml':false}"/>
                    YAML</label>
                </div>

            </div>
            <span class="prompt">When a <g:message code="domain.ScheduledExecution.title"/> with the same name already exists:</span>
            <div class="presentation">

                <div>
                    <g:radio name="dupeOption" value="update" id="dupeOption1" checked="${params.dupeOption?params.dupeOption=='update':true}"/>
                    <label for="dupeOption1"><em>Update</em> the existing <g:message code="domain.ScheduledExecution.title"/></label>
                </div>

                <div>
                    <g:radio name="dupeOption" value="skip" id="dupeOption2" checked="${params.dupeOption=='skip'}"/>
                    <label for="dupeOption2"><em>Skip</em> the uploaded <g:message code="domain.ScheduledExecution.title"/></label>
                </div>

                <div>
                    <g:radio name="dupeOption" value="create" id="dupeOption3"  checked="${params.dupeOption=='create'}"/>
                    <label for="dupeOption3"><em>Create</em> a new <g:message code="domain.ScheduledExecution.title"/></label>
                </div>

            </div>
            <div class="buttons">
                <div id="uploadFormButtons">
                    <g:actionSubmit id="createFormCancelButton" value="Cancel"/>
                    <g:actionSubmit action="upload" value="Upload" id="uploadFormUpload" onclick="['uploadFormButtons','schedUploadSpinner'].each(Element.toggle)" />
                    <g:actionSubmit action="uploadAndExecute" value="Upload And Run" id="uploadFormUploadAndExec" onclick="['uploadFormButtons','schedUploadSpinner'].each(Element.toggle)" />
                </div>
                <div id="schedUploadSpinner" class="spinner block" style="display:none;">
                    <img src="${resource(dir:'images',file:'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/>
                    Uploading File...
                </div>
            </div>
        </div>
    </g:form>
</div>
</div>
