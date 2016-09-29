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
<div class="jobstats" style="clear:both;">
    <table class="table table-bordered table-condensed" >

        <tr>
            <th style="width: 20%" class="text-muted text-center  text-header">
                <g:message code="Execution.plural" />
            </th>
        <g:if test="${lastrun}">
            <th style="width: 20%" class="text-muted text-center  text-header">
                <g:message code="success.rate" />
            </th>
        </g:if>
        <g:if test="${scheduledExecution.execCount > 0}">
            <th style="width: 20%" class="text-muted text-center  text-header">
                <g:message code="average.duration" />
            </th>
        </g:if>
        </tr>
        <tr>
            <td class="text-center">
                <span class="h3 ">
                    <g:formatNumber number="${total}" />
                </span>
            </td>
        <g:if test="${lastrun}">
            <g:set var="successrate" value="${params.float('success')?:successrate}"/>
            <g:set var="ratecolors" value="${['text-success','text-muted','text-warning','text-danger']}"/>
            <g:set var="ratelevels" value="${[0.9f,0.75f,0.5f]}"/>
            <g:set var="successindex" value="${ratelevels.findIndexOf{it<=(successrate)}}"/>
            <g:set var="successcolor" value="${successindex>=0?ratecolors[successindex]:ratecolors[-1]}"/>
            <td class="text-center">
                <span class="h3 ${successcolor}">
                    <g:formatNumber number="${successrate}" type="percent"/>
                </span>
            </td>
        </g:if>
        <g:if test="${scheduledExecution.execCount>0}">
            <td class="text-center">
                    <span class="h3 ">
                        <g:timeDuration time="${scheduledExecution.execCount>0?  scheduledExecution.totalTime /scheduledExecution.execCount  : 0}"/>
                    </span>
                </td>
        </g:if>

        </tr>
    </table>
</div>
