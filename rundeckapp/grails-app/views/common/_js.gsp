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
        tailExecutionOutput: '${createLink(controller: "execution", action: "tailExecutionOutput")}',
        reportsEventsFragment:"${createLink(controller:'reports',action:'eventsFragment')}"
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
    var updateNowRunning = function(count) {
        var nrtitle = "Now Running (" + count + ")";
        if ($('nowrunninglink')) {
            $('nowrunninglink').innerHTML = nrtitle;
        }
        $$('.nowrunningcount').each(function(e){e.innerHTML = "("+count+")";});
        if(typeof(_pageUpdateNowRunning)=="function"){
            _pageUpdateNowRunning(count);
        }
    };
    var _setLoading=function(element,text){
        element=$(element);
        if(null==text){
            text="Loading&hellip;";
        }
        element.innerHTML='<span class="loading"><img src="'+appLinks.iconSpinner+'" alt="Loading"/> '+text+"</span>";
        return element;
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
                elem.onmouseover=function(){
                    new MenuController().showRelativeTo(elem,ident+'_tooltip');
                };
                elem.onmouseout=function(){
                    $(ident+'_tooltip').hide();
                };
            }
        });
    };
    Element.addMethods( {
      loading: _setLoading
    });
      /** node filter preview code */

    var node_filter_keys=${['','Name','Type','Tags','OsName','OsFamily','OsArch','OsVersion'].encodeAsJSON()};
    function _updateMatchedNodes(data,elem,project,localnodeonly,inparams){
        if(!project){
            return;
        }
        var params ={project:project,view:'embed',declarenone:true,fullresults:true};
        if(null!=inparams){
            Object.extend(params,inparams);
        }
        if(localnodeonly){
            params['localNodeOnly']='true';
        }
        for(var i in node_filter_keys){
            var key=node_filter_keys[i];
            if(data['nodeInclude'+key]){
                params['nodeInclude'+key]=data['nodeInclude'+key];
            }
            if(data['nodeExclude'+key]){
                params['nodeExclude'+key]=data['nodeExclude'+key];
            }
        }
        if(data['nodeExcludePrecedence']=="true"){
            params['nodeExcludePrecedence']="true";
        }else{
            params['nodeExcludePrecedence']="false";
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
        if(null==callback){
            callback=_setFilterSuccess;
        }
        var str=name+"="+value;
        new Ajax.Request("${createLink(controller:'user',action:'addFilterPref')}",{parameters:{filterpref:str}, evalJSON:true,onSuccess:function(response){
            if(typeof(callback)=='function'){
                callback(response,name);
            }
        }});
    }
</script>