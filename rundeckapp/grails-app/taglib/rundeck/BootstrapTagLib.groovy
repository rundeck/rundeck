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
        if ((attrs.separator || attrs.divider ||
                (!attrs.header && !attrs.headerCode && !attrs.href && !attrs.action))
                && !body) {
            out << '<li role="separator" class="divider"></li>'
            if (!attrs.header && !attrs.headerCode && !attrs.href && !attrs.action) {
                return
            }
        }
        if (attrs.header || attrs.headerCode || (!attrs.href && !attrs.action)) {
            out << '<li class="dropdown-header">'
            if (attrs.headerCode) {
                out << message(code: attrs.headerCode)
            } else if (attrs.header) {
                out << attrs.header
            } else if (body) {
                out << body()
            }
            out << '</li>'
            if (!attrs.href && !attrs.action) {
                return
            }
        }
        def isDisabled = null != attrs.disabled ? attrs.disabled : (null != attrs.enabled ? !attrs.enabled : false)
        out << '<li'
        if (isDisabled) {
            out << ' class="disabled"'
        }
        out << '>'
        out << '<a href="'
        out << getAnchorHref(isDisabled, attrs)
        out << '"'
        out << ' title="'
        if (isDisabled && attrs.disabledTitleCode) {
            out << message(code: attrs.disabledTitle).encodeAsHTML()
        } else if (isDisabled && attrs.disabledTitle) {
            out << attrs.disabledTitle?.encodeAsHTML()
        } else if (!isDisabled && attrs.titleCode) {
            out << message(code: attrs.titleCode).encodeAsHTML()
        } else if (!isDisabled && attrs.title) {
            out << attrs.title?.encodeAsHTML()
        }
        out << '">'
        if (attrs.icon) {
            out << icon(name: attrs.icon)
            out << ' '
        }
        if (attrs.code) {
            out << message(code: attrs.code).encodeAsHTML()
        } else if (body) {
            out << body()
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
