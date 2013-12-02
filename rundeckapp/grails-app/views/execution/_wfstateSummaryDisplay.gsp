%{--
  Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  --}%
<div class="container">
    <div class="row row-space">
        <div class="col-sm-12">

            <span class=" execstate execstatedisplay overall h4"
                  data-bind="attr: { 'data-execstate': executionState() } ">
            </span>

            <span data-bind="visible: completed()">
                after <span data-bind="text: execDurationHumanized(), attr: {title: execDurationSimple() } ">
                <g:if test="${execution.dateCompleted}">
                    <g:relativeDate start="${execution.dateStarted}" end="${execution.dateCompleted}"/>
                </g:if>
            </span>
                <span class="timerel">at
                    <span data-bind="text: formatTimeAtDate(endTime()), attr: {title: endTime() }">
                        <g:if test="${execution.dateCompleted}">
                            <g:relativeDate atDate="${execution.dateCompleted}"/>
                        </g:if>
                    </span>
                </span>
            </span>

                started by <g:username user="${execution.user}"/>
            <span class="timerel">at
                <span data-bind="text: formatTimeAtDate(startTime()), attr: {title: startTime() }">
                    <g:if test="${execution.dateStarted}">
                        <g:relativeDate atDate="${execution.dateStarted}"/>
                    </g:if>
                </span>
            </span>
            %{--<g:render template="/scheduledExecution/execStatusText" model="${[execution: execution]}"/>--}%



        </div>
    </div>

<div data-bind="if: !stateLoaded()">
        <div class="row-space-lg row">
            <div class="col-sm-12">

                <div data-bind="if: errorMessage()">
                    <div class="well well-lg" data-bind="visible: errorMessage()" style="display: none">
                        <div class="text-warning" data-bind="text: errorMessage()">
                        </div>
                    </div>
                </div>

                <div data-bind="if: !errorMessage()">
                    <div class="well well-lg text-muted">
                        Waiting for state info…
                    </div>
                </div>

            </div>
        </div>
</div>


<div data-bind="if: stateLoaded()">
        <div class="row row-space">
            <div class="col-sm-4 ">
                <section data-bind="if: !completed()" class="section-space">
                    <span class=" h4" data-bind="css: { 'text-info': runningNodes(), 'text-muted': !runningNodes() }">
                        <span class=" " data-bind="text: runningNodes().length"></span>
                        <span class=" " data-bind="text: pluralize(runningNodes().length,'Step')"></span>
                        running
                    </span>
                </section>

                <section data-bind="if: !completed()" class="section-space">
                    <span class=" h4"
                          data-bind="css: { 'text-success': succeededNodes(), 'text-muted': !succeededNodes() }">
                        <span data-bind="text: succeededNodes().length"></span>
                        <span class=" " data-bind="text: pluralize(succeededNodes().length,'Node')"></span>
                        completed
                    </span>
                </section>

                <section data-bind="if: failedNodes().length > 0 " class="section-space">
                    <span class="text-danger h4">
                        <span data-bind="text: failedNodes().length"></span>
                        <span class=" " data-bind="text: pluralize(failedNodes().length,'Node')"></span>
                        failed
                    </span>

                </section>

                <section data-bind="if: partialNodes().length > 0 " class="section-space">

                    <span class="text-warning h4">
                        <span data-bind="text: partialNodes().length"></span>
                        <span class=" " data-bind="text: pluralize(partialNodes().length,'Node')"></span>
                        partially completed
                    </span>

                </section>

                <section data-bind="if: !completed()" class="section-space">
                    <span data-bind="if: waitingNodes().length > 0" class="text-muted h4">
                        <span class="" data-bind="text: waitingNodes().length"></span>
                        <span class=" " data-bind="text: pluralize(waitingNodes().length,'Node')"></span>
                        waiting to run
                    </span>
                </section>

                <section data-bind="if: completed() && notstartedNodes().length > 0" class="section-space">
                    <span data-bind="if: notstartedNodes()" class="text-info h4">
                        <span class="" data-bind="text: notstartedNodes().length"></span>
                        <span class=" " data-bind="text: pluralize(notstartedNodes().length,'Node')"></span>
                        <span class=" " data-bind="text: pluralize(notstartedNodes().length,'was','were')"></span> not started
                    </span>
                </section>

            </div>
            <div class="col-sm-8">

                %{--display up to 5 failed nodes--}%
                <div class="">

                    <section data-bind="visible: runningNodes().length > 0, if: runningNodes().length > 0" >
                        <div data-bind="foreach: runningNodes()">
                            <div>
                                <g:render template="nodeCurrentStateSimpleKO"/>
                            </div>
                        </div>
                    </section>
                    <section data-bind="if: failedNodes().length > 0" class="section-space">
                        <div data-bind="foreach: failedNodes()">
                            <div>
                                <g:render template="nodeCurrentStateSimpleKO"/>
                            </div>
                        </div>
                    </section>
                    %{--display up to 5 partial nodes nodes--}%
                    <div data-bind="if:  partialNodes().length > 0" >
                        <div data-bind="foreach: partialNodes()">
                            <div>
                                <g:render template="nodeCurrentStateSimpleKO"/>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
</div>
</div>
