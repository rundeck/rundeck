<html>
<head>
    <g:set var="rkey" value="${g.rkey()}" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="jobs"/>
    <title><g:message code="gui.menu.Workflows"/></title>
    <g:javascript library="yellowfade"/>
    <g:javascript library="pagehistory"/>
    <g:javascript library="prototype/effects"/>
    <g:javascript library="executionOptions"/>
    <script type="text/javascript">

        function showError(message){
             $('error').innerHTML+=message;
             $("error").show();
        }
        var _jobExecUnloadHandlers=new Array();
        function _registerJobExecUnloadHandler(handler){
            _jobExecUnloadHandlers.push(handler);
        }
        function unloadExec(){
            if(_jobExecUnloadHandlers.length>0){
                for(var i =0;i<_jobExecUnloadHandlers.length;i++){
                    _jobExecUnloadHandlers[i].call();
                }
                _jobExecUnloadHandlers.clear();
            }
            $('execDiv').hide();
            $('indexMain').show();
            $('execDivContent').innerHTML='';
            $('busy').hide();
        }
        function requestError(item,trans){
            unloadExec();
            showError("Failed request: "+item+" . Result: "+trans.getStatusText());
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
                    }else if(result.error==='invalid'){
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
                Event.observe($('execFormCancelButton'),'click',function(evt) {
                    Event.stop(evt);
                    unloadExec();
                    return false;
                },false);
                $('execFormCancelButton').name = "_x";
            }
            if ($('execFormRunButton')) {
                Event.observe($('execFormRunButton'),'click', function(evt) {
                    Event.stop(evt);
                    execSubmit('execDivContent');
                    return false;
                },false);
            }
            $('indexMain').hide();
            $('execDiv').show();
            $('busy').hide();
        }

       


        //set box filterselections

        function _setFilterSuccess(response,name){
            var data=eval("("+response.responseText+")"); // evaluate the JSON;
            if(data){
                var bfilters=data.filterpref;
                //reload page
                document.location="${createLink(controller:'menu',action:'workflows')}"+(bfilters[name]?"?filterName="+encodeURIComponent(bfilters[name]):'');
            }
        }
        function setFilter(name,value){
            if(!value){
                value="!";
            }
            var str=name+"="+value;
            new Ajax.Request("${createLink(controller:'user',action:'addFilterPref')}",{parameters:{filterpref:str}, evalJSON:true,onSuccess:function(response){
                _setFilterSuccess(response,name);
            }});
        }


        /** START history
         *
         */
        var histControl = new HistoryControl('histcontent',{compact:true,nofilters:true,recentFilter:'1d',projFilter:'${session.project}'});
        function loadHistory(){
            histControl.loadHistory();
        }
        /** now running section update */
        var savedcount=0;
        function _pageUpdateNowRunning(count){
            if(count!==savedcount){
                savedcount=count;
                loadHistory();
            }
        }
        var lastRunExec=0;
        /**
         * Handle embedded content updates
         */
        function _updateBoxInfo(name,data){
            if(name==='events' && data.lastDate){
                histControl.setHiliteSince(data.lastDate);
            }
            if (name == 'nowrunning' && data.lastExecId && data.lastExecId != lastRunExec) {
                lastRunExec = data.lastExecId;
                loadHistory();
            }
        }

        //now running
        var runupdate;
        function loadNowRunning(){
            runupdate=new Ajax.PeriodicalUpdater({success:'nowrunning'},'${createLink(controller:"menu",action:"nowrunningFragment")}',{
                evalScripts:true,
                parameters:{projFilter:'${session.project}'},
                onFailure:function (response) {
                    showError("AJAX error: Now Running ["+ runupdate.url+"]: "+response.status+" "+response.statusText);
                    runupdate.stop();
                }
            });
        }


        /////////////
        // Job context detail popup code
        /////////////

        var doshow=false;
        var popvis=false;
        var lastHref;
        var targetLink;
        function popJobDetails(elem){
            if(doshow && $('jobIdDetailHolder')){
                new MenuController().showRelativeTo(elem,$('jobIdDetailHolder'));
                popvis=true;
                if(targetLink){
                    $(targetLink).removeClassName('glow');
                    targetLink=null;
                }
                $(elem).addClassName('glow');
                targetLink=elem;
            }
        }
        var motimer;
        var mltimer;
        function bubbleMouseover(evt){
            if(mltimer){
                clearTimeout(mltimer);
                mltimer=null;
            }
        }
        function jobLinkMouseover(elem,evt){
            if(mltimer){
                clearTimeout(mltimer);
                mltimer=null;
            }
            if(motimer){
                clearTimeout(motimer);
                motimer=null;
            }
            if(popvis && lastHref===elem.href){
                return;
            }
            var delay=50;
            if(popvis){
                delay=50;
            }
            motimer=setTimeout(showJobDetails.curry(elem),delay);
        }
        function doMouseout(){
            if(popvis && $('jobIdDetailHolder')){
                popvis=false;
                Try.these(
//                    function(){Effect.Fade($('jobIdDetailHolder'),{duration:0.5});},
                    function(){$('jobIdDetailHolder').hide();}
                    );
            }
            if(targetLink){
                $(targetLink).removeClassName('glow');
                targetLink=null;
            }
        }
        function jobLinkMouseout(elem,evt){
            //hide job details
            if(motimer){
                clearTimeout(motimer);
                motimer=null;
            }
            doshow=false;
            mltimer=setTimeout(doMouseout,500);
        }
        function showJobDetails(elem){
            //get url
            var href=elem.href;
            var match=href.match(/\/job\/show\/(.+)$/);
            if(!match){
                return;
            }
            lastHref=href;
            doshow=true;
            //match is id
            var matchId=match[1];
            var viewdom=$('jobIdDetailHolder');
            var bcontent=$('jobIdDetailContent');
            if(!viewdom){
                viewdom = $(document.createElement('div'));
                viewdom.addClassName('bubblewrap');
                viewdom.setAttribute('id','jobIdDetailHolder');
                viewdom.setAttribute('style','display:none;');

                Event.observe(viewdom,'click',function(evt){
                    evt.stopPropagation();
                },false);

                var btop = new Element('div');
                btop.addClassName('bubbletop');
                viewdom.appendChild(btop);
                bcontent = new Element('div');
                bcontent.addClassName('bubblecontent');
                bcontent.setAttribute('id','jobIdDetailContent');
                viewdom.appendChild(bcontent);
                document.body.appendChild(viewdom);
                Event.observe(viewdom,'mouseover',bubbleMouseover);
                Event.observe(viewdom,'mouseout',jobLinkMouseout.curry(viewdom));
            }
            bcontent.loading();


            new Ajax.Updater('jobIdDetailContent','${createLink(controller:'scheduledExecution',action:'detailFragment')}',{
                parameters:{id:matchId},
                evalScripts:true,
                onComplete: function(trans){
                    if(trans.request.success()){
                        popJobDetails(elem);
                    }
                },
                onFailure: function(trans){
                    bcontent.innerHTML='';
                    viewdom.hide();
                }
            });
        }
        function initJobIdLinks(){
            $$('a.jobIdLink').each(function(e){
                Event.observe(e,'mouseover',jobLinkMouseover.curry(e));
                Event.observe(e,'mouseout',jobLinkMouseout.curry(e));
            });
        }
         function filterToggle(evt) {
            ['${rkey}filter','${rkey}filter-toggle'].each(Element.toggle);
            ['outsidefiltersave'].each($('${rkey}filter').visible()?Element.hide:Element.show);
        }
        function filterToggleSave(evt) {
            ['${rkey}filter','${rkey}fsave'].each(Element.show);
            ['${rkey}filter-toggle','${rkey}fsavebtn'].each(Element.hide);
            ['outsidefiltersave'].each($('${rkey}filter').visible()?Element.hide:Element.show);
        }
        function init(){
            loadNowRunning();
            <g:if test="${!(grailsApplication.config.rundeck?.gui?.enableJobHoverInfo in ['false',false])}">
            initJobIdLinks();
            </g:if>
            Event.observe(document.body,'click',function(evt){
                //click outside of popup bubble hides it
                doMouseout();
            },false);
            Event.observe(document,'keydown',function(evt){
                //escape key hides popup bubble
                if(evt.keyCode===27 ){
                    doMouseout();
                }
                return true;
            },false);

            $$('.obs_filtertoggle').each(function(e) {
                Event.observe(e, 'click', filterToggle);
            });
            $$('.obs_filtersave').each(function(e) {
                Event.observe(e, 'click', filterToggleSave);
            });
        }
        Event.observe(window,'load',init);
    </script>
    <g:javascript library="yellowfade"/>
    <g:render template="/framework/remoteOptionValuesJS"/>
    <style type="text/css">
    .error{
        color:red;
    }

    .bubblewrap {
        position: absolute;
        width: 600px;
        height: 250px;
    }
        #histcontent table{
            width:100%;
        }
    </style>
</head>
<body>


<div class="pageBody solo" >
    <span class="prompt">Now running <span class="nowrunningcount">(0)</span></span>
    <div id="nowrunning"><span class="note empty">No running Jobs</span></div>

    <div id="error" class="error message" style="display:none;"></div>
</div>
<div class="runbox jobs" id="indexMain">
    <g:render template="workflowsFull" model="${[jobgroups:jobgroups,wasfiltered:wasfiltered?true:false,nowrunning:nowrunning,nextExecutions:nextExecutions,jobauthorizations:jobauthorizations,authMap:authMap,nowrunningtotal:nowrunningtotal,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true,rkey:rkey]}"/>
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
