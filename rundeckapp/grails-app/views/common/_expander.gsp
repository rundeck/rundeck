<span class="${classnames?classnames:''} action expandComponentControl toggle ${open=='true'?'expanded':'closed'}" onmousedown="${key?'Expander.toggle(this,\''+key+'\')':jsfunc?jsfunc:'Expander.toggle(this)'};return false;" style="padding:2px;${style}" id="_exp_${key}"><!--
    -->${text!=null && !imgfirst ?text:''}<!--
    --><g:img file="icon-tiny-disclosure${open=='true'?'-open':''}.png" width="12px" height="12px"/><!--
    -->${text!=null && imgfirst ?text:''}<!--
--></span>