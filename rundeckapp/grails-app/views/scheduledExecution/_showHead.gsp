        <g:set var="execInfo" value="${scheduledExecution?scheduledExecution:execution}"/>
        <div class="jobInfo" id="jobInfo_${execution?execution.id:''}">
            <g:if test="${scheduledExecution}">
                <g:link controller="scheduledExecution" action="show" id="${scheduledExecution.extid}" class="jobIcon ${execution?.status=='true'?'jobok':execution?.cancelled?'jobwarn':'joberror'}" absolute="${absolute?'true':'false'}">
                    <g:if test="${iconName}">
                        <g:if test="${!noimgs}"><img src="${resource(dir:'images',file:iconName+'.png')}" alt="job" style="border:0;"/></g:if>
                    </g:if>
                    <g:else>
                        <g:set var="fileName" value="${scheduledExecution.scheduled?'clock':'job'}"/>
                        <g:if test="${execution}">
                            <g:set var="fileName" value="${execution.status=='true'?'job-ok':null==execution.dateCompleted?'job-running':execution.cancelled?'job-warn':'job-error'}"/>
                        </g:if>
                        <g:if test="${!noimgs}"><img src="${resource(dir:'images',file:"icon-"+fileName+".png")}" alt="job" style="border:0;"/></g:if>
                    </g:else>
                <span class="jobName">${scheduledExecution?.jobName.encodeAsHTML()}</span></g:link>
            </g:if>
            <g:else>
                <span class="jobIcon ${execution?.status=='true'?'jobok':execution?.cancelled?'jobwarn':'joberror'}">
                    <g:if test="${iconName}">
                        <g:if test="${!noimgs}"><img src="${resource(dir:'images',file:iconName+'.png')}" alt="job" style="border:0;"/></g:if>
                    </g:if>
                    <g:else>

                        <g:set var="fileName" value="job"/>
                        <g:if test="${execution}">
                            <g:set var="fileName" value="${execution.status=='true'?'job-ok':null==execution.dateCompleted?'job-running':execution.cancelled?'job-warn':'job-error'}"/>
                        </g:if>
                        <g:if test="${!noimgs}"><img src="${resource(dir:'images',file:"icon-"+fileName+".png")}" alt="job" style="border:0;"/></g:if>
                    </g:else>
                </span>
          </g:else>
            <span class="jobInfoPart">
                <g:if test="${execInfo instanceof ScheduledExecution && execInfo?.description}"><span class="jobDesc">${execInfo?.description.encodeAsHTML()}</span></g:if>
                <g:if test="${execInfo instanceof ScheduledExecution && execInfo.groupPath}">
                    <span class="jobGroup">
                   <span class="grouplabel"><g:link controller="menu" action="jobs" params="${[groupPath:execInfo.groupPath]}" title="${'View '+g.message(code:'domain.ScheduledExecution.title')+'s in this group'}"  absolute="${absolute?'true':'false'}">

                        <g:if test="${!noimgs}"><img src="${resource(dir:'images',file:'icon-small-folder.png')}" width="16px" height="15px" alt=""/></g:if>
                        ${execInfo.groupPath}

                    </g:link>
                    </span>
                    </span>
                </g:if>
            </span>
            
            <g:if test="${execution}">
                <span class="jobInfoPart">
                    <span class="partContent">
                    <g:if test="${!noimgs}"><img src="${resource(dir:'images',file:'icon-tiny-rarrow-sep.png')}" alt=""/></g:if>
                    <g:link
                        controller="execution"
                        action="show"
                        id="${execution.id}"
                         absolute="${absolute?'true':'false'}"
                        params="${followparams?.findAll{it.value}}">Execution at <g:relativeDate atDate="${execution.dateStarted}" /> by <span class="username">${execution.user}</span></g:link>
                    </span>
                </span>
            </g:if>
            <g:if test="${execInfo instanceof ScheduledExecution && execInfo?.uuid}">
                <div><span class="jobuuid desc" title="UUID for this job">UUID: ${execInfo?.uuid.encodeAsHTML()}</span></div>
                <div><span class="jobid desc" title="internal ID for this job">ID: ${execInfo?.id}</span></div>
            </g:if>

        </div>
        