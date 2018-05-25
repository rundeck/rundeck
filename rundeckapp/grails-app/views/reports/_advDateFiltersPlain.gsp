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

<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: Aug 8, 2008
  Time: 5:18:30 PM
  To change this template use File | Settings | File Templates.
--%>
<%
    def gcal = new java.util.GregorianCalendar()
    gcal.setTime(new Date())
    def now = gcal.getTime()
    def CUR_YEAR = gcal.get(java.util.GregorianCalendar.YEAR)
%>
    <div id="extDateFilters"
        class="panel panel-default"
        style="${query.recentFilter!='-'  ? 'display:none;' : ''}">
        <div class="panel-body">
        <table class="table  table-condensed table-hover" style="border:0">
        <g:if test="${!hidestart}">
            <div>
        <tr><td style="width:150px">
                <span class="checkbox">
                    <g:checkBox name="dostartafterFilter"
                                value="${query?.dostartafterFilter}"
                                id="dostartafterFilter"
                                class="checkbox"
                                onclick="if(this.checked){\$('startafterfilterCtrls').show()}else{\$('startafterfilterCtrls').hide()}"/>
                    <label for="dostartafterFilter">Started After:</label>
                </span>
        </td><td>
                <span class="form-inline"  id="startafterfilterCtrls"
                     style="white-space:nowrap; ${query?.dostartafterFilter ? '' : 'display:none;'}">
                    <g:datepickerUI name="startafterFilter"
                                  value="${query?.startafterFilter?:now}"
                                  id="startafterFilter"
                                  class="form-control input-sm"/>
                                  
                </span>
        </td></tr>
            </div>
            <div>
        <tr><td style="width:150px">
                <span class="checkbox">
                    <g:checkBox name="dostartbeforeFilter"
                                value="${query?.dostartbeforeFilter}"
                                id="dostartbeforeFilter"
                                class="checkbox"
                                onclick="if(this.checked){\$('startbeforefilterCtrls').show()}else{\$('startbeforefilterCtrls').hide()}"/>
                    <label for="dostartbeforeFilter">Started Before:</label>
                </span>
        </td><td>
                <span class="form-inline"  id="startbeforefilterCtrls"
                     style="white-space:nowrap; ${query?.dostartbeforeFilter ? '' : 'display:none;'}">
                    <g:datepickerUI name="startbeforeFilter"
                                  years="${CUR_YEAR == 2007 ? 2007 : CUR_YEAR..2007}"
                                  value="${query?.startbeforeFilter?:now}"
                                  id="startbeforeFilter"
                                  class="form-control input-sm"/>
                                  
                </span>
        </td></tr>
            </div>
        </g:if>
        <div>
        <tr><td style="width:150px">
            <span class="checkbox">
                <g:checkBox name="doendafterFilter"
                            value="${query?.doendafterFilter}"
                            id="doendafterFilter"
                            class="checkbox"
                            onclick="if(this.checked){\$('endafterfilterCtrls').show()}else{\$('endafterfilterCtrls').hide()}"/>
                <label for="doendafterFilter">Ended After:</label>
            </span>
        </td><td>
            <span class="form-inline"  id="endafterfilterCtrls"
                style="white-space:nowrap; ${query?.doendafterFilter ? '' : 'display:none;'}">
               <g:datepickerUI name="endafterFilter"
                             years="${CUR_YEAR == 2007 ? 2007 : CUR_YEAR..2007}"
                             value="${query?.endafterFilter?:now}"
                             id="endafterFilter"
                             class="form-control input-sm"/>

           </span>
        </td></tr>
        </div>
        <div>
        <tr><td style="width:150px">
            <span class="checkbox">
                <g:checkBox name="doendbeforeFilter"
                            value="${query?.doendbeforeFilter}"
                            id="doendbeforeFilter"
                            class="checkbox"
                            onclick="if(this.checked){\$('endbeforefilterCtrls').show()}else{\$('endbeforefilterCtrls').hide()}"/>
                <label for="doendbeforeFilter">Ended Before:</label>
            </span>
        </td><td>
            <span class="form-inline"  id="endbeforefilterCtrls"
                style="white-space:nowrap; ${query?.doendbeforeFilter ? '' : 'display:none;'}">
               <g:datepickerUI name="endbeforeFilter"
                             years="${CUR_YEAR == 2007 ? 2007 : CUR_YEAR..2007}"
                             value="${query?.endbeforeFilter?:now}"
                             id="endbeforeFilter"
                             class="form-control input-sm"/>
           </span>
        </td></tr>
        </div>
        </table>
    </div>
</div>
