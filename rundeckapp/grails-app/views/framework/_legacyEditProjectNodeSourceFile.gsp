<g:form action="saveProjectNodeSourceFile" method="post"
        params="${[project: params.project, index: index]}"
        useToken="true"
        class="form-horizontal">
    <div class="">
        <div class="" id="createform">
            <div class="card-header">
                <h3 class="card-title">
                    <g:message code="edit.nodes.file"/>
                </h3>
                <p class="category">${sourceDesc}</p>
            </div>

            <div class="card-content">

                <div class="form-group">
                    <label class="control-label col-sm-2">
                        <g:message code="project.node.file.source.label"/>
                    </label>

                    <div class="col-sm-10">
                        <p class="form-control-static">
                            ${index}.
                            <g:if test="${providerDesc}">
                                <g:render template="/framework/renderPluginDesc" model="${[
                                        serviceName: 'ResourceModelSource',
                                        description: providerDesc,
                                ]}"/>
                            </g:if>
                        </p>
                    </div>
                </div>


                <div class="form-group">
                    <label class="control-label  col-sm-2">
                        <g:message code="file.display.format.label"/>
                    </label>

                    <div class="col-sm-10">
                        <p class="form-control-static"><code>${fileFormat}</code></p>
                    </div>
                </div>
                <g:if test="${sourceDesc}">
                    <div class="form-group">
                        <label class="control-label  col-sm-2">
                            <g:message code="project.node.file.source.description.label" />
                        </label>

                        <div class="col-sm-10">
                            <p class="form-control-static text-info">${sourceDesc}</p>
                        </div>
                    </div>
                </g:if>
                <textarea
                        name="fileText"
                        class="form-control code apply_ace"
                        data-ace-autofocus='true'
                        data-ace-session-mode="${fileFormat}"
                        data-ace-height="500px"
                        data-ace-control-syntax="${fileFormat ? 'false' : 'true'}"
                        data-ace-control-soft-wrap="true">${fileText}</textarea>
                <g:if test="${saveError}">
                    <h3><g:message code="project.nodes.edit.save.error.message" /></h3>
                    <div class="text-warning">${saveError}</div>
                </g:if>
                <g:if test="${fileEmpty}">
                    <div class="text-warning"><g:message code="project.nodes.edit.empty.description" /></div>
                </g:if>
            </div>


            <div class="card-footer">
                <g:submitButton name="cancel" value="${g.message(code: 'button.action.Cancel', default: 'Cancel')}" class="btn btn-default reset_page_confirm"/>
                <g:submitButton name="save" value="${g.message(code: 'button.action.Save', default: 'Save')}" class="btn btn-cta reset_page_confirm"/>
            </div>
        </div>
    </div>
</g:form>