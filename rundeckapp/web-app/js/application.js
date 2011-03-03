var Ajax;
if (Ajax && (Ajax != null)) {
	Ajax.Responders.register({
	  onCreate: function() {
        if($('spinner') && Ajax.activeRequestCount>0)
          Effect.Appear('spinner',{duration:0.5,queue:'end'});
	  },
	  onComplete: function() {
        if($('spinner') && Ajax.activeRequestCount==0)
          Effect.Fade('spinner',{duration:0.5,queue:'end'});
	  }
	});
}



function selectTopTab(name){
    var tabController = dojo.widget.byId('mainTabContainer');
    var tab = dojo.widget.byId(name+'Tab');
    tabController.selectChild(tab);
}

function toggleDisclosure(id,iconid,closeUrl,openUrl){
    $(id).toggle();
    if($(id).visible()){
        $(iconid).setAttribute("src",openUrl);
    }else{

        $(iconid).setAttribute("src",closeUrl);
    }
}


function myToggleClassName(elem, name) {
    if ($(elem).hasClassName(name)) {
        $(elem).removeClassName(name);
    } else {
        $(elem).addClassName(name);
    }
}


/** job cancellation code*/

function updatecancel(data,elem,id){

    var orig=data;
    if(typeof(data) == "string"){
        eval("data="+data);
    }
    if(data['cancelled']){
        if($(elem)){
            $(elem).innerHTML='<span class="fail">Killed</span>';
        }
        if($('exec-'+id+'-spinner')){
            $('exec-'+id+'-spinner').innerHTML='<img src="'+appLinks.iconTinyWarn+'" alt=""/>';
        }
        if($('exec-'+id+'-dateCompleted')){
            $('exec-'+id+'-dateCompleted').innerHTML='';
            if($('exec-'+id+'-dateCompleted').onclick){
                $(elem).onclick=$('exec-'+id+'-dateCompleted').onclick;
            }
        }

    }else if(data['status']!='success'){
        if($(elem)){
            $(elem).innerHTML='<span class="fail">'+(data['error']?data['error']:'Failed to Kill job.')+'</span>';
        }
    }else if(data['status']=='success'){
        if($(elem)){
            $(elem).innerHTML='Job Completed.';
        }
        if($('exec-'+id+'-spinner')){
            $('exec-'+id+'-spinner').innerHTML='<img src="'+appLinks.iconTinyOk+'" alt=""/>';
        }
        if($('exec-'+id+'-dateCompleted')){
            $('exec-'+id+'-dateCompleted').innerHTML='';
            if($('exec-'+id+'-dateCompleted').onclick){
                $(elem).onclick=$('exec-'+id+'-dateCompleted').onclick;
            }
        }
    }
}

function canceljob(id,elem){
    if($(elem)){
        $(elem).innerHTML='<img src="'+appLinks.iconSpinner+'" alt="Spinner"/> Killing Job ...';
    }
    new Ajax.Request(appLinks.executionCancelExecution, {
        parameters: "id="+id,
        onSuccess: function(transport) {
            try{
                updatecancel(transport.responseText,elem,id);
            }catch(e){
                alert(e);
            }
        },
        onFailure: function(reponse) {
            updatecancel({error:"Request failed: "+reponse.statusText},elem,id);
            cancelJobError("Request failed: "+reponse.statusText,elem,id);
        }
    });
}

function cancelJobError(mesg,elem,id){
}

