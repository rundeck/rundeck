<%--
 bootstrap collapse version of _expander.gsp
--%><span class="${enc(attr:classnames?:'')} ${classnames&&classnames.indexOf('btn-')>=0?'': 'btn-link'} btn  ${open=='true'?'active':''}" data-toggle="collapse" data-target="#${key}" id="_exp_${enc(attr:key)}"><!--
--><g:enc rawtext="${text != null && !imgfirst ? text:''}"/><!--
--> <i class="glyphicon glyphicon-chevron-${open == 'true' ? 'down' : 'right'}"></i> <!--
--><g:enc rawtext="${text != null && imgfirst ? text:''}"/><!--
--></span>
