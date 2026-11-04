import type {
  Node,
  NodeAttributes,
} from "../components/job/resources/types/nodeTypes";

type UiAttributes = Partial<Record<string, string | null>>;

const CSSColors =
  "aliceblue antiquewhite aqua aquamarine azure beige bisque black blanchedalmond blue blueviolet brown burlywood cadetblue chartreuse chocolate coral cornflowerblue cornsilk crimson cyan darkblue darkcyan darkgoldenrod darkgray darkgreen darkkhaki darkmagenta darkolivegreen darkorange darkorchid darkred darksalmon darkseagreen darkslateblue darkslategray darkturquoise darkviolet deeppink deepskyblue dimgray dodgerblue firebrick floralwhite forestgreen fuchsia gainsboro ghostwhite gold goldenrod gray green greenyellow honeydew hotpink indianred indigo ivory khaki lavender lavenderblush lawngreen lemonchiffon lightblue lightcoral lightcyan lightgoldenrodyellow lightgreen lightgrey lightpink lightsalmon lightseagreen lightskyblue lightslategray lightsteelblue lightyellow lime limegreen linen magenta maroon mediumaquamarine mediumblue mediumorchid mediumpurple mediumseagreen mediumslateblue mediumspringgreen mediumturquoise mediumvioletred midnightblue mintcream mistyrose moccasin navajowhite navy oldlace olive olivedrab orange orangered orchid palegoldenrod palegreen paleturquoise palevioletred papayawhip peachpuff peru pink plum powderblue purple red rosybrown royalblue saddlebrown salmon sandybrown seagreen seashell sienna silver skyblue slateblue slategray snow springgreen steelblue tan teal thistle tomato turquoise violet wheat white whitesmoke yellow yellowgreen".split(
    " ",
  );

export function cssForIcon(attrs: UiAttributes) {
  const classnames = [];
  const fgColor = iconFgCss(attrs, "ui:icon:color");
  if (fgColor) {
    classnames.push(fgColor);
  }
  const bgColor = iconBgCss(attrs);
  if (bgColor) {
    classnames.push(bgColor);
  }
  return classnames.join(" ");
}

export function cssForNode(node: NodeAttributes, nodes: Node[]) {
  return {
    server: node.islocal,
    "col-lg-2": nodes.length > 20,
    "col-md-3": nodes.length > 12,
    "col-sm-4": nodes.length > 8 && nodes.length < 13,
  };
}

export function styleForNode(attrs: UiAttributes) {
  const styles: Record<string, string> = {};
  const uiColor = attrs["ui:color"];
  if (!nodeFgCss(attrs) && isStyleFg(uiColor)) {
    styles["color"] = uiColor;
  }
  const uiBgcolor = attrs["ui:bgcolor"];
  if (!nodeBgCss(attrs) && isStyleBg(uiBgcolor)) {
    styles["background-color"] = uiBgcolor;
  }
  return styles;
}

export function statusIconStyle(attrs: UiAttributes) {
  const styles: Record<string, string> = {};
  if (!iconFgCss(attrs, "ui:status:color")) {
    const uiIconColor = attrs["ui:status:color"];
    const uiColor = attrs["ui:color"];
    if (isStyleFg(uiIconColor)) {
      styles["color"] = uiIconColor;
    } else if (isStyleFg(uiColor)) {
      styles["color"] = uiColor;
    }
  }
  if (!iconBgCss(attrs)) {
    const uiIconBgcolor = attrs["ui:status:bgcolor"];
    const uiBgcolor = attrs["ui:bgcolor"];
    if (isStyleBg(uiIconBgcolor)) {
      styles["background-color"] = uiIconBgcolor;
    } else if (isStyleBg(uiBgcolor)) {
      styles["background-color"] = uiBgcolor;
    }
  }
  return styles;
}

export function statusIconCss(attrs: UiAttributes) {
  const classnames = [];
  const fgColor = iconFgCss(attrs, "ui:status:color");
  if (fgColor) {
    classnames.push(fgColor);
  }
  const bgColor = iconBgCss(attrs);
  if (bgColor) {
    classnames.push(bgColor);
  }
  return classnames.join(" ");
}

