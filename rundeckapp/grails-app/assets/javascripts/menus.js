var MenuController = Class.create({
    menuStart : new Hash(),
    menuHideTimeout: new Hash(),


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
        Try.these(Element.show.curry(menu));
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
