%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%

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
    <g:render template="/framework/jobOptionsKO"
              model="[
                      paramsPrefix        : 'extra.',
                      selectedargstring   : selectedargstring,
                      selectedoptsmap     : selectedoptsmap,
                      notfound            : commandnotfound,
                      authorized          : authorized,
                      optionSelections    : scheduledExecution?.options ? scheduledExecution.options : null,
                      scheduledExecutionId: scheduledExecution.extid,
                      jobexecOptionErrors : jobexecOptionErrors,
                      optiondependencies  : optiondependencies,
                      dependentoptions    : dependentoptions,
                      remoteOptionData    : remoteOptionData,
                      optionordering      : optionordering
              ]"/>
</div>


<div class="form-group">
    <label class="col-sm-2 control-label" for="extra.loglevel">Log level</label>

    <div class="col-sm-10">
        <label class="radio-inline">
            <g:radio name="extra.loglevel" value="INFO" checked="${scheduledExecution?.loglevel != 'DEBUG'}"/>
            <g:message code="loglevel.normal" />
        </label>
        <label class="radio-inline">
            <g:radio name="extra.loglevel" value="DEBUG" checked="${scheduledExecution?.loglevel == 'DEBUG'}"/>
            <g:message code="loglevel.debug" />
        </label>

        <div class="help-block">
            <g:message code="scheduledExecution.property.loglevel.help" />
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