export function isAnsiFg(str: string | null | undefined): str is string {
  return (
    str != null &&
    /^ansi-fg-(light-)?(black|green|red|yellow|blue|magenta|cyan|white)$/.test(
      str,
    )
  );
}

export function isStyleFg(str: string | null | undefined): str is string {
  return (
    str != null &&
    (/^#[0-9a-fA-F]{3,6}$/.test(str) || CSSColors.indexOf(str) >= 0)
  );
}

export function isAnsiBg(str: string | null | undefined): str is string {
  return (
    str != null &&
    /^ansi-bg-(black|green|red|yellow|blue|magenta|cyan|white|default)$/.test(
      str,
    )
  );
}

export function isStyleBg(str: string | null | undefined): str is string {
  return (
    str != null &&
    (/^#[0-9a-fA-F]{3,6}$/.test(str) || CSSColors.indexOf(str) >= 0)
  );
}

export function nodeFgCss(attrs: UiAttributes) {
  const uiColor = attrs["ui:color"];
  if (isAnsiFg(uiColor)) {
    return uiColor;
  }
  return null;
}

export function nodeBgCss(attrs: UiAttributes) {
  const uiBgcolor = attrs["ui:bgcolor"];
  if (isAnsiBg(uiBgcolor)) {
    return uiBgcolor;
  }
  return null;
}

export function iconFgCss(attrs: UiAttributes, attrName: string) {
  const uiIconColor = attrs[attrName];
  const uiColor = attrs["ui:color"];
  if (isAnsiFg(uiIconColor)) {
    return uiIconColor;
  } else if (isAnsiFg(uiColor)) {
    return uiColor;
  }
  return null;
}

export function iconBgCss(attrs: UiAttributes) {
  const uiIconBgcolor = attrs["ui:icon:bgcolor"];
  const uiBgcolor = attrs["ui:bgcolor"];
  if (isAnsiBg(uiIconBgcolor)) {
    return uiIconBgcolor;
  } else if (isAnsiBg(uiBgcolor)) {
    return uiBgcolor;
  }
  return null;
}

export function styleForIcon(attrs: UiAttributes) {
  const styles: Record<string, string> = {};
  if (!iconFgCss(attrs, "ui:icon:color")) {
    const uiIconColor = attrs["ui:icon:color"];
    const uiColor = attrs["ui:color"];
    if (isStyleFg(uiIconColor)) {
      styles["color"] = uiIconColor;
    } else if (isStyleFg(uiColor)) {
      styles["color"] = uiColor;
    }
  }
  if (!iconBgCss(attrs)) {
    const uiIconBgcolor = attrs["ui:icon:bgcolor"];
    const uiBgcolor = attrs["ui:bgcolor"];
    if (isStyleBg(uiIconBgcolor)) {
      styles["background-color"] = uiIconBgcolor;
    } else if (isStyleBg(uiBgcolor)) {
      styles["background-color"] = uiBgcolor;
    }
  }
  return styles;
}

export function glyphiconForName(name: string) {
  if (name.match(/^glyphicon-[a-z-]+$/)) {
    return "glyphicon " + name;
  } else if (name.match(/^fa-[a-z-]+$/)) {
    return "fas " + name;
  } else if (name.match(/^fab-[a-z-]+$/)) {
    return "fab fa-" + name.substring(4);
  }
  return "";
}

export function glyphiconBadges(attributes: UiAttributes): Array<string> {
  const badges = [];
  if (attributes["ui:badges"]) {
    const found = attributes["ui:badges"].split(/,\s*/g);
    for (let i = 0; i < found.length; i++) {
      if (found[i].match(/^glyphicon-[a-z0-9-]+$/)) {
        badges.push(found[i]);
      } else if (found[i].match(/^fa-[a-z0-9-]+$/)) {
        badges.push(found[i]);
      } else if (found[i].match(/^fab-[a-z0-9-]+$/)) {
        badges.push(found[i]);
      }
    }
  }

  return badges;
}

export function expandNodeAttributes(attrs: UiAttributes, str: string) {
  return str.replace(
    /\$\{node\.([a-zA-Z-.]+)\}/g,
    function (match, g1, offset, string) {
      if (attrs[g1]) {
        return attrs[g1] || "";
      } else {
        return string;
      }
    },
  );
}
