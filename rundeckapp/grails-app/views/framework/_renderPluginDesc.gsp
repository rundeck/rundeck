<%@ page contentType="text/html;charset=UTF-8" %>
<g:if test="${description}">

    <g:if test="${showPluginIcon}">
        <stepplugin:pluginIcon service="${serviceName}"
                               name="${description.name}"
                               width="16px"
                               height="16px">
            <i class="rdicon icon-small plugin"></i>
        </stepplugin:pluginIcon>
    </g:if>
    <g:if test="${showNodeIcon}">
        <i class="rdicon icon-small node"></i>
    </g:if>
    <span class=" text-info">
        <g:if test="${!hideTitle}">
            <stepplugin:message
                    service="${serviceName}"
                    name="${description.name}"
                    code="plugin.title"
                    default="${description.title}"/>
        </g:if>
    </span>
    <g:if test="${!hideDescription}">
        <g:if test="${!fullDescription}">

            <g:render template="/scheduledExecution/description"
                      model="[description: stepplugin.messageText(
                              service: serviceName,
                              name: description.name,
                              code: 'plugin.description',
                              default: description.description
                      ),
                              service    : serviceName,
                              name       : description.name,
                              markdownCss: 'small text-muted',
                              textCss    : 'small text-muted',
                              mode       : 'collapsed', rkey: g.rkey()]"/>
        </g:if>
        <g:else>
            <small class="text-muted"><stepplugin:message
                    service="${serviceName}"
                    name="${description.name}"
                    code="plugin.description"
                    default="${description.description}"/></small>

        </g:else>
    </g:if>

</g:if>