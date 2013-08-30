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
        new Ajax.Updater(row,'${g.createLink(controller: 'user', action: 'renderApiToken')}',{
            parameters:{login:login,token:token},
            onComplete:function(req2){
                if(req2.request.success()){
                    addRowBehavior($(row));
                    Effect.Appear($(row));
                }
            }

        });
    }
    function generateToken(login,elem){
        new Ajax.Request('${g.createLink(controller: 'user', action: 'generateApiToken')}.json',{
            parameters:{login:login},
            evalJSON:true,
            onComplete:function(req){
                var success=req.request.success();
                var json=req.responseJSON;
                var error = !success?req:json.error?json.error:null;
                if( !error && json.result){
                    addTokenRow(elem,login,json.apitoken);
                }else{
                    $(elem).down('.gentokenerror').innerHTML="Error: "+error;
                    $(elem).down('.gentokenerror').show();
                }
            }
        });
    }
    function clearToken(elem){
        var login=$(elem).down('input[name="login"]').value;
        var token=$(elem).down('input[name="token"]').value;
        new Ajax.Request('${g.createLink(controller: 'user', action: 'clearApiToken')}.json',{
            parameters:{login:login,token:token},
            evalJSON:true,
            onComplete:function(req){
                var success=req.request.success();
                var json=req.responseJSON;
                var error = !success?req:json.error?json.error:null;
                if( !error && json.result){
                    jQuery('#'+elem.identify()+' .modal').modal('hide');
                    //remove element
                    Effect.DropOut(elem);
                }else{
                    $(elem).up('.userapitoken').down('.gentokenerror').innerHTML="Error: "+error;
                    $(elem).up('.userapitoken').down('.gentokenerror').show();
                }
            }
        });
    }
    function clearShow(elem){
//        $(elem).down('.clearconfirm').show();
//        $(elem).down('.cleartokenbtn').hide();
    }
    function clearHide(elem){
//        $(elem).down('.clearconfirm').hide();
//        $(elem).down('.cleartokenbtn').show();
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
        <h3>User Profile: ${user.login}
            <small>
            <g:link action="edit" params="[login: params.login]" class=" textbtn textbtn-default btn-sm">
                <i class="glyphicon-pencil glyphicon"></i>
                Edit
            </g:link>
            </small>
        </h3>
    </div>
    <div class="col-sm-2">

    </div>
</div>

<div class="pageBody" id="userProfilePage">
    <g:render template="/common/messages"/>

    <tmpl:user user="${user}"/>
</div>
</body>
</html>


