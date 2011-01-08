<div id="${containerId}" class="progressContainer small ${className} ${indefinite?'indefinite':''} ${(!completePercent || completePercent < 1 ) ? 'empty':''}" >
    <div
        id="${barId}"
        class="progressBar ${(!completePercent || completePercent < 1 ) ? 'empty':''} ${className} ${completePercent>=100?'full':''}"
        title="${title}"
        style="width:${completePercent>100?'100':completePercent}"
    ><div class="progresscontent"><g:if test="${showpercent}">${completePercent>100?'100':completePercent}%</g:if>${completePercent<100||showOverrun?remaining:''}<g:if test="${indefinite||showOverrun&&completePercent>110}"> <g:img file="icon-tiny-disclosure-waiting.gif"/> </g:if>${innerContent}</div></div>
</div>