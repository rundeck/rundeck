<div class="modal fade" id="selectProject" role="dialog" aria-labelledby="selectProjectLabel" aria-hidden="true">
    <div class="modal-dialog modal-sm">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title" id="selectProjectLabel"><g:message code="select.project" /></h4>
            </div>
            <g:form controller="scheduledExecution">
                <div class="modal-body" id="selectProjectContent">

                    <input type="hidden" id="jobid" name="id"/>
                    <g:select name="project" from="${projectNames}" id="jobProject" value=""
                              class="form-control input-sm"/>
                </div>
                <div class="modal-footer">
                    <button type="button"
                            class="btn btn-danger"
                            data-bind="click: cancel"
                            data-dismiss="modal" ><g:message code="cancel"/></button>

                    <g:actionSubmit action="copy"
                                    value="${message(code:'yes')}"
                                    id="submittbn"
                                    class="btn btn-default"/>
                </div>
            </g:form>
        </div>
    </div>
</div>