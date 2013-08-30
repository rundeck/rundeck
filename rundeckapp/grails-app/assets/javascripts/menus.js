var MenuController = Class.create({
    menuStart : new Hash(),
    menuHideTimeout: new Hash(),
    doMenuToggle: function(item, menuname) {
        if (Element.visible(menuname)) {
            this.doMenuHide(menuname);
        } else {
            this.doMenuShow(item, menuname);
        }
    },
    doMenuShow: function(item, menuname) {
        this.doMenuHideAll();
        if(this.menuHideTimeout.get(menuname)){
            clearTimeout(this.menuHideTimeout.get(menuname));
            this.menuHideTimeout.unset(menuname);
        }
        var menu = $(menuname);
        var menuLink = $(item);
        Element.hide(menu);
        Element.absolutize(menu);
        menu.setStyle({'zIndex':100});
        var h = menuLink.offsetHeight;
        var cw = menuLink.offsetWidth;
        var pos = menuLink.viewportOffset();

        var page = document.viewport.getWidth();
        var pt = document.viewport.getScrollOffsets();
        var w = menu.getWidth();
        var t = page - (pos.left + (w + 16));
        if (t > 0) {
            t = 0;
        }
        if (t < 0 && w > cw) {
            t = cw - w;
        }
//      alert("t: "+t+", pos.left: "+pos.left+", w: "+w+", cw: "+cw+", page: "+page+", pt: "+pt);

        Element.clonePosition(menu, menuLink, { setWidth:   false, setHeight:  false,offsetTop:h,offsetLeft:t});

        menuLink.addClassName('menuSelected');
        menuLink.addClassName('menuopen');
        menuLink.addClassName('menu_click_' + menu.identify());
        Element.show(menu);
        this.menuStart.set(menuname, true);
        menuLink.down("img.disclosureicon").addClassName("menu_discl_" + menu.identify());
        menuLink.down("img.disclosureicon").src = AppImages.disclosureOpen;

    },
    /**
     * Position and show the aux element below the target, optionally at a specific offset from the top left corner.
     */
    showRelativeTo: function(target,aux,offx,offy){
        var menu = $(aux);
        var menuLink = $(target);
        Element.hide(menu);
        Element.absolutize(menu);
        menu.setStyle({'zIndex':100});
        var h = menuLink.offsetHeight;
        var cw = 0;//menuLink.offsetWidth;
        if (null != offy) {
            h=offy;
        }
        if (null != offx) {
            cw=offx;
        }
        var pos = menuLink.viewportOffset();

        var page = document.viewport.getWidth();
        var pt = document.viewport.getScrollOffsets();
        var w = menu.getWidth();
        var t = page - (pos.left + (w + 16 + cw) );
        if (t < 0 && page>w) {
            cw +=t;
        }
        if(Prototype.Browser.IE){
            h+=pt[1];
            cw+=pt[0];
        }
        Element.clonePosition(menu, menuLink, { setWidth:   false, setHeight:  false,offsetTop:h,offsetLeft:cw});
        Try.these(function(){Effect.Appear(menu,{duration: 0.3});},Element.show.curry(menu));
    },
    doMenuHide: function(menu, time) {
        if (time) {
            if(this.menuHideTimeout.get(menu)){
                clearTimeout(this.menuHideTimeout.get(menu));
                this.menuHideTimeout.unset(menu);
            }
            var to=setTimeout(this.doMenuHide.bind(this,menu), time);
            this.menuHideTimeout.set(menu,to);
        } else {
            var id = Element.identify(menu);
            if (this.menuStart.get(id)) {
                Element.hide(menu);
                this.menuStart.unset(id);
                var cls = 'menu_click_' + id;
                var cls2 = 'menu_discl_' + id;
                $$("." + cls).each(function(i) {$(i).removeClassName("menuSelected", "menuopen", cls)});
                $$("img." + cls2).each(function(i) {
                    $(i).removeClassName(cls2);
                    $(i).src = AppImages.disclosure;
                });
            }
        }
    },
    doMenuHideAll: function() {
        var did = false;
        var menus=this;
        this.menuStart.each(function(m) {
            menus.doMenuHide(m.key);
            did = true;
        });
        return did;
    },
    _menukeyDownHandler: function(e) {
        var code;
        if (e.keyCode) code = e.keyCode;
        else if (e.which) code = e.which;
        if (code == Event.KEY_ESC) {
            if (Try.these(this.doMenuHideAll.bind(this))) {
                return false;
            }
        }
    },
    _mouseoutMenuHide: function(e, menuname) {
        var target = Event.element(e);
        var reltarget = Event.relatedTarget(e);
        if (e.relatedTarget) {
            reltarget = $(e.relatedTarget);
        }
        if (!reltarget.descendantOf(menuname)) {
            this.doMenuHide(menuname,1500);
        }
    },
    _mouseoverMenuRestore: function(e, menuname) {
        var target = Event.element(e);
        var reltarget = Event.relatedTarget(e);
        if (e.relatedTarget) {
            reltarget = $(e.relatedTarget);
        }
        if (!reltarget.descendantOf(menuname)) {
                
            if(this.menuHideTimeout.get(menuname)){
                clearTimeout(this.menuHideTimeout.get(menuname));
                this.menuHideTimeout.unset(menuname);
            }
        }
    }
});