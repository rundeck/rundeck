<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title><g:message code="main.app.name"/> - User Profile</title>
    <g:javascript library="prototype/effects"/>
    <g:javascript>
    function addTokenRow(elem,login,token){
        var table=$(elem).down('table.apitokentable');
        var row=table.insertRow(-1);
        $(row).addClassName('apitokenform');
        $(row).style.opacity=0;
        jQuery(row).load(_genUrl('${g.createLink(controller: 'user', action: 'renderApiToken')}',{login:login,token:token}),function(resp,status,jqxhr){
                    addRowBehavior($(row));
                    Effect.Appear($(row));
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
                    Effect.DropOut(elem);
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
        $$(' tr.apitokenform').each(addRowBehavior);
    }
    function highlightNew(elem){
        $$(' tr.apitokenform.newtoken').each( Effect.Appear);
    }
    </g:javascript>
</head>
<body>

<div class="row">
    <div class="col-sm-10">
        <h3>User: <g:enc>${user.login}</g:enc>
        </h3>
    </div>
    <div class="col-sm-2">

    </div>
</div>

<div class="pageBody" id="userProfilePage">
    <g:render template="/common/messages"/>
    <g:jsonToken id='api_req_tokens' url="${request.forwardURI}"/>
    <tmpl:user user="${user}" edit="${true}"/>
</div>
</body>
</html>


