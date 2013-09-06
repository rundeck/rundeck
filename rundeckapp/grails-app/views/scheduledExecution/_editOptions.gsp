<g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
</g:if>
<g:hasErrors bean="${scheduledExecution}">
    <div class="errors">
        <g:renderErrors bean="${scheduledExecution}" as="list"/>
    </div>
</g:hasErrors>
<g:render template="/common/messages"/>

<g:set var="rkey" value="${g.rkey()}"/>

<input type="hidden" name="id" value="${scheduledExecution?.extid}"/>
<div class="note error" style="display: none" id="editerror">

</div>

<span class="prompt">Job Options:</span>
<div class="presentation">
    <g:hasErrors bean="${scheduledExecution}" field="argString">
        <div class="fieldError">
            <g:renderErrors bean="${scheduledExecution}" as="list" field="argString"/>
        </div>
    </g:hasErrors>
    <div id="optionSelect">
        <g:render template="/framework/commandOptions"
                  model="[paramsPrefix:'extra.',selectedargstring:selectedargstring,selectedoptsmap:selectedoptsmap,notfound:commandnotfound,authorized:authorized,optionSelections:scheduledExecution?.options?scheduledExecution.options:null,scheduledExecutionId:scheduledExecution.id,jobexecOptionErrors:jobexecOptionErrors, optiondependencies: optiondependencies, dependentoptions: dependentoptions, optionordering: optionordering]"/>
    </div>
</div>


<g:if test="${scheduledExecution.loglevel != 'DEBUG'}">
    <div class="form-group">
      <div class="col-sm-offset-2 col-sm-10">
      <div class="checkbox">
          <label>
            <g:checkBox name="extra.debug" value="true" checked="false"/>
            <g:message code="enable.verbose.logging" />
        </label>
    </div>
    </div>
    </div>
</g:if>

<g:if test="${failedNodes}">
    <g:hiddenField name="_replaceNodeFilters" value="true"/>
    <g:hiddenField name="extra.nodeIncludeName" value="${failedNodes}"/>
    <span class="prompt">Retry On Nodes</span>
    <div class="presentation matchednodes embed" id="matchednodes_${rkey}" >
        <span class="btn btn-default btn-sm depress2 receiver" title="Display matching nodes" onclick="_EOUpdateMatchedNodes()">Show Matches</span>
    </div>
    <g:set var="jsdata" value="${[nodeIncludeName:failedNodes]}"/>
    <g:javascript>
        var nodeFilterData_${rkey}=${jsdata.encodeAsJSON()};
        function _EOUpdateMatchedNodes(){
            _updateMatchedNodes(nodeFilterData_${rkey},'matchednodes_${rkey}','${scheduledExecution?.project}',false,{requireRunAuth:true});
        }
        fireWhenReady('matchednodes_${rkey}',_EOUpdateMatchedNodes);
    </g:javascript>
</g:if>
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
