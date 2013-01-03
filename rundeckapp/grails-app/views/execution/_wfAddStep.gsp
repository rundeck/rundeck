<span><g:message code="${addMessage}"/></span>
<g:if test="${descriptionMessage}">
    <span><g:message code="${descriptionMessage}"/></span>
</g:if>
<div class="info note"><g:message code="${chooseMessage}"/></div>

<div style="margin:10px;" class=" add_step_buttons">

        <table>
            <tbody class="node_step_section">
            <tr>
                <th>
                    <span class="prompt">Node Steps:</span> <span
                        class="info note">Executes for each matched Node</span>
                </th>
            </tr>
            <tr>
                <td>
                    <div class="action textbtn add_node_step_type"
                         data-node-step-type="command"
                         title="Click to add a Command step">
                        <g:img file="icon-small-shell.png" width="16px" height="16px"/>
                        Command - <span class="info note">Execute a remote command</span>
                    </div>
                </td>
            </tr>
            <tr>
                <td>
                    <div class="action textbtn  add_node_step_type"
                          data-node-step-type="script"
                          title="Click to add a Script step">
                        <g:img file="icon-small-shell.png" width="16px" height="16px"/>
                        Script - <span class="info note">Execute an inline script</span>
                    </div>
                    </td>
                </tr>
            <tr>
                <td>
                    <div class="action textbtn  add_node_step_type"
                          data-node-step-type="scriptfile"
                          title="Click to add a Script file or URL step">
                        <g:img file="icon-small-shell.png" width="16px" height="16px"/>
                        Script file or URL
                        - <span class="info note">Execute a local script file or a script from a URL</span>
                    </div>
                </td>
            </tr>
            <g:if test="${nodeStepDescriptions}">
                <tr>
                    <th>
                        Node Step Plugins
                    </th>
                </tr>
                        <g:each in="${nodeStepDescriptions}" var="typedesc">

                            <tr>
                                <td>
                                <div class="action textbtn  add_node_step_type"
                                  data-node-step-type="${typedesc.name.encodeAsHTML()}"
                                  title="Click to add a ${typedesc.title?.encodeAsHTML()} step">
                                ${typedesc.title?.encodeAsHTML()}
                             - <span class="info note">${typedesc.description.encodeAsHTML()}</span>
                                </div>
                            </td>
                            </tr>
                        </g:each>
            </g:if>
            </tbody>
            <tbody class="step_section ">
            <tr>
                <th>
                    <span class="prompt">Steps:</span> <span class="info note">Executes once in the workflow</span>
                </th>
            </tr>
            <tr>
                <td>
                    <div class="action textbtn  add_step_type"
                          data-step-type="job"
                          title="Click to add a Job Reference step">
                        <g:img file="icon-small-job.png" width="16px" height="16px"/>
                        Job Reference - <span class="info note">Execute another Job</span>
                    </div>
                </td>
            </tr>
            <g:if test="${stepDescriptions}">
                <tr>
                    <th>Step Plugins</th>
                </tr>
                        <g:each in="${stepDescriptions}" var="typedesc">

                            <tr>
              <td>
                      <div class="action textbtn  add_step_type"
                            data-step-type="${typedesc.name.encodeAsHTML()}"
                                  title="Click to add a ${typedesc.title.encodeAsHTML()} step">
                                ${typedesc.title.encodeAsHTML()}
                             - <span class="info note">${typedesc.description.encodeAsHTML()}</span>
                            </div>
              </td>
                            </tr>
                        </g:each>

            </g:if>
        </tbody>
        </table>
</div>

<div style="margin:10px; text-align:right;">
    <span class="action button small cancel_add_step_type" title="Cancel adding new item">Cancel</span>
</div>