<span><g:message code="${addMessage}"/></span>
<g:if test="${descriptionMessage}">
    <span><g:message code="${descriptionMessage}"/></span>
</g:if>
<div class="info note"><g:message code="${chooseMessage}"/></div>

<div style="margin:10px;" class="node_step_section">
    <span class="prompt">Node Steps:</span>

    <div class="presentation">
        <span class="info note">Executes for each matched Node</span>
        <table>
            <tr>
                <td>Built-in:</td>
                <td>
                    <span class="button action add_node_step_type"
                          data-node-step-type="command"
                          title="Execute a remote command"><g:img file='icon-tiny-add.png'/> Command</span>
                    <span class="button action add_node_step_type"
                          data-node-step-type="script"
                          title="Execute  an inline script"><g:img file='icon-tiny-add.png'/> Script</span>
                    <span class="button action add_node_step_type"
                          data-node-step-type="scriptfile"
                          title="Execute a script file or URL"><g:img
                            file='icon-tiny-add.png'/> Script file or URL</span>
                </td>
            </tr>
            <g:if test="${nodeStepDescriptions}">
                <tr>
                    <td>Plugins:</td>
                    <td>

                        <g:each in="${nodeStepDescriptions}" var="typedesc">
                            <span class="button action add_node_step_type"
                                  data-node-step-type="${typedesc.name.encodeAsHTML()}"
                                  title="${typedesc.title.encodeAsHTML()}: ${typedesc.description.encodeAsHTML()}">
                                <g:img file='icon-tiny-Reportcenter.png'/>
                                ${typedesc.title.encodeAsHTML()}
                            </span>
                        </g:each>
                    </td>
                </tr>
            </g:if>
        </table>
    </div>
</div>

<div style="margin:10px;" class="step_section">
    <span class="prompt">Steps:</span>

    <div class="presentation">
        <span class="info note">Executes once in the workflow</span>
        <table>
            <tr>
                <td>Built-in:</td>
                <td>
                    <span class="button action add_step_type"
                          data-step-type="job"
                          title="Execute another Job"><g:img
                            file='icon-tiny-add.png'/> Job Reference</span>
                </td>
            </tr>
            <g:if test="${stepDescriptions}">
                <tr>
                    <td>Plugins:</td>
                    <td>
                        <g:each in="${stepDescriptions}" var="typedesc">
                            <span class="button action add_step_type"
                                  data-step-type="${typedesc.name.encodeAsHTML()}"
                                  title="${typedesc.title.encodeAsHTML()}: ${typedesc.description.encodeAsHTML()}">
                                <g:img file='icon-tiny-Reportcenter.png'/>
                                ${typedesc.title.encodeAsHTML()}
                            </span>
                        </g:each>
                    </td>
                </tr>
            </g:if>
        </table>
    </div>
</div>

<div style="margin:10px; text-align:right;">
    <span class="action button small cancel_add_step_type" title="Cancel adding new item">Cancel</span>
</div>