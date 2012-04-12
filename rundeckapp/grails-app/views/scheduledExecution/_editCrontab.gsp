<script type="text/javascript">
    function changeCronExpression(elem){
        $('crontooltip').innerHTML='';
        var params={crontabString:$F(elem)};
        new Ajax.Updater('cronstrinfo',
            '${createLink(controller:'scheduledExecution',action:'checkCrontab')}',{
            parameters:params,
            evalScripts:true
            }
            );
    }
    var cronSects=['Second','Minute','Hour','Day of Month','Month','Day of Week','Year'];
    function tkeyup(el){
        $('cronstrinfo').innerHTML='';
        var pos=getCaretPos(el);
        var f =$F(el);
        //find # of space chars prior to pos
        var sub=f.substring(0,pos);
        var c = sub.split(' ').size();
        if(c>=1&&c<=7){
            $('crontooltip').innerHTML=cronSects[c-1];
        }else{
            $('crontooltip').innerHTML='';
        }
    }
    function getCaretPos(el) {
        var rng, ii = -1;
        if (typeof el.selectionStart == "number") {
            ii = el.selectionStart;
        } else if (document.selection && el.createTextRange) {
            rng = document.selection.createRange();
            rng.collapse(true);
            rng.moveStart("character", -el.value.length);
            ii = rng.text.length;
        }
        return ii;
    }
</script>

<g:set var="useCrontabString" value="${scheduledExecution?.crontabString?true:scheduledExecution?.shouldUseCrontabString()?true:false}"/>

<input type="hidden" name="dayOfMonth" value="${scheduledExecution?.dayOfMonth}"/>


<label> <g:radio name="useCrontabString" value="false" id="useCrontabStringFalse" checked="${!useCrontabString}"/> Simple</label>
<label title="Specify a crontab formatted string" class="${hasErrors(bean:scheduledExecution,field:'crontabString','fieldError')}"> <g:radio name="useCrontabString" value="true" id="useCrontabStringTrue" checked="${useCrontabString}"/> Crontab</label>
<wdgt:eventHandler for="useCrontabStringFalse" state="unempty">
    <wdgt:action visible="true" target="cronsimple"/>
    <wdgt:action visible="false" target="cronstrtab"/>
</wdgt:eventHandler>
<wdgt:eventHandler for="useCrontabStringTrue" state="unempty">
    <wdgt:action visible="false" target="cronsimple"/>
    <wdgt:action visible="true" target="cronstrtab"/>
</wdgt:eventHandler>

<div class="crontab tabtarget" id="cronsimple" style="${wdgt.styleVisible(unless:useCrontabString)}">

    <div class="floatl sepR" id="hourTab">
        <div>
            <g:select name="hour" from="${(0..23).collect{it<10?'0'+it.toString():it.toString()}}" value="${scheduledExecution?.hour}"/>
            :
            <g:select name="minute" from="${(0..59).collect{it<10?'0'+it.toString():it.toString()}}" value="${scheduledExecution?.minute}"/>
        </div>
    </div>

    <div class="floatl sepR">
        <g:set var="isDayOfWeekDefault" value="${(scheduledExecution?.dayOfWeek.equals('*'))? true: false }"/>
        <g:checkBox name="everyDayOfWeek"
                    id="everyDayOfWeek"
                    value="true"
                    checked="${isDayOfWeekDefault}"
        />

        <label for="everyDayOfWeek">Every Day</label>

        <div  class="checklist sepT"
             style="${wdgt.styleVisible(unless:scheduledExecution?.dayOfWeek.equals('*'))}"
             id="DayOfWeekDialog"
             >
            <g:each in="${UtilityTagLib.daysofweekord}" var="day">
                <div>
                    <label for="crontab.dayOfWeek.${day}">
                    <g:checkBox name="crontab.dayOfWeek.${day}" id="crontab.dayOfWeek.${day}" class="crontab.dayOfWeek"
                                value="true"
                                checked="${crontab?crontab['dayOfWeek.'+day]:false}"
                    />
                    <g:dayOfWeek name="${day}"/></label>
                </div>
            </g:each>
        </div>
        <wdgt:eventHandler for="everyDayOfWeek" state="unempty" visible="false" target="DayOfWeekDialog"/>
    </div>

    <div class="floatl">
        <g:set var="isMonthDefault" value="${(scheduledExecution?.month.equals('*'))? true: false }"/>
        <g:checkBox
            name="everyMonth"
            id="everyMonth"
            value="true"
            checked="${isMonthDefault}"
        />

        <label for="everyMonth">Every Month</label>

        <div class="checklist sepT"
             style="${wdgt.styleVisible(unless:scheduledExecution?.month.equals('*'))}"
             id="MonthDialog">
            <g:each in="${UtilityTagLib.monthsofyearord}" var="month">
                <div>
                    <label for="crontab.month.${month}">
                        <g:checkBox name="crontab.month.${month}" id="crontab.month.${month}" class="crontab.month"
                                checked="${crontab?crontab['month.'+month]:false}"
                                value="true"
                    />
                    <g:month name="${month}"/></label>
                </div>
            </g:each>
        </div>
        <wdgt:eventHandler for="everyMonth" state="unempty" visible="false" target="MonthDialog"/>
    </div>
    <div class="clear">
    </div>


</div>

<div class=" crontab tabtarget" style="${wdgt.styleVisible(if:useCrontabString)}" id="cronstrtab">
    <div  style="font-size:12pt">
        <g:textField name="crontabString" value="${scheduledExecution?.crontabString?scheduledExecution?.crontabString:scheduledExecution?.generateCrontabExression()}" onchange="changeCronExpression(this);" onblur="changeCronExpression(this);" onkeyup='tkeyup(this);' onclick='tkeyup(this);' style="font-size:12pt" size="50"/>
        <span id="crontooltip" class="info note"></span>
    </div>
    <span id="cronstrinfo">
        </span>
    
    <div class="info note"  style="padding:10px;">
        <div>
            Ranges: <code>1-3</code>.  Lists: <code>1,4,6</code>. Increments: <code>0/15</code> "every 15 units starting at 0".  
        </div>
        See: <a href="${g.message(code:'documentation.reference.cron.url')}" class="external">Cron reference</a> for formatting help
    </div>
</div>