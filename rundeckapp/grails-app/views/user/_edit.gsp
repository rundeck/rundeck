<g:javascript>
function _acl_form_check(prefix){
    _acl_form_check_input(prefix+'_t');
    _acl_form_check_input(prefix+'_f');
}
function _acl_form_check_input(elem){
    var tdt = $(elem).parentNode;
    if($F(elem)=="true" && !$(tdt).hasClassName("enabled") || $F(elem)==null && $(tdt).hasClassName("enabled")){
        $(tdt).addClassName('changed');
    }else if($F(elem)=="false" && !$(tdt).hasClassName("disabled") || $F(elem)==null && $(tdt).hasClassName("disabled")){
        $(tdt).addClassName('changed');
    }else{
        $(tdt).removeClassName('changed');
    }
}
function _acl_set(group,value){
    $$('table.'+group+' td.authItem.'+value+' input').each(function(i){
        $(i).setValue(true);
    });
    $$('table.'+group+' td.authItem input').each(function(i){
        _acl_form_check_input(i);
    });
}
function _acl_toggle(prefix,key){
    if(null==$F(prefix+'_'+key+'_t')){
        $(prefix+'_'+key+'_t').setValue(true);
    }
    else if(null==$F(prefix+'_'+key+'_f')){
        $(prefix+'_'+key+'_f').setValue(true);
    }
    _acl_form_check(prefix+'_'+key);
    return false;
}
</g:javascript>
    <table class="simpleForm">
    <tr>
        <td>
            User:
        </td>
        <td>
            <g:if test="${newuser}">
                <g:textField name="login" value="${user.login}"/>
            </g:if>
            <g:elseif test="user.login">
                ${user.login}
                <g:hiddenField name="login" value="${user.login}"/>
            </g:elseif>
        </td>
    </tr>
    <tr>
        <td>
            First Name:
        </td>
        <td>
            <g:textField name="firstName" value="${user.firstName}"/>
        </td>
    </tr>
    <tr>
        <td>
            Last Name:
        </td>
        <td>
            <g:textField name="lastName" value="${user.lastName}"/>
        </td>
    </tr>
    <tr>
        <td>
            Email:
        </td>
        <td>
            <g:textField name="email" value="${user.email}"/>
        </td>
    </tr>
    </table>

