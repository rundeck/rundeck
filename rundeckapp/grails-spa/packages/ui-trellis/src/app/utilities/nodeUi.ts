const CSSColors = 'aliceblue antiquewhite aqua aquamarine azure beige bisque black blanchedalmond blue blueviolet brown burlywood cadetblue chartreuse chocolate coral cornflowerblue cornsilk crimson cyan darkblue darkcyan darkgoldenrod darkgray darkgreen darkkhaki darkmagenta darkolivegreen darkorange darkorchid darkred darksalmon darkseagreen darkslateblue darkslategray darkturquoise darkviolet deeppink deepskyblue dimgray dodgerblue firebrick floralwhite forestgreen fuchsia gainsboro ghostwhite gold goldenrod gray green greenyellow honeydew hotpink indianred indigo ivory khaki lavender lavenderblush lawngreen lemonchiffon lightblue lightcoral lightcyan lightgoldenrodyellow lightgreen lightgrey lightpink lightsalmon lightseagreen lightskyblue lightslategray lightsteelblue lightyellow lime limegreen linen magenta maroon mediumaquamarine mediumblue mediumorchid mediumpurple mediumseagreen mediumslateblue mediumspringgreen mediumturquoise mediumvioletred midnightblue mintcream mistyrose moccasin navajowhite navy oldlace olive olivedrab orange orangered orchid palegoldenrod palegreen paleturquoise palevioletred papayawhip peachpuff peru pink plum powderblue purple red rosybrown royalblue saddlebrown salmon sandybrown seagreen seashell sienna silver skyblue slateblue slategray snow springgreen steelblue tan teal thistle tomato turquoise violet wheat white whitesmoke yellow yellowgreen'.split(' ')

export function cssForIcon(attrs: any) {
  let classnames = []
  let fgColor = iconFgCss(attrs, 'ui:icon:color')
  if (fgColor) {
    classnames.push(fgColor)
  }
  let bgColor = iconBgCss(attrs, 'ui:icon:bgcolor')
  if (bgColor) {
    classnames.push(bgColor)
  }
  return classnames.join(' ')
}

export function cssForNode(node: any, nodes: Array<any>) {
  return {
    server: node.islocal,
    'col-lg-2': nodes.length > 20,
    'col-md-3': nodes.length > 12,
    'col-sm-4': nodes.length > 8 && nodes.length < 13,
  }
}

export function styleForNode(attrs: any) {
  let styles: any = {}
  let uiColor = attrs['ui:color']
  if (!nodeFgCss(attrs) && isStyleFg(uiColor)) {
    styles['color'] = uiColor
  }
  let uiBgcolor = attrs['ui:bgcolor']
  if (!nodeBgCss(attrs) && isStyleBg(uiBgcolor)) {
    styles['background-color'] = uiBgcolor
  }
  return styles
}

export function statusIconStyle(attrs: any) {
  var styles: any = {}
  if (!iconFgCss(attrs, 'ui:status:color')) {
    var uiIconColor = attrs['ui:status:color']
    var uiColor = attrs['ui:color']
    if (isStyleFg(uiIconColor)) {
      styles['color'] = uiIconColor
    } else if (isStyleFg(uiColor)) {
      styles['color'] = uiColor
    }
  }
  if (!iconBgCss(attrs, 'ui:status:bgcolor')) {
    var uiIconBgcolor = attrs['ui:status:bgcolor']
    var uiBgcolor = attrs['ui:bgcolor']
    if (isStyleBg(uiIconBgcolor)) {
      styles['background-color'] = uiIconBgcolor
    } else if (isStyleBg(uiBgcolor)) {
      styles['background-color'] = uiBgcolor
    }
  }
  return styles
}

export function statusIconCss(attrs: any) {
  let classnames = []
  let fgColor = iconFgCss(attrs, 'ui:status:color')
  if (fgColor) {
    classnames.push(fgColor)
  }
  let bgColor = iconBgCss(attrs, 'ui:status:bgcolor')
  if (bgColor) {
    classnames.push(bgColor)
  }
  return classnames.join(' ')
}

export function isAnsiFg(str: string) {
  return str != null && str.match(/^ansi-fg-(light-)?(black|green|red|yellow|blue|magenta|cyan|white)$/)
}

export function isStyleFg(str: string) {
  return str != null && str.match(/^#[0-9a-fA-F]{3,6}$/) || CSSColors.indexOf(str) >= 0
}

export function isAnsiBg(str: string) {
  return str != null && str.match(/^ansi-bg-(black|green|red|yellow|blue|magenta|cyan|white|default)$/)
}

export function isStyleBg(str: string) {
  return str != null && str.match(/^#[0-9a-fA-F]{3,6}$/) || CSSColors.indexOf(str) >= 0
}

export function nodeFgCss(attrs: any) {
  let uiColor = attrs['ui:color']
  if (isAnsiFg(uiColor)) {
    return uiColor
  }
  return null
}

export function nodeBgCss(attrs: any) {
  let uiBgcolor = attrs['ui:bgcolor']
  if (isAnsiBg(uiBgcolor)) {
    return uiBgcolor
  }
  return null
}

export function iconFgCss(attrs: any, attrName: string) {
  let uiIconColor = attrs[attrName]
  let uiColor = attrs['ui:color']
  if (isAnsiFg(uiIconColor)) {
    return uiIconColor
  } else if (isAnsiFg(uiColor)) {
    return uiColor
  }
  return null
}

export function iconBgCss(attrs: any, attrName: string) {
  let uiIconBgcolor = attrs['ui:icon:bgcolor']
  let uiBgcolor = attrs['ui:bgcolor']
  if (isAnsiBg(uiIconBgcolor)) {
    return uiIconBgcolor
  } else if (isAnsiBg(uiBgcolor)) {
    return uiBgcolor
  }
  return null
}

export function styleForIcon(attrs: any) {
  let styles: any = {}
  if (!iconFgCss(attrs, 'ui:icon:color')) {
    let uiIconColor = attrs['ui:icon:color']
    let uiColor = attrs['ui:color']
    if (isStyleFg(uiIconColor)) {
      styles['color'] = uiIconColor
    } else if (isStyleFg(uiColor)) {
      styles['color'] = uiColor
    }
  }
  if (!iconBgCss(attrs, 'ui:icon:bgcolor')) {
    let uiIconBgcolor = attrs['ui:icon:bgcolor']
    let uiBgcolor = attrs['ui:bgcolor']
    if (isStyleBg(uiIconBgcolor)) {
      styles['background-color'] = uiIconBgcolor
    } else if (isStyleBg(uiBgcolor)) {
      styles['background-color'] = uiBgcolor
    }
  }
  return styles
}

export function glyphiconForName(name: string) {
  if (name.match(/^glyphicon-[a-z-]+$/)) {
    return 'glyphicon ' + name
  } else if (name.match(/^fa-[a-z-]+$/)) {
    return 'fas ' + name
  } else if (name.match(/^fab-[a-z-]+$/)) {
    return 'fab fa-' + name.substring(4)
  }
  return ''
}