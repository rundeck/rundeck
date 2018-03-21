package rundeck

class BootstrapTagLib {
    def static namespace = "bs"
    static defaultEncodeAs = [taglib: 'none']
//    static encodeAsForTags = [li: [taglib: 'none'],lisep: [taglib: 'none']]

    /**
     * ul for dropdown-menu
     */
    def dropdown = { attrs, body ->
        out << '<ul class="dropdown-menu '

        if (attrs.right) {
            out << 'dropdown-menu-right '
        }
        if (attrs.css) {
            out << attrs.css.encodeAsHTML()
        }
        out << '" role="menu" '
        if (attrs.labelId) {
            out << "aria-labelledby=\"${attrs.labelId.encodeAsHTML()}\" "
        }
        out << '>'
        out << body()
        out << '</ul>'
    }
    /**
     * toggle button for a dropdown-menu
     */
    def dropdownToggle = { attrs, body ->
        out << '<a href="'
        out << getAnchorHref(false, attrs)
        out << '" class="dropdown-toggle '
        if (attrs.right) {
            out << 'dropdown-toggle-right '
        }
        if (attrs.css) {
            out << attrs.css.encodeAsHTML()
        }
        out << '" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false"'
        if(attrs.id){
            out<< " id=\"${attrs.id.encodeAsHTML()}\""
        }
        out << '>'
        if (attrs.code) {
            out << message(code: attrs.code).encodeAsHTML()
        } else if (body) {
            out << body()
        }
        out << ' <span class="caret"></span>'
        out << '<span class="sr-only">Toggle Dropdown</span>'
        out << '</a>'
    }
    /**
     * li for dropdown-menu, if no header/headerCode and no href/action and body is specified, a separator will be
     * rendered.
     * if attr separator/divider is true, will render a separator. if header/headerCode is specified, or body with no
     * href/action,
     * a dropdown-header will be rendered.
     * Otherwise a link will be rendered using href or controller,action,params.
     *
     * Content can be specified as the body content, or using 'code' attribute, and optional 'icon' attribute
     * @attr separator render separator line (also: 'divider')
     * @attr header render attr text as a header
     * @attr headerCode message code for header text
     * @attr disabled if true, render a disabled item
     * @attr enabled if false, render a disabled item
     * @attr hidden if true, do not render the item(s)
     * @attr shown if true, do render the item(s)
     * @attr href URL for link
     * @attr action controller action name for link, also controller/params are needed
     * @attr controller controller name for generated link
     * @attr params params for generated link
     * @attr title title text to use
     * @attr titleCode message code for title text
     * @attr disabledTitle title for disabled item
     * @attr disabledTitleCode message code for disabled item title
     * @attr icon glyphicon name to render before content
     * @attr code message code for link content
     */
    def menuitem = { attrs, body ->
        def isHidden = null != attrs.hidden ? attrs.hidden : (null != attrs.shown ? !attrs.shown : false)
        if (isHidden) {
            return
        }
        def separator = attrs.remove('separator')
        def divider = attrs.remove('divider')
        def headercode = attrs.remove('headerCode')
        def href = attrs.remove('href')
        def action = attrs.remove('action')
        def controller = attrs.remove('controller')
        def params = attrs.remove('params')
        def header = attrs.remove('header')
        if ((separator || divider ||
                (!header && !headercode && !href && !action))
                && !body) {
            out << '<li role="separator" class="divider"></li>'
            if (!header && !headercode && !href && !action) {
                return
            }
        }
        if (header || headercode || (!href && !action)) {
            out << '<li class="dropdown-header">'
            if (headercode) {
                out << message(code: headercode)
            } else if (header) {
                out << header
            } else if (body) {
                out << body()
            }
            out << '</li>'
            if (!href && !action) {
                return
            }
        }
        def disabled = attrs.remove('disabled')
        def enabled = attrs.remove('enabled')
        boolean isDisabled = null != disabled ? disabled : (null != enabled ? !enabled : false)
        out << '<li'
        if (isDisabled) {
            out << ' class="disabled"'
        }
        out << '>'
        out << '<a href="'
        out << getAnchorHref(isDisabled, [href: href, action: action, controller: controller, params: params])
        out << '"'


        out << ' title="'
        def disabledTitleCode = attrs.remove('disabledTitleCode')
        def disabledTitle = attrs.remove('disabledTitle')
        def titleCode = attrs.remove 'titleCode'
        def title = attrs.remove 'title'
        if (isDisabled && disabledTitleCode) {
            out << g.message(code: disabledTitle).encodeAsHTML()
        } else if (isDisabled && disabledTitle) {
            out << disabledTitle?.encodeAsHTML()
        } else if (!isDisabled && titleCode) {
            out << g.message(code: titleCode).encodeAsHTML()
        } else if (!isDisabled && title) {
            out << title?.encodeAsHTML()
        }
        out << '"'


        def iconAfter = attrs.remove('iconAfter')
        def iconAttr = attrs.remove('icon')
        def code = attrs.remove('code')

        //remaining attrs
        attrs.each { k, v ->
            out << " ${k.encodeAsHTML()}=\"" + (v.toString()).encodeAsHTMLAttribute() + '"'
        }


        out << '>'
        if (iconAttr && !iconAfter) {
            out << g.icon(name: iconAttr)
            out << ' '
        }
        if (code) {
            out << g.message(code: code).encodeAsHTML()
        } else if (body) {
            out << body()
        }
        if (iconAttr && iconAfter) {
            out << ' '
            out << g.icon(name: iconAttr)
        }
        out << '</a>'

        out << '</li>'
    }

    private String getAnchorHref(boolean isDisabled, Map attrs) {
        if (!isDisabled && attrs.href) {
            return attrs.href?.encodeAsHTML()
        } else if (!isDisabled && attrs.action) {
            return createLink(attrs.subMap(['action', 'controller', 'params'])).encodeAsHTML()
        }
        return '#'
    }
}
