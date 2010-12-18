<div id="${containerId}" class="progressContainer small ${className} ${(!completePercent || completePercent < 1 ) ? 'empty':''}" >
    <div
        id="${barId}"
        class="progressBar ${(!completePercent || completePercent < 1 ) ? 'empty':''} ${className} ${completePercent>=100?'full':''}"
        title="${title}"
        style="width:${completePercent>100?'100':completePercent<10?40:completePercent}"
    ><div class="progresscontent"><g:if test="${showpercent}">${completePercent>100?'100':completePercent}%</g:if>${completePercent<100?remaining:''}${innerContent}</div></div>
</div>