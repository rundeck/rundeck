/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//= require noconflict
//= require_self
//= require versionIdentity
//= require actionHandlers

// methods for modifying inner html or text content

function clearHtml(elem) {
  if (typeof (jQuery) !== 'undefined') {
    jQuery(elem).html('')
  } else if (typeof ($) !== 'undefined') {
    $(elem).innerHTML = ''
  }
}

function setHtml(elem, html) {
  clearHtml(elem);
  appendHtml(elem, html);
}

function appendHtml(elem, html) {
  $(elem).innerHTML += html;
}

function setText(elem, text) {
  clearHtml(elem);
  appendText(elem, text);
}

function appendText(elem, text) {
  if(typeof(jQuery)!=='undefined'){
    jQuery(elem).append(document.createTextNode(text))
  }else if(typeof($)!=='undefined'){
    $(elem).appendChild(document.createTextNode(text));
  }
}
/**
 * take escaped text and unescape html encoding
 * @param text
 * @returns unescaped text
 */
function html_unescape(text) {
  return jQuery('<div/>').html(text.split('<').join('&lt;').split('>').join('&gt;')).text();
}
/**
 * Load json data which is html encoded in a script element
 * @param id
 * @returns {*}
 */
function loadJsonData(id) {
  var dataElement = document.getElementById(id);
  // unescape the content of the span
  if (!dataElement) {
    return null;
  }
  var jsonText = dataElement.textContent || dataElement.innerText;
  return jsonText && jsonText != '' ? JSON.parse(jsonText) : null;
}

function parseUrlParams(href) {
  var data = {};
  var parts = href.split('?', 2);
  if (parts.length == 2) {
    href = parts[1];
    parts = href.split('&');
    for (var i = 0; i < parts.length; i++) {
      var comps = parts[i].split('=', 2);
      if (comps.length == 2) {
        data[comps[0]] = comps[1];
      }
    }
  }
  return data;
}

function toggleDisclosure(id, iconid, closeUrl, openUrl) {
  $(id).toggle();
  if ($(id).visible()) {
    $(iconid).setAttribute("src", openUrl);
  } else {

    $(iconid).setAttribute("src", closeUrl);
  }
}


function myToggleClassName(elem, name) {

    jQuery(elem).toggleClass(name)

}


var Expander = {
  toggle: function (elem, contain, expression) {
    var e = typeof (elem) === 'string' ? jQuery('#' + elem) : jQuery(elem)
    if (e.length < 1) {
      return;
    }
    var content = typeof(contain)==='string'?jQuery('#' + contain):contain?jQuery(contain):[]

    var holder;
    var icnh;
    if (content.length < 1) {
      holder = e.closest(".expandComponentHolder")
      if (holder) {
        content = holder.find(".expandComponent").first()
        icnh = holder.find(".expandComponentControl").first()
      }
    } else {
      if (e.hasClass('expandComponentControl')) {
        icnh = e;
      }
      if (e.hasClass('expandComponentHolder')) {
        holder = e;
        if (!icnh) {
          icnh = holder.find(".expandComponentControl").first()
        }
      } else {
        holder = e.closest(".expandComponentHolder")
      }
    }
    var value = false;
    if (content.length) {
      value = !content.is(':visible')
    } else if (icnh) {
      var icn = icnh.find('.glyphicon').first()
      if (icn) {
        value = icn.hasClass('glyphicon-chevron-down')
      }
    }
    Expander.setOpen(elem, contain, value, expression);
    return value;
  },
  setOpen: function (elem, contain, value, expression) {
    var e = typeof (elem) === 'string' ? jQuery('#' + elem) : jQuery(elem)
    if (e.length < 1) {
      return;
    }
    var content = typeof(contain)==='string'?jQuery('#' + contain):contain?jQuery(contain):[]
    var holder = []
    var icnh = []
    if (content.length < 1) {
      holder = e.closest(".expandComponentHolder")
      if (holder.length) {
        content = holder.find(".expandComponent").first()
        icnh = holder.find(".expandComponentControl").first()
      }
    }
    if (!holder.length || !icnh.length) {
      if (e.hasClass('expandComponentControl')) {
        icnh = e;
      }
      if (e.hasClass('expandComponentHolder')) {
        holder = e;
        if (icnh.length) {
          icnh = holder.find(".expandComponentControl").first()
        }
      } else {
        holder = e.closest(".expandComponentHolder")
      }
    }
    if ( content.length) {
      if (value) {
        content.show()
      } else {
        content.hide()
      }
      if (null != expression) {
        //also set open related expression match
        jQuery(expression).each(function (i, e) {
          if (value) {
            jQuery(e).show()
          } else {
            jQuery(e).hide()
          }
        });
      }
    }
    if (holder.length) {
      if (value) {
        holder.addClass("expanded")
      } else {
        holder.removeClass("expanded")
      }
    } else if (icnh.length && content.length) {
      if (value) {
        icnh.addClass("expanded")
        icnh.removeClass("closed")
      } else {
        icnh.removeClass("expanded")
        icnh.addClass("closed")
      }
    }
    if (icnh.length) {
      var icn = icnh.find('.glyphicon')
      if (icn.length) {
        if (value) {
          icn.addClass('glyphicon-chevron-down')
          icn.removeClass('glyphicon-chevron-right')
        } else {
          icn.addClass('glyphicon-chevron-right')
          icn.removeClass('glyphicon-chevron-down')
        }
      }
    }
  },
  open: function (elem, contain) {
    Expander.setOpen(elem, contain, true);
  },
  close: function (elem, contain) {
    Expander.setOpen(elem, contain, false);
  }
};

