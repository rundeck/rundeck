<g:set var="usewidth" value="${width?:100}"/>
%{--<div id="${containerId}" class="progressContainer small ${className} ${indefinite?'indefinite':''} ${(!completePercent || completePercent < 1 ) ? 'empty':''}" style="width:${usewidth+9}px">--}%
    %{--<div--}%
        %{--id="${barId}"--}%
        %{--class="progressBar ${(!completePercent || completePercent < 1 ) ? 'empty':''} ${className} ${completePercent>=100?'full':''}"--}%
        %{--title="${title}"--}%
        %{--style="width:${completePercent>100? usewidth:completePercent}px"--}%
    %{--><div class="progresscontent"><g:if test="${showpercent}">${completePercent>100? 100:completePercent}%</g:if>${completePercent<100||showOverrun?remaining:''}<g:if test="${indefinite||showOverrun&&completePercent>110}"> <g:img file="icon-tiny-disclosure-waiting.gif"/> </g:if>${innerContent}</div></div>--}%
%{--</div>--}%
<g:set var="overrun" value="${indefinite || showOverrun && completePercent > 110}"/>
<div id="${containerId}" class="progress ${overrun ? 'progress-striped active' : ''} ${progressClass ?: ''}" style="${height?'height: '+height+'px':''}"
    data-bind="${bind?'css: '+bind+' > 109 ? \'progress-striped active\' : \'\'' : ''}"
>
    <g:set var="textToBind" value="${bindText?:bind + ' + \'%\''}"/>
    <div id="${progressId}" class="progress-bar  ${progressBarClass?:''}"
         role="progressbar" aria-valuenow="${completePercent}" aria-valuemin="0" aria-valuemax="100"
         style="width: ${completePercent}%;"
         data-bind="${bind?'style: { width: '+bind+' < 101 ? '+bind+'+ \'%\' : \'100%\' }, text: '+textToBind +(progressBind?:''):''}">
        <g:if test="${showpercent}">${completePercent > 100 ? 100 : completePercent}%</g:if>${completePercent < 100 || showOverrun ? remaining : ''}<g:if
                test="${overrun}"> <g:img
                    file="icon-tiny-disclosure-waiting.gif"/> </g:if>${innerContent}
        <span class="sr-only">${completePercent}% Complete</span>
    </div>
</div>
