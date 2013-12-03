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
                data-execstate="${execState}"
                  data-bind="attr: { 'data-execstate': executionState() } ">
            </span>

            <span data-bind="visible: completed()" style="${wdgt.styleVisible(if:execution.dateCompleted)}">
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

                started
            <span class="timerel">at
                <span data-bind="text: formatTimeAtDate(startTime()), attr: {title: startTime() }">
                    <g:if test="${execution.dateStarted}">
                        <g:relativeDate atDate="${execution.dateStarted}"/>
                    </g:if>
                </span>
            </span>
            by <g:username user="${execution.user}"/>
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
        <div class="row row-space" data-bind="if: completed()">
            <div class="col-sm-12" >

                <table class="table table-bordered table-condensed">

                    <tr>
                        <th colspan="4" class="text-muted table-footer text-small">
                            Node Summary
                        </th>
                    </tr>
                    <tr>
                       <th style="width: 25%" class="text-muted text-center h5 text-header">Complete</th>
                       <th style="width: 25%" class="text-muted text-center h5 text-header">Failed</th>
                       <th style="width: 25%" class="text-muted text-center h5 text-header">Incomplete</th>
                       <th style="width: 25%" class="text-muted text-center h5 text-header">Not Started</th>
                    </tr>
                    <tr>
                        <td>
                            <div class="text-center">
                                <span class="h1 text-muted"
                                      data-bind="text: percentageFixed(completedNodes().length,activeNodes().length) + '%'"></span>
                            </div>

                            <div class="text-center">
                                <span class="text-muted"
                                      data-bind="text: completedNodes().length+'/'+activeNodes().length"></span>
                            </div>
                        </td>

                        <td >


                            <div class="text-center">
                                <span class=" h1"
                                      data-bind="css: {'text-danger': failedNodes().length > 0 , 'text-muted': failedNodes().length < 1 } ">
                                    <span data-bind="text: failedNodes().length"></span>
                                </span>
                            </div>
                        </td>
                        <td>
                            <div class="text-center">

                                <span class=" h1"
                                      data-bind="css: {'text-warning': partialNodes().length > 0 , 'text-muted': partialNodes().length < 1 } ">
                                    <span class="" data-bind="text: partialNodes().length"></span>
                                </span>

                            </div>
                        </td>
                        <td>

                            <div class="text-center">
                                <span class=" h1"
                                      data-bind="css: {'text-warning': notstartedNodes().length > 0 , 'text-muted': notstartedNodes().length < 1 } ">
                                    <span class="" data-bind="text: notstartedNodes().length"></span>
                                </span>
                            </div>

                        </td>
                    </tr>
                </table>
            </div>
        </div>
        <div class="row row-space" data-bind="if: !completed()">
            <div class="col-sm-12" >
                <table class="table table-bordered">

                    <tr>
                        <th colspan="3" class="text-muted table-footer text-small">
                            Node Summary
                        </th>
                    </tr>
                    <tr>
                       <th style="width: 33%" class="text-muted text-center h5 text-header">Waiting</th>
                       <th style="width: 33%" class="text-muted text-center h5 text-header">Running</th>
                       <th style="width: 33%" class="text-muted text-center h5 text-header">Done</th>
                    </tr>
                    <tr>
                        <td>
                                <div class="text-center">
                                    <span class="h1 text-muted" data-bind="text: waitingNodes().length"></span>
                                </div>
                        </td>
                        <td>

                            <div class="text-center">
                                <span class=" h1"
                                      data-bind="css: {'text-info': runningNodes().length > 0 , 'text-muted': runningNodes().length < 1 } ">
                                    <span class=" " data-bind="text: runningNodes().length"></span>
                                </span>
                            </div>


                        </td>
                        <td >

                            <div class="text-center">
                                <span class=" h1"
                                      data-bind="css: {'text-info': completedNodes().length > 0 , 'text-muted': completedNodes().length < 1 } ">
                                    <span data-bind="text: completedNodes().length"></span>
                                </span>
                            </div>
                        </td>
                    </tr>
                </table>
            </div>
        </div>


        <div class="row " data-bind="if: !completed()">
            <div class="col-sm-3 text-muted h4 text-right">
                Now Running
            </div>
            <div class="col-sm-9">
                <section data-bind="visible: runningNodes().length > 0, if: runningNodes().length > 0" >
                    <div data-bind="foreach: runningNodes()">
                        <div>
                            <g:render template="nodeCurrentStateSimpleKO"/>
                        </div>
                    </div>
                </section>
            </div>
        </div>

        <div class="row " data-bind="if: failedNodes().length > 0 ">
            <div class="col-sm-3 text-muted h4 text-right">
                <span data-bind="text: failedNodes().length"></span>
                Failed Nodes
            </div>
            <div class="col-sm-9">
                <div data-bind="if: failedNodes().length > 0" >
                    <div data-bind="foreach: failedNodes()">
                        <div>
                            <g:render template="nodeCurrentStateSimpleKO"/>
                        </div>
                    </div>
                </div>
            </div>
            </div>

    <div class="row " data-bind="if: partialNodes().length > 0">
        <div class="col-sm-3 text-muted h4 text-right">
            <span data-bind="text: partialNodes().length"></span>
            Incomplete Nodes
        </div>

        <div class="col-sm-9">
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