var Expander = {
    toggle: function(elem, contain,expression) {
        var e = $(elem);
        if (!e) {
            return;
        }
        var content;
        if ($(contain)) {
            content = $(contain);
        }
        var holder;
        var icnh;
        if (!content) {
            holder = e.up(".expandComponentHolder");
            if (holder) {
                content = holder.down(".expandComponent");
                icnh = holder.down(".expandComponentControl");
            }
        } else {
            if (e.hasClassName('expandComponentControl')) {
                icnh = e;
            }
            if(e.hasClassName('expandComponentHolder')){
                holder=e;
                if(!icnh){
                    icnh = holder.down(".expandComponentControl");
                }
            }else{
                holder=e.up(".expandComponentHolder");
            }
        }
        var value=false;
        if (content) {
            value=!Element.visible(content);
        }else if(icnh){
            var icn = icnh.down("img");
            if(icn){
                value=icn.src == AppImages.disclosure;
            }
        }
        Expander.setOpen(elem,contain,value,expression);
        return value;
    },
    setOpen: function(elem, contain,value,expression) {
        var e = $(elem);
        if (!e) {
            return;
        }
        var content;
        if ($(contain)) {
            content = $(contain);
        }
        var holder;
        var icnh;
        if (!content) {
            holder = e.up(".expandComponentHolder");
            if (holder) {
                content = holder.down(".expandComponent");
                icnh = holder.down(".expandComponentControl");
            }
        }
        if(!holder || !icnh){
            if (e.hasClassName('expandComponentControl')) {
                icnh = e;
            }
            if(e.hasClassName('expandComponentHolder')){
                holder=e;
                if(!icnh){
                    icnh = holder.down(".expandComponentControl");
                }
            }else{
                holder=e.up(".expandComponentHolder");
            }
        }
        if (content) {
            if(value){
                Element.show(content);
            }else{
                Element.hide(content);
            }
            if (null != expression) {
                //also set open related expression match
                $$(expression).each(function(e) {
                    if(value){
                        Element.show(e);
                    }else{
                        Element.hide(e);
                    }
                });
            }
        }
        if(holder){
            if(value){
                Element.addClassName(holder, "expanded");
            }else{
                Element.removeClassName(holder, "expanded");
            }
        }else if(icnh && content){
            if(value){
                Element.addClassName(icnh, "expanded");
                Element.removeClassName(icnh, "closed");
            }else{
                Element.removeClassName(icnh, "expanded");
                Element.addClassName(icnh, "closed");
            }
        }
        if (icnh) {
            var icn = icnh.down("img");
            if(icn){
                icn.src = value ? AppImages.disclosureOpen : AppImages.disclosure;
            }
        }
    },
    open: function(elem,contain){
        Expander.setOpen(elem,contain,true);
    },
    close: function(elem,contain){
        Expander.setOpen(elem,contain,false);
    }
};


if(Prototype.Version=="1.6.0.2"  && Prototype.Browser.IE){
    //change getStyle function to prevent JS exception in IE
    Object.extend(Element,{
        getStyle_Orig:Element.Methods.getStyle
    });
    Object.extend(Element,
    {
        getStyle : function(element, style) {

            element = $(element);
            if((element==document || element==document.body)&& style=='position'){
                return null;
            }else{
                return Element.getStyle_Orig(element,style);
            }
       }
   });
}

/**
 * WidgetBox controller
 * Create a new WBoxController({views:{key:value}}). Each view key is an identifier, and each value is the ID of
 * a DOM element to use as content location.
 *
 * Add boxes with addBox(viewid,WBox) to add a box to the view. Each box should define a set of one or more tabs. (see WBox).
 *
 * once boxes are added, the WBoxController can render and load the tabs with: _pageInit() method.
 *
 */

