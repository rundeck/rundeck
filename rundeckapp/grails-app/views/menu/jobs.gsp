<%@ page import="grails.util.Environment" %>
<html>
<head>
    <g:set var="rkey" value="${g.rkey()}" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="jobs"/>
    <title><g:message code="gui.menu.Workflows"/> - ${session.project.encodeAsHTML()}</title>
    <g:javascript library="yellowfade"/>
    <g:javascript library="pagehistory"/>
    <g:javascript library="prototype/effects"/>
    <g:javascript library="executionOptions"/>
    <g:if test="${grails.util.Environment.current == Environment.DEVELOPMENT}">
        <g:javascript src="knockout-3.0.0.debug.js"/>
    </g:if>
    <g:else>
        <g:javascript src="knockout-3.0.0-min.js"/>
    </g:else>
    <g:javascript src="knockout.mapping-latest.js"/>
    <g:javascript src="moment.min.js"/>
    <asset:javascript src="momentutil.js"/>
    <g:javascript src="historyKO.js"/>
    <!--[if (gt IE 8)|!(IE)]><!--> <g:javascript library="ace/ace"/><!--<![endif]-->
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
            new Effect.BlindDown('indexMain', {duration: 0.2});
            new Effect.BlindUp('execDiv', {duration: 0.2, afterFinish:function(e){
                $('execDivContent').innerHTML = '';
            }});
            $('busy').hide();
        }
        function requestError(item,trans){
            unloadExec();
            showError("Failed request: "+item+" . Result: "+trans.getStatusText());
        }
        function loadExec(id,eparams) {
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
                        if (result.follow && result.href) {
                            document.location = result.href;
                        }else{
                            unloadExec();
                        }
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
                    $('formbuttons').loading("Starting Execution…");
                    return false;
                },false);
            }
            new Effect.BlindUp('indexMain', {duration: 0.2});
            new Effect.BlindDown('execDiv', {duration: 0.2});
            $('busy').hide();
        }

       


        //set box filterselections

        function _setFilterSuccess(response,name){
            var data=eval("("+response.responseText+")"); // evaluate the JSON;
            if(data){
                var bfilters=data.filterpref;
                //reload page
                document.location="${createLink(controller:'menu',action:'jobs')}"+(bfilters[name]?"?filterName="+encodeURIComponent(bfilters[name]):'');
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


        /** now running section update */
        function _pageUpdateNowRunning(count){
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
            }
        }

        //now running
        var runupdate;
        function loadNowRunning(){
            runupdate=new Ajax.PeriodicalUpdater({success:'nowrunning'},'${createLink(controller:"menu",action:"nowrunningFragment",params: execQueryParams?:[projFilter: session.project])}',{
                evalScripts:true,
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
            var delay=0;
            if(popvis){
                delay=0;
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
            mltimer=setTimeout(doMouseout,0);
        }
        function showJobDetails(elem){
            //get url
            var href=elem.href || elem.getAttribute('data-href');
            var match=href.match(/\/job\/.+?\/(.+)$/);
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
                viewdom.setAttribute('style','display:none;width:600px;height:250px;');

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
                        $('jobIdDetailContent').select('.apply_ace').each(function (t) {
                            _applyAce(t);
                        })
                    }
                },
                onFailure: function(trans){
                    bcontent.innerHTML='';
                    viewdom.hide();
                }
            });
        }
        function initJobIdLinks(){
            $$('.jobIdLink').each(function(e){
                Event.observe(e,'mouseover',jobLinkMouseover.curry(e));
                Event.observe(e,'mouseout',jobLinkMouseout.curry(e));
            });
        }
         function filterToggle(evt) {
            ['${rkey}filter','${rkey}filter-toggle'].each(Element.toggle);
            if($('outsidefiltersave')){
                $('${rkey}filter').visible()? $('outsidefiltersave').hide: $('outsidefiltersave').show;
            }
        }
        function filterToggleSave(evt) {
            ['${rkey}filter','${rkey}fsave'].each(Element.show);
            ['${rkey}filter-toggle','${rkey}fsavebtn'].each(Element.hide);
            if ($('outsidefiltersave')) {
                $('${rkey}filter').visible() ? $('outsidefiltersave').hide : $('outsidefiltersave').show;
            }
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

        jQuery(document).ready(function () {
            init();
            if (jQuery('#activity_section')) {
                var history = new History("${g.createLink(controller: 'reports', action: 'eventsAjax', absolute: true)}");
                ko.applyBindings(history, document.getElementById('activity_section'));
                setupActivityLinks('activity_section', history, "${g.createLink(controller: 'reports', action: 'eventsAjax', absolute: true)}");
            }
        });
    </script>
    <g:javascript library="yellowfade"/>
    <g:render template="/framework/remoteOptionValuesJS"/>
    <style type="text/css">
    .error{
        color:red;
    }

        #histcontent table{
            width:100%;
        }
    </style>
</head>
<body>


<g:if test="${flash.bulkDeleteResult?.errors}">
    <div class="alert alert-dismissable alert-warning">
        <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
        <ul>
            <g:each in="${flash.bulkDeleteResult.errors*.message}" var="message">
                <li>${message.encodeAsHTML()}</li>
            </g:each>
        </ul>
    </div>
</g:if>
<g:if test="${flash.bulkDeleteResult?.success}">
    <div class="alert alert-dismissable alert-info">
        <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
        <ul>
        <g:each in="${flash.bulkDeleteResult.success*.message}" var="message">
            <li>${message.encodeAsHTML()}</li>
        </g:each>
        </ul>
    </div>
</g:if>
<div class="runbox primary jobs" id="indexMain">
    <div id="error" class="error message" style="display:none;"></div>
    <g:render template="workflowsFull" model="${[jobgroups:jobgroups,wasfiltered:wasfiltered?true:false,nowrunning:nowrunning, clusterMap: clusterMap,nextExecutions:nextExecutions,jobauthorizations:jobauthorizations,authMap:authMap,nowrunningtotal:nowrunningtotal,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true,rkey:rkey]}"/>
</div>
<div id="execDiv" style="display:none">
    <div id="execDivContent" >

    </div>
</div>

<div class="row row-space" id="activity_section">
    <div class="col-sm-12 ">
        <h4 class="text-muted "><g:message code="page.section.Activity"/></h4>
        <g:render template="/reports/activityLinks"
                  model="[filter: [projFilter:session.project, jobIdFilter: '!null',], knockoutBinding: true, showTitle:true]"/>
    </div>
</div>
</body>
</html>
