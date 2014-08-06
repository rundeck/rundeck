//= require noconflict
//= require_self
//= require menus

// methods for modifying inner html or text content

function clearHtml(elem){
    $(elem).innerHTML = '';
}
function setHtml(elem,html){
    clearHtml(elem);
    appendHtml(elem,html);
}
function appendHtml(elem,html){
    $(elem).innerHTML+=html;
}

function setText(elem,text){
    clearHtml(elem);
    appendText(elem,text);
}
function appendText(elem,text){
    $(elem).appendChild(document.createTextNode(text));
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
            setHtml(elem,'<span class="fail">Killed</span>');
        }
        if($('exec-'+id+'-spinner')){
            clearHtml($('exec-'+id+'-spinner'));
            var img=new Element('img');
            img.src= appLinks.iconTinyWarn;
            $('exec-'+id+'-spinner').appendChild(img);
        }
        if($('exec-'+id+'-dateCompleted')){
            clearHtml($('exec-'+id+'-dateCompleted'));
            if($('exec-'+id+'-dateCompleted').onclick){
                $(elem).onclick=$('exec-'+id+'-dateCompleted').onclick;
            }
        }

    }else if(data['status']!='success'){
        if($(elem)){
            var sp = new Element('span');
            sp.addClassName('fail');
            setText(sp, (data['error'] ? data['error'] : 'Failed to Kill job.'));
            clearHtml(elem);
            $(elem).appendChild(sp);
        }
    }else if(data['status']=='success'){
        if($(elem)){
            setText(elem,'Job Completed.');
        }
        if($('exec-'+id+'-spinner')){
            clearHtml('exec-' + id + '-spinner');
            var img = new Element('img');
            img.src = appLinks.iconTinyOk;
            $('exec-' + id + '-spinner').appendChild(img);
        }
        if($('exec-'+id+'-dateCompleted')){
            clearHtml('exec-' + id + '-dateCompleted');
            if($('exec-'+id+'-dateCompleted').onclick){
                $(elem).onclick=$('exec-'+id+'-dateCompleted').onclick;
            }
        }
    }
}

