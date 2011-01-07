<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="jobs"/>
    <title><g:message code="main.app.name"/></title>
    <script type="text/javascript">

        function execSubmit(elem){
            var params=Form.serialize(elem);
            new Ajax.Request(
                '${createLink(controller:"scheduledExecution",action:"runJobInline")}', {
                parameters: params,
                evalScripts:true,
                onComplete: function(trans) {
                    var result={};
                    if(trans.responseJSON){
                        result=trans.responseJSON;
                    }else if(trans.responseText){
                        result=eval(trans.responseText);
                    }
                    if(result.id){
                        unloadExec();
                    }else if(result.error=='invalid'){
                        //reload form for validation
                        loadExec(null,params+"&dovalidate=true");
                    }else{
                        unloadExec();
                        showError(result.message?result.message:result.error?result.error:"Failed request");
                    }
                },
                onFailure: requestError.curry("runJobInline")
            });
        }
        function loadedFormSuccess(){
            if ($('execFormCancelButton')) {
                $('execFormCancelButton').onclick = function() {
                    unloadExec();
                    return false;
                };
                $('execFormCancelButton').name = "_x";
            }else{
                console.log("no");
            }
            if ($('execFormRunButton')) {
                $('execFormRunButton').onclick = function(evt) {
                    Event.stop(evt);
                    execSubmit('execDivContent');
                    return false;
                };
            }
            $('indexMain').hide();
            $('execDiv').show();
            $('busy').hide();
        }
        function loadExec(id,eparams) {
            $('busy').innerHTML = '<img src="' + appLinks.iconSpinner + '" alt=""/> Loading...';
            $('busy').show();
            $("error").hide();
            var params=eparams;
            if(!params){
                params={id:id};
            }
            new Ajax.Updater(
                'execDivContent',
                '${createLink(controller:"scheduledExecution",action:"executeFragment")}', {
                parameters: params,
                evalScripts:true,
                onComplete: function(transport) {
                    if (transport.request.success()) {
                        loadedFormSuccess();
                    }
                },
                onFailure: requestError.curry("executeFragment for [" + id + "]")
            });

        }
        function unloadExec(){
            $('execDiv').hide();
            $('indexMain').show();
            $('execDivContent').innerHTML='';
            $('busy').hide();
        }

        function requestError(item,trans){
            unloadExec();
            showError("Failed request: "+item+" . Result: "+trans.getStatusText());
        }
        function showError(message){
             $('error').innerHTML+=message;
             $("error").show();
        }
       


        //set box filterselections
        function setFilter(name,value){
            if(!value){
                value="!";
            }
            var str=name+"="+value;
            new Ajax.Request("${createLink(controller:'user',action:'addFilterPref')}",{parameters:{filterpref:str}, evalJSON:true,onSuccess:function(response){
                _setFilterSuccess(response,name);
            }});
        }
        function _setFilterSuccess(response,name){
            var data=eval("("+response.responseText+")") // evaluate the JSON;
            if(data){
                var bfilters=data['filterpref'];
                //reload page
                document.location="${createLink(controller:'menu',action:'workflows')}"+(bfilters[name]?"?filterName="+encodeURIComponent(bfilters[name]):'');
            }
        }

        /** START history
         *
         */
        function loadHistory(){
            new Ajax.PeriodicalUpdater('histcontent',"${createLink(controller:'reports',action:'eventsFragment')}",{
                parameters:{compact:true,nofilters:true,jobIdFilter:'!null',recentFilter:'1d',userFilter:'${session.user}',projFilter:'${session.project}'},
                evalScripts:true,
                frequency:5,
                onComplete: function(transport) {
                    if (transport.request.success()) {
                        Element.show('histcontent');
                    }
                },
            });
        }

        //now running
        var runupdate;
        function loadNowRunning(){
            runupdate=new Ajax.PeriodicalUpdater('nowrunning','${createLink(controller:"menu",action:"nowrunningFragment")}',{
                evalScripts:true,
                parameters:{},
            });
        }
        function init(){
            loadNowRunning();
        }
        Event.observe(window,'load',init);
    </script>
    <g:javascript library="yellowfade"/>
    <g:render template="/framework/remoteOptionValuesJS"/>
    <style type="text/css">
    #nowrunning{
        max-height: 150px;
        overflow-y: auto;
        margin: 0 0 10px 0;
    }
    .error{
        color:red;
    }
    </style>
</head>
<body>


<div class="pageBody solo" id="indexMain">
    <g:if test="${flash.savedJob}">
        <div style="margin-bottom:10px;">
        <span class="popout message note" style="background:white">
            ${flash.savedJobMessage?flash.savedJobMessage:'Saved changes to Job'}:
            <g:link controller="scheduledExecution" action="show" id="${flash.savedJob.id}">${flash.savedJob.jobName}</g:link>
        </span>
        </div>
        <g:javascript>
            fireWhenReady('jobrow_${flash.savedJob.id}',doyft.curry('jobrow_${flash.savedJob.id}'));

        </g:javascript>
    </g:if>

    <div id="nowrunning"></div>
    <div id="error" class="error" style="display:none;"></div>
    <g:render template="workflowsFull" model="${[jobgroups:jobgroups,wasfiltered:wasfiltered?true:false,nowrunning:nowrunning,nextExecutions:nextExecutions,jobauthorizations:jobauthorizations,authMap:authMap,nowrunningtotal:nowrunningtotal,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true]}"/>
</div>
<div id="execDiv" style="display:none">

    <div id="execDivContent" >

    </div>
</div>
<div class="runbox">History</div>
    <div class="pageBody">
        <div id="histcontent"></div>
        <g:javascript>
            fireWhenReady('histcontent',loadHistory);
        </g:javascript>
    </div>
</body>
</html>