var WBoxController = Class.create({
    options: new Hash(),
    key:"",
    /** set of elements in which to display box content, keyed by arbitrary identifier, value is element or element id*/
    views:new Hash(),
    _pageInit: function() {
        var myself = this;
        this.views.each(function(view) {
            var box = myself.boxSet.get(view.key);
            if(box){
                myself._initView(view.key,box);
            }
        });
        this._loadBoxes();
    },
    boxes: new Array(),
    boxSet:new Hash(),
    boxTabNames:new Hash(),
    boxTabLoaded: new Hash(),
    boxInfo:new Hash(),
    maxedBox:"",
    maxedBoxTab:0,
    initialize: function(options) {
        this.options = Object.clone(options);
        this.boxTabNames = $H();
        this.boxSet = $H();
        if (null != this.options.views) {
            this.views = $H(this.options.views);
        }
        if(options.maxview && options.container){
            this._initMaxview();
        }
        if (this.options.key) {
            this.key = this.options.key;
        }
    },
    /**
     * Add a box for a particular view
     * @param viewid view identifier
     * @param box box definition
     */
    addBox: function(viewid, box) {
        this.boxes.push(box);
        this.boxSet.set(viewid, box);
        this._initBox(viewid, box);
    },
    _initBox: function(viewid, box) {
        box._setController(this);
        box._setView(viewid);

    },
    _initMaxview:function(){
        //initialize the maxview container
        //create max button
        var boxctl = this.options.maxview + '_ctl';
        var boxmax = this.options.maxview + '_max';

        $(boxctl).parentNode.addClassName("full");
        var controller=this;



        var boxc = this.options.maxview + '_box';
        $(boxc).addClassName("wbox");
        var btabs=this.options.maxview + '_tabs';
        $(btabs).innerHTML="Minimize ";
        $(btabs).removeClassName("btabs");
        $(btabs).addClassName("boxclose");
        $(btabs).addClassName("action");
        $(btabs).addClassName("textbtn");
        $(btabs).onclick=function(e){
              controller.minimizeTab();
        };
        var im = document.createElement("img");
        im.src=AppImages.iconTinyRemoveXGray;
        $(btabs).appendChild(im);


    },
    _initView: function(viewid,box) {
        var elem = $(this.views.get(viewid));
        elem.innerHTML = '';

        if(!box.options.noTabs){
            console.log("initview with tabs");
            //generate boxctl
            var boxctl1 = document.createElement('div');
            $(boxctl1).addClassName('boxct');
            //generate boxctl
            var boxctl = document.createElement('div');
            $(boxctl).addClassName('boxctl');
            $(boxctl).setAttribute('id', viewid + "_ctl");

            //create title span
            if(!box.options.noTitle){
                var titlegroup = document.createElement('span');
                $(titlegroup).addClassName('titlegroup');
                $(titlegroup).setAttribute('id', viewid + '_max');
                $(titlegroup).setAttribute('title', 'Click to maximize');
                var boxtitle = document.createElement('span');
                $(boxtitle).addClassName('boxtitle');
                $(boxtitle).setAttribute('id', viewid + '_title');
                $(titlegroup).appendChild(boxtitle);
                $(boxctl).appendChild(titlegroup);
            }



            //generate tab box
            var tb = document.createElement('span');
            $(tb).addClassName('btabs');
            $(tb).setAttribute('id', viewid + '_tabs');
            boxctl.appendChild(tb);

            boxctl1.appendChild(boxctl);
            elem.appendChild(boxctl1);
        }
        //now create box content div
        var boxc = document.createElement('div');
        $(boxc).setAttribute('id', viewid + '_box');

        elem.appendChild(boxc);
        $(boxc).hide();
    },
    /**
     * Load content boxes 
     */
    _loadBoxes: function() {
        var myself = this;
        this.boxes.each(function(box) {
            //generate tabs if necessary
            myself._genTabs(box);
            myself._loadBox(box);
        });
    },
    _loadBox: function (box) {
        var viewid = box.viewid;
        var tabnum = box.getSelectedTab();
        this.selectTab(box, tabnum);
        if (!this.boxTabLoaded.get(viewid + "_tab_" + tabnum)) {
            var params = box.getTab(tabnum).params;
            var boxc = box.viewid + "_box";
            $(boxc).show();
            var tabcontent=boxc;
            if(!box.options.noTabs){
                tabcontent = box.viewid + "_tab_" + tabnum;
            }
            $(tabcontent).loading();
            this.boxTabLoaded.set(viewid + "_tab_" + tabnum, true);
            if (box.getTab(tabnum).reload) {
                new Ajax.PeriodicalUpdater(tabcontent, box.getTab(tabnum).url, {parameters:params, frequency:box.getTab(tabnum).reload,evalScripts:true});
            } else {
                new Ajax.Updater(tabcontent, box.getTab(tabnum).url, {parameters:params, evalScripts:true});
            }
        }

    },
    _loadBoxMaximized: function (box,tabnum) {
        var viewid = box.viewid;

        var data=box.getTab(tabnum);
        var maxdata=box.getTab(tabnum).maximize;
        if(!maxdata){
            maxdata=data;
        }
        var url =maxdata.url?maxdata.url:data.url;
        var params = maxdata.params?Object.clone(maxdata.params):Object.clone(data.params);
        if(maxdata.addParams){
            Object.extend(params,maxdata.addParams);
        }
        if(maxdata.removeParams){
            $A(maxdata.removeParams).each(function(v){
                delete params[v]; 
            });
        }
        var title=maxdata.title?maxdata.title:data.title;
        

        if(this.options.maxview && this.options.container){
            var cb=$(this.options.container);
            var mv=$(this.options.maxview);

            var mvc=$(this.options.maxview + "_box");
            cb.hide();
            mv.show();
            $(mvc).show();
            $(mvc).loading();
            this._updateMaximizedtitle(title);

            if (box.getTab(tabnum).reload) {
                box.getTab(tabnum).updater=new Ajax.PeriodicalUpdater(mvc, url, {parameters:params, frequency:box.getTab(tabnum).reload,evalScripts:true});
            } else {
                new Ajax.Updater(mvc, url, {parameters:params, evalScripts:true});
            }
        }
    },
    _updateMaximizedtitle: function(value){
        var mvt=$(this.options.maxview + "_title");
        if(mvt){
            $(mvt).innerHTML=value;
        }
    },
    _updateTitle: function(box, value) {
        if((box.viewid + '_title')){
            $(box.viewid + '_title').innerHTML = value;
        }
    },
    /**
     * Update tab data for a particular named tab
     * @param name name of the tab
     * @param data data to update
     */
    updateDataForTab: function(name, data) {
        //identify box and tab # to update
        var box = this.boxTabNames.get(name);
        if (box) {
            var num = box.getTabNumByName(name);
            if (num > 0) {
                var tab = box.getTab(num);
                Object.extend(tab, data);
                
                if (box.getSelectedTab() == num) {
                    this._displayBoxData(box, num);
                }
                return tab;
            }
        }
    },
    _displayBoxData: function(box, tabnum) {
        var data = box.getTab(tabnum);
        if (data && data.title && !box.options.noTitle) {
            var title = data.title + (data.total ? " (" + data.total + ")" : '');
            this._updateTitle(box, title);

            if($(box.viewid+'_max')){
                $(box.viewid+'_max').addClassName('action');
                $(box.viewid+'_max').onclick=function(e){
                    box.maximizeTab(tabnum);
                };
            }
        }
    },

    /**
     *
     * @param box
     * @param tab
     */
    maximizeTab:function(box,tabnum){
        if(this.options.maxview && this.options.container){
            this.maxedBox=box;
            this.maxedBoxTab=tabnum;
            this._loadBoxMaximized(box,tabnum);
        }
    },
    /**
     * Move child nodes from one element to another
     * @param elem1
     * @param elem2
     */
    _moveChildren:function(elem1, elem2) {
        var n1 = $(elem1);
        var n2 = $(elem2);
        var length = n1.childNodes.length;
        for (var i = 0 ; i < length ; i++) {
            var cn = n1.removeChild(n1.childNodes[0]);
            n2.appendChild(cn);
        }
    },
    /**
     *
     * @param box
     * @param tab
     */
    minimizeTab:function(){
        if(this.options.maxview && this.options.container){
            var cb=$(this.options.container);
            var mv=$(this.options.maxview);
            var box=this.maxedBox;
            this.maxedBox=null;
            var tabnum=this.maxedBoxTab;
            this.maxedBoxTab=0;
            this.reloadTabForBox(box, tabnum);
            var mvc=$(this.options.maxview + "_box");
            var tabcontent = $(box.viewid + "_tab_" + tabnum);
            mv.hide();
            cb.show();
            $(mvc).innerHTML='';
//            this._moveChildren(this.options.maxview + "_box",box.viewid + "_tab_" + tabnum);
        }
    },
    /**
     * Select the specified tab in the specified box
     * @param box the box object
     * @param tab the tab number
     */
    selectTab: function(box, tab) {
        var viewid = box.viewid;
        if(!box.options.noTabs){
            for (var i = 1 ; i <= box.tabs.length ; i++) {
                if ($(viewid + '_tb_' + i)) {
                    $(viewid + '_tb_' + i).removeClassName('selected');
                }
                if ($(viewid + '_tab_' + i)) {
                    $(viewid + '_tab_' + i).hide();
                }
            }
            if ($(viewid + '_tb_' + tab)) {
                $(viewid + '_tb_' + tab).addClassName('selected');
            }
            $(viewid + '_tab_' + tab).show();
        }
        this._displayBoxData(box, tab);
    },
    /**
     * Select the specified tab and load the content if necessary
     * @param box the box object
     * @param tab the tab number
     */
    selectTabForBox: function(box, tab) {
        this.selectTab(box, tab);
        var viewid = box.viewid;
        if (!this.boxTabLoaded.get(viewid + "_tab_" + tab)) {
            this._loadBox(box);
        }
    },
    /**
     * Reload content for a tab by name
     * @param name name of the tab
     */
    reloadTabForName: function(name) {

        //identify box and tab # to update
        var box = this.boxTabNames.get(name);
        if (box) {
            var num = box.getTabNumByName(name);
            if (num > 0) {
                this.reloadTabForBox(box, num);
            }
        }
    },
    /**
     * Reload content of a tab. if maximized, reloads the maximized view
     * @param box box object
     * @param tab tab number
     */
    reloadTabForBox: function(box, tab) {
        var viewid = box.viewid;
        this.boxTabLoaded.set(viewid + "_tab_" + tab, false);
        if(this.maxedBox==box && this.maxedBoxTab==tab){
            this.maximizeTab(box, tab);
        }else{
            this.selectTabForBox(box, tab);
        }
    },
    /**
     * Reload content of a tab. if maximized, reloads the maximized view
     * @param box box object
     * @param tab tab number
     */
    startTabUpdater: function(box, tab) {
        if(box.getTab(tab).updater){
            box.getTab(tab).updater.start();
        }
    },
    /**
     * Reload content of a tab. if maximized, reloads the maximized view
     * @param box box object
     * @param tab tab number
     */
    stopTabUpdater: function(box, tab) {
        if(box.getTab(tab).updater){
            box.getTab(tab).updater.stop();
        }
    },
    _genTabs: function(box) {

        //generate the tabs for use by the box box
        var tabs = box.viewid + "_tabs";
        var boxcontent = box.viewid + "_box";

        if (box.tabs.length > 0) {
            //id is box${rnum}tabs
            if ($(tabs)) {
                $(tabs).innerHTML = '';
            }
            var myself = this;
            box.tabs.each(function(tab, index) {
                //identify the tab by name:

                myself.boxTabNames.set(tab.name, box);
                if(!box.options.noTabs){
                    //create tab link in the tabs section, if there are 2+ tabs
                    if (box.tabs.length > 1) {
                        var tabe = document.createElement('span');
                        $(tabe).addClassName('btab');
                        $(tabe).setAttribute('id', box.viewid + "_tb_" + (index + 1));
                        $(tabe).innerHTML = tab.title;
                        $(tabe).onclick = function() {
                            box.selectTab(index + 1);
                        };

                        $(tabs).appendChild(tabe);
                    }

                    //create tab content div in the box, default hidden
                    var tabc = document.createElement('div');
                    $(tabc).addClassName('wbox');
                    $(tabc).setAttribute('id', box.viewid + '_tab_' + (index + 1));
                    $(tabc).setStyle('display:none;');
                    $(tabc).innerHTML = "tab " + (index + 1);
                    //$(tabc).setStyle('height: 200px;');

                    $(boxcontent).appendChild(tabc);
                }
            });

        }
    }
});

