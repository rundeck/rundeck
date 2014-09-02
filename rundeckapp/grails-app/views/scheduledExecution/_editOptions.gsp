<g:hasErrors bean="${scheduledExecution}">
    <div class="errors">
        <g:renderErrors bean="${scheduledExecution}" as="list"/>
    </div>
</g:hasErrors>
<g:render template="/common/messages"/>

<g:set var="rkey" value="${g.rkey()}"/>

<input type="hidden" name="id" value="${enc(attr:scheduledExecution?.extid)}"/>
<div class="note error" style="display: none" id="editerror">

</div>

<g:hasErrors bean="${scheduledExecution}" field="argString">
    <div class="fieldError">
        <g:renderErrors bean="${scheduledExecution}" as="list" field="argString"/>
    </div>
</g:hasErrors>
<div id="optionSelect">
    <g:render template="/framework/commandOptions"
              model="[paramsPrefix:'extra.',selectedargstring:selectedargstring,selectedoptsmap:selectedoptsmap,notfound:commandnotfound,authorized:authorized,optionSelections:scheduledExecution?.options?scheduledExecution.options:null,scheduledExecutionId:scheduledExecution.extid,jobexecOptionErrors:jobexecOptionErrors, optiondependencies: optiondependencies, dependentoptions: dependentoptions, optionordering: optionordering]"/>
</div>


<div class="form-group">
    <label class="col-sm-2 control-label" for="extra.loglevel">Log level</label>

    <div class="col-sm-10">
        <label class="radio-inline">
            <g:radio name="extra.loglevel" value="INFO" checked="${scheduledExecution?.loglevel != 'DEBUG'}"/>
            Normal
        </label>
        <label class="radio-inline">
            <g:radio name="extra.loglevel" value="DEBUG" checked="${scheduledExecution?.loglevel == 'DEBUG'}"/>
            Debug
        </label>

        <div class="help-block">
            Debug level produces more output
        </div>
    </div>
</div>

<g:javascript>
    fireWhenReady('optionSelect', function() {
        $$('input[type=text]').each(function(e) {
            Event.observe(e, 'keydown', noenter);
        });
        $$('input[type=password]').each(function(e) {
            Event.observe(e, 'keydown', noenter);
        });
    });
</g:javascript>
