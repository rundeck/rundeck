<g:set var="rkey" value="${g.rkey()}"/>

<table class="simpleForm" cellpadding="0" cellspacing="0">

    <g:if test="${execdata!=null && execdata.id && execdata instanceof ScheduledExecution && execdata.scheduled}">
        <tr>
        <td >Schedule:</td>
        <td colspan="3">
            <g:render template="/scheduledExecution/showCrontab" model="${[scheduledExecution:execdata,crontab:crontab]}"/>
        </td>
        </tr>
        <g:if test="${nextExecution}">
            <tr>
            <td></td>
            <td colspan="3">
              Next execution
                    <g:relativeDate elapsed="${nextExecution}" untilClass="timeuntil"/>
                    at <span class="timeabs">${nextExecution}</span>
            </td>
            </tr>
        </g:if>
    </g:if>
    <g:if test="${execdata!=null && execdata.id && execdata instanceof ScheduledExecution && execdata.multipleExecutions}">
        <tr>
        <td ><g:message code="scheduledExecution.property.multipleExecutions.label"/></td>
        <td colspan="3">
            <g:message code="yes" />
        </td>
        </tr>
    </g:if>

    <g:if test="${execdata instanceof ExecutionContext && execdata?.workflow}">
        <tr>
            <td>Workflow:</td>
            <td colspan="3">
                <g:render template="/execution/execDetailsWorkflow" model="${[workflow:execdata.workflow,context:execdata,noimgs:noimgs,project:execdata.project]}"/>
            </td>
        </tr>
        <g:if test="${execdata instanceof ScheduledExecution && execdata.options}">
            <tr>
                <td>Options:</td>
                <td  colspan="3">
                    <g:render template="/scheduledExecution/optionsSummary" model="${[options:execdata.options]}"/>
                </td>
            </tr>
        </g:if>
        <g:if test="${execdata.argString}">
            <tr>
                <td>
                    Options:
                </td>
                <td colspan="3">
                    <span class="argString">${execdata?.argString.encodeAsHTML()}</span>
                </td>
            </tr>
        </g:if>
    </g:if>
    <tr>
        <td>Log level:</td>
        <td colspan="3">
            ${execdata?.loglevel}
        </td>
    </tr>
    <g:set var="NODE_FILTERS" value="${['','Name','Tags','OsName','OsFamily','OsArch','OsVersion']}"/>
    <g:set var="NODE_FILTER_MAP" value="${['':'Hostname','OsName':'OS Name','OsFamily':'OS Family','OsArch':'OS Architecture','OsVersion':'OS Version']}"/>

    <g:if test="${execdata?.doNodedispatch}">
    <tbody class="section">
    <g:if test="${!nomatchednodes}">
            <tr>
                <td>Node Filters:</td>
                <td id="matchednodes_${rkey}" class="matchednodes embed" colspan="3">
                    <g:set var="jsdata" value="${execdata.properties.findAll{it.key==~/^node(In|Ex)clude.*$/ &&it.value}}"/>
                    <g:javascript>
                        _g_nodeFilterData['${rkey}']=${jsdata.encodeAsJSON()};
                    </g:javascript>
                    <span class="action textbtn  textbtn query " title="Display matching nodes" onclick="_updateMatchedNodes(_g_nodeFilterData['${rkey}'],'matchednodes_${rkey}','${execdata?.project}',false,{requireRunAuth:true});">
                        <g:render template="/framework/displayNodeFilters" model="${[displayParams:execdata]}"/>
                    </span>

                </td>

            </tr>
        </g:if>
        %{--<tr>--}%
    %{--<g:if test="${NODE_FILTERS.find{execdata?.('nodeInclude'+it)}}">--}%

        %{--<td>Include Nodes:</td>--}%
        %{--<td>--}%
            %{--<table>--}%
                %{--<g:each var="key" in="${NODE_FILTERS}">--}%
                    %{--<g:if test="${execdata?.('nodeInclude'+key)}">--}%
                        %{--<tr>--}%
                            %{--<td>--}%
                        %{--${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}:--}%
                        %{--</td>--}%
                            %{--<td>--}%
                        %{--${execdata?.('nodeInclude'+key).encodeAsHTML()}--}%
                            %{--</td>--}%
                        %{--</tr>--}%
                    %{--</g:if>--}%

                %{--</g:each>--}%
            %{--</table>--}%

        %{--</td>--}%
    %{--<!--</tr>-->--}%
    %{--</g:if>--}%
    %{--<g:if test="${NODE_FILTERS.find{execdata?.('nodeExclude'+it)}}">--}%
        %{--<!--<tr>-->--}%
            %{--<td class="displabel">Exclude Nodes:</td>--}%
            %{--<td>--}%

                %{--<table>--}%
                    %{--<g:each var="key" in="${NODE_FILTERS}">--}%
                        %{--<g:if test="${execdata?.('nodeExclude'+key)}">--}%
                            %{--<tr>--}%
                                %{--<td>--}%
                            %{--${NODE_FILTER_MAP[key]?NODE_FILTER_MAP[key]:key}:--}%
                            %{--</td>--}%
                                %{--<td>--}%
                            %{--${execdata?.('nodeExclude'+key).encodeAsHTML()}--}%
                                %{--</td>--}%
                            %{--</tr>--}%
                        %{--</g:if>--}%

                    %{--</g:each>--}%
                %{--</table>--}%
            %{--</td>--}%

    %{--</g:if>--}%
    %{--</tr>--}%

    </tbody>
    </g:if>
    <g:else>
        <g:if test="${!nomatchednodes}">
        <tbody class="section">
        <tr>
            <td>Node:</td>
            <td id="matchednodes_${rkey}" class="matchednodes embed"  colspan="3">
                <span class="action textbtn depress2 receiver"  title="Display matching nodes" onclick="_updateMatchedNodes({},'matchednodes_${rkey}','${execdata?.project}', true, {requireRunAuth:true})">Show Matches</span>
                
            </td>
        </tr>
        </tbody>
        </g:if>
    </g:else>
    %{--<tr>--}%
        %{--<td>Project:</td>--}%
        %{--<td  colspan="3">${execdata?.project}</td>--}%
    %{--</tr>--}%
    <g:if test="${execdata?.doNodedispatch}">

        <tr>
            <td>Thread Count:</td>
            <td>${execdata?.nodeThreadcount}</td>
            <td class="displabel">Keep going:</td>
            <td>${execdata?.nodeKeepgoing}</td>
        </tr>
        <tr>
            <td><g:message code="scheduledExecution.property.nodeRankAttribute.label"/>:</td>
            <td>${execdata?.nodeRankAttribute? execdata?.nodeRankAttribute.encodeAsHTML() : 'Node Name'}</td>
            <td class="displabel"><g:message code="scheduledExecution.property.nodeRankOrder.label"/>:</td>
            <td><g:message code="scheduledExecution.property.nodeRankOrder.${null==execdata?.nodeRankOrderAscending || execdata?.nodeRankOrderAscending?'ascending':'descending'}.label"/></td>
        </tr>
    </g:if>
    <g:if test="${execdata instanceof ScheduledExecution && execdata.notifications}">
        <tr>
        <g:each var="notify" in="${execdata.notifications}" status="i">
                <td class="displabel">Notify <g:message code="notification.event.${notify.eventTrigger}"/>:</td>
                <td>
                    <g:if test="${notify.type=='url'}">
                        <g:expander key="webhook${rkey}_${i}">Webhook</g:expander>
                        <span class="webhooklink note" id="webhook${rkey}_${i}" style="display:none;" title="URLs: ${notify.content.encodeAsHTML()}">${notify.content.encodeAsHTML()}</span>
                    </g:if>
                    <g:else>
                        ${notify.content.encodeAsHTML()}
                    </g:else>
                </td>

        </g:each>
        </tr>
    </g:if>
   
    
</table>