/**
 * WBox holds details about a content box with a set of 1+ tabs.
 * new WBox('name',{tabs:[
 *  {..tab properties...},
 * ]})
 *
 * Each tab should supply these properties:
 *
 * {name:'tabname',
 *   url:contenturl,
 * title:'Tab Title',
 * linkUrl:tabTitleUrl, //optional,
 * params:{} // optional parameters for teh contenturl
 * },
 */
var WBox = Class.create({
    name:'',
    viewid:'',
    selectedTab:1,
    options: new Hash(),
    controller:null,
    tabs:new Array(),
    /**
     * Create a WBox
     * @param name the name of the box
     * @param options the options for the box. should include the "tabs" definition
     */
    initialize: function(name, options) {
        this.tabs = $A();
        this.name = name;
        this.options = Object.clone(options);
        if (null != this.options.tabs) {
            var mytabs = this.tabs;
            this.options.tabs.each(function(item) {
                mytabs.push(item);
            });
        } else {
            this.tabs.push({
                url:this.options.url,
                name:this.name,
                title:this.name
            });
        }
        if(this.options.selectedTab){
            this.selectedTab=this.options.selectedTab;
        }
    },
    _setController: function(controller) {
        this.controller = controller;
    },
    _setView: function(viewid) {
        this.viewid = viewid;
    },
    /**
     * Select the specified tab for this box, loading content if necessary
     * @param num tab number
     */
    selectTab: function(num) {
        var reload=false;
        if(this.selectedTab==num){
            reload=true;
        }
        this.selectedTab = num;
        if(reload){
            this.controller.reloadTabForBox(this, num);
        }else{
            this.controller.selectTabForBox(this, num);
        }
    },
    /**
     * Maximize the specified tab for this box
     * @param num tab number
     */
    maximizeTab: function(num){
//        alert('maximize: '+this.name+", tab: "+num);
        this.controller.maximizeTab(this,num);
    },
    /**
     * Reload tab
     * @param num the tab number
     */
    reloadTab: function(num) {
        this.selectTab(num);
        this.controller.reloadTabForBox(this, num);
    },
    /**
     * Stop tab auto updater
     * @param num the tab number
     */
    stopTabUpdater: function(num) {
        this.controller.stopTabUpdater(this, num);
    },
    /**
     * Start tab auto updater
     * @param num the tab number
     */
    startTabUpdater: function(num) {
        this.controller.startTabUpdater(this, num);
    },
    /**
     * Return the selected tab number (1 indexed)
     */
    getSelectedTab: function() {
        return this.selectedTab;
    },
    /**
     * Return the tab definition for the index
     * @param index number of the tab (1 indexed)
     */
    getTab: function(index) {
        return this.tabs[index - 1];
    },
    /**
     * Return the tab number for the tab name
     * @param name name of the tab
     */
    getTabNumByName: function(name) {
        var found = 0;
        this.tabs.each(function(tab, index) {
            if (tab.name == name) {
                found = index + 1;
            }
        });
        return found;
    }

});

