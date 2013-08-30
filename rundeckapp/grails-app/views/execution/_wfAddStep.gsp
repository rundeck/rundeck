<span><g:message code="${addMessage}"/></span>
<g:if test="${descriptionMessage}">
    <span><g:message code="${descriptionMessage}"/></span>
</g:if>
<div class="info note"><g:message code="${chooseMessage}"/></div>

<div style="margin:10px;" class=" add_step_buttons">

            <div class="node_step_section">
                <span class="prompt">Node Steps:</span>
                <span class="info note">Executes for each matched Node</span>
                <ul>
                    <li class="action textbtn add_node_step_type"
                             data-node-step-type="command"
                             >
                            <g:img file="icon-small-shell.png" width="16px" height="16px"/>
                            Command <span class="info note">- Execute a remote command</span>
                    </li>
                    <li class="action textbtn  add_node_step_type"
                              data-node-step-type="script"
                              >
                            <g:img file="icon-small-shell.png" width="16px" height="16px"/>
                            Script <span class="info note">- Execute an inline script</span>
                    </li>
                    <li class="action textbtn  add_node_step_type"
                              data-node-step-type="scriptfile"
                              >
                            <g:img file="icon-small-shell.png" width="16px" height="16px"/>
                            Script file or URL
                            <span class="info note">- Execute a local script file or a script from a URL</span>
                    </li>
                    <li class="action textbtn add_node_step_type" data-node-step-type="job">
                        <g:img file="icon-small-job.png" width="16px" height="16px"/>
                        Job Reference <span class="info note">- Execute another Job for each Node</span>
                    </li>
                <g:if test="${nodeStepDescriptions}">
                    <li class="note">
                        <g:plural for="${nodeStepDescriptions}" code="node.step.plugin" verb="is" /> <g:message
                                code="installed"/>:
                    </li>
                    <g:each in="${nodeStepDescriptions}" var="typedesc">

                        <li  class="action textbtn  add_node_step_type" data-node-step-type="${typedesc.name.encodeAsHTML()}">
                            <g:img file="icon-small-Node.png" width="16px" height="16px"/>
                            ${typedesc.title?.encodeAsHTML()}
                            <span class="info note">- ${typedesc.description.encodeAsHTML()}</span>
                        </li>
                    </g:each>
                </g:if>
                </ul>
            </div>
            <div class="step_section ">
                <span class="prompt">Workflow Steps:</span> <span class="info note">Executes once in the workflow</span>
                <ul>
                    <li class="action textbtn  add_step_type" data-step-type="job">
                        <g:img file="icon-small-job.png" width="16px" height="16px"/>
                        Job Reference <span class="info note">- Execute another Job</span>
                    </li>
                <g:if test="${stepDescriptions}">
                    <li class="note">
                        <g:plural for="${stepDescriptions}" code="workflow.step.plugin" verb="is" /> <g:message code="installed" />:
                    </li>
                    <g:each in="${stepDescriptions}" var="typedesc">
                        <li class="action textbtn  add_step_type" data-step-type="${typedesc.name.encodeAsHTML()}">
                            &diams;
                            ${typedesc.title.encodeAsHTML()}
                            <span class="info note">- ${typedesc.description.encodeAsHTML()}</span>
                        </li>
                    </g:each>

                </g:if>
                </ul>
            </div>
</div>

<div style="margin:10px; ">
    <span class="btn btn-default btn-sm cancel_add_step_type" >Cancel</span>
</div>
