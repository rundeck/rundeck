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
<span class="execution-state-summary ">
    <div class="">

        <details class="more-info details-reset  details-inline">
            <summary>

                <span class="execution-summary-status">
                    <span class=" execstate execstatedisplay overall"
                          data-execstate="${enc(attr:execState)}"
                          data-bind="attr: { 'data-execstate': executionState(), 'data-statusstring': executionStatusString() } ">
                    </span>
                    <span data-bind="if: displayStatusString">
                        <span class="exec-status-text custom-status"
                              data-bind="text: executionStatusString() ">
                        </span>
                    </span>
                </span>


                <span data-bind="if: scheduled()">
                    <g:message code="for"/>


                    <span data-bind="text: formatTimeAtDate(startTime()), attr: {title: startTime() }">
                        <g:if test="${execution.dateStarted}">
                            <g:relativeDate atDate="${execution.dateStarted}"/>
                        </g:if>
                    </span>

                    (<span data-bind="text: startTimeAgo()"></span>)
                </span>
                <span data-bind="
                if: !queued() && execDurationSimple() != '',
                 attr: {title: execDurationHumanized() },
                 css: {'text-info':!completed(), 'text-secondary': completed()},
                 click: toggleHumanizedDisplay"
                      class="execution-duration autoclickable">
                    <i class="glyphicon glyphicon-time " data-bind="visible: !completed()"></i>
                    <i class="fas fa-flag-checkered" data-bind="visible: completed()"></i>
                    <span data-bind="text: execDurationDisplay()" ></span>
                </span>
                <span class="timerel execution-started">

                    <span data-bind="if: !scheduled() && !completed()">

                        <span data-bind="text: formatTimeAtDate(startTime()), attr: {title: startTime() }">
                            <g:if test="${execution.dateStarted}">
                                <g:relativeDate atDate="${execution.dateStarted}"/>
                            </g:if>
                        </span>
                    </span>
                </span>

                <span class="timerel execution-completed text-secondary" data-bind="visible: completed()">

                    <g:message code="at"/>
                    <span data-bind="text: formatTimeAtDate(endTime()), attr: {title: endTime() }">
                        <g:if test="${execution.dateCompleted}">
                            <g:relativeDate atDate="${execution.dateCompleted}"/>
                        </g:if>
                    </span>
                </span>
                <span class="more-indicator-verbiage more-info-icon"><g:icon name="chevron-right"/></span>
                <span class="less-indicator-verbiage more-info-icon"><g:icon name="chevron-down"/></span>
            </summary>

            <dl  class="execution-full-dates">
                <dt>
                    <!-- ko if: !scheduled() && !queued() -->
                    <g:message code="start" />
                    (<span data-bind="text: startTimeAgo()"></span>)
                    <!-- /ko -->
                </dt>
                <dd>
                    <span data-bind="text: startTime()">
                    <g:if test="${execution.dateStarted}">
                        ${execution.dateStarted}
                        <g:relativeDate atDate="${execution.dateStarted}"/>
                    </g:if>
                    </span>

                </dd>
                <!-- ko if: completed() -->
                <dt><g:message code="end" /> (<span data-bind="text: endTimeAgo()"></span>)</dt>
                <dd >
                    <span data-bind="text: endTime()">
                    <g:if test="${execution.dateCompleted}">
                        ${execution.dateCompleted}
                        <g:relativeDate atDate="${execution.dateCompleted}"/>
                    </g:if>
                    </span>

                </dd>
                <!-- /ko -->
            </dl>
        </details>

        <span class="execution-user">
            <i class="glyphicon glyphicon-user text-secondary"></i>
            <g:username user="${execution.user}"/>
        </span>

        <div data-bind="visible: retryExecutionId()" class="execution-retry">
          <span class="execstate" data-execstate="RETRY"><g:message code="retried" /></span> <g:message code="as.execution" />
          <a data-bind="attr: { 'href': retryExecutionUrl() }">
              <span data-bind="text: '#'+retryExecutionId()"></span>
          </a>

          <span class="text-secondary"><g:message code="execution.retry.attempt.x.of.max.ko" args="${['text: retryExecutionAttempt()','text: retry()']}"/></span>
        </div>
    </div>
  <g:if test="${clusterModeEnabled && execution.serverNodeUUID}">
      <span id="execRemoteServerUUID" class="execution-cluster-id">

          <span data-server-uuid="${execution.serverNodeUUID}"
                data-server-name="${execution.serverNodeUUID.substring(0,8)}"
                data-show-id="false"
                data-name-class="text-secondary"
                class="rundeck-server-uuid">
          </span>
          <i class="fas fa-dot-circle text-muted cluster-status-icon"></i>
      </span>
  </g:if>
</span>
