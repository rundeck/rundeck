<%@ page import="grails.converters.deep.JSON; grails.util.Environment" %>
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
    <table class="simpleForm" id="_commandOptions">
        <g:each var="optName" in="${optionordering?:optsmap.keySet().sort()}">
            <g:set var="optionSelect" value="${optsmap[optName].selopt }"/>
            <g:set var="optRequired" value="${optionSelect.required}"/>
            <g:set var="optDescription" value="${optionSelect.description}"/>
            <g:set var="fieldName" value="${usePrefix+'option.'+optName}"/>
            <g:set var="optionHasValue" value="${optionSelect.defaultValue || selectedoptsmap && selectedoptsmap[optName]}"/>
            <g:set var="hasError" value="${jobexecOptionErrors?jobexecOptionErrors[optName]:null}"/>
            <g:set var="fieldNamekey" value="${rkey+'_'+optName+'_label'}"/>
            <g:set var="fieldhiddenid" value="${rkey+'_'+optName+'_h'}"/>
            <tr>
                <td class="${hasError?'fieldError':''} remoteoptionfield" id="${fieldNamekey}"><span style="display:none;" class="remotestatus"></span> ${optName.encodeAsHTML()}:
                <g:if test="${Environment.current == Environment.DEVELOPMENT && grailsApplication.config.rundeck?.debug}">
                    (${optiondependencies? optiondependencies[optName]:'-'})(${dependentoptions? dependentoptions[optName]:'-'})
                </g:if>
                </td>
                %{--determine if option has all dependencies met--}%
                <g:set var="optionDepsMet" value="${!optiondependencies[optName] || selectedoptsmap && optiondependencies[optName].every {selectedoptsmap[it]}}" />
                <td>
                    <g:if test="${optionSelect.realValuesUrl !=null}">
                        <g:set var="holder" value="${rkey+'_'+optName+'_hold'}"/>
                        <span id="${holder}" >
                            <g:if test="${!optionDepsMet}">
                                <span class="info note">
                                    Select a value for these options: ${optiondependencies[optName].join(', ').encodeAsHTML()}
                                </span>
                            </g:if>
                                <g:hiddenField name="${fieldName}" value="${selectedoptsmap?selectedoptsmap[optName]:''}" id="${fieldhiddenid}"/>
                            <span class="loading"></span>
                        </span>
                        <g:if test="${Environment.current == Environment.DEVELOPMENT && grailsApplication.config.rundeck?.debug}">
                        <a onclick="_remoteOptionControl('_commandOptions').loadRemoteOptionValues('${optName.encodeAsJavaScript()}');return false;" href="#">${optName.encodeAsHTML()} reload</a>
                        </g:if>
                    </g:if>
                    <g:else>
                        <g:render template="/framework/optionValuesSelect"
                            model="${[elemTarget:rkey+'_'+optName,optionSelect:optionSelect, fieldPrefix:usePrefix,fieldName:'option.'+optName,selectedoptsmap:selectedoptsmap,fieldkey: fieldhiddenid]}"/>
                    </g:else>

                    <span id="${optName.encodeAsHTML()+'_state'}">
                        <g:if test="${ optRequired }">
                            <span class="reqwarning" style="${wdgt.styleVisible(unless:optionHasValue)}">
                            <img src="${resource( dir:'images',file:'icon-small-warn.png' )}" class="warnimg"
                                 alt="Required Option" title="Required Option"  width="16px" height="16px" />
                                <g:if test="${hasError && hasError.contains('required')}">
                                    <span class="error label">${hasError.encodeAsHTML()}</span>
                                </g:if>
                            </span>
                        </g:if>
                        <g:if test="${hasError && !hasError.contains('required')}">
                            <span class="error label">${hasError.encodeAsHTML()}</span>
                        </g:if>
                    </span>
                    <div class="info note">${optDescription?.encodeAsHTML()}</div>
                </td>
            </tr>
        </g:each>

        <%--
        Javascript for configuring remote option cascading/dependencies
        --%>
        <g:javascript>
            fireWhenReady('_commandOptions', function(){
                var remoteOptions = _remoteOptionControl('_commandOptions');
                <g:if test="${optionsDependenciesCyclic}">
                    remoteOptions.cyclic=true;
                </g:if>
        <g:each var="optName" in="${optionordering ?: optsmap.keySet().sort()}">
            <g:set var="optionSelect" value="${optsmap[optName].selopt}"/>
            <g:set var="fieldName" value="${usePrefix + 'option.' + optName}"/>
            <g:set var="fieldNamekey" value="${rkey + '_' + optName + '_label'}"/>
            <g:set var="holder" value="${rkey + '_' + optName + '_hold'}"/>
            <g:set var="fieldhiddenid" value="${rkey + '_' + optName + '_h'}"/>
            <g:set var="optionDepsMet"
                   value="${!optiondependencies[optName] || selectedoptsmap && optiondependencies[optName].every {selectedoptsmap[it]}}"/>
            <g:if test="${optiondependencies[optName]}">
                remoteOptions.addOptionDependencies("${optName.encodeAsJavaScript()}", ${optiondependencies[optName] as JSON});
            </g:if>
            <g:if test="${dependentoptions[optName]}">
                <%-- If option has dependents, register them to refresh when this option value changes --%>
                remoteOptions.addOptionDeps("${optName.encodeAsJavaScript()}", ${dependentoptions[optName] as JSON});


                <g:if test="${optionSelect.enforced}">
                <%-- Will be a drop down list, so trigger change automatically. --%>
                    remoteOptions.setOptionAutoReload("${optName.encodeAsJavaScript()}",true);
                </g:if>
            </g:if>
            <g:if test="${optionSelect.realValuesUrl != null}">
                <%-- If option has a remote URL, register data used for ajax reload --%>
                remoteOptions.addOption("${optName.encodeAsJavaScript()}","${holder.encodeAsJavaScript()}",'${scheduledExecutionId.encodeAsJavaScript()}','${optName.encodeAsJavaScript()}','${usePrefix.encodeAsJavaScript()}','${selectedoptsmap ? selectedoptsmap[optName]?.encodeAsJavaScript() : ''}','${fieldNamekey.encodeAsJavaScript()}',true);

                <g:if test="${!optiondependencies[optName] || optionsDependenciesCyclic}">
                    remoteOptions.loadonstart["${optName.encodeAsJavaScript()}"]=true;
                </g:if>
                <g:else>
                    remoteOptions.setOptionAutoReload("${optName.encodeAsJavaScript()}",true);
                </g:else>
                <g:if test="${optionSelect.multivalued}">
                    remoteOptions.setFieldMultiId('${optName.encodeAsJavaScript()}','${fieldhiddenid.encodeAsJavaScript()}');
                </g:if>
                <g:else>
                    remoteOptions.setFieldId('${optName.encodeAsJavaScript()}','${fieldhiddenid.encodeAsJavaScript()}');
                </g:else>
            </g:if>
            <g:else>
                    remoteOptions.addLocalOption("${optName.encodeAsJavaScript()}");
            </g:else>
        </g:each>
        <%-- register observers for field value changes --%>
                remoteOptions.observeChanges();
                if(typeof(_registerJobExecUnloadHandler)=='function'){
                    _registerJobExecUnloadHandler(remoteOptions.unload.bind(remoteOptions));
                }
            });
        </g:javascript>
    </table>
        <g:if test="${optionsDependenciesCyclic}">
            <g:message code="remote.options.warning.cyclicDependencies" />
        </g:if>
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
    <g:if test="${selectedargstring}"><div>Old value: ${selectedargstring.encodeAsHTML()}</div></g:if>
</g:elseif>
<g:elseif test="${!authorized}">
    <div class="info note">Not authorized to execute chosen command.</div>
    <g:if test="${selectedargstring}"><div>Old value: ${selectedargstring.encodeAsHTML()}</div></g:if>
</g:elseif>
<g:else>
    <span class="info note">None for this job</span>
</g:else>