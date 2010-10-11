<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: Aug 8, 2008
  Time: 5:18:30 PM
  To change this template use File | Settings | File Templates.
--%>
<%
def gcal=new java.util.GregorianCalendar()
gcal.setTime(new Date())
def CUR_YEAR=gcal.get(java.util.GregorianCalendar.YEAR)
%>
<tr>
    <td colspan="2" id="extDateFilters" style="${params.recentFilter && params.recentFilter!='-' ? 'display:none;':''} background: #ccc; border:1px solid #aaa; text-align:left;">
                            <g:if test="${!hidestart}">
                            <div>
                                <span class="prompt">

                                    <g:checkBox name="dostartafterFilter"
                                                value="${query?.dostartafterFilter}"
                                                id="dostartafterFilter"
                                                onclick="if(this.checked){\$('startafterfilterCtrls').show()}else{\$('startafterfilterCtrls').hide()}"/>
                                    <label for="dostartafterFilter">Started After:</label>
                                </span>
                                <div class="presentation" id="startafterfilterCtrls" style="white-space:nowrap; ${query?.dostartafterFilter?'':'display:none;'}">
                                    <g:datePicker name="startafterFilter"
                                                  years="${CUR_YEAR==2007?2007:CUR_YEAR..2007}"
                                                  value="${query?.startafterFilter}"
                                                  id="startafterFilter"/>
                                </div>
                            </div>
                            <div>
                                <span class="prompt">
                                    <g:checkBox name="dostartbeforeFilter"
                                                value="${query?.dostartbeforeFilter}"
                                                id="dostartbeforeFilter"
                                                onclick="if(this.checked){\$('startbeforefilterCtrls').show()}else{\$('startbeforefilterCtrls').hide()}"/>
                                    <label for="dostartbeforeFilter">Started Before:</label>
                                </span>
                                <div class="presentation" id="startbeforefilterCtrls" style="white-space:nowrap; ${query?.dostartbeforeFilter?'':'display:none'}">
                                    <g:datePicker name="startbeforeFilter"
                                                  years="${CUR_YEAR==2007?2007:CUR_YEAR..2007}"
                                                  value="${query?.startbeforeFilter}"
                                                  id="startbeforeFilter"/>
                                </div>
                            </div>
                                </g:if>
                            <div>
                                <span class="prompt">
                                    <g:checkBox name="doendafterFilter"
                                                value="${query?.doendafterFilter}"
                                                id="doendafterFilter"
                                                onclick="if(this.checked){\$('endafterfilterCtrls').show()}else{\$('endafterfilterCtrls').hide()}"/>
                                    <label for="doendafterFilter">Ended After:</label>
                                </span>
                                <div class="presentation" id="endafterfilterCtrls" style="white-space:nowrap; ${query?.doendafterFilter?'':'display:none'}">

                                    <g:datePicker name="endafterFilter"
                                                  years="${CUR_YEAR==2007?2007:CUR_YEAR..2007}"
                                                  value="${query?.endafterFilter}"
                                                  id="endafterFilter"/>
                                </div>
                            </div>
                            <div>
                                <span class="prompt">
                                    <g:checkBox name="doendbeforeFilter"
                                                value="${query?.doendbeforeFilter}"
                                                id="doendbeforeFilter"
                                                onclick="if(this.checked){\$('endbeforefilterCtrls').show()}else{\$('endbeforefilterCtrls').hide()}"/>
                                    <label for="doendbeforeFilter">Ended Before:</label>
                                </span>
                                <div class="presentation" id="endbeforefilterCtrls" style="white-space:nowrap; ${query?.doendbeforeFilter?'':'display:none'}">

                                    <g:datePicker name="endbeforeFilter"
                                                  years="${CUR_YEAR==2007?2007:CUR_YEAR..2007}"
                                                  value="${query?.endbeforeFilter}"
                                                  id="endbeforeFilter"/>
                                </div>

                            </div>
    </td>
</tr>