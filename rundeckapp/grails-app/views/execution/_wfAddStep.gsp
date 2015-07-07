<div class="panel-heading">
    <span class="h3 "><g:message code="${addMessage}"/></span>
</div>
<div class=" add_step_buttons panel-body">
<div class="row">
    <div class="col-sm-12">
        <g:if test="${descriptionMessage}">
            <span><g:message code="${descriptionMessage}"/></span>
        </g:if>
        <div class="h4"><g:message code="${chooseMessage}"/></div>
    </div>
</div>
<div class="row row-space">
<div class="col-sm-12">
    <ul class="nav nav-tabs" >
            <li class="active node_step_section"><a href="#addnodestep" data-toggle="tab">Node Steps</a></li>
            <li class="step_section"><a href="#addwfstep" data-toggle="tab">Workflow Steps</a></li>
    </ul>
    <div class="tab-content">
            <div class="node_step_section tab-pane active " id="addnodestep">
                <div class="list-group">
                <div class="list-group-item">
                    <span class=" list-group-item-heading h4 text-info">
                        Node Steps execute for each matched Node
                    </span>
                </div>
                    <a class="list-group-item  add_node_step_type"
                             data-node-step-type="command"
                        href="#"
                             >
                            <i class="rdicon icon-small shell"></i>
                            Command <span class="text-info">- Execute a remote command</span>
                    </a>
                    <a class="list-group-item textbtn  add_node_step_type" href="#"
                              data-node-step-type="script"
                              >
                        <i class="rdicon icon-small script"></i>
                            Script <span class="text-info">- Execute an inline script</span>
                    </a>
                    <a class="list-group-item textbtn  add_node_step_type" href="#"
                              data-node-step-type="scriptfile"
                              >
                        <i class="rdicon icon-small scriptfile"></i>
                            Script file or URL
                            <span class="text-info">- Execute a local script file or a script from a URL</span>
                    </a>
                    <a class="list-group-item textbtn add_node_step_type" data-node-step-type="job" href="#">
                        <i class="glyphicon glyphicon-book"></i>
                        Job Reference <span class="text-info">- Execute another Job for each Node</span>
                    </a>
                <g:if test="${nodeStepDescriptions}">
                    <div class="list-group-item text-muted ">
                        <g:plural for="${nodeStepDescriptions}" code="node.step.plugin" />
                    </div>
                    <g:each in="${nodeStepDescriptions.sort{a,b->a.name<=>b.name}}" var="typedesc">

                        <a  class="list-group-item textbtn  add_node_step_type" data-node-step-type="${enc(attr:typedesc.name)}"
                            href="#">
                            <i class="rdicon icon-small plugin"></i>
                            <g:enc>${typedesc.title}</g:enc>
                            <span class="text-info">-
                            <g:render template="/scheduledExecution/description"
                                      model="[description: typedesc.description, textCss: '',
                                              mode: 'hidden', rkey: g.rkey()]"/>
                            </span>
                        </a>
                    </g:each>
                </g:if>
                </div>
            </div>
            <div class="step_section tab-pane " id="addwfstep">
                <div class="list-group">
                <div class="list-group-item">
                    <span class=" list-group-item-heading h4 text-info">
                        Workflow Steps execute once in the workflow
                    </span>
                </div>

                <a class="list-group-item textbtn add_step_type" data-step-type="job" href="#">
                    <i class="glyphicon glyphicon-book"></i>
                    Job Reference <span class="text-info">- Execute another Job</span>
                </a>
                <g:if test="${stepDescriptions}">
                    <div class="list-group-item text-muted ">
                        <g:plural for="${stepDescriptions}" code="workflow.step.plugin" />
                    </div>
                    <g:each in="${stepDescriptions.sort{a,b->a.name<=>b.name}}" var="typedesc">
                        <a class="list-group-item textbtn  add_step_type" data-step-type="${enc(attr:typedesc.name)}" href="#">
                            <i class="rdicon icon-small plugin"></i>
                            <g:enc>${typedesc.title}</g:enc>
                            <span class="text-info">-
                                <g:render template="/scheduledExecution/description"
                                          model="[description: typedesc.description, textCss: '',
                                                  mode: 'hidden', rkey: g.rkey()]"/>
                            </span>
                        </a>
                    </g:each>

                </g:if>
                </div>
            </div>
    </div>
</div>
</div>
</div>

<div class="panel-footer">
    <span class="btn btn-default btn-sm cancel_add_step_type" >Cancel</span>
</div>
