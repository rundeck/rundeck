<g:set var="usewidth" value="${width?:100}"/>
<g:set var="overrun" value="${indefinite || showOverrun && completePercent > 110}"/>
<div id="${enc(attr: containerId)}" class="progress ${overrun ? 'progress-striped active' : ''} ${enc(attr:progressClass?:'')}" style="${height?'height: '+ enc(attr:height)+'px':''}"
    data-bind="${raw(bind?'css: '+ enc(attr:bind)+' > 109 ? \'progress-striped active\' : \'\'' : '')}"
>
    <g:set var="textToBind" value="${bindText?: enc(attr:bind) + ' + \'%\''}"/>
    <div id="${enc(attr:progressId)}" class="progress-bar  ${enc(attr:progressBarClass?:'')}"
         role="progressbar" aria-valuenow="${enc(attr:completePercent)}" aria-valuemin="0" aria-valuemax="100"
         style="width: ${enc(attr:completePercent)}%;"
         data-bind="${raw(bind?'style: { width: '+ enc(attr:bind)+' < 101 ? '+ enc(attr:bind)+'+ \'%\' : \'100%\' }, text: '+ enc(attr:textToBind) +(progressBind? enc(attr:progressBind):''):'')}">
        <g:if test="${showpercent}"><g:enc>${completePercent > 100 ? 100 : completePercent}</g:enc>%</g:if><g:enc>${completePercent < 100 || showOverrun ? remaining : ''}</g:enc><g:if
                test="${overrun}"> <g:img
                    file="icon-tiny-disclosure-waiting.gif"/> </g:if><g:enc>${innerContent}</g:enc>
        <span class="sr-only"><g:enc>${completePercent}</g:enc>% Complete</span>
    </div>
</div>
