<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title>%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%

<g:appTitle/> - <g:message code="userController.page.profile.title" />: ${user.login}</title>
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
    function changeLanguage() {
        var url = '${g.createLink(controller: 'user', action: 'profile')}';
        window.location.href = url + "?lang="+jQuery("#language").val();
    }
    function setLanguage() {
        //grails stores current locale in http session under below key
        var selectedLanguage = '${session[org.springframework.web.servlet.i18n.SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME]}';
        jQuery("#language").val(selectedLanguage);
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
    <div class="col-sm-12">
        <span class="pull-right">
           	<label for="language"><g:message code="user.profile.language.label" /></label>
            <select name="language" id="language" onchange="changeLanguage();">
                <option value="">English</option>
                <option value="es_419">Español</option>
            </select>
        </span>
    </div>
</div>

<div class="pageBody" id="userProfilePage">
    <g:render template="/common/messages"/>
    <g:jsonToken id='api_req_tokens' url="${request.forwardURI}"/>
    <tmpl:user user="${user}" edit="${true}"/>
</div>
</body>
</html>


