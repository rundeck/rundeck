package rundeck

import grails.util.TypeConvertingMap
import grails.web.mapping.UrlMapping
import org.grails.taglib.TagOutput
import org.grails.taglib.encoder.OutputContextLookupHelper
import org.springframework.web.servlet.support.RequestContextUtils

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


    /**
     * Creates next/previous links to support pagination for the current controller, using bootstrap 4 markup<br/>
     *
     * &lt;bs:paginate total="${Account.count()}" /&gt;<br/>
     *
     * @emptyTag
     *
     * @attr total REQUIRED The total number of results to paginate
     * @attr action the name of the action to use in the link, if not specified the default action will be linked
     * @attr controller the name of the controller to use in the link, if not specified the current controller will be linked
     * @attr id The id to use in the link
     * @attr params A map containing request parameters
     * @attr prev The text to display for the previous link (defaults to "Previous" as defined by default.paginate.prev property in I18n messages.properties)
     * @attr next The text to display for the next link (defaults to "Next" as defined by default.paginate.next property in I18n messages.properties)
     * @attr omitPrev Whether to not show the previous link (if set to true, the previous link will not be shown)
     * @attr omitNext Whether to not show the next link (if set to true, the next link will not be shown)
     * @attr omitFirst Whether to not show the first link (if set to true, the first link will not be shown)
     * @attr omitLast Whether to not show the last link (if set to true, the last link will not be shown)
     * @attr max The number of records displayed per page (defaults to 10). Used ONLY if params.max is empty
     * @attr maxsteps The number of steps displayed for pagination (defaults to 10). Used ONLY if params.maxsteps is empty
     * @attr offset Used only if params.offset is empty
     * @attr mapping The named URL mapping to use to rewrite the link
     * @attr fragment The link fragment (often called anchor tag) to use
     */
    Closure paginate = { Map attrsMap ->
        TypeConvertingMap attrs = (TypeConvertingMap) attrsMap
        def writer = out
        if (attrs.total == null) {
            throwTagError("Tag [paginate] is missing required attribute [total]")
        }

        def messageSource = grailsAttributes.messageSource
        def locale = RequestContextUtils.getLocale(request)

        def total = attrs.int('total') ?: 0
        def offset = params.int('offset') ?: 0
        def max = params.int('max')
        def maxsteps = (attrs.int('maxsteps') ?: 10)

        if (!offset) offset = (attrs.int('offset') ?: 0)
        if (!max) max = (attrs.int('max') ?: 10)

        Map linkParams = [:]
        if (attrs.params instanceof Map) linkParams.putAll((Map) attrs.params)
        linkParams.offset = offset - max
        linkParams.max = max
        if (params.sort) linkParams.sort = params.sort
        if (params.order) linkParams.order = params.order

        Map linkTagAttrs = [:]
        def action
        if (attrs.containsKey('mapping')) {
            linkTagAttrs.mapping = attrs.mapping
            action = attrs.action
        } else {
            action = attrs.action ?: params.action
        }
        if (action) {
            linkTagAttrs.action = action
        }
        if (attrs.controller) {
            linkTagAttrs.controller = attrs.controller
        }
        if (attrs.containsKey(UrlMapping.PLUGIN)) {
            linkTagAttrs.put(UrlMapping.PLUGIN, attrs.get(UrlMapping.PLUGIN))
        }
        if (attrs.containsKey(UrlMapping.NAMESPACE)) {
            linkTagAttrs.put(UrlMapping.NAMESPACE, attrs.get(UrlMapping.NAMESPACE))
        }
        if (attrs.id != null) {
            linkTagAttrs.id = attrs.id
        }
        if (attrs.fragment != null) {
            linkTagAttrs.fragment = attrs.fragment
        }
        linkTagAttrs.params = linkParams

        // determine paging variables
        def steps = maxsteps > 0
        int currentstep = ((offset / max) as int) + 1
        int firststep = 1
        int laststep = Math.round(Math.ceil(total / max)) as int

        // display previous link when not on firststep unless omitPrev is true
        if (currentstep > firststep && !attrs.boolean('omitPrev')) {
            linkTagAttrs.put('class', 'page-link')
            linkParams.offset = offset - max
            writer << """<li class="page-item">${callLink((Map) linkTagAttrs.clone()) {
                (attrs.prev ?: messageSource.getMessage('paginate.prev', null, messageSource.getMessage('default.paginate.prev', null, 'Previous', locale), locale))
            }}</li>"""
        }

        // display steps when steps are enabled and laststep is not firststep
        if (steps && laststep > firststep) {
            linkTagAttrs.put('class', 'page-link')

            // determine begin and endstep paging variables
            int beginstep = currentstep - (Math.round(maxsteps / 2.0d) as int) + (maxsteps % 2)
            int endstep = currentstep + (Math.round(maxsteps / 2.0d) as int) - 1

            if (beginstep < firststep) {
                beginstep = firststep
                endstep = maxsteps
            }
            if (endstep > laststep) {
                beginstep = laststep - maxsteps + 1
                if (beginstep < firststep) {
                    beginstep = firststep
                }
                endstep = laststep
            }

            // display firststep link when beginstep is not firststep
            if (beginstep > firststep && !attrs.boolean('omitFirst')) {
                linkParams.offset = 0
                writer << """<li class="page-item">${callLink((Map) linkTagAttrs.clone()) { firststep.toString() }}</li>"""
            }
            //show a gap if beginstep isn't immediately after firststep, and if were not omitting first or rev
            if (beginstep > firststep + 1 && (!attrs.boolean('omitFirst') || !attrs.boolean('omitPrev'))) {
                writer << '<li class="page-item"><span>...</span></li>'
            }

            // display paginate steps
            (beginstep..endstep).each { int i ->
                if (currentstep == i) {
                    writer << """<li class="active"><span>${i}</span></li>"""
                } else {
                    linkParams.offset = (i - 1) * max
                    writer << """<li class="page-item">${callLink((Map) linkTagAttrs.clone()) { i.toString() }}</li>"""
                }
            }

            //show a gap if beginstep isn't immediately before firststep, and if were not omitting first or rev
            if (endstep + 1 < laststep && (!attrs.boolean('omitLast') || !attrs.boolean('omitNext'))) {
                writer << '<li class="page-item"><span>...</span></li>'
            }
            // display laststep link when endstep is not laststep
            if (endstep < laststep && !attrs.boolean('omitLast')) {
                linkParams.offset = (laststep - 1) * max
                writer << """<li class="page-item">${callLink((Map) linkTagAttrs.clone()) { laststep.toString() }}</li>"""
            }
        }

        // display next link when not on laststep unless omitNext is true
        if (currentstep < laststep && !attrs.boolean('omitNext')) {
            linkTagAttrs.put('class', 'page-link')
            linkParams.offset = offset + max
            writer << """<li class="page-item">${callLink((Map) linkTagAttrs.clone()) {
                (attrs.next ? attrs.next : messageSource.getMessage('paginate.next', null, messageSource.getMessage('default.paginate.next', null, 'Next', locale), locale))
            }}</li>"""
        }
    }


    private callLink(Map attrs, Object body) {
        TagOutput.captureTagOutput(tagLibraryLookup, 'g', 'link', attrs, body, OutputContextLookupHelper.lookupOutputContext())
    }


}
