<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants; com.dtolabs.rundeck.core.plugins.configuration.PropertyScope" %>
<g:set var="groupSet" value="${[:]}"/>
<g:set var="secondary" value="${[]}"/>
<g:set var="ungrouped" value="${[]}"/>

<g:each in="${properties}" var="prop">
    <g:set var="scopeUnset" value="${!prop.scope || prop.scope.isUnspecified()}"/>
    <g:set var="scopeProject" value="${prop.scope && prop.scope.isProjectLevel()}"/>
    <g:set var="scopeInstance" value="${prop.scope && prop.scope.isInstanceLevel()}"/>
    <g:set var="scopeFramework" value="${prop.scope && prop.scope.isFrameworkLevel()}"/>

    <g:set var="scopeTest" value="${scopeUnset ||
            (allowedScope == PropertyScope.Instance && scopeInstance) ||
            (allowedScope == PropertyScope.Project && scopeProject) ||
            (allowedScope == PropertyScope.Framework && scopeFramework)
    }"/>
    <g:if test="${scopeTest}">
    %{--determine grouping--}%
        <g:if test="${prop.renderingOptions?.get(StringRenderingConstants.GROUPING)?.toString() == 'secondary'}">
            %{--secondary grouping--}%
            <g:set var="groupName" value="${prop.renderingOptions?.get(StringRenderingConstants.GROUP_NAME)?.toString()?:'-'}"/>
            %{
                secondary<<groupName
            }%
            %{
                if(!groupSet[groupName]){
                    groupSet[groupName]=[prop]
                }else{
                    groupSet[groupName]<<prop
                }
            }%
        </g:if>
        <g:elseif test="${prop.renderingOptions?.get(StringRenderingConstants.GROUP_NAME)}">
            %{--primary grouping--}%
            <g:set var="groupName" value="${prop.renderingOptions?.get(StringRenderingConstants.GROUP_NAME)?.toString()}"/>
            %{
                if(!groupSet[groupName]){
                    groupSet[groupName]=[prop]
                }else{
                    groupSet[groupName]<<prop
                }
            }%
        </g:elseif>
        <g:else>
            %{--no grouping--}%
            %{
            ungrouped<<prop
            }%
        </g:else>
    </g:if>
</g:each>
%{--Render ungrouped--}%
<g:each in="${ungrouped}" var="prop">
    <g:render
            template="/framework/pluginConfigPropertyFormField"
            model="${[prop         : prop,
                      prefix       : prefix,
                      error        : report?.errors ? report.errors[prop.name] : null,
                      values       : values,
                      fieldname    : (fieldnamePrefix ?: '') + prop.name,
                      origfieldname: (origfieldnamePrefix ?: '') + prop.name
            ]}"/>
</g:each>
<g:set var="defaultGroupName" value="${g.message(code:'plugin.property.secondary.groupName',default:'More')}"/>
<g:each in="${groupSet.keySet()}" var="group">
    <g:set var="groupProps" value="${groupSet[group]}"/>
    <g:set var="gkey" value="${g.rkey()}"/>
    <g:set var="hasValue" value="${values && groupProps*.name.find{values[it]}}"/>
    <g:set var="isSecondary" value="${group in secondary}"/>

    <hr/>

    <div class="form-group">
        <span class="col-sm-12 ">
            <g:if test="${isSecondary}">

                <g:collapser text="${group!='-'?group:defaultGroupName}"
                             key="propgroup_${gkey}"
                             open="${hasValue?'true':'false'}"
                             classnames=" control-label input-lg"
                />

            </g:if>
            <g:else>
                <span class="control-label input-lg">${group!='-'?group:defaultGroupName}</span>
            </g:else>
        </span>
    </div>

    <div id="propgroup_${gkey}" class="${wdgt.css(if:isSecondary,then:'collapse collapse-expandable')} ${wdgt.css(if:hasValue,then:'in')}">
        <g:each in="${groupProps}" var="prop">
            <g:render
                    template="/framework/pluginConfigPropertyFormField"
                    model="${[prop         : prop,
                              prefix       : prefix,
                              error        : report?.errors ? report.errors[prop.name] : null,
                              values       : values,
                              fieldname    : (fieldnamePrefix ?: '') + prop.name,
                              origfieldname: (origfieldnamePrefix ?: '') + prop.name
                    ]}"/>
        </g:each>
    </div>
</g:each>