function _isIe(version) {
  return typeof (Prototype) === 'object' && Prototype.Browser.IE && $$('html')[0].hasClassName('ie' + version);
}


function stopEvent (e) {
  if (e.preventDefault) {
    e.preventDefault()
    e.stopPropagation()
  } else {
    e.returnValue = false
    e.cancelBubble = true
  }
}

/**
 * keypress handler which disallows Return key
 * @param e event
 */
function noenter(e) {
  if (e && e.keyCode == Event.KEY_RETURN) {

    stopEvent(e)
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
    stopEvent(e)
  }
  return !(e && kCode != 0 && chars.indexOf(String.fromCharCode(kCode)) >= 0);
}

function _applyAce(e, height) {
  if (_isIe(8) || _isIe(7) || _isIe(6)) {
    return;
  }
  jQuery(e).width( "100%")
      .height( height != null ? height : "200px");

  jQuery(e).addClass('ace_editor');
  var editor = ace.edit(generateId(e));
  editor.setTheme("ace/theme/" + (jQuery(e).data('aceSessionTheme') || 'chrome'));
  editor.getSession().setMode("ace/mode/" + (jQuery(e).data('aceSessionMode') || 'sh'));
  editor.setReadOnly(true);
}

function _setupMarkdeepPreviewTab(tabid, id, getter) {
  "use strict";
  jQuery('#' + tabid).on('show.bs.tab', function () {
    //load markdeep preview

    var el = jQuery('#' + id);
    el.text('Loading...');
    window.markdeep.format(getter() + '\n', true, function (t) {
      jQuery(el).html(t);
    });
  });
}
var _ace_modes = [
  //add more mode files in rundeckapp/web-app/js/ace
  "batchfile",
  "diff",
  "dockerfile",
  "golang",
  "groovy",
  "html",
  "java",
  "javscript",
  "json",
  "markdown",
  "perl",
  "php",
  "powershell",
  "properties",
  "python",
  "ruby",
  "sh",
  "sql",
  "xml",
  "yaml"
];

function getAceSyntaxMode(aceeditor) {
  "use strict";
  return aceeditor.getSession().getMode().$id.split('/')[2];
}

function setAceSyntaxMode(mode, aceeditor) {
  var allowedMode = _ace_modes.indexOf(mode) >= 0 ? mode : null;
  aceeditor.getSession().setMode("ace/mode/" + (allowedMode || 'sh'));
}

function _setupAceTextareaEditor(textarea, callback, autoCompleter) {
  if (_isIe(8) || _isIe(7) || _isIe(6)) {
    return;
  }
  jQuery(textarea).hide();
  var _shadow = jQuery('<div></div>');
  var data = jQuery(textarea).data();
  var width = data.aceWidth ? data.aceWidth : "100%";
  var height = data.aceHeight ? data.aceHeight : "560px";
  _shadow.css({
      width: width,
      height: height
    })
    .addClass('ace_text')
    .text(jQuery(textarea).val())
    .insertBefore(textarea);

  //create editor
  var editor = ace.edit(generateId(_shadow));
  editor.$blockScrolling = Infinity;

  var checkResize;
  if (data.aceResizeMax) {
    var heightPx = parseInt(height.replace(/px$/, ''));
    var lineheight = editor.renderer.lineHeight;
    var checkSize = Math.floor(heightPx / lineheight);
    var checkSizeMax = parseInt(data.aceResizeMax);
    if (checkSize < checkSizeMax) {

      var checkSizeInc = 5;
      var pxInc = lineheight;
      checkResize = function (editor) {
        "use strict";
        var lineCount = editor.session.getLength();
        if (lineCount > checkSize && checkSize < checkSizeMax) {
          var diff = Math.min(checkSizeMax - checkSize, lineCount - checkSize);
          var increment = Math.max(checkSizeInc, diff);
          checkSize += increment;
          heightPx += increment * pxInc;
          _shadow.css({
            height: heightPx + 'px'
          });
          editor.resize();
        }
      };

      if (data.aceResizeAuto) {
        checkResize(editor);
      }
    }
  }

  setAceSyntaxMode(data.aceSessionMode, editor);
  editor.setTheme("ace/theme/chrome");
  editor.getSession().on('change', function (e) {
    jQuery(textarea).val(editor.getValue());
    if (callback) {
      callback(editor);
    }
    if (checkResize) {
      checkResize(editor);
    }
  });
  if (data.aceAutofocus) {
    editor.focus();
  }

  //add controls
  var addSoftWrapCheckbox = data.aceControlSoftWrap ? data.aceControlSoftWrap : false;
  if (addSoftWrapCheckbox) {
    var _soft = jQuery('<input/>')
      .attr('type', 'checkbox')
      .on('change', function (e) {
        editor.getSession().setUseWrapMode(this.checked);
      });
    var _soft_label = jQuery('<label></label>')
      .append('Soft Wrap');
    var _ctrls = jQuery('<div></div>')
      .addClass('checkbox ace_text_controls')
      .append(_soft)
      .append(_soft_label)
      .insertBefore(_shadow);
  }

  //add syntax dropdown
  var addSyntaxSelect = data.aceControlSyntax ? data.aceControlSyntax : false;
  if (addSyntaxSelect) {
    var allowSyntaxModes = data.aceAllowedSyntaxModes ? data.aceAllowedSyntaxModes.split(',') : [];
    var sel = jQuery('<select/>')
      .addClass('form-control')
      .on('change', function (e) {
        setAceSyntaxMode(jQuery(this).val(), editor);
      });
    sel.append(jQuery('<option/>').attr('value', '-').text('-None-'));
    for (var i = 0; i < _ace_modes.length; i++) {
      var mode = _ace_modes[i];
      if(allowSyntaxModes.length>0 && allowSyntaxModes.indexOf(mode)<0){
        continue
      }
      var option = jQuery('<option/>').attr('value', mode).text(mode);
      sel.append(option);
      if (mode == data.aceSessionMode) {
        option.attr('selected', 'true');
      }
    }
    var label = jQuery('<label></label>')
      .append('Syntax Mode: ')
      .append(sel);
    var wrap = jQuery('<div></div>')
      .addClass('ace_text_controls form-inline')
      .append(label)
      .insertBefore(_shadow);
  }
  if (autoCompleter) {
    var langTools = ace.require("ace/ext/language_tools");
    var lang = ace.require("ace/lib/lang");
    editor.setOptions({
      enableBasicAutocompletion: true,
      enableLiveAutocompletion: true
    });
    var extCompleter = {
      identifierRegexps: [
        /[@%a-zA-Z_0-9\.\$\-\u00A2-\uFFFF]/
      ],
      getCompletions: function (editor, session, pos, prefix, callback) {
        if (prefix.length === 0) {
          callback(null, []);
          return
        }
        callback(null, autoCompleter(editor, session, pos, prefix));
      },
      getDocTooltip: function (item) {
        if (item.type == "rdvar" && !item.docHTML && item.title) {
          item.docHTML = [
            "<b>", lang.escapeHTML(item.title || ''), "</b>", "<hr></hr>",
            lang.escapeHTML(item.desc || '')
          ].join("");
        }
      }
    };
    langTools.addCompleter(extCompleter);
  }
  return editor;
}
/**
 * Return true if the event is a keycode for a control key
 * @param e
 * @returns {boolean}
 */
function controlkeycode(e) {
  var keycodes = [
    Event.KEY_BACKSPACE, Event.KEY_DELETE, Event.KEY_TAB, Event.KEY_RETURN, Event.KEY_ESC, Event.KEY_PAGEDOWN,
    Event.KEY_PAGEUP, Event.KEY_END, Event.KEY_HOME, Event.KEY_INSERT, Event.KEY_LEFT, Event.KEY_RIGHT,
    Event.KEY_DOWN, Event.KEY_UP
  ];
  if (e.keyCode && keycodes.indexOf(e.keyCode) >= 0) {
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
    stopEvent(e)
  }
  return !(e && kCode != 0 && !String.fromCharCode(kCode).match(regex));
}

function fireWhenReady(elem, func) {
  if (jQuery('#' + elem).size() > 0) {
    func();
  } else {
    jQuery(document).ready(func);
  }
}

/**
 * Generate a URL query string
 * @param params
 * @returns {string}
 * @private
 */
function _genUrlQuery (params) {
  var urlparams = []
  if (typeof (params) == 'string') {
    urlparams = [params]
  } else if (typeof (params) == 'object') {
    for (var e in params) {
      urlparams.push(encodeURIComponent(e) + "=" + encodeURIComponent(params[e]))
    }
  }
  return urlparams.join("&")
}
/**
 * Generate a URL
 * @param url
 * @param params
 * @returns {string}
 * @private
 */
function _genUrl(url, params) {
  let paramString = _genUrlQuery(params)
  return url + (paramString.length ? ((url.indexOf('?') > 0 ? '&' : '?') + paramString) : '')
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
function _pageLink(url, params, text, css, behavior) {
  var a = new Element('a');
  a.href = _genUrl(url, params)
  setText(a, text);
  a.addClassName(css);

  Event.observe(a, 'click', function (evt) {
    if (behavior && !behavior(a, params)) {
      evt.preventDefault();
    }
  });
  return a;
}

function totalPageCount(max, total) {
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
 * @param options optional behavior configuration, {maxsteps: 10} the maximum number of page links to show, others will
 *     be skipped and a "skipped:true" page will be passed instead
 * @param func function called with paging parameters: {offset:number,
 *      prevPage: true/false,
 *      nextPage: true/false,
 *      currentPage: true/false,
 *      page: number,
 *      disabled: true/false,
 *      skipped: true/false
 *      }
 */
function foreachPage(offset, max, total, options, func) {
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
  if (typeof (options) == 'function') {
    func = options;
  } else if (options) {
    jQuery.extend(opts, options);
  }
  var pages = totalPageCount(max, total);
  var curpage = Math.floor(offset / max) + 1;

  //calculate starting page given a window for maximum number of links to show
  var leftwindow = Math.floor(opts.maxsteps / 2);

  var startpage = curpage - leftwindow;

  if (startpage + opts.maxsteps > pages) {
    startpage = pages - opts.maxsteps;
  }

  if (startpage < 0) {
    startpage = 0;
  }

  //determine indicators for skipped steps
  var skipbefore = startpage > 0;
  var skipafter = startpage + opts.maxsteps < pages;

  //previous
  func({
    offset: (offset - max),
    page: curpage - 1,
    prevPage: true,
    disabled: curpage <= 1,
    max: max
  });
  //if skipping before curpage
  if (skipbefore) {
    func({
      skipped: true,
      disabled: true,
      max: max
    });
  }


  //generate intermediate pages
  for (var i = startpage;
    (i - startpage) < opts.maxsteps && (max * i) < total; i++) {
    //page
    func({
      offset: (max * i),
      currentPage: (i + 1 == curpage),
      page: i + 1,
      normal: true,
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
    page: curpage + 1,
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
function paginate(elem, offset, total, max, options) {
  var e = $(elem);
  if (!e) {
    return;
  }
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
    //message text
    'paginate.next': 'Next',
    'paginate.prev': 'Previous',
    'paginate.ff': '»',
    'paginate.rew': '«',
    //css classes
    nextClass: '',
    prevClass: '',
    stepClass: '',
    currentStepClass: 'active',
    //url parameter names
    offsetParam: 'offset',
    maxParam: 'max',
    //variables
    maxsteps: 10,
    insertion: 'bottom',
    behavior: null,
    ulCss: 'pagination pagination-sm'
  };
  if (options) {
    jQuery.extend(opts, options);
  }
  if (!opts.baseUrl) {
    return;
  }
  var pages = Math.floor(total / max);
  if (pages != (total / max)) {
    pages += 1;
  }
  var curpage = Math.floor(offset / max) + 1;
  var page = new Element('ul');
  page.addClassName(opts.ulCss);

  //generate paginate links
  var firststep = 1;
  while (curpage - firststep >= opts.maxsteps) {
    //shift window
    firststep += opts.maxsteps;
  }
  console.log("total, offset, curpage,firststep,maxsteps", total, offset, curpage, firststep, opts.maxsteps);
  var a;
  var li;

  if (firststep > 1) {
    //elipsis
    a = _pageLink(opts.baseUrl, {
      offset: max * (firststep - 2),
      max: max
    }, opts['paginate.rew'], opts['prevClass'], opts.stepBehavior);
    li = new Element('li');
    li.appendChild(a);
    page.appendChild(li);
  }
  li = new Element('li');
  if (curpage > firststep || firststep > 1) {
    //previous
    a = _pageLink(opts.baseUrl, {
      offset: (offset - max),
      max: max
    }, opts['paginate.prev'], opts['prevClass'], opts.prevBehavior);
  } else {
    a = new Element('span');
    setText(a, opts['paginate.prev']);
    li.addClassName('disabled');
  }
  li.appendChild(a);
  page.appendChild(li);
  //generate intermediate pages
  for (var i = 0; i < opts.maxsteps && (max * (i + firststep - 1)) < total; i++) {
    a = _pageLink(opts.baseUrl, {
      offset: max * (i + firststep - 1),
      max: max
    }, firststep + i, opts['stepClass'], opts.stepBehavior);
    li = new Element('li');
    if (i + firststep == curpage) {
      li.addClassName(opts['currentStepClass']);
    }
    li.appendChild(a);
    page.appendChild(li);
  }

  li = new Element('li');
  if (offset < total - max) {
    //next
    a = _pageLink(opts.baseUrl, {
      offset: (offset + max),
      max: max
    }, opts['paginate.next'], opts['nextClass'], opts.nextBehavior);
  } else {
    a = new Element('span');
    setText(a, opts['paginate.next']);
    li.addClassName('disabled');
  }
  li.appendChild(a);
  page.appendChild(li);
  if (pages >= firststep + opts.maxsteps) {
    //elipsis

    a = _pageLink(opts.baseUrl, {
      offset: max * (firststep + opts.maxsteps - 1),
      max: max
    }, opts['paginate.ff'], opts['nextClass'], opts.stepBehavior);
    li = new Element('li');
    li.appendChild(a);
    page.appendChild(li);
  }

  var insert = {};
  insert[opts.insertion] = page;
  clearHtml(e);
  e.insert(insert);
}


/**
 * jQuery/bootstrap utility functions
 */
function _initPopoverContentRef(parent, options) {
  var sel = '[data-toggle=popover][data-popover-content-ref]';
  var result;
  options = options || {};
  if (options['element']) {
    result = jQuery(options['element']);
  } else if (typeof (parent) == 'string') {
    result = jQuery(parent + ' ' + sel);
  } else if (typeof (parent) == 'undefined') {
    result = jQuery(sel);
  } else if (typeof (parent) == 'object') {
    result = jQuery(parent).find(sel);
  }
  result.each(function (i, e) {
    if ('true' == jQuery(e).data('popover-content-ref-inited')) {
      //init only once
      return;
    }
    var ref = jQuery(e).data('popover-content-ref') || options.contentRef;
    var opts = {
      html: true,
      content: function () {
        const id = generateId()
        const html = jQuery(ref).html()
        return html.replace(/\$CREF\$/g, id)
      },
      trigger: jQuery(e).data('trigger') || options.trigger || 'click'
    };
    var templateClass = jQuery(e).data('popover-template-class') || options.templateClass;
    if (templateClass) {
      opts.template = jQuery.fn.popover.Constructor.DEFAULTS.template.replace(
        /class="popover"/,
        "class=\"popover " + templateClass + "\""
      );
    }
    jQuery(e).popover(opts).on('shown.bs.popover', function () {
      jQuery(e).toggleClass('active');
      if (typeof (options.onShown) === 'function') {
        options.onShown(e)
      }
    }).on('hidden.bs.popover', function () {
      jQuery(e).toggleClass('active');
      if (typeof (options.onHidden) === 'function') {
        options.onHidden(e)
      }
    });
    jQuery(e).data('popover-content-ref-inited', 'true');
  });
}
/**
 * jQuery/bootstrap utility functions
 */
function _initPopoverContentFor(parent, options) {
  var sel = '[data-toggle=popover-for]';
  var result;
  options = options || {};
  if (options['element']) {
    result = jQuery(options['element']);
  } else if (typeof (parent) == 'string') {
    result = jQuery(parent + ' ' + sel);
  } else if (typeof (parent) == 'undefined') {
    result = jQuery(sel);
  } else if (typeof (parent) == 'object') {
    result = jQuery(parent).find(sel);
  }
  result.each(function (i, e) {
    var ref = jQuery(e).data('target') || e.href() || options.target;
    var found = jQuery(ref);
    jQuery(e).on(found.data('trigger') || options.trigger || 'click', function () {
      found.popover('toggle');
    });
    found.on('shown.bs.popover', function () {
      jQuery(e).toggleClass('active');
    }).on('hidden.bs.popover', function () {
      jQuery(e).toggleClass('active');
    });
  });
}

/** page init */
function _initAffix() {
  //affixed elements
  jQuery("a[href='#top']").click(function () {
    jQuery("#main-panel").animate({
      scrollTop: 0
    }, "slow");
    return false;
  });
  jQuery("a[href='#bottom']").click(function () {
    //window.scrollTo(0, document.documentElement.scrollHeight || document.body.scrollHeight);
    var body = jQuery("#main-panel")
    body.animate({
      scrollTop: body[0].scrollHeight
    }, "fast");
    return false;
  });
  jQuery('[data-affix=top]').each(function (i, e) {
    var padd = jQuery(e).data('affix-padding-top');
    var top = jQuery(e).offset().top - (padd ? padd : 0);
    jQuery(e).affix({
      offset: {
        top: top
      }
    });
    jQuery(e).closest('[data-affix=wrap]').height(jQuery(e).height());
  });
}
/** fix placeholder text for IE8 */
function _initIEPlaceholder() {
  if (typeof (Prototype) !== 'object' || !Prototype.Browser.IE) {
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

function _initCollapseExpander() {
  jQuery(document).on('show.bs.collapse', '.collapse.collapse-expandable', function (e) {
    var elem = jQuery(this);
    var hrefs = jQuery('[data-toggle=collapse][href=\'#' + elem.attr('id') + '\']')
      .addClass('active')
      .children('.glyphicon')
      .removeClass('glyphicon-chevron-right')
      .addClass('glyphicon-chevron-down');
    var hrefs = jQuery('[data-toggle=collapse][data-target=\'#' + elem.attr('id') + '\']')
      .addClass('active')
      .children('.glyphicon')
      .removeClass('glyphicon-chevron-right')
      .addClass('glyphicon-chevron-down');
  });
  jQuery(document).on('hide.bs.collapse', '.in.collapse-expandable', function (e) {
    var elem = jQuery(this);
    var hrefs = jQuery('[data-toggle=collapse][href=\'#' + elem.attr('id') + '\']')
      .removeClass('active')
      .children('.glyphicon')
      .removeClass('glyphicon-chevron-down')
      .addClass('glyphicon-chevron-right');
    var hrefs = jQuery('[data-toggle=collapse][data-target=\'#' + elem.attr('id') + '\']')
      .removeClass('active')
      .children('.glyphicon')
      .removeClass('glyphicon-chevron-down')
      .addClass('glyphicon-chevron-right');
  });
}

function _toggleAnsiColor(e) {
  var test = jQuery(this).find('input')[0].checked;
  _setAnsiColor(test)
}
function _setAnsiColor(test){
  var ansicolor = jQuery('.ansicolor');
  if (!test) {
    ansicolor.removeClass('ansicolor-on');
  } else {
    ansicolor.addClass('ansicolor-on');
  }
}

function _initAnsiToggle() {
  jQuery('.ansi-color-toggle').on('change', _toggleAnsiColor);
  jQuery('.nodes_run_content').on('change', '.ansi-color-toggle', _toggleAnsiColor);
}
/**
 * Create a beforeSend ajax handler to include request tokens in ajax request. The tokens are either read from
 * data stored in the dom on the element with given id, by the _ajaxReceiveTokens, or by loading json text
 * embedded int the body of the element.
 * @param id id of embedded token json script element
 * @returns {boolean}
 * @private
 */
function _createAjaxSendTokensHandler (id) {
  return function (jqxhr, settings) {
    return _ajaxSendTokens(id, jqxhr, settings)
  }
}

/**
 * Use as a beforeSend ajax handler to include request tokens in ajax request. The tokens are either read from
 * data stored in the dom on the element with given id, by the _ajaxReceiveTokens, or by loading json text
 * embedded int the body of the element.
 * @param id id of embedded token json script element
 * @param jqxhr
 * @param settings
 * @returns {boolean}
 * @private
 */
function _ajaxSendTokens(id, jqxhr, settings) {
  if (window._rundeck && window._rundeck.token) {
    jqxhr.setRequestHeader('X-RUNDECK-TOKEN-KEY', window._rundeck.token.TOKEN);
    jqxhr.setRequestHeader('X-RUNDECK-TOKEN-URI', window._rundeck.token.URI);
    return true;
  } else {
    var elem = jQuery('#' + id);
    var data = {};
    if (elem && elem.data('rundeck-token-key') && elem.data('rundeck-token-uri')) {
      data = {
        TOKEN: elem.data('rundeck-token-key'),
        URI: elem.data('rundeck-token-uri')
      };
    } else {
      data = loadJsonData(id);
      clearHtml(document.getElementById(id));
    }
    if (data && data.TOKEN && data.URI) {
      jqxhr.setRequestHeader('X-RUNDECK-TOKEN-KEY', data.TOKEN);
      jqxhr.setRequestHeader('X-RUNDECK-TOKEN-URI', data.URI);
    }
    return true;
  }

}
/**
 * Create a ajaxSuccess event handler for ajax requests, to replace request tokens for an element in the dom.
 * @param id
 * @private
 */
function _createAjaxReceiveTokensHandler (id) {
  return function (data, status, jqxhr) {
    return _ajaxReceiveTokens(id, data, status, jqxhr);
  }
}

/**
 * Use as a ajaxSuccess event handler for ajax requests, to replace request tokens for an element in the dom.
 * @param id
 * @param data
 * @param status
 * @param jqxhr
 * @private
 */
function _ajaxReceiveTokens(id, data, status, jqxhr) {
  var elem = jQuery('#' + id);
  if (jqxhr.getResponseHeader('X-RUNDECK-TOKEN-KEY') && jqxhr.getResponseHeader('X-RUNDECK-TOKEN-URI')) {
    try {
      elem.data('rundeck-token-key', jqxhr.getResponseHeader('X-RUNDECK-TOKEN-KEY'));
      elem.data('rundeck-token-uri', jqxhr.getResponseHeader('X-RUNDECK-TOKEN-URI'));
    } catch (e) {}
  }
}

function _initTokenRefresh() {
  jQuery(document).ajaxComplete(function (evt, xhr, opts) {
    if (xhr.getResponseHeader('X-RUNDECK-TOKEN-KEY') && xhr.getResponseHeader('X-RUNDECK-TOKEN-URI')) {
      try {
        jQuery('#SYNCHRONIZER_TOKEN').val(xhr.getResponseHeader('X-RUNDECK-TOKEN-KEY'));
        jQuery('#SYNCHRONIZER_URI').val(xhr.getResponseHeader('X-RUNDECK-TOKEN-URI'));
      } catch (e) {

      }
    }
  });
}
/**
 * Strip text up to first line with '---', return the rest
 * @param text
 * @private
 */
function _jobDescriptionRunbook(text) {
  return text.replace(/^(.|[\r\n])*?(\r\n|\n)---(\r\n|\n)/, '');
}

function _hasJobDescriptionRunbook(text) {
  "use strict";

  return text != _jobDescriptionRunbook(text);
}
/**
 * Apply markdeep formatting to contents of an element
 * @param el
 * @private
 */
function _applyMarkdeep(el) {
  "use strict";
  if (typeof (window.markdeep) != 'undefined') {
    var text = jQuery(el).text();
    jQuery(el).text('Loading...');
    window.markdeep.format(text + '\n', true, function (t, err) {
      if (!err) {
        jQuery(el).html(t);
      } else {
        jQuery(el).text('');
        jQuery(el).append(jQuery('<pre><code></code></pre>').text(text));
      }
    });
  } else {
    console.log("Markdeep was not loaded");
  }
}
/**
 * Sanitize HTML content
 * @param t content
 * @param callback called with (true/false,sanitizedcontent, errmsg)
 * @returns {*} promise
 * @private
 */
function _remoteSanitizeHTML(t, callback) {
  "use strict";
  return jQuery.ajax({
    url: appLinks.scheduledExecutionSanitizeHtml,
    method: 'POST',
    dataType: 'json',
    contentType: 'application/json',
    data: JSON.stringify({
      content: t
    }),
    success: function (data, res) {
      callback(true, data.content);
    },
    error: function (jqxhr, resp, err) {
      callback(false, null, err);
    }
  });
}
/**
 * Initialize markdeep and automatically apply to .markdeep elements
 * replaces window.markdeep.format with asynchronous version for sanitizing
 * @private
 */
function _initMarkdeep() {
  if (typeof (window.markdeep) != 'undefined') {
    var orig = window.markdeep;
    window.markdeep = Object.freeze({
      format: function (t, e, c) {
        "use strict";
        _remoteSanitizeHTML(orig.format(t, e), function (suc, sanitized, err) {
          if (suc) {
            c(sanitized);
          } else {
            console.log("Error: could not sanitize content: " + err);
            c(t, 'Failed to sanitize content');
          }
        });

      },
      formatDiagram: orig.formatDiagram,
      stylesheet: orig.stylesheet
    });
    jQuery('.markdeep').each(function (i, el) {
      "use strict";
      _applyMarkdeep(el);
    });
  }
}

function _initPopoverMousedownCatch (sel, allowed, callback) {
  jQuery(sel || 'body').on('mousedown', function (e) {
    if (jQuery(e.target).closest(allowed).length < 1) {

      jQuery(sel || 'body').off('mousedown')
      if (typeof (callback) === 'function') {
        callback(e)
      }
    }
  })
}
function _initStopPropagationOnClick(){
  jQuery('body').on('click',function(event){
    let closest = jQuery(event.target).closest('[data-click-stop-propagation]')
    if(closest.length>0){
      event.stopPropagation()
    }
  });
}

/**
 * Add timeZone url parameter to href from moment tz guess
 * @private
 */
function _initTZParamGuess () {
  if (typeof (moment) === 'function' && typeof (moment.tz) !== 'undefined') {
    let tz = moment.tz.guess()
    jQuery('a._guess_tz_param').each(function () {
      let anchor = jQuery(this)
      let param = anchor.data('tzUrlParam') || 'timeZone'
      let href = anchor.attr('href')
      if (href.indexOf(param + '=') < 0) {
        anchor.attr('href', _genUrl(href, {[param]: tz}))
      }
    })
  }
}

/**
 * set moment locale from meta tag
 * @private
 */
function _initMomentLocale () {
  if (typeof (moment) === 'function') {
    let m = jQuery('html').attr('lang')
    if (m) {
      moment.locale(m)
    }
  }
}
(function () {
  window.markdeepOptions = {
    mode: 'script',
    detectMath: false
  };
  if (typeof (jQuery) == 'function') {
    jQuery.ajaxSetup({
      headers: {
        'x-rundeck-ajax': 'true'
      }
    });
    jQuery(document).ready(function () {
      jQuery.support.transition = false;
      _initMomentLocale()
      jQuery('.has_tooltip').tooltip({html: true});
      jQuery('.has_popover').popover({});
      _initPopoverContentRef();
      _initPopoverContentFor();
      _initAffix();
      _initIEPlaceholder();
      _initCollapseExpander();
      _initAnsiToggle();
      _initMarkdeep();
      _initStopPropagationOnClick();
      _initTZParamGuess()
    });
  }
})();


var updateNowRunning = function (count) {
  var nrtitle = "Now Running (" + count + ")";
  if ($('nowrunninglink')) {
    setText($('nowrunninglink'), nrtitle);
  }
  $$('.nowrunningcount').each(function (e) {
    setText(e, "(" + count + ")");
  });
  if (typeof (_pageUpdateNowRunning) === "function") {
    _pageUpdateNowRunning(count);
  }
};
var _setLoading = function (element, text) {
  element = $(element);
  if (null === text || typeof (text) == 'undefined') {
    text = "Loading…";
  }
  if (element.tagName === 'TBODY') {
    var tr = new Element('tr');
    var td = new Element('td');
    tr.appendChild(td);
    element.appendChild(tr);

    var sp = new Element('span');
    sp.addClassName('loading');
    var img = new Element('img');
    img.src = appLinks.iconSpinner;
    $(sp).appendChild(img);
    appendText(sp, ' ' + text);
    td.appendChild(sp);
  } else {
    var sp = new Element('span');
    sp.addClassName('loading');
    var img = new Element('i');
    img.addClassName('fas fa-spinner fa-pulse')
    $(sp).appendChild(img);
    appendText(sp, ' ' + text);
    clearHtml(element);
    element.appendChild(sp);
  }
  return element;
};

if (typeof (Prototype) === 'object') {
  Element.addMethods({
    loading: _setLoading
  });
}
/** node filter preview code */

function _updateMatchedNodes(data, elem, project, localnodeonly, inparams, callback, errcallback) {
  var i;
  if (!project) {
    return;
  }
  var params = jQuery.extend({
    view: 'embed',
    declarenone: true,
    fullresults: true
  }, data);
  if (null !== inparams) {
    jQuery.extend(params, inparams);
  }
  if (localnodeonly) {
    params.localNodeOnly = 'true';
  }

  if (typeof (data.nodeExcludePrecedence) == 'string' && data.nodeExcludePrecedence === "false" ||
    typeof (data.nodeExcludePrecedence) == 'boolean' && !data.nodeExcludePrecedence) {
    params.nodeExcludePrecedence = "false";
  } else {
    params.nodeExcludePrecedence = "true";
  }
  jQuery('#' + elem).load(_genUrl(appLinks.frameworkNodesFragment, params), function (response, status, xhr) {
    jQuery('#' + elem).removeClass('depress');
    if (status == 'success') {
      if (typeof (callback) == 'function') {
        callback(xhr);
      }
    } else if (typeof (errcallback) == 'function') {
      errcallback(response, status, xhr);
    }
  });
}

//set box filterselections
function setFilter(name, value, callback) {
  if (!value) {
    value = "!";
  }
  if (null === callback) {
    callback = _setFilterSuccess;
  }
  var str = name + "=" + value;
  return jQuery.ajax({
    type: 'POST',
    url: _genUrl(appLinks.userAddFilterPref, {
      filterpref: str
    }),
    beforeSend: _createAjaxSendTokensHandler('filter_select_tokens'),
    success: function (data, status, jqxhr) {
      if (typeof (callback) === 'function') {
        callback(data, name);
      } else if (typeof (_setFilterSuccess) == 'function') {
        try {
          _setFilterSuccess(data, name);
        } catch (e) {}
      }
    }
  }).success(_createAjaxReceiveTokensHandler('filter_select_tokens'));
}
var generateId = (function () {
  var counter = 0;
  return function (elem) {
    var j = elem ? jQuery(elem) : null;
    if (j && j.attr('id')) {
      return j.attr('id');
    }
    var id = '_id' + (counter++);
    if (j) {
      j.attr('id', id);
    }
    return id;
  }
})();

function _loadMessages(id) {
  if (typeof (window.Messages) != 'object') {
    window.Messages = {};
  }
  jQuery.extend(window.Messages, loadJsonData(id));
}
/**
 * expand i18n message template
 * @param template template of the form "text {0} {1} ..." with placeholders numbered from 0
 * @param data substitution data, an array, a scalar, or an object containing 'value' entry
 * @param pluralize if true, treat the template as two templates "singular|plural" separated by | and use the plural
 *     template if data value {0} is not '1'
 *
 * @returns {*|string|XML|void}
 */
function messageTemplate(template, data, pluralize) {
  "use strict";
  var pluralTemplate = null;
  if (pluralize) {
    var arr = template.split('|');
    template = arr[0];
    pluralTemplate = arr[1];
  }
  var values = [];
  if (typeof (data) != 'object') {
    values = [data];
  } else if (jQuery.isArray(data)) {
    values = data;
  } else if (typeof (data) == 'object') {
    values = data['value'];
    if (!jQuery.isArray(values)) {
      values = [values];
    }
  }
  for (var i = 0; i < values.length; i++) {
    if (typeof (values[i]) == 'function') {
      values[i] = values[i]();
    }
  }
  if (pluralize && values[0] != 1) {
    template = pluralTemplate;
  }
  var text = template.replace(/\{(\d+)\}/g, function (match, g1, offset, string) {
    var val = parseInt(g1);
    if (val >= 0 && val < values.length) {
      return values[val];
    } else {
      return string;
    }
  });
  return text;
}
/**
 * Returns the i18n message for the given code, or the code itself if message is not found.  Requires
 * calling the "g:jsMessages" tag from the taglib to define messages.
 * @param code
 * @param args template argument values
 * @returns {*}
 */
function message(code, args) {
  if (typeof (window.Messages) == 'object') {
    var msg = Messages[code];
    if (!msg) {
      if (typeof (_messageMissingError) == 'function') {
        _messageMissingError("Message not found: " + code);
      }
    }
    return msg ? args ? messageTemplate(msg, args) : msg : code;
  } else {
    if (typeof (_messageMissingError) == 'function') {
      _messageMissingError("Message not found: " + code);
    }
    return code;
  }
}

/**
 * jquery highlight
 */
jQuery.fn.highlight = function (speed) {
  jQuery(this).each(function () {
    var el = jQuery(this);
    el.before("<div/>");
    el.prev()
      .width(el.width())
      .height(el.height())
      .css({
        "position": "absolute",
        "background-color": "#ffff99",
        "opacity": ".9"
      })
      .fadeOut(speed || 500);
  });
};

/**
 * jquery scroll to element
 */
jQuery.fn.scrollTo = function (speed) {
  jQuery(this).each(function () {
    var el = jQuery(this);
    jQuery('html, body').animate({
      scrollTop: el.offset().top
    }, speed || 1000);
  });
};

/**
 * Extract form data
 * @param selected
 * @param rmprefixes Array of form field name prefixes to remove
 * @param reqprefixes Array of form field name prefixes to require (only fields with these prefixes will be serialized)
 * @returns {{}}
 */
function jQueryFormData(selected, rmprefixes,reqprefixes, rmkeyprefixes) {
  const data = {};
  selected.find('input, textarea, select').each(function (n, el) {
    let name = jQuery(el).attr('name');
    const attr = jQuery(el).attr('type');
    if ((attr === 'checkbox' || attr === 'radio') && !el.checked) {
      return;
    }
    if(name) {
      if(reqprefixes){
        if (!ko.utils.arrayFirst(reqprefixes, function (el) {
          return name.startsWith(el);
        })) {
          return;
        }
      }
      if(rmkeyprefixes){
        if (ko.utils.arrayFirst(rmkeyprefixes, function (el) {
          return name.startsWith(el);
        })) {
          return;
        }
      }
      if(rmprefixes) {
        rmprefixes.forEach(function (val) {
          if (name.startsWith(val)) {
            name = name.substring(val.length);
          }
        });
      }
      if (data[name] && typeof(data[name]) === 'string') {
        data[name] = [data[name], jQuery(el).val()];
      } else if (data[name] && jQuery.isArray(data[name])) {
        data[name].push(jQuery(el).val());
      } else {
        data[name] = jQuery(el).val();
      }
    }

  });
  return data
}
