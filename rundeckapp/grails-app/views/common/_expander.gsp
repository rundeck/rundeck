<span class="${enc(attr:classnames?:'')} ${classnames&&classnames.indexOf('textbtn-')>=0?'': 'textbtn-default'} textbtn  expandComponentControl toggle ${open=='true'?'expanded':'closed'}" onmousedown="${key?'Expander.toggle(this,\''+key+'\')':jsfunc?jsfunc:'Expander.toggle(this)'};return false;" style="padding:2px;${enc(attr:style)}" id="_exp_${enc(attr:key)}"><!--
    -->${text!=null && !imgfirst ? text:''}<!--
    --><b class="glyphicon glyphicon-chevron-${open == 'true' ? 'down' : 'right'}"></b><!--
    -->${text!=null && imgfirst ? text:''}<!--
--></span>
