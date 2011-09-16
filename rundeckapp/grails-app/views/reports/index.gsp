<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="events"/>
    <g:ifServletContextAttribute attribute="RSS_ENABLED" value="true">
    <link rel="alternate" type="application/rss+xml" title="RSS 2.0" href="${createLink(controller:"feed",action:"index",params:paginateParams?paginateParams:[:])}"/>
    </g:ifServletContextAttribute>
    <title><g:message code="gui.menu.Events"/></title>
    <g:javascript>
                

        var pagefirstload=true;
        function _pageUpdateNowRunning(count, perc) {
            document.title = "Now Running (" + count + ")";
            if($('nrlocal')){
                $('nrlocal').innerHTML = '' + count;
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
                $("loaderror").innerHTML += message;
                $("loaderror").show();
            }
        }
        var bfilters=${filterPref.encodeAsJSON()};
        <g:set var="pageparams" value="${[offset:params.offset,max:params.max]}"/>
        <g:set var="eventsparams" value="${paginateParams}"/>
        var eventsparams=${eventsparams.encodeAsJSON()};
        var pageparams=${pageparams.encodeAsJSON()};
        var links = {
            events:'${createLink(controller:"reports",action:"eventsFragment")}'
        };
        var boxctl ;
        function _pageInit() {
            try{
            boxctl = new WBoxController({views:{db2:'evtsholder'},key:'nowrunning'});
            //eventsparams.filterName=bfilters['events'];
            if(pageparams && pageparams.offset){
                Object.extend(eventsparams,pageparams);
            }
            boxctl.addBox('db2', new WBox('box2', {noTitle:true,noTabs:true,tabs:[
                {   name:'events',
                    url:links['events'],
                    notitle:true,
                    params:eventsparams
                },
            ]}));
            boxctl._pageInit();
            }catch(e){
                console.log("error: "+e);
            }
        }

        var checkUpdatedUrl='';
        function _updateBoxInfo(name, data) {
            if(boxctl){
                try{
                    if(data.url && data.url.indexOf("?")>0){
                        //remove params
                        data.params={};
                    }
                boxctl.updateDataForTab(name, data);
                }catch(e){

                }
            }
            if(name=='events' && data.checkUpdatedUrl){
                checkUpdatedUrl=data.checkUpdatedUrl;
                _updateEventsCount(0);
                _scheduleSinceCheck();
            }
            if(name=='events' && data.rssUrl && $('rsslink')){
                $('rsslink').href=data.rssUrl;
                $$('link[rel="alternate"]').each(function(elem){$(elem).href=data.rssUrl});
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
            var data=eval("("+response.responseText+")"); // evaluate the JSON;
            if(data && data.since){
                var count=data.since.count;
                //display badge
                _updateEventsCount(count);
            }else {
                showError(data.error && data.error.message? data.error.message : 'Invalid data response');
            }
            _scheduleSinceCheck();
        }

        function _setFilterSuccess(response,name){
            var data=eval("("+response.responseText+")"); // evaluate the JSON;
            if(data){
                var bfilters=data['filterpref'];
                boxctl.updateDataForTab(name,{params:{filterName:bfilters[name]}});
                //reload page
                //document.location="${createLink(controller:'reports',action:'index')}"+(bfilters[name]?"?filterName="+bfilters[name]:'');
                try{
                    boxctl.reloadTabForName(name);
                }catch(e){
                    console.log("error: "+e);
                }

            }
        }

        function _updateEventsCount(count){
            if(count>0){
                $('eventsCountContent').innerHTML=count+" new";
                $('eventsCountBadge').show();
            }else{
                $('eventsCountBadge').hide();
            }
        }
        
        Event.observe(window, 'load', _pageInit);
    </g:javascript>
    <style type="text/css">
    table.dashboxes td.dashbox {
        width: auto;
    }

    table.dashboxes td.dashbox.small {
        width: auto;
    }

    td.dashbox div.wbox {
        max-height: none;
        width: auto;
        height: auto;
    }

    td.dashbox.small div.wbox {
        width: auto;
    }
    span.action.large{
        font-size:12pt;
        clear:both;
    }
    span.action.large.closed{
        %{--background: #ddd url(<g:resource dir='images' file='bggrad-rev.png'/>) repeat-x 0px 0px;--}%
        background: #ddd ;
        -moz-border-radius: 3px;
        -webkit-border-radius: 3px;
        border:1px solid #aaa;
        margin:10px 5px 10px 0;

    }
    span.action.large.closed:hover{
        background: #eee;
    }
    div.expanderwrap {
        line-height:12pt;
        margin-top:10px;
    }
    </style>
</head>
<body>
<div>


<div class="pageBody solo">
    <g:ifServletContextAttribute attribute="RSS_ENABLED" value="true">
    <a title="RSS 2.0" class="floatr" href="${createLink(controller:"feed",action:"index",params:paginateParams)}" id="rsslink"><img src="${resource(dir:'images',file:'feed.png')}" width="14px" height="14px" alt=""/> RSS</a>
    </g:ifServletContextAttribute>
    <g:render template="/common/messages"/>

    <span class="badgeholder"  id="eventsCountBadge" style="display:none"><span class="badge newcontent active" id="eventsCountContent" onclick="boxctl.reloadTabForName('events');" title="click to load new events"></span></span>
    <div id="evtsholder">
    <g:render template="eventsFragment" model="${[paginateParams:paginateParams,params:params,reports:reports,filterName:filterName]}"/>
    </div>

    </div>
</div>
</body>
</html>