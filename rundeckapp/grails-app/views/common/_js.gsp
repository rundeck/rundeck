<%@ page import="rundeck.filters.ApiRequestFilters" %>
<script type="text/javascript">
    <g:set var="currentProject" value="${params.project?:request.project}"/>
    <g:set var="projParams" value="${currentProject?[project:currentProject]:[:]}"/>
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
        reportsEventsFragment:"${createLink(controller:'reports',action:'eventsFragment',params:projParams)}",
        frameworkViewResourceModelConfig: "${createLink(action: 'viewResourceModelConfig', controller: 'framework')}",
        frameworkCheckResourceModelConfig: "${createLink(action: 'checkResourceModelConfig', controller: 'framework')}",
        frameworkEditResourceModelConfig: "${createLink(action: 'editResourceModelConfig', controller: 'framework')}",
        frameworkCreateResourceModelConfig: "${createLink(action: 'createResourceModelConfig', controller: 'framework')}",
        frameworkNodes: "${createLink(controller:"framework",action:"nodes",params:projParams)}",
        frameworkNodesFragment: "${createLink(controller:"framework",action:"nodesFragment",params:projParams)}",
        frameworkAdhoc: "${createLink(controller:"framework",action:"adhoc",params:projParams)}",
        frameworkReloadNodes: "${createLink(controller:"framework",action:"reloadNodes",params:projParams)}",
        reportsEventsAjax: "${g.createLink(controller: 'reports', action: 'eventsAjax',params:projParams)}",
        menuNowrunningAjax: "${g.createLink(controller: 'menu', action: 'nowrunningAjax',params:projParams)}",
        scheduledExecutionRunAdhocInline: "${createLink(controller:'scheduledExecution',action:'runAdhocInline',params:projParams)}",
        scheduledExecutionCreate: "${createLink(controller:'scheduledExecution',action:'create',params:projParams)}",
        scheduledExecutionExecuteFragment: '${createLink(controller:"scheduledExecution",action:"executeFragment",params:projParams)}',
        scheduledExecutionRunJobInline: '${createLink(controller:"scheduledExecution",action:"runJobInline",params:projParams)}',
        scheduledExecutionDetailFragment: '${createLink(controller:'scheduledExecution',action:'detailFragment',params: projParams)}',
        executionFollowFragment: "${createLink(controller:'execution',action:'followFragment',params:projParams)}",
        menuJobs: "${createLink(controller:'menu',action:'jobs',params: projParams)}",
        userAddFilterPref: "${createLink(controller:'user',action:'addFilterPref',params:projParams)}",

        workflowEdit: '${createLink(controller:"workflow",action:"edit",params:projParams)}',
        workflowRender: '${createLink(controller:"workflow",action:"render",params:projParams)}',
        workflowSave: '${createLink(controller:"workflow",action:"save",params:projParams)}',
        workflowReorder: '${createLink(controller:"workflow",action:"reorder",params:projParams)}',
        workflowRemove: '${createLink(controller:"workflow",action:"remove",params:projParams)}',
        workflowUndo: '${createLink(controller:"workflow",action:"undo",params:projParams)}',
        workflowRedo: '${createLink(controller:"workflow",action:"redo",params:projParams)}',
        workflowRevert: '${createLink(controller:"workflow",action:"revert",params:projParams)}',
        workflowRenderUndo: '${createLink(controller:"workflow",action:"renderUndo",params:projParams)}',

        editOptsRenderUndo: '${createLink(controller:"editOpts",action:"renderUndo",params:projParams)}',
        editOptsEdit: '${createLink(controller:"editOpts",action:"edit",params:projParams)}',
        editOptsRender: '${createLink(controller:"editOpts",action:"render",params:projParams)}',
        editOptsSave: '${createLink(controller:"editOpts",action:"save",params:projParams)}',
        editOptsRenderAll: '${createLink(controller:"editOpts",action:"renderAll",params:projParams)}',
        editOptsRenderSummary: '${createLink(controller:"editOpts",action:"renderSummary",params:projParams)}',
        editOptsRemove: '${createLink(controller:"editOpts",action:"remove",params:projParams)}',
        editOptsUndo: '${createLink(controller:"editOpts",action:"undo",params:projParams)}',
        editOptsRedo: '${createLink(controller:"editOpts",action:"redo",params:projParams)}',
        editOptsRevert: '${createLink(controller:"editOpts",action:"revert",params:projParams)}',
        menuJobsPicker: '${createLink(controller:"menu",action:"jobsPicker",params:projParams)}',
        scheduledExecutionGroupTreeFragment: '${createLink(controller:"scheduledExecution",action:"groupTreeFragment",params:projParams)}',
        storageKeysApi: '${createLink(uri:"/api/${ApiRequestFilters.API_CURRENT_VERSION}/storage/")}'
    } ;
    //compatibility with WB javascript:
    var AppImages = {
        disclosure: appLinks.disclosureIcon,
        disclosureOpen: appLinks.disclosureIconOpen,
        disclosureWait: "${resource(dir:'images',file:'icon-tiny-disclosure-waiting.gif')}",
        iconTinyRemoveX: '${resource(dir:"images",file:"icon-tiny-removex.png")}',
        iconTinyRemoveXGray: '${resource(dir:"images",file:"icon-tiny-removex-gray.png")}',
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
            $$(_tooltipElemSelector).each(function(e){
                $(e).removeClassName('glow');
                $(e).removeClassName('active');
                $(e).removeAttribute('data-rdtooltip');
            });
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
                    $(elem).setAttribute('data-rdtooltip', 'true');
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
        if(null==_tooltipElemSelector){
            _tooltipElemSelector = selector;
            Event.observe(document.body, 'click', function (evt) {
                //click outside of popup bubble hides it
                if (!evt.element().hasAttribute('data-rdtooltip')) {
                    tooltipMouseOut();
                }
            }, false);
        }else{
            _tooltipElemSelector = _tooltipElemSelector+', '+selector;
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
    var initClicktipForElements=function(selector){
        $$(selector).each(function(elem){
            var ident=elem.identify();
            if($(ident+'_tooltip')){
                var out=function(evt){
                    if(!_tooltiptimer){
                        _tooltiptimer=setTimeout(tooltipMouseOut,50);
                        _tooltipelem = null;
                    }
                };
                var over = function (evt) {
                    var oldelem= _tooltipelem;
                    if(_tooltipelem && _tooltipelem != elem){
                        clearTimeout(_tooltiptimer);
                        tooltipMouseOut();
                    }
                    if(_tooltipelem == elem || oldelem==elem){
                        out(evt);
                        return;
                    }
                    if (_tooltiptimer) {
                        clearTimeout(_tooltiptimer);
                        tooltipMouseOut();
                    }

                    $(elem).addClassName('active');
                    new MenuController().showRelativeTo(elem, ident + '_tooltip');
                    _tooltipelem = elem;
                    $(elem).setAttribute('data-rdtooltip', 'true');
                };
                Event.observe(elem,'click',over,true);
            }
        });
        if(null==_tooltipElemSelector){
            _tooltipElemSelector = selector;
            Event.observe(document.body, 'click', function (evt) {
                //click outside of popup bubble hides it
                if(!evt.element().hasAttribute('data-rdtooltip')){
                    tooltipMouseOut();
                }
            }, false);
        }else{
            _tooltipElemSelector = _tooltipElemSelector+', '+selector;
        }

    };
    Element.addMethods( {
      loading: _setLoading
    });
      /** node filter preview code */

    function _updateMatchedNodes(data,elem,project,localnodeonly,inparams,callback){
        var i;
        if(!project){
            return;
        }
        var params =Object.extend({view:'embed',declarenone:true,fullresults:true},data);
        if(null!==inparams){
            Object.extend(params,inparams);
        }
        if(localnodeonly){
            params.localNodeOnly='true';
        }

        if(typeof(data.nodeExcludePrecedence) == 'string' && data.nodeExcludePrecedence==="false"
            || typeof(data.nodeExcludePrecedence)=='boolean' && !data.nodeExcludePrecedence){
            params.nodeExcludePrecedence="false";
        }else{
            params.nodeExcludePrecedence="true";
        }
//        $(elem).loading();
        new Ajax.Updater(elem,appLinks.frameworkNodesFragment,{parameters:params,evalScripts:true,
         onComplete: function(transport) {
             $(elem).removeClassName('depress');
             if (transport.request.success()) {
                 if(typeof(callback)=='function'){
                     callback(transport);
                 }
             }
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
