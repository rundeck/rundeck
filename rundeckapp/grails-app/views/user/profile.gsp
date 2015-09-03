<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title><g:appTitle/> - <g:message code="userController.page.profile.title" />: ${user.login}</title>
    <g:javascript library="prototype/effects"/>
    <g:javascript>
    function addTokenRow(elem,login,token){
        var table=$(elem).down('.apitokentable');
        var row=new Element('li');
        table.insert(row);
        $(row).addClassName('apitokenform');
        $(row).style.opacity=0;
        jQuery(row).load(_genUrl('${g.createLink(controller: 'user', action: 'renderApiToken')}',{login:login,token:token}),function(resp,status,jqxhr){
                    addRowBehavior($(row));
                    jQuery($(row)).fadeTo("slow",1);
        });
    }
    function tokenAjaxError(elem,msg){
        setText($(elem).down('.gentokenerror-text'),"Error: "+msg);
        $(elem).down('.gentokenerror').show();
    }
    function generateToken(login,elem){
        jQuery.ajax({
            type:'POST',
            dataType:'json',
            url:_genUrl(appLinks.userGenerateApiToken,{login:login}),
            beforeSend:_ajaxSendTokens.curry('api_req_tokens'),
            success:function(data,status,jqxhr){
                if( data.result){
                    addTokenRow(elem,login,data.apitoken);
                }else{
                    tokenAjaxError(elem,data.error);
                }
            },
            error:function(jqxhr,status,error){
                tokenAjaxError(elem,jqxhr.responseJSON&&jqxhr.responseJSON.error?jqxhr.responseJSON.error:error);
            }
        }).success(_ajaxReceiveTokens.curry('api_req_tokens'));
    }
    function clearToken(elem){
        var login=$(elem).down('input[name="login"]').value;
        var token=$(elem).down('input[name="token"]').value;
        var nelem=$(elem).up('.userapitoken');
         jQuery.ajax({
            type:'POST',
            dataType:'json',
            url:_genUrl(appLinks.userClearApiToken,{login:login,token:token}),
            beforeSend:_ajaxSendTokens.curry('api_req_tokens'),
            success:function(data,status,jqxhr){
                if( data.error){
                    tokenAjaxError(elem,data.error);
                }else if(data.result){
                    //remove row element
                    jQuery($(elem)).fadeOut("slow");
                }
            },
            error:function(jqxhr,status,error){
                tokenAjaxError(nelem,jqxhr.responseJSON&&jqxhr.responseJSON.error?jqxhr.responseJSON.error:error);
            },complete:function(){
                jQuery('#'+elem.identify()+' .modal').modal('hide');
            }
        }).success(_ajaxReceiveTokens.curry('api_req_tokens'));
    }
    function mkhndlr(func){
        return function(e){e.stop();func();return false;};
    }
    function addRowBehavior(e){
        Event.observe(e.down('.clearconfirm input.yes'),'click',mkhndlr(clearToken.curry(e)));
    }
    function addBehavior(elem,login){
        Event.observe($(elem).down('.gentokenbtn'),'click',mkhndlr(generateToken.curry(login,elem)));
        $$(' .apitokenform').each(addRowBehavior);
    }
    function highlightNew(elem){
        jQuery(' .apitokenform.newtoken').fadeTo('slow',1);
    }
    </g:javascript>
</head>
<body>

<div class="row">
    <div class="col-sm-12">
        <h3>
            <g:link action="profile" params="[login: user.login]">
                <g:icon name="user"/>
                ${user.login}
            </g:link>

            <g:link action="edit"
                    params="[login: user.login]"
                    class="small btn btn-link btn-sm"
                    title="${message(code:'userController.action.edit.description',args:[user.login])}">
                <g:icon name="edit"/>
                <g:message code="button.Edit.label" />
            </g:link>
        </h3>
    </div>
    <div class="col-sm-12">
        <div class="help-block">
            <g:message code="userController.page.profile.description" />
        </div>
    </div>
</div>

<div class="pageBody" id="userProfilePage">
    <g:render template="/common/messages"/>
    <g:jsonToken id='api_req_tokens' url="${request.forwardURI}"/>
    <tmpl:user user="${user}" edit="${true}"/>
</div>
</body>
</html>


