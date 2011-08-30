<%--
used by _editOptions.gsp template
--%>
<g:set var="rkey" value="${g.rkey()}"/>
<g:if test="${ optionSelections}">
    <g:set var="usePrefix" value="${paramsPrefix?paramsPrefix:''}"/>
    <g:set var="showDTFormat" value="${false}"/>
    <g:hiddenField name="${usePrefix+'argString'}" value=""/>
    <%
        def optsmap=[:]
        if(optionSelections){
            optionSelections.each{
                optsmap[it.name]=[selopt:it]
            }
        }
    %>
    <table>
        <tr>
            <td style="vertical-align:top">
    <table class="simpleForm">
        <g:each var="optName" in="${optsmap.keySet().sort()}">
            <g:set var="optionSelect" value="${optsmap[optName].selopt }"/>
            <g:set var="optRequired" value="${optionSelect.required}"/>
            <g:set var="optDescription" value="${optionSelect.description}"/>
            <g:set var="fieldName" value="${usePrefix+'option.'+optName}"/>
            <g:set var="optionHasValue" value="${optionSelect.defaultValue || selectedoptsmap && selectedoptsmap[optName]}"/>
            <g:set var="hasError" value="${jobexecOptionErrors?jobexecOptionErrors[optName]:null}"/>
            <g:set var="fieldNamekey" value="${rkey+'_'+optName+'_label'}"/>
            <tr>
                <td class="${hasError?'fieldError':''} remoteoptionfield" id="${fieldNamekey}"><span style="display:none;" class="remotestatus"></span> ${optName}:</td>
                <td>
                    <g:if test="${optionSelect.valuesUrl !=null}">
                        <g:set var="holder" value="${rkey+'_'+optName+'_hold'}"/>
                        <span id="${holder}" >
                        </span>
                        <g:javascript>
                            _loadRemoteOptionValues("${holder}",'${scheduledExecutionId}','${optName}','${usePrefix}','${selectedoptsmap?selectedoptsmap[optName]:''}','${fieldNamekey}',true);
                        </g:javascript>
                    </g:if>
                    <g:else>
                        <g:render template="/framework/optionValuesSelect"
                            model="${[elemTarget:rkey+'_'+optName,optionSelect:optionSelect, fieldPrefix:usePrefix,fieldName:'option.'+optName,selectedoptsmap:selectedoptsmap]}"/>
                    </g:else>

                    <span id="${optName.encodeAsHTML()+'_state'}">
                        <g:if test="${ optRequired }">
                            <span class="reqwarning" style="${wdgt.styleVisible(unless:optionHasValue)}">
                            <img src="${resource( dir:'images',file:'icon-small-warn.png' )}" class="warnimg"
                                 alt="Required Option" title="Required Option"  width="16px" height="16px" />
                                <g:if test="${hasError && hasError.contains('required')}">
                                    <span class="error label">${hasError}</span>
                                </g:if>
                            </span>
                        </g:if>
                        <g:if test="${hasError && !hasError.contains('required')}">
                            <span class="error label">${hasError}</span>
                        </g:if>
                    </span>
                    <div class="info note">${optDescription}</div>
                </td>
            </tr>
        </g:each>
    </table>
    </td>
   <g:if test="${showDTFormat}">
       <td style="vertical-align:top" >
     <div class="info note help">

        <g:expander key="argStringDateFormatHelp">datestamp format</g:expander>

        <table id="argStringDateFormatHelp" style="display:none">
            <thead>
                <tr >
                    <th colspan="4">
                        <span style="color:green">$<!-- -->{DATE:<em>XXYYZZ</em>}</span> can be used with these formatting characters:
                    </th>
                </tr>
            </thead>
            <tr>
                <td>y</td> 	<td>Year</td>
                <td>M</td> 	<td>Month in year</td>
            </tr>
            <tr>
                <td>w</td> 	<td>Week in year</td>
                <td>W</td> 	<td>Week in month</td>
            </tr>
            <tr>
                <td>D</td> 	<td>Day in year</td>
                <td>d</td> 	<td>Day in month</td>
            </tr>
            <tr>
                <td>a</td> 	<td>Am/pm marker</td>
                <td>H</td> 	<td>Hour in day (0-23)</td>
            </tr>
            <tr>
                <td>k</td> 	<td>Hour in day (1-24)</td>
                <td>K</td> 	<td>Hour in am/pm (0-11)</td>
            </tr>
            <tr>
                <td>h</td> 	<td>Hour in am/pm (1-12)</td>
                <td>m</td> 	<td>Minute in hour</td>
            </tr>
            <tr>
                <td>s</td> 	<td>Second in minute</td>
                <td>S</td> 	<td>Millisecond</td>
            </tr>
            <!--<tr><td>Z</td> 	<td>Time zone</td></tr>-->
        </table>
    </div>
    </td>
    </g:if>

        </tr>
    </table>
</g:if>
<g:elseif test="${notfound}">
    <div class="info note">Choose a valid command (notfound).</div>
    <g:if test="${selectedargstring}"><div>Old value: ${selectedargstring}</div></g:if>
</g:elseif>
<g:elseif test="${!authorized}">
    <div class="info note">Not authorized to execute chosen command.</div>
    <g:if test="${selectedargstring}"><div>Old value: ${selectedargstring}</div></g:if>
</g:elseif>
<g:else>
    <span class="info note">None for this job</span>
</g:else>