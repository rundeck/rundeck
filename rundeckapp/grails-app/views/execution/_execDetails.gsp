<g:set var="rkey" value="${g.rkey()}"/>

<table class="simpleForm" cellpadding="0" cellspacing="0">

    <g:if test="${execdata!=null && execdata.id && execdata instanceof ScheduledExecution && execdata.scheduled}">
        <tr>
        <td >Schedule:</td>
        <td>
            <g:render template="/scheduledExecution/showCrontab" model="${[scheduledExecution:execdata,crontab:crontab]}"/>
        </td>
        </tr>
        <g:if test="${nextExecution}">
            <tr>
            <td></td>
            <td>
              Next execution
                    <g:relativeDate elapsed="${nextExecution}" untilClass="timeuntil"/>
                    at <span class="timeabs">${nextExecution}</span>
            </td>
            </tr>
        </g:if>
    </g:if>

    <tr>
        <td>Project:</td>
        <td>${execdata?.project}</td>
    </tr>
    <g:if test="${execdata instanceof ExecutionContext && execdata?.workflow}">
        <g:if test="${execdata instanceof ScheduledExecution && execdata.options}">
            <tr>
                <td>Options:</td>
                <td >
                    <g:render template="/scheduledExecution/optionsSummary" model="${[options:execdata.options]}"/>
                </td>
            </tr>
        </g:if>
        <g:if test="${execdata.argString}">
            <tr>
                <td>
                    Options:
                </td>
                <td>
                    <span class="argString">${execdata?.argString.encodeAsHTML()}</span>
                </td>
            </tr>
        </g:if>
        <tr>
            <td>Workflow:</td>
            <td>
                <g:render template="/execution/execDetailsWorkflow" model="${[workflow:execdata.workflow,context:execdata]}"/>
            </td>
        </tr>
    </g:if>
    <tr>
        <td>Log level:</td>
        <td>
            ${execdata?.loglevel}
        </td>
    </tr>
    <g:set var="NODE_FILTERS" value="${['','Name','Type','Tags','OsName','OsFamily','OsArch','OsVersion']}"/>
    <g:set var="NODE_FILTER_MAP" value="${['':'Hostname','OsName':'OS Name','OsFamily':'OS Family','OsArch':'OS Architecture','OsVersion':'OS Version']}"/>

    <g:if test="${execdata?.doNodedispatch}">
    <tbody class="section">
    <tr>
        <td>Include Nodes:</td>
        <td>
            <table>
                <g:each var="key" in="${NODE_FILTERS}">
                    <g:if test="${execdata?.('nodeInclude'+key)}">
                        <tr>
                            <td>
                        ${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}:
                        </td>
                            <td>
                        ${execdata?.('nodeInclude'+key).encodeAsHTML()}
                            </td>
                        </tr>
                    </g:if>

                </g:each>
            </table>

        </td>
    </tr>
    <tr>
        <td>Exclude Nodes:</td>
        <td>
            
            <table>
                <g:each var="key" in="${NODE_FILTERS}">
                    <g:if test="${execdata?.('nodeExclude'+key)}">
                        <tr>
                            <td>
                        ${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}:
                        </td>
                            <td>
                        ${execdata?.('nodeExclude'+key).encodeAsHTML()}
                            </td>
                        </tr>
                    </g:if>

                </g:each>
            </table>
        </td>
    </tr>
        <tr>
            <td>Matched Nodes:</td>
            <td id="matchednodes_${rkey}" class="matchednodes embed">
                <g:set var="jsdata" value="${execdata.properties.findAll{it.key==~/^node(In|Ex)clude.*$/ &&it.value}}"/>
                <g:javascript>
                    var nodeFilterData_${rkey}=${jsdata.encodeAsJSON()};
                </g:javascript>
                <span class="action textbtn depress2 receiver" title="Display matching nodes" onclick="_updateMatchedNodes(nodeFilterData_${rkey},'matchednodes_${rkey}','${execdata?.project}')">Show Matches</span>

            </td>

        </tr>
    </tbody>
    </g:if>
    <g:else>
        <tbody class="section">
        <tr>
            <td>Node:</td>
            <td id="matchednodes_${rkey}" class="matchednodes embed">
                <span class="action textbtn depress2 receiver"  title="Display matching nodes" onclick="_updateMatchedNodes({},'matchednodes_${rkey}','${execdata?.project}',true)">Show Matches</span>
                
            </td>
        </tr>
        </tbody>
    </g:else>
    <g:if test="${execdata?.doNodedispatch}">

        <tr>
            <td>Thread Count:</td>
            <td>${execdata?.nodeThreadcount}</td>
        </tr>
        <tr>
            <td>Keep going:</td>
            <td>${execdata?.nodeKeepgoing}</td>
        </tr>
    </g:if>
    <g:if test="${execdata instanceof ScheduledExecution && execdata.notifications}">
        <g:each var="notify" in="${execdata.notifications}">
            <tr>
                <td>Notify <g:message code="notification.event.${notify.eventTrigger}"/>:</td>
                <td>${notify.content.encodeAsHTML()}</td>
            </tr>
        </g:each>
    </g:if>
   
    
</table>
