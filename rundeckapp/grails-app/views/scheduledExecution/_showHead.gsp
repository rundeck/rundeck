        <g:set var="execInfo" value="${scheduledExecution?scheduledExecution:execution}"/>
        <div class="jobInfo" id="jobInfo_${execution?execution.id:''}">
            <g:if test="${scheduledExecution}">
                <g:link controller="scheduledExecution" action="show" id="${scheduledExecution?.id}">
                    <g:if test="${iconName}">
                        <img src="${resource(dir:'images',file:iconName+'.png')}" alt="job" style="border:0;"/>
                    </g:if>
                    <g:else>
                        <g:set var="fileName" value="${scheduledExecution.scheduled?'clock':'job'}"/>
                        <g:if test="${execution}">
                            <g:set var="fileName" value="${execution.status=='true'?'job-ok':null==execution.dateCompleted?'job-running':execution.cancelled?'job-warn':'job-error'}"/>
                        </g:if>
                        <img src="${resource(dir:'images',file:"icon-"+fileName+".png")}" alt="job" style="border:0;"/>
                    </g:else>
                <span class="jobName">${scheduledExecution?.jobName.encodeAsHTML()}</span></g:link>
            </g:if>
            <g:else>
                <span class="jobIcon">
                    <g:if test="${iconName}">
                        <img src="${resource(dir:'images',file:iconName+'.png')}" alt="job" style="border:0;"/>
                    </g:if>
                    <g:else>

                        <g:set var="fileName" value="job"/>
                        <g:if test="${execution}">
                            <g:set var="fileName" value="${execution.status=='true'?'job-ok':null==execution.dateCompleted?'job-running':execution.cancelled?'job-warn':'job-error'}"/>
                        </g:if>
                        <img src="${resource(dir:'images',file:"icon-"+fileName+".png")}" alt="job" style="border:0;"/>
                    </g:else>
                </span>
          </g:else>
            <span class="jobInfoPart">
                <g:if test="${execInfo instanceof ScheduledExecution && execInfo?.description}"><span class="jobDesc">${execInfo?.description.encodeAsHTML()}</span></g:if>
                <g:if test="${execInfo instanceof ScheduledExecution && execInfo.groupPath}">
                    <span class="jobGroup">
                   <span class="grouplabel"><g:link controller="menu" action="jobs" params="${[groupPath:execInfo.groupPath]}" title="${'View '+g.message(code:'domain.ScheduledExecution.title')+'s in this group'}">

                        <img src="${resource(dir:'images',file:'icon-small-folder.png')}" width="16px" height="15px" alt=""/>
                        ${execInfo.groupPath}

                    </g:link>
                    </span>
                    </span>
                </g:if>
            </span>
            
            <g:if test="${execution}">
                <span class="jobInfoPart">
                    <span class="partContent">
                    <img src="${resource(dir:'images',file:'icon-tiny-rarrow-sep.png')}" alt=""/>
                    <g:link
                        controller="execution"
                        action="show"
                        id="${execution.id}"
                        params="${followparams?.findAll{it.value}}">Execution at <g:relativeDate atDate="${execution.dateStarted}" /> by <span class="username">${execution.user}</span></g:link>
                    </span>
                </span>
            </g:if>

        </div>
        