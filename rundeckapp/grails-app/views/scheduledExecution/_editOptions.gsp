<g:if test="${flash.message}">
    <div class="message">${flash.message}</div>
</g:if>
<g:hasErrors bean="${scheduledExecution}">
    <div class="errors">
        <g:renderErrors bean="${scheduledExecution}" as="list"/>
    </div>
</g:hasErrors>

<g:set var="rkey" value="${g.rkey()}"/>

<input type="hidden" name="id" value="${scheduledExecution?.id}"/>
<div class="note error" style="display: none" id="editerror">

</div>

<g:if test="${!scheduledExecution.adhocExecution }">
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
</g:if>
<g:if test="${scheduledExecution.adhocExecution && (scheduledExecution.adhocFilepath||scheduledExecution.adhocLocalString)}">
    <span class="prompt">Script Arguments:</span>
    <div class="presentation">
        <g:hasErrors bean="${scheduledExecution}" field="argString">
            <div class="fieldError">
                <g:renderErrors bean="${scheduledExecution}" as="list" field="argString"/>
            </div>
        </g:hasErrors>

        <span class="input">
            <input type='text' name="extra.argString" value='${scheduledExecution?.argString?.encodeAsHTML()}' id="schedJobArgString" size="40"/>
            <g:hasErrors bean="${scheduledExecution}" field="argString">
                <img src="${resource( dir:'images',file:'icon-small-warn.png' )}" alt="Error"  width="16px" height="16px"/>
            </g:hasErrors>
        </span>
        <span class="info note">
            Enter arguments to the script.
        </span>
    </div>
</g:if>

<span class="prompt">Log level:</span>
<div class="presentation">
    <g:select name="extra.loglevel"
              from="${['1. Debug','2. Verbose','3. Information','4. Warning','5. Error']}"
              keys="${['DEBUG','VERBOSE','INFO','WARN','ERR']}"
              value="${scheduledExecution.loglevel?scheduledExecution.loglevel:'WARN'}"
        />

    <div class="info note">
        Higher numbers produce less output.
    </div>
</div>

<g:if test="${failedNodes}">
    <g:hiddenField name="_replaceNodeFilters" value="true"/>
    <g:hiddenField name="extra.nodeIncludeName" value="${failedNodes}"/>
    <span class="prompt">Retry On Nodes</span>
    <div class="presentation matchednodes embed" id="matchednodes_${rkey}" >
        <span class="action textbtn depress2 receiver" title="Display matching nodes" onclick="_EOUpdateMatchedNodes()">Show Matches</span>
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