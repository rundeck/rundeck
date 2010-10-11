    <table>
        <tr>
            <td>

    <table width="300px">
        
        <g:each in="${['hostname','osFamily','osArch','osVersion','osName','type','username']}" var="key">
            <tr><td class="key"><g:message code="${'node.metadata.'+key}"/></td>
                <td class="value">${node[key]}</td></tr>
        </g:each>
        <tr><td class="key"><g:message code="node.metadata.tags"/></td>
            <td class="value">${node['tags']?node['tags'].join(','):''}</td></tr>
    </table>

            </td>
            <td style="vertical-align:top">
                <g:if test="${node.settings}">
                    <table width="300px">
                        <tr><th colspan="2" style="font-size:9pt;">Settings</th></tr>
                        <g:each var="setting" in="${node.settings.keySet()}">
                            <tr>
                                <td class="key setting">${setting}:</td>
                                <td class="setting Value">${node.settings[setting]}</td>
                            </tr>
                        </g:each>
                    </table>
                </g:if>
            </td>
        </tr>
    </table>