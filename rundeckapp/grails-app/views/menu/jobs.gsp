<%@ page import="grails.util.Environment" %>
<html>
<head>
    <g:set var="rkey" value="${g.rkey()}" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="jobs"/>
    <title><g:message code="gui.menu.Workflows"/> - <g:enc>${params.project ?: request.project}</g:enc></title>
    <g:javascript library="yellowfade"/>
    <g:javascript library="pagehistory"/>
    <g:javascript library="prototype/effects"/>
    <g:javascript library="executionOptions"/>
    <asset:javascript src="historyKO.js"/>
    <!--[if (gt IE 8)|!(IE)]><!--> <g:javascript library="ace/ace"/><!--<![endif]-->
    <script type="text/javascript">
        /** knockout binding for activity */
        var pageActivity;
        function showError(message){
             appendText($('error'),message);
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
                clearHtml('execDivContent');
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
            jQuery('#execDivContent').load(_genUrl(appLinks.scheduledExecutionExecuteFragment, params),function(response,status,xhr){
                if (status=='success') {
                    loadedFormSuccess();
                } else{
                    requestError("executeFragment for [" + id + "]",xhr);
                }
            });
        }
        function execSubmit(elem){
            var params=Form.serialize(elem);
            new Ajax.Request(
                appLinks.scheduledExecutionRunJobInline, {
                parameters: params,
                evalScripts:true,
                onComplete: function(trans) {
                    var result={};
                    if(trans.responseJSON){
                        result=trans.responseJSON;
                    }
                    if(result.id){
                        if (result.follow && result.href) {
                            document.location = result.href;
                        }else{
                            if(!pageActivity.selected()){
                                pageActivity.activateNowRunningTab();
                            }
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

        function _setFilterSuccess(data,name){
            if(data){
                var bfilters=data.filterpref;
                //reload page
                document.location=_genUrl(appLinks.menuJobs , bfilters[name] ? {filterName:bfilters[name]} : {});
            }
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
            var delay=1500;
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

            jQuery('#jobIdDetailContent').load(_genUrl(appLinks.scheduledExecutionDetailFragment, {id: matchId}),function(response,status,xhr){
                if (status=='success') {
                    popJobDetails(elem);
                    $('jobIdDetailContent').select('.apply_ace').each(function (t) {
                        _applyAce(t);
                    });
                }else{
                    clearHtml(bcontent);
                    viewdom.hide();
                }
            });
        }

        function initJobIdLinks(){
            $$('.hover_show_job_info').each(function(e){
                Event.observe(e,'mouseover',jobLinkMouseover.curry(e));
                Event.observe(e,'mouseout',jobLinkMouseout.curry(e));
            });

            jQuery('.act_job_action_dropdown').click(function(){
                var id=jQuery(this).data('jobId');
                var el=jQuery(this).parent().find('.dropdown-menu');
                el.load(
                    _genUrl(appLinks.scheduledExecutionActionMenuFragment,{id:id})
                );
            });
            jQuery(document.body).on('click','.act_job_delete_single',function(){
                var el=jQuery(this);
                var id=el.data('jobId');
                jQuery('.job_bulk_edit').click();//show bulk edit mode
                //check only the checkbox with this job id by passing an array
                jQuery(':input[name=ids]').val([id]);
               jQuery('#bulk_del_confirm').modal('toggle');
            });
        }
         function filterToggle(evt) {
            ['${enc(js:rkey)}filter','${enc(js:rkey)}filter-toggle'].each(Element.toggle);
        }
        function filterToggleSave(evt) {
            ['${enc(js:rkey)}filter','${enc(js:rkey)}fsave'].each(Element.show);
            ['${enc(js:rkey)}filter-toggle','${enc(js:rkey)}fsavebtn'].each(Element.hide);
        }
        function init(){
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
                pageActivity = new History(appLinks.reportsEventsAjax, appLinks.menuNowrunningAjax);
                ko.applyBindings(pageActivity, document.getElementById('activity_section'));
                setupActivityLinks('activity_section', pageActivity);
            }
            jQuery('.act_execute_job').on('click',function(evt){
                evt.preventDefault();
               loadExec(jQuery(this).data('jobId'));
            });
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
                <li><g:enc>${message}</g:enc></li>
            </g:each>
        </ul>
    </div>
</g:if>
<g:if test="${flash.bulkDeleteResult?.success}">
    <div class="alert alert-dismissable alert-info">
        <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
        <ul>
        <g:each in="${flash.bulkDeleteResult.success*.message}" var="message">
            <li><g:enc>${message}</g:enc></li>
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
        <h4 class="text-muted "><g:message code="page.section.Activity.for.jobs" /></h4>
        <g:render template="/reports/activityLinks"
                  model="[filter: [projFilter: params.project ?: request.project, jobIdFilter: '!null',], knockoutBinding: true, showTitle:true]"/>
    </div>
</div>
</body>
</html>
