<%--
TODO: Remove resources stuff
--%>
<table >
    <g:if test="${projects}">
        <tr class="project_group"><td class="key">Projects</td>
            <td class="value">
                <g:each in="${projects}" var="project">
                    ${project.name}
                    <g:img file="icon-tiny-edit.png" width="12px" height="12px"/>
                </g:each>
            </td>
        </tr>
    </g:if>
    <g:each in="${['entity.deployment-basedir','entity.deployment-install-root','entity.deployment-startup-rank','entity.tags']}" var="key">
        <g:if test="${null!=resource.properties[key]}">
        <tr><td class="key"><g:message code="${'resource.metadata.'+key}"/></td>
            <td class="value">${resource.properties[key]}</td></tr>
        </g:if>
    </g:each>
</table>
