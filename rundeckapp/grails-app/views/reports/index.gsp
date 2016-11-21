<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="events"/>
    %{--
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

<g:ifServletContextAttribute attribute="RSS_ENABLED" value="true">
    <link rel="alternate" type="application/rss+xml" title="RSS 2.0" href="${createLink(controller:"feed",action:"index",params:paginateParams?paginateParams:[:])}"/>
    </g:ifServletContextAttribute>
    <title><g:message code="gui.menu.Events"/> - <g:enc>${params.project ?: request.project}</g:enc></title>
    <g:javascript library="yellowfade"/>
    <g:javascript library="pagehistory"/>
    <g:javascript>
                

        var pagefirstload=true;
        function _pageUpdateNowRunning(count, perc) {
            document.title = "Now Running (" + count + ")";
            if($('nrlocal')){
                setText($('nrlocal'), '' + count);
            }
            if(pagefirstload){
                pagefirstload=false;
                if(count > 0){
                    Expander.toggle('_exp_dashholder','dashholder');
                }
            }
        }
        function showError(message) {
            if ($('loaderror')) {
                appendText($("loaderror"),message);
                $("loaderror").show();
            }
        }

        <g:set var="pageparams" value="${[offset:params.offset,max:params.max]}"/>
        <g:set var="eventsparams" value="${paginateParams}"/>
        var eventsparams;
        var pageparams;
        var autoLoad=${params.refresh == 'true' ? true : false};
        var links = {
            events:'${createLink(controller: "reports", action: "eventsFragment", params: [project: params.project])}',
            nowrunning:'${createLink(controller: "menu", action: "nowrunningFragment",params: [project:params.project,max:params.max?:""])}',
            baseUrl:"${createLink(controller: "reports", action: "index", params: [project: params.project])}"
        };
        var runupdate;
        var nroffset=0;
        var nrtimeout;
        function loadNowRunningHref(href){
            var data=parseUrlParams(href);
            if(data.offset){
                nowRunningOffset(data.offset);
            }
        }
        function nowRunningOffset(val){
            nroffset=val;
            clearTimeout(nrtimeout);
            loadNowRunning();
        }
        function loadNowRunning(){
            jQuery('#nowrunning').load(_genUrl(links.nowrunning,jQuery.extend({},eventsparams,{offset:nroffset})),
                function(response, status, xhr){
                    if ( status == "error" ) {
                        showError("AJAX error: Now Running [" + links.nowrunning + "]: " + xhr.status + " "+ xhr.statusText);

                    }else{
                        //reschedule
                        nrtimeout=setTimeout(loadNowRunning,5000);
                    }
                }
            );
        }
        /** START history
         *
         */
        var histControl = new HistoryControl('histcontent',{xcompact:true,nofilters:true});
        function loadHistory(params){
            histControl.loadHistory( params|| eventsparams );
        }
        function setAutoLoad(auto){
            autoLoad=auto;

            $$('input.autorefresh').each(function(e){
                e.checked=auto;
            });
        }
        function _pageInit() {
            eventsparams=loadJsonData('eventsparamsJSON');
            pageparams=loadJsonData('pageparamsJSON')
            try{
                if(pageparams && pageparams.offset){
                    Object.extend(eventsparams,pageparams);
                }
            }catch(e){
                console.log("error: "+e);
            }
            loadNowRunning();
            $$('input.autorefresh').each(function(e){
                var changeHandler=function(evt){
                    Object.extend(eventsparams,{refresh:e.checked});
                    autoLoad=e.checked;
                    url=_genUrl(links.baseUrl,eventsparams);
                    if(typeof(history.pushState)=='function'){
                        history.pushState(eventsparams, pageTitle, url);
                    }else{
                        document.location=url;
                    }
                    if(autoLoad){
                        loadHistory();
                        $('eventsCountBadge').hide();
                    }else{
                        _scheduleSinceCheck();
                    }
                };
                Event.observe(e,'change',  changeHandler);
                if(Prototype.Browser.IE
                    && $(e).tagName.toLowerCase()=='input'
                    && ($(e).type.toLowerCase()=='radio' ||$(e).type.toLowerCase()=='checkbox')){
                    Event.observe(e,'click',  changeHandler);
                }

            });
            jQuery('body').on("click",".paginate.nowrunning a",function(e) {
                e.preventDefault();
                //console.log("click: "+jQuery(this).attr('href'));
                loadNowRunningHref(jQuery(this).attr('href'));
            });
        }

        var lastRunExec = 0;
        var lastRunTime = 0;
        var checkUpdatedUrl='';
        var pageTitle="${enc(js:g.message(code: 'gui.menu.Events'))} - ${enc(js:params.project ?: request.project)}";
        var firstLoad=true;
        var firstparams=Object.extend({refresh:${params.refresh == 'true' ? true : false}},eventsparams);
        window.onpopstate = function(event) {
            if(firstLoad){
                firstLoad=false;
            }else if(event.state){
                Object.extend(eventsparams,event.state);
                setAutoLoad(event.state.refresh);
                loadHistory();
            }else {
                eventsparams=firstparams;
                setAutoLoad(eventsparams.refresh);
                loadHistory();
            }
        };
        function _updateBoxInfo(name, data) {
            if(name==='events' && data.total){
                $$('._obs_histtotal').each(function(e){
                    setText($(e),data.total);
                });
            }
            if(name=='events' && data.checkUpdatedUrl ){
                checkUpdatedUrl=data.checkUpdatedUrl;
                if(!autoLoad){
                    _updateEventsCount(0);
                    _scheduleSinceCheck();
                }
            }
            if(name==='events' && data.lastDate){
                histControl.setHiliteSince(data.lastDate);
            }
            if(name=='events' && data.rssUrl && $('rsslink')){
                $('rsslink').href=data.rssUrl;
                $$('link[rel="alternate"]').each(function(elem){$(elem).href=data.rssUrl});
            }
            if(name=='events' && data.total){
                var e = $('evtsholder').down('.paginate');
                if(e){
                    var pagefunc=function(e,params){
                        loadHistory(jQuery.extend({},eventsparams,params));
                        history.pushState(params, pageTitle, e.href);
                    };
                    paginate(e,data.offset,data.total,data.max,{
                        baseUrl:links.baseUrl,
                        ulCss:'pagination pagination-sm pagination-embed',
                        'paginate.prev':"${g.message(code: 'default.paginate.prev',default:'-')}",
                        'paginate.next':"${g.message(code: 'default.paginate.next',default:'+')}",
                        'paginate.rew':"${g.message(code: 'default.paginate.rew',default:'«')}",
                        'paginate.ff':"${g.message(code: 'default.paginate.ff',default:'»')}",
                        maxsteps:20,
                        prevBehavior:pagefunc,
                        stepBehavior:pagefunc,
                        nextBehavior:pagefunc
                    });
                }
            }
            if (name == 'nowrunning' && data.lastExecId && data.lastExecId != lastRunExec) {
                lastRunExec = data.lastExecId;
                if(autoLoad){
                    loadHistory();
                }
            }
            if(name=='nowrunning' && data.count !=null && data.count<1 && nroffset>0){
                //now running page is advanced, but dataset has dropped to 0
                nowRunningOffset(0);
            }
        }
        var sincechecktimer=null;
        function _scheduleSinceCheck(){
            if(sincechecktimer){
                clearTimeout(sincechecktimer);
            }
            sincechecktimer=setTimeout(_checkSince,5000);
        }
        function _checkSince(){
            var url=checkUpdatedUrl;
            new Ajax.Request(url, {
                 evalJSON:true,
                 onSuccess: function(transport) {
                     _checkSinceSuccess(transport);
                 },
                 onFailure: function() {
                 }
             });
            
        }
        function _checkSinceSuccess(response){
            var data=JSON.parse(response.responseText); // evaluate the JSON;
            if(data && data.since){
                var count=data.since.count;
                //display badge
                _updateEventsCount(count);
            }else {
                showError(data.error && data.error.message? data.error.message : 'Invalid data response');
            }
            if(!autoLoad){
                _scheduleSinceCheck();
            }else{
                $('eventsCountBadge').hide();
            }
        }

        function _setFilterSuccess(data,name){
            if(data){
                var bfilters=data['filterpref'];
                eventsparams={filterName:bfilters[name]};
                pageparams={filterName:bfilters[name]};
                var selected = bfilters[name]?true:false;
                jQuery('.obs_filter_is_selected').each(function(x,el){
                    jQuery(this).collapse(selected?'show':'hide');
                });
                jQuery('.obs_filter_is_deselected').each(function(x,el){
                    jQuery(this).collapse(!selected?'show':'hide');
                });
                jQuery('.obs_selected_filter_name').each(function(x,el){
                    if(this.tagName=='INPUT'){
                        jQuery(this).val(bfilters[name]);
                    }else{
                        setText(this,bfilters[name]);
                    }
                });
                loadHistory();
                //reload page
//                document.location="${createLink(controller:'reports',action:'index')}"+(bfilters[name]?"?filterName="+bfilters[name]:'');
            }
        }

        function _updateEventsCount(count){
            if(count>0){
                setText($('eventsCountContent'),count+" new");
                $('eventsCountBadge').show();
            }else{
                $('eventsCountBadge').hide();
            }
        }
        function bulkEditEnable(){
            jQuery('.obs_bulk_edit_enable').show().addClass('bulk_edit_enabled');
            jQuery('.obs_bulk_edit_disable').hide();
        }
        function bulkEditDisable(){
            jQuery('.obs_bulk_edit_enable').hide().removeClass('bulk_edit_enabled');
            jQuery('.obs_bulk_edit_disable').show();
        }
        function bulkEditSelectAll(){
            jQuery('input.bulk_edit').each(function(ndx,el){
                el.checked=true;
            });
        }
        function bulkEditDeselectAll(){
            jQuery('input.bulk_edit').each(function(ndx,el){
                el.checked=false;
            });
        }
        function bulkEditToggleAll(){
            jQuery('input.bulk_edit').each(function(ndx,el){
                el.checked=!el.checked;
            });
        }
        function init(){
            _pageInit();
            jQuery('#histcontent').on('click','.autoclickable',function(evt){
                var ac=jQuery(this).parent('.autoclick')[0];
                //if bulk edit
                if(jQuery(ac).find('.bulk_edit_enabled').length>0){
                    var dc=jQuery(ac).find('input._defaultInput')[0]
                    dc.checked=!dc.checked;
                }else{
                    var dc=jQuery(ac).find('a._defaultAction')[0]
                    dc.click();
                }
            });
            jQuery('.act_bulk_edit_enable').click(bulkEditEnable);
            jQuery('.act_bulk_edit_disable').click(bulkEditDisable);
            jQuery('.act_bulk_edit_selectall').click(bulkEditSelectAll);
            jQuery('.act_bulk_edit_deselectall').click(bulkEditDeselectAll);
            jQuery('.act_bulk_edit_toggleall').click(bulkEditToggleAll);
        }
        
        jQuery(init);
    </g:javascript>
    <g:embedJSON id="eventsparamsJSON" data="${eventsparams}"/>
    <g:embedJSON id="pageparamsJSON" data="${pageparams}"/>
</head>
<body>
<div>


<div class="pageBody">
    <g:render template="/common/messages"/>


    <div id="evtsholder" class="eventspage">
    <g:render template="eventsFragment" model="${[paginateParams:paginateParams,params:params,includeBadge:true,includeAutoRefresh:true,reports:reports,filterName:filterName, filtersOpen: true, includeNowRunning:true]}"/>
    </div>

    </div>
</div>
</body>
</html>