/**
 * BubbleController
 */
var BubbleController = Class.create({
    _popClickHandler:null,
    _popTypeHandler:null,
    _target:null,
    _elem:null,
    _offx:null,
    _offy:null,

    initialize: function(target, elem, options) {
        if (null == elem) {
            elem = $(target).identify() + "_popup";
        }
        this._target = target;
        this._elem = elem;
        if (null != options) {
            this._offx = options.offx;
            this._offy = options.offy;
        }
    },

    hide: function () {
        if ($(this._target)) {
            $(this._target).removeClassName('glow');
        }
        var bubble = this._getTargetBubble();
        if ($(bubble)) {
            Try.these(
                function() {
                    Effect.Fade($(bubble), {duration:0.5});
                },
                function() {
                    $(bubble).hide()
                }
                );
        }

        Event.stopObserving(document.body, 'click', this._popClickHandler, false);
        Event.stopObserving(document.body, 'keydown', this._popTypeHandler, false);
        this._popClickHandler = null;
        this._popTypeHandler = null;
    },

    _getTargetBubble: function() {
        //prepare content element
        var bubble;
        if (!$(this._elem).hasClassName('bubblewrap')) {
            var found = $(this._elem).up('.bubblewrap');
            if (found) {
                bubble = found;
            } else {
                bubble = $(document.createElement('div'));
                bubble.addClassName('bubblewrap');
                bubble.setStyle({display:'none'});

                Event.observe(bubble, 'click', function(evt) {
                    evt.stopPropagation();
                }, false);

                var btop = new Element('div');
                btop.addClassName('bubbletop');
                bubble.appendChild(btop);
                var bcontent = new Element('div');
                bcontent.addClassName('bubblecontent');
                var removed = $(this._elem).parentNode.removeChild($(this._elem));
                bcontent.appendChild(removed);
                $(removed).show();
                bubble.appendChild(bcontent);
                document.body.appendChild(bubble);
            }
        } else {
            bubble = this._elem;
        }
        return bubble;
    },
    show: function (evt) {
        if ($(this._target) && $(this._elem)) {
            var bubble = this._getTargetBubble();

            new MenuController().showRelativeTo(this._target, $(bubble), this._offx, this._offy);
            $(this._target).addClassName('glow');
            evt.stopPropagation();

            var hidefunc = this.hide.bind(this);
            this._popClickHandler = function(evt) {
                hidefunc();
            };
            this._popTypeHandler = function(evt) {
                if (evt.keyCode === 27) {
                    hidefunc();
                }
                return true;
            };

            Event.observe(document.body, 'click', this._popClickHandler, false);
            Event.observe(document.body, 'keydown', this._popTypeHandler, false);
        }
    },
    startObserving: function() {
        Event.observe(this._target, 'click', this.show.bind(this));
    }

});
/**
 * keypress handler which disallows Return key
 * @param e event
 */
function noenter(e) {
    if(e && e.keyCode == Event.KEY_RETURN){
        Event.stop(e);
    }
    return !(e && e.keyCode == Event.KEY_RETURN);
}

/**
 * keypress handler which disallows any chars in the input string
 * @param chars string containing chars to disallow
 * @param e event
 */
function nochars(chars,e) {
    if(e && e.charCode!=0 && chars.indexOf(String.fromCharCode(e.charCode))>=0){
        Event.stop(e);
    }
    return !(e && e.charCode!=0 && chars.indexOf(String.fromCharCode(e.charCode))>=0);
}
/**
 * keypress handler which allows only chars matching the input regular expression
 * @param regex string to match allowed chars
 * @param e event
 */
function onlychars(regex,e) {
    if(e && e.charCode!=0 && !String.fromCharCode(e.charCode).match(regex)){
        Event.stop(e);
    }
    return !(e && e.charCode!=0 && !String.fromCharCode(e.charCode).match(regex));
}
function fireWhenReady(elem,func){
    if($(elem)){
        func();
    }else{
        Event.observe(document,'dom:loaded', function(e){func();});
    }
}