function canceljob(id,elem){
    if($(elem)){
        clearHtml(elem);
        var img = new Element('img');
        img.src = appLinks.iconSpinner;
        $(elem).appendChild(img);
        appendText(elem,' Killing Job ...');
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
            var icn = icnh.down('.glyphicon');
            if (icn) {
                value = icn.hasClassName('glyphicon-chevron-down');
            } else {
                icn = icnh.down("img");
                if(icn){
                    value=icn.src == AppImages.disclosure;
                }
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
            var icn = icnh.down('.glyphicon');
            if (icn) {
                if (value) {
                    icn.addClassName('glyphicon-chevron-down');
                    icn.removeClassName('glyphicon-chevron-right');
                } else {
                    icn.addClassName('glyphicon-chevron-right');
                    icn.removeClassName('glyphicon-chevron-down');
                }
            } else {
                 icn = icnh.down("img");
                if(icn){
                    icn.src = value ? AppImages.disclosureOpen : AppImages.disclosure;
                }
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

function _isIe(version) {
    return Prototype.Browser.IE && $$('html')[0].hasClassName('ie' + version);
}


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
function nochars(chars, e) {
    var kCode = e.keyCode ? e.keyCode : e.charCode;
    if (e && kCode != 0 && chars.indexOf(String.fromCharCode(kCode)) >= 0) {
        Event.stop(e);
    }
    return !(e && kCode != 0 && chars.indexOf(String.fromCharCode(kCode)) >= 0);
}
function _applyAce(e,height){
    if (_isIe(8) || _isIe(7) || _isIe(6)) {
        return;
    }
    $(e).setStyle({
        width: "100%",
        height: height!=null ? height : "200px"
    });
    $(e).addClassName('ace_editor');
    var editor = ace.edit(e.identify());
    editor.setTheme("ace/theme/chrome");
    editor.getSession().setMode("ace/mode/sh");
    editor.setReadOnly(true);
}
/**
 * Return true if the event is a keycode for a control key
 * @param e
 * @returns {boolean}
 */
function controlkeycode(e) {
    var keycodes = [
        Event.KEY_BACKSPACE , Event.KEY_DELETE , Event.KEY_TAB , Event.KEY_RETURN , Event.KEY_ESC , Event.KEY_PAGEDOWN ,
        Event.KEY_PAGEUP , Event.KEY_END , Event.KEY_HOME , Event.KEY_INSERT , Event.KEY_LEFT , Event.KEY_RIGHT ,
        Event.KEY_DOWN , Event.KEY_UP
    ];
    if (e.keyCode && keycodes.indexOf(e.keyCode)>=0) {
        return true;
    }
    return false;
}
/**
 * keypress handler which allows only chars matching the input regular expression
 * @param regex string to match allowed chars
 * @param e event
 */
function onlychars(regex, e) {
    var kCode = e.keyCode ? e.keyCode : e.charCode;
    if (e && kCode != 0 && !String.fromCharCode(kCode).match(regex)) {
        Event.stop(e);
    }
    return !(e && kCode != 0 && !String.fromCharCode(kCode).match(regex));
}
function fireWhenReady(elem,func){
    if(jQuery('#'+elem)){
        func();
    }else{
        jQuery.ready(func);
    }
}

/**
 * Generate a URL
 * @param url
 * @param params
 * @returns {string}
 * @private
 */
function _genUrl(url,params){
    var urlparams = [];
    for (var e in params) {
        urlparams.push(encodeURIComponent(e) + "=" + encodeURIComponent(params[e]));
    }
    return url + (url.indexOf('?') > 0 ? '&' : '?') + urlparams.join("&");
}
/**
 * Generate a link
 * @param url
 * @param params
 * @param text
 * @param css
 * @param behavior
 * @returns {HTMLElement}
 * @private
 */
function _pageLink(url,params,text,css,behavior){
    var a=new Element('a');
    a.href=_genUrl(url,params)
    setText(a,text);
    a.addClassName(css);

        Event.observe(a, 'click', function (evt) {
            if (behavior && !behavior(a,params)){
                evt.preventDefault();
            }
        });
    return a;
}
function totalPageCount(max,total){
    var pages = Math.floor(total / max);
    if (pages != (total / max)) {
        pages += 1;
    }
    return pages;
}
/**
 * Call a function for each page in a set of results.  The function will be passed
 * an object as described below.
 * @param offset
 * @param max
 * @param total
 * @param options optional behavior configuration, {maxsteps: 10} the maximum number of page links to show, others will be skipped and a "skipped:true" page will be passed instead
 * @param func function called with paging parameters: {offset:number,
 *      prevPage: true/false,
 *      nextPage: true/false,
  *      currentPage: true/false,
 *      page: number,
 *      disabled: true/false,
 *      skipped: true/false
 *      }
 */
function foreachPage(offset,max,total, options, func){
    if (!total) {
        return;
    } else {
        total = parseInt(total);
    }
    if (!offset) {
        offset = 0;
    } else {
        offset = parseInt(offset);
    }
    if (!max) {
        max = 20;
    } else {
        max = parseInt(max);
    }
    var opts = {
        //max number of page links to show
        maxsteps: 10
    };
    if(typeof(options)=='function'){
        func=options;
    }else if (options) {
        Object.extend(opts, options);
    }
    var pages = totalPageCount(max,total);
    var curpage = Math.floor(offset / max) + 1;

    //calculate starting page given a window for maximum number of links to show
    var leftwindow = Math.floor( opts.maxsteps / 2 );

    var startpage = curpage - leftwindow;

    if(startpage + opts.maxsteps > pages){
        startpage = pages - opts.maxsteps;
    }

    if(startpage < 0){
        startpage = 0;
    }

    //determine indicators for skipped steps
    var skipbefore=startpage>0;
    var skipafter= startpage + opts.maxsteps < pages;

    //previous
    func({
        offset:(offset-max),
        page: curpage - 1,
        prevPage:true,
        disabled: curpage <= 1,
        max:max
    });
    //if skipping before curpage
    if(skipbefore){
        func({
            skipped: true,
            disabled: true,
            max: max
        });
    }


    //generate intermediate pages
    for (var i = startpage; (i-startpage) < opts.maxsteps && (max * i) < total; i++) {
        //page
        func({
            offset: (max * i),
            currentPage: (i+1==curpage),
            page:i+1,
            normal:true,
            max: max
        });
    }
    //if skipping after curpage
    if (skipafter) {
        func({
            skipped: true,
            disabled: true,
            max: max
        });
    }
    //next
    func({
        offset: (offset + max),
        nextPage: true,
        page: curpage+1,
        disabled: curpage >= pages,
        max: max
    });
}
/**
 * generate pagination links
 * @param elem
 * @param offset
 * @param total
 * @param max
 * @param options
 */
function paginate(elem,offset,total,max,options){
    var e = $(elem);
    if(!e){
        return;
    }
    if (!total) {
        return;
    } else {
        total = parseInt(total);
    }
    if(!offset){
        offset=0;
    }else{
        offset=parseInt(offset);
    }
    if(!max){
        max=20;
    } else {
        max = parseInt(max);
    }
    var opts={
        //message text
        'paginate.next':'Next','paginate.prev':'Previous',
        //css classes
        nextClass:'',prevClass:'', stepClass:'', currentStepClass:'active',
        //url parameter names
        offsetParam:'offset',maxParam:'max',
        //variables
        maxsteps:10,
        insertion:'bottom',
        behavior:null,
        ulCss:'pagination pagination-sm'
    };
    if(options){
        Object.extend(opts,options);
    }
    if(!opts.baseUrl){
        return;
    }
    var pages= Math.floor(total/max);
    if(pages!=(total/max)){
        pages+=1;
    }
    var curpage = Math.floor(offset/max) + 1;
    var page=new Element('ul');
    page.addClassName(opts.ulCss);

    //generate paginate links
    var firststep=1
    var a;
    var li = new Element('li');
    if(curpage>firststep){
        //previous
        a= _pageLink(opts.baseUrl, {offset: (offset - max), max: max}, opts['paginate.prev'], opts['prevClass'], opts.prevBehavior);
    }else{
        a=new Element('span');
        setText(a,opts['paginate.prev']);
        li.addClassName('disabled');
    }
    li.appendChild(a);
    page.appendChild(li);
    //generate intermediate pages
    var step=1;
    for(var i=0;i<opts.maxsteps && (max * i)<total;i++){
        var a = _pageLink(opts.baseUrl, {offset: max * i , max: max}, i+1, opts['stepClass'], opts.stepBehavior);
        var li = new Element('li');
        if (i + 1 == curpage) {
            li.addClassName(opts['currentStepClass']);
        }
        li.appendChild(a);
        page.appendChild(li);
    }
    li = new Element('li');
    if (offset<total-max) {
        //next
        a = _pageLink(opts.baseUrl, {offset: (offset + max), max: max}, opts['paginate.next'], opts['nextClass'], opts.nextBehavior);
    } else {
        a = new Element('span');
        setText(a,opts['paginate.next']);
        li.addClassName('disabled');
    }
    li.appendChild(a);
    page.appendChild(li);
    if(pages>opts.maxsteps){

    }

    var insert= {};
    insert[opts.insertion]=page;
    clearHtml(e);
    e.insert(insert);
}


/**
 * jQuery/bootstrap utility functions
 */
function _initPopoverContentRef(parent){
    var sel= '[data-toggle=popover][data-popover-content-ref]';
    jQuery(parent!=null?parent+' '+sel:sel).each(function (i, e) {
        var ref = jQuery(e).data('popover-content-ref');
        jQuery(e).popover({html: true, content: jQuery(ref).html()}).on('shown.bs.popover',function(){
            jQuery(e).toggleClass('active');
        }).on('hidden.bs.popover',function(){
                jQuery(e).toggleClass('active');
            });
    });
}
/**
 * jQuery/bootstrap utility functions
 */
function _initPopoverContentFor(parent){
    var sel= '[data-toggle=popover-for]';
    jQuery(parent!=null?parent+' '+sel:sel).each(function (i, e) {
        var ref = jQuery(e).data('target')|| e.href();
        var found=jQuery(ref);
        jQuery(e).on(found.data('data-trigger')||'click',function(){
            found.popover('toggle');
        });
        found.on('shown.bs.popover',function(){
            jQuery(e).toggleClass('active');
        }).on('hidden.bs.popover',function(){
                jQuery(e).toggleClass('active');
            });
    });
}

/** page init */
function _initAffix(){
    //affixed elements
    jQuery("a[href='#top']").click(function () {
        jQuery("html, body").animate({ scrollTop: 0 }, "slow");
        return false;
    });
    jQuery('[data-affix=top]').each(function (i, e) {
        var padd = jQuery(e).data('affix-padding-top');
        var top= jQuery(e).offset().top - (padd ? padd : 0);
        jQuery(e).affix({ offset: { top:  top } });
        jQuery(e).closest('[data-affix=wrap]').height(jQuery(e).height());
    });
}
/** fix placeholder text for IE8 */
function _initIEPlaceholder(){
    if(!Prototype.Browser.IE){
        return;
    }
    jQuery('[placeholder]').focus(function () {
        var input = jQuery(this);
        if (input.val() == input.attr('placeholder')) {
            input.val('');
            input.removeClass('placeholder');
        }
    }).blur(function () {
            var input = jQuery(this);
            if (input.val() == '' || input.val() == input.attr('placeholder')) {
                input.addClass('placeholder');
                input.val(input.attr('placeholder'));
            }
        }).blur();
    jQuery('[placeholder]').parents('form').submit(function () {
        jQuery(this).find('[placeholder]').each(function () {
            var input = jQuery(this);
            if (input.val() == input.attr('placeholder')) {
                input.val('');
            }
        })
    });
}
function _initCollapseExpander(){
    jQuery(document).on('show.bs.collapse', '.collapse.collapse-expandable',function(e){
        var elem=jQuery(this);
        var hrefs=jQuery('[data-toggle=collapse][href=\'#'+elem.attr('id')+'\']')
            .addClass('active')
            .children('.glyphicon')
            .removeClass('glyphicon-chevron-right')
            .addClass('glyphicon-chevron-down');
    });
    jQuery(document).on('hide.bs.collapse', '.in.collapse-expandable',function(e){
        var elem=jQuery(this);
        console.log(elem.attr('class'));
        var hrefs=jQuery('[data-toggle=collapse][href=\'#'+elem.attr('id')+'\']')
            .removeClass('active')
            .children('.glyphicon')
            .removeClass('glyphicon-chevron-down')
            .addClass('glyphicon-chevron-right');
    });
}
function _toggleAnsiColor(e){
    var test = jQuery(this).find('input')[0].checked;
    var ansicolor = jQuery('.ansicolor');
    if (!test) {
        ansicolor.removeClass('ansicolor-on');
    } else {
        ansicolor.addClass('ansicolor-on');
    }
}
function _initAnsiToggle(){
    jQuery('.ansi-color-toggle').on('change',_toggleAnsiColor);
    jQuery('.nodes_run_content').on('change','.ansi-color-toggle',_toggleAnsiColor);
}

(function(){
    if(typeof(jQuery)=='function'){
        jQuery(document).ready(function () {
            jQuery('.has_tooltip').tooltip({});
            _initPopoverContentRef();
            _initPopoverContentFor();
            _initAffix();
            _initIEPlaceholder();
            _initCollapseExpander();
            _initAnsiToggle();
        });
    }
})();
