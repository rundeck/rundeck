
<div class="floatl clear crontab">
    <g:set var="crontab" value="${scheduledExecution?.timeAndDateAsBooleanMap()}"/>
    <div class="floatl ">
        <div class="checklist">
            at <span class="cronselected" style="display:inline;">${scheduledExecution?.hour}
    :
    ${scheduledExecution?.minute?.size()<2 ? "0"+scheduledExecution?.minute : scheduledExecution?.minute}
            <g:if test="${scheduledExecution?.seconds !='0'}">
                : ${scheduledExecution?.seconds?.size()<2 ? "0"+scheduledExecution?.seconds : scheduledExecution?.seconds}
            </g:if>
    </span>
        </div>
    </div>
    <g:set var="isDayOfWeekDefault" value="${(scheduledExecution?.dayOfWeek?.equals('*'))? true: false }"/>



        <g:if test="${scheduledExecution?.dayOfWeek == '*' || scheduledExecution?.dayOfMonth=='*'}">
            <div class="floatl sepL">
            <div class="checklist">
                <div class="cronselected">every day</div>
            </div>
            </div>
        </g:if>
        <g:elseif test="${scheduledExecution?.dayOfWeek!='?' && ScheduledExecution.crontabSpecialValue(scheduledExecution?.dayOfWeek)}">
            <div class="floatl checklist sepL">
                on
            </div>
            <div class="checklist floatl">
                <div class="cronselected"><g:cronItem value="${scheduledExecution?.dayOfWeek}" unit="weekday"/></div>
            </div>
        </g:elseif>
        <g:elseif test="${scheduledExecution?.dayOfWeek=='?'}">
            <div class="floatl checklist sepL">
                on
            </div>
            <div class="checklist floatl">
                <div class="cronselected"><g:cronItem value="${scheduledExecution?.dayOfMonth}" unit="day"/></div>
            </div>
        </g:elseif>
        <g:else>
            <div class="floatl checklist sepL">
            every
            </div>
            <div class="floatl ">
            <div class="checklist action hidedeselected"
                 onclick="myToggleClassName(this,'hidedeselected');"
                 title="Click to toggle excluded days of the week"
                 id="DayOfWeekDialog"
                >
                <g:each in="${UtilityTagLib.daysofweekord}">
                    <div class="${ (crontab['dayOfWeek.'+it.toUpperCase()]) ? 'cronselected' : 'crondeselected' }">
                        <g:dayOfWeek name="${it}"/>
                    </div>
                </g:each>
            </div>
            </div>
        </g:else>


    <g:if test="${scheduledExecution?.month!='*'}">
    <div class="floatl checklist sepL">
        of
    </div>
    <div class="floatl">
        <g:if test="${scheduledExecution?.month == '*'|| ScheduledExecution.crontabSpecialValue(scheduledExecution?.month)}">
            <div class="checklist">
                <div class="cronselected"><g:cronItem value="${scheduledExecution?.month}" unit="month"/></div>
            </div>
        </g:if>
        <g:else>
            <div class="checklist hidedeselected action"
                 id="MonthDialog"
                 onclick="myToggleClassName(this,'hidedeselected');"
                 title="Click to toggle excluded months ">

                <g:each in="${UtilityTagLib.monthsofyearord}">
                    <div class="${ (crontab['month.'+it.toUpperCase()]) ? 'cronselected' : 'crondeselected' }">
                        <g:month name="${it}"/>
                    </div>
                </g:each>
            </div>
        </g:else>
    </div>
    </g:if>
    <g:if test="${scheduledExecution?.year!='*'}">
        
        <div class="floatl checklist sepL">
            of
        </div>
        <div class="floatl">
            <div class="checklist">
                <div class="cronselected"><g:cronItem value="${scheduledExecution?.year}" unit="year"/></div>
            </div>
        </div>
    </g:if>
</div>