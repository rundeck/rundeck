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
<div class="jobstats row">

    <div class="col-xs-12 col-sm-4 job-stats-item">

        <span class="job-stats-value" id="jobstat_execcount_total" data-execcount="${execCount}">
            <g:formatNumber number="${execCount}"/>
        </span>
        <span class="text-table-header"><g:message code="Execution.plural"/></span>


    </div>
    <g:if test="${lastrun || reflastrun}">

        <div class="col-xs-12 col-sm-4 job-stats-item">

            <g:set var="successrate" value="${params.float('success') ?: successrate}"/>
            <g:set var="ratecolors"
                   value="${['text-success', 'text-info', 'text-warning', 'text-danger']}"/>
            <g:set var="ratelevels" value="${[0.9f, 0.75f, 0.5f]}"/>
            <g:set var="successindex" value="${ratelevels.findIndexOf { it <= (successrate) }}"/>
            <g:set var="successcolor"
                   value="${successindex >= 0 ? ratecolors[successindex] : ratecolors[-1]}"/>

            <span class="job-stats-value ${successcolor}" data-successrate="${successrate}">
                <g:formatNumber number="${successrate}" type="percent"/>

            </span>

            <span class="text-table-header"><g:message code="success.rate"/></span>

        </div>
    </g:if>



        <div class="col-xs-12 col-sm-4 job-stats-item">
            <g:set var="executionService" bean="${rundeck.services.ExecutionService}"/>
            <g:set var="avgduration" value="${executionService.getAverageDuration(scheduledExecution.uuid)}"/>


            <span class="job-stats-value" data-avgduration="${avgduration}">
                <g:if test="${avgduration>0}">
                    <g:timeDuration time="${avgduration}"/>
                </g:if>
                <g:else>
                    <span class="text-muted">
                        -
                    </span>
                </g:else>
            </span>
            <span class="text-table-header"><g:message code="average.duration"/></span>
        </div>

</div>
