<script type="text/javascript">
    var appLinks = {
        disclosureIcon: '${resource(dir:"images",file:"icon-tiny-disclosure.png")}',
        disclosureIconOpen: '${resource(dir:"images",file:"icon-tiny-disclosure-open.png")}',
        iconTinyWarn: '${resource(dir:"images",file:"icon-tiny-warn.png")}',
        iconTinyOk: '${resource(dir:"images",file:"icon-tiny-ok.png")}',
        iconSmallRemoveX: '${resource(dir:"images",file:"icon-small-removex.png")}',
        iconTinyRemoveX: '${resource(dir:"images",file:"icon-tiny-removex.png")}',
        iconSpinner: '${resource(dir:"images",file:"icon-tiny-disclosure-waiting.gif")}',
        executionCancelExecution: '${createLink(controller:"execution",action:"cancelExecution")}.json',
        tailExecutionOutput: '${createLink(controller: "execution", action: "tailExecutionOutput")}.json',
        reportsEventsFragment:"${createLink(controller:'reports',action:'eventsFragment')}",
        frameworkViewResourceModelConfig: "${createLink(action: 'viewResourceModelConfig', controller: 'framework')}",
        frameworkCheckResourceModelConfig: "${createLink(action: 'checkResourceModelConfig', controller: 'framework')}",
        frameworkEditResourceModelConfig: "${createLink(action: 'editResourceModelConfig', controller: 'framework')}",
        frameworkCreateResourceModelConfig: "${createLink(action: 'createResourceModelConfig', controller: 'framework')}"
    } ;
    //compatibility with WB javascript:
    var AppImages = {
        disclosure: appLinks.disclosureIcon,
        disclosureOpen: appLinks.disclosureIconOpen,
        disclosureWait: "${resource(dir:'images',file:'icon-tiny-disclosure-waiting.gif')}",
        iconTinyRemoveX: '${resource(dir:"images",file:"icon-tiny-removex.png")}',
        iconTinyRemoveXGray: '${resource(dir:"images",file:"icon-tiny-removex-gray.png")}',
        iconSmallNodeObject:'${resource(dir:"images",file:"icon-small-NodeObject.png")}',
        iconSmallPrefix:'${resource(dir:"images",file:"icon-small-")}'
    };
    var _g_nodeFilterData={};
    var updateNowRunning = function(count) {
        var nrtitle = "Now Running (" + count + ")";
        if ($('nowrunninglink')) {
            $('nowrunninglink').innerHTML = nrtitle;
        }
        $$('.nowrunningcount').each(function(e){e.innerHTML = "("+count+")";});
        if(typeof(_pageUpdateNowRunning)==="function"){
            _pageUpdateNowRunning(count);
        }
    };
    var _setLoading=function(element,text){
        element=$(element);
        if(null===text || typeof(text)=='undefined'){
            text="Loading&hellip;";
        }
        if(element.tagName==='TBODY'){
            var tr = new Element('tr');
            var td = new Element('td');
            tr.appendChild(td);
            element.appendChild(tr);
            td.innerHTML='<span class="loading"><img src="'+appLinks.iconSpinner+'" alt="Loading"/> '+text+'</span>';
        }else{
            element.innerHTML='<span class="loading"><img src="'+appLinks.iconSpinner+'" alt="Loading"/> '+text+'</span>';
        }
        return element;
    };

    var _tooltipElemSelector=null;
    var _tooltiptimer=null;
    var _tooltipelem=null;

    var tooltipMouseOut=function(){
        _tooltiptimer=null;
        _tooltipelem=null;
        if(_tooltipElemSelector){
            $$('.tooltipcontent').each(Element.hide);
            $$(_tooltipElemSelector).each(function(e){$(e).removeClassName('glow');});
        }
    };
    /**
     * initialize a tooltip detail view for the matching elements.
     * The tooltipe element is identified by the 'id' of the matching element
     * with "_tooltip" appended.
     *
     * E.g. if the matched element has id "key" then the element with
     * id "key_tooltip" will be shown when the element is hovered over.
     * @param selector a selector expression to identify a set of elements
     */
    var initTooltipForElements=function(selector){
        _tooltipElemSelector=selector;
        $$(selector).each(function(elem){
            var ident=elem.identify();
            if($(ident+'_tooltip')){
                var over = function(evt){
                    if(_tooltiptimer && _tooltipelem==elem){
                        return;
                    }
                    if(_tooltiptimer){
                        clearTimeout(_tooltiptimer);
                        tooltipMouseOut();
                    }

                    $(elem).addClassName('glow');
                    new MenuController().showRelativeTo(elem,ident+'_tooltip');
                };
                var out=function(evt){
                    if(!_tooltiptimer){
                        _tooltiptimer=setTimeout(tooltipMouseOut,50);
                        _tooltipelem = elem;
                    }
                };
                Event.observe(elem,'mouseenter',over);
                Event.observe(elem,'mouseleave', out);
            }
        });
        Event.observe(document.body, 'click', function(evt) {
            //click outside of popup bubble hides it
            tooltipMouseOut();
        }, false);
    };
    Element.addMethods( {
      loading: _setLoading
    });
      /** node filter preview code */

    var node_filter_keys=${['','Name','Type','Tags','OsName','OsFamily','OsArch','OsVersion'].encodeAsJSON()};
    function _updateMatchedNodes(data,elem,project,localnodeonly,inparams){
        var i;
        if(!project){
            return;
        }
        var params ={project:project,view:'embed',declarenone:true,fullresults:true};
        if(null!==inparams){
            Object.extend(params,inparams);
        }
        if(localnodeonly){
            params.localNodeOnly='true';
        }
        for(i in node_filter_keys){
            var key=node_filter_keys[i];
            if(data['nodeInclude'+key]){
                params['nodeInclude'+key]=data['nodeInclude'+key];
            }
            if(data['nodeExclude'+key]){
                params['nodeExclude'+key]=data['nodeExclude'+key];
            }
        }
        if(typeof(data.nodeExcludePrecedence) == 'string' && data.nodeExcludePrecedence==="true"
            || typeof(data.nodeExcludePrecedence)=='boolean' && data.nodeExcludePrecedence){
            params.nodeExcludePrecedence="true";
        }else{
            params.nodeExcludePrecedence="false";
        }
        $(elem).loading();
        new Ajax.Updater(elem,"${createLink(controller:'framework',action:'nodesFragment')}",{parameters:params,evalScripts:true,
         onSuccess: function(transport) {
            $(elem).removeClassName('depress');
         }});
    }

    //set box filterselections
    function setFilter(name,value,callback){
        if(!value){
            value="!";
        }
        if(null===callback){
            callback=_setFilterSuccess;
        }
        var str=name+"="+value;
        new Ajax.Request("${createLink(controller:'user',action:'addFilterPref')}",{parameters:{filterpref:str}, evalJSON:true,onSuccess:function(response){
            if(typeof(callback)==='function'){
                callback(response,name);
            }else{
                try{
                    _setFilterSuccess(response,name);
                }catch(e){
                }
            }
        }});
    }
</script>