<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="jobs"/>
    <title><g:message code="main.app.name"/></title>
    <script type="text/javascript">

        function loadExec(id){
            $('busy').innerHTML='<img src="'+ appLinks.iconSpinner+'" alt=""/> Loading...';
            $('busy').show();
            new Ajax.Updater(
                'execDivContent',
                '${createLink(controller:"scheduledExecution",action:"executeInline")}',{
                parameters: "id="+id,
                evalScripts:true,
                 onComplete: function(transport) {

                     if($('execFormCancelButton')){
                         $('execFormCancelButton').onclick=function(){unloadExec();return false;};
                     }
                     $('indexMain').hide();
                     $('execDiv').show();
                     $('busy').hide();
                 },
                 onFailure: function() {
                     $('busy').hide();
                     $('indexMain').show();
                     $('execDiv').hide();
                     showError("Error performing request: execute for ["+id+"]");
                 }
                });

        }
        function unloadExec(){
            $('execDiv').hide();
            $('indexMain').show();
            $('execDivContent').innerHTML='';
            $('busy').hide();
        }

        function showError(message){
             $("error").innerHTML+=message;
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

    <g:render template="workflowsFull" model="${[jobgroups:jobgroups,wasfiltered:wasfiltered?true:false,nowrunning:nowrunning,nextExecutions:nextExecutions,jobauthorizations:jobauthorizations,authMap:authMap,nowrunningtotal:nowrunningtotal,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true]}"/>
</div>
<div id="execDiv" style="display:none">

    <div id="error" class="error note" style="display:none;">
    </div>
    <div id="execDivContent" >

    </div>
</div>
</body>
</html>
