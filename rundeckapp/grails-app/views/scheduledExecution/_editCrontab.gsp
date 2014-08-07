<%@ page import="rundeck.UtilityTagLib" %>
<script type="text/javascript">
    function changeCronExpression(elem){
        clearHtml($('crontooltip'));
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
        clearHtml('cronstrinfo');
        var pos=getCaretPos(el);
        var f =$F(el);
        //find # of space chars prior to pos
        var sub=f.substring(0,pos);
        var c = sub.split(' ').size();
        if(c>=1&&c<=7){
            setText($('crontooltip'),cronSects[c-1]);
        }else{
            clearHtml('crontooltip');
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

<input type="hidden" name="dayOfMonth" value="${enc(attr:scheduledExecution?.dayOfMonth)}"/>
<g:hiddenField name="useCrontabString" value="${useCrontabString}" id="useCrontabString"/>
<ul class="nav nav-tabs crontab-edit">
    <li class="${!useCrontabString ? 'active' : ''}">
        <a data-toggle="tab" data-crontabstring="false" href="#cronsimple">Simple</a>

    </li>
    <li class="${useCrontabString ? 'active' : ''}">
        <a data-toggle="tab" data-crontabstring="true" href="#cronstrtab">Crontab</a>
    </li>
</ul>
<g:javascript>
jQuery(window).load(function(){
    jQuery('.crontab-edit a[data-toggle="tab"]').on('show.bs.tab', function (e) {
        jQuery('#useCrontabString').val(jQuery(e.delegateTarget).data('crontabstring'));
    })
});
</g:javascript>

<div class="tab-content">
<div class="tab-pane ${!useCrontabString?'active':''}" id="cronsimple">
<div class="panel panel-default panel-tab-content form-inline crontab tabtarget" >
    <div class="panel-body">
    <div class="col-sm-4" id="hourTab">
        <div>
            <g:set var="hourString" value="${rundeck.ScheduledExecution.zeroPaddedString(2, scheduledExecution?.hour)}"/>
            <g:set var="minString"
                   value="${rundeck.ScheduledExecution.zeroPaddedString(2, scheduledExecution?.minute)}"/>
            <g:select name="hour" from="${(0..23).collect{it<10?'0'+it.toString():it.toString()}}" value="${hourString}"/>
            :
            <g:select name="minute" from="${(0..59).collect{it<10?'0'+it.toString():it.toString()}}" value="${minString}"/>
        </div>
    </div>

    <div class="col-sm-4">
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

    <div class="col-sm-4">
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
    </div>
</div>
</div>

<div class="tab-pane ${useCrontabString ? 'active' : ''}" id="cronstrtab">
<div class="panel panel-default panel-tab-content crontab tabtarget"  >

    <div class="panel-body">
    <div class="container">
    <div class="row">
    <div class="col-sm-4">
        <div  class="form-group">
            <g:textField name="crontabString"
                         value="${scheduledExecution?.crontabString?scheduledExecution?.crontabString:scheduledExecution?.generateCrontabExression()}"
                         onchange="changeCronExpression(this);"
                         onblur="changeCronExpression(this);"
                         onkeyup='tkeyup(this);'
                         onclick='tkeyup(this);'
                         class="form-control input-sm"
                         size="50"/>

        </div>
    </div>
    <div class="col-sm-4">
        <span id="crontooltip" class="label label-info form-control-static"></span>
    </div>
    <span id="cronstrinfo"></span>

    </div>
    <div class="row">
    <div class="text-muted col-sm-12">
        <div>
            Ranges: <code>1-3</code>.  Lists: <code>1,4,6</code>. Increments: <code>0/15</code> "every 15 units starting at 0".
        </div>
        See: <a href="${g.message(code:'documentation.reference.cron.url')}" class="external" target="_blank">Cron reference</a> for formatting help
    </div>
    </div>
    </div>
    </div>
</div>
</div>

</div>
