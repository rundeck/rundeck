/**
  markdeep.js
  Version 0.12

  Copyright 2015-2016, Morgan McGuire, http://casual-effects.com
  All rights reserved.

  -------------------------------------------------------------

  See http://casual-effects.com/markdeep for documentation on how to
  use this script make your plain text documents render beautifully
  in web browsers.

  Markdeep was created by Morgan McGuire. It extends the work of:

   - John Gruber's original Markdown
   - Ben Hollis' Maruku Markdown dialect
   - Michel Fortin's Markdown Extras dialect
   - Ivan Sagalaev's highlight.js
   - Contributors to the above open source projects

  -------------------------------------------------------------
 
  You may use, extend, and redistribute this code under the terms of
  the BSD license at https://opensource.org/licenses/BSD-2-Clause.

  and highlight.js(https://github.com/isagalaev/highlight.js) by Ivan
  Sagalaev, which is used for code highlighting. Each has their
  respective license with them.
*/
/**
 See http://casual-effects.com/markdeep for @license and documentation.

 markdeep.min.js version 0.12
 Copyright 2015-2016, Morgan McGuire 
 All rights reserved.
 (BSD 2-clause license)

 highlight.min.js 8.8.0 from https://highlightjs.org/
 Copyright 2006, Ivan Sagalaev
 All rights reserved.
 (BSD 3-clause license)
*/
(function() {
'use strict';

// For minification. This is admittedly scary.
var _ = String.prototype;
_.rp = _.replace;
_.ss = _.substring;

// Regular expression version of String.indexOf
_.regexIndexOf = function(regex, startpos) {
    var i = this.ss(startpos || 0).search(regex);
    return (i >= 0) ? (i + (startpos || 0)) : i;
}

/** Enable for debugging to view character bounds in diagrams */
var DEBUG_SHOW_GRID = false;

/** Overlay the non-empty characters of the original source in diagrams */
var DEBUG_SHOW_SOURCE = DEBUG_SHOW_GRID;

/** Use to suppress passing through text in diagrams */
var DEBUG_HIDE_PASSTHROUGH = DEBUG_SHOW_SOURCE;

/** In pixels of lines in diagrams */
var STROKE_WIDTH = 2;

/** A box of these denotes a diagram */
var DIAGRAM_MARKER = '*';

// http://stackoverflow.com/questions/1877475/repeat-character-n-times
var DIAGRAM_START = Array(5 + 1).join(DIAGRAM_MARKER);

/** attribs are optional */
function entag(tag, content, attribs) {
    return '<' + tag + (attribs ? ' ' + attribs : '') + '>' + content + '</' + tag + '>';
}


function measureFontSize(fontStack) {
    var canvas = document.createElement('canvas');
    var ctx = canvas.getContext('2d');
    ctx.font = '10pt ' + fontStack;
    return ctx.measureText("M").width;
}


var codeFontStack = "Menlo,\'Lucida Console\',monospace";
var codeFontSize  = 105.1316178 / measureFontSize(codeFontStack) + 'px';

//console.log(codeFontSize);

var BODY_STYLESHEET = entag('style', 'body{max-width:680px;' +
    'margin:auto;' +
    'padding:20px;' +
    'text-align:justify;' +
    'line-height:140%; ' +
    '-webkit-font-smoothing:antialiased;-moz-osx-font-smoothing:grayscale;font-smoothing:antialiased;' +
    'color:#222;' +
    'font-family:Palatino,Georgia,"Times New Roman",serif;}');

/** You can embed your own stylesheet AFTER the <script> tags in your
    file to override these defaults. */
var STYLESHEET = entag('style',
    'body{' +
    'counter-reset: h1 h2 h3 h4 h5 h6;' +
    '}' +
                       
    '.md code,pre{' +
    'font-family:' + codeFontStack + ';' +
    'font-size:' + codeFontSize + ';' +
    'line-height:140%;' + 
    '}' +

    '.md div.title{' +
    'font-size:26px;' +
    'font-weight:800;' +
    'padding-bottom:5px;' +
    'line-height:120%;' +
    'text-align:center;' +
    '}' +

    '.md div.afterTitles{height:10px;}' +

    '.md div.subtitle{' +
    'text-align:center;' +
    '}' +
    '.md .image{display:inline-block}' +
    '.md div.imagecaption,.md div.tablecaption,.md div.listingcaption{' +
    'margin:0.2em 0 10px 0;' +
    'font-style:italic;' +
    '}' +

    '.md div.imagecaption{' +
    'margin-bottom:0px' +
    '}' +

    '.md img{' +
    'max-width:100%;' +
    '}' +

     // Justification tends to handle URLs and code blocks poorly
     // when inside of a bullet, so disable it there
    'li{text-align:left};' +

     // Force captions on line listings down close and then center them
    '.md div.tilde{' +
    'margin:20px 0 -10px 0;' +
    'text-align:center;' + 
    '}' +

    '.md blockquote.fancyquote{' + 
    'margin-top:25px;' +
    'margin-bottom:25px;' +
    'text-align:left;' +
    'line-height:160%;' +
    '}' +

    '.md blockquote.fancyquote::before{' +
    'content: "“";' +
    'color:#DDD;' +
    'font-family:Times New Roman;' +
    'font-size: 45px;' +
    'line-height: 0;' +
    'margin-right: 6px;' +
    'vertical-align: -0.3em;' +
    '}' +

    '.md span.fancyquote{' +
    'font-size:118%;' +
    'color:#777;' +
    'font-style:italic;' +
    '}' +

    '.md span.fancyquote::after{' +
    'content: "”";' +
    'font-style:normal;' +
    'color:#DDD;' +
    'font-family:Times New Roman;' +
    'font-size: 45px;' +
    'line-height: 0;' +
    'margin-left: 6px;' +
    'vertical-align: -0.3em;' +
    '}' +

    '.md blockquote.fancyquote .author{' +
    'width:100%;' +
    'margin-top: 10px;' + 
    'display:inline-block;' +
    'text-align:right;' +
    '}' +

    '.md small{font-size:60%;}' +

    '.md div.title,contents,.md .tocHeader,h1,h2,h3,h4,h5,h6,.md .shortTOC,.md .mediumTOC{' +
    'font-family:Verdana,Helvetica,Arial,sans-serif;' +
    '}' +

    '.md svg.diagram{' +
    'display:block;' +
    'font-family:' + codeFontStack + ';' +
    'font-size:' + codeFontSize + ';' +
    'text-align:center;' +
    'stroke-linecap:round;' +
    'stroke-width:' + STROKE_WIDTH + 'px;'+
    'stroke:#000;fill:#000;' +
    '}' +

    '.md svg.diagram .opendot{' +
    'fill:#FFF' +
    '}' +

    '.md svg.diagram text{' +
    'stroke:none;' +
    '}' +

    '.md a:link.url{font-family:Georgia,Palatino,\'Times New Roman\';}' +

    'h1,.tocHeader{' +
    'padding-bottom:3px;' +
    'padding-top:15px;' +
    'border-bottom:3px solid;' +
    'border-top:none;' +
    'font-size:20px;' +
    'font-weight:bold;' +
    'clear:both;' +
    '}' +

    'h1{' +
    'counter-reset: h2 h3 h4 h5 h6;' +
    '}' +

    'h2{' +
    'counter-reset: h3 h4 h5 h6;' +
    'font-family:Helvetica,Arial,sans-serif;' +
    'padding-bottom:3px;' +
    'padding-top:15px;' +
    'border-bottom:2px solid #999;' +
    'border-top:none;' +
    'color:#555;' +
    'font-size:18px;' +
    'clear:both;' +
    '}' +

    'h3,h4,h5,h6{' +
    'font-family:Helvetica,Arial,sans-serif;' +
    'padding-bottom:3px;' +
    'padding-top:15px;' +
    'border-top:none;' +
    'color:#555;' +
    'font-size:16px;' +
    'clear:both;' +
    '}' +

    'h3{counter-reset: h4 h5 h6;}' +
    'h4{counter-reset: h5 h6;}' +
    'h5{counter-reset: h6;}' +

    '.md table{' +
    'border-collapse:collapse;' +
    'line-height:140%; ' +
    '}' +

    '.md table.table{' +
    'margin:auto;' +
    '}' +

    '.md table.calendar{' +
    'width:100%;' +
    'margin:auto;' +
    'font-size:11px;' +
    'font-family:Helvetica,Arial,sans-serif;' +
    '}' +

    '.md table.calendar th{' +
    'font-size:16px;' +
    '}' +

    '.md .today{' +
    'background:#ECF8FA;' +
    '}' +

    '.md div.tablecaption{' +
    'text-align: center;' +
    '}' +

    '.md table.table th{' +
    'color:#FFF;' +
    'background-color:#AAA;' +
    'border:1px solid #888;' +
     // top right bottom left
    'padding:8px 15px 8px 15px;' +
    '}' +

    '.md table.table td{' +
     // top right bottom left
    'padding:5px 15px 5px 15px;' +
    'border:1px solid #888;' +
    '}' +

    '.md table.table tr:nth-child(even){'+
    'background:#EEE;' +
    '}' +

    '.md pre.tilde{' +
    'border-top: 1px solid #CCC;' + 
    'border-bottom: 1px solid #CCC;' + 
    'padding: 5px 0 5px 20px;' +
    'margin-bottom: 30px;' +
    'background: #FCFCFC;' +
    '}' +

    '.md a:link, .md a:visited{color:#38A;text-decoration:none;}' +
    '.md a:hover{text-decoration:underline}' +

    '.md dt{' +
    'font-weight:700;' +
    '}' +

    '.md dd{' +
    'padding-bottom:18px;' +
    '}' +

    '.md code{' +
    'white-space:pre;' +
    '}' +

    '.md .endnote{' +
    'font-size:13px;' +
    'line-height:15px;' +
    'padding-left:10px;' +
    'text-indent:-10px;' +
    '}' +

    '.md .bib{' +
    'padding-left:80px;' +
    'text-indent:-80px;' +
    'text-align:left;' +
    '}' +

    '.markdeepFooter{font-size:9px;text-align:right;padding-top:80px;color:#999;}' +

    '.md .mediumTOC{float:right;font-size:12px;line-height:15px;border-left:1px solid #CCC;padding-left:15px;margin:15px 0px 15px 25px;}' +

    '.md .mediumTOC .level1{font-weight:600;}' +

    '.md .longTOC .level1{font-weight:600;display:block;padding-top:12px; margin-bottom:-20px;}' +
     
    '.md .shortTOC{text-align:center;font-weight:bold;margin-top:15px;font-size:14px;}');

var MARKDEEP_LINE = '<!-- Markdeep: --><style class="fallback">body{visibility:hidden;white-space:pre;font-family:monospace}</style><script src="markdeep.min.js"></script><script src="https://casual-effects.com/markdeep/latest/markdeep.min.js"></script><script>window.alreadyProcessedMarkdeep||(document.body.style.visibility="visible")</script>';

var MARKDEEP_FOOTER = '<div class="markdeepFooter"><i>formatted by <a href="http://casual-effects.com/markdeep" style="color:#999">Markdeep&nbsp;&nbsp;&nbsp;</a></i><div style="display:inline-block;font-size:13px;font-family:\'Times New Roman\',serif;vertical-align:middle;transform:translate(-3px,-1px)rotate(135deg);">&#x2712;</div></div>';

var DEFAULT_OPTIONS = {
    mode: 'markdeep',
    detectMath: true
};

var max = Math.max;
var min = Math.min;
var sign = Math.sign || function (x) {
    return ( +x === x ) ? ((x === 0) ? x : (x > 0) ? 1 : -1) : NaN;
};

/** Get an option, or return the corresponding value from DEFAULT_OPTIONS */
function option(key) {
    if (window.markdeepOptions && (window.markdeepOptions[key] !== undefined)) {
        return window.markdeepOptions[key];
    } else if (DEFAULT_OPTIONS[key] !== undefined) {
        return DEFAULT_OPTIONS[key];
    } else {
        console.warn('Illegal option: "' + key + '"');
        return undefined;
    }
}

/** Converts <>&" to their HTML escape sequences */
function escapeHTMLEntities(str) {
    return String(str).rp(/&/g, '&amp;').rp(/</g, '&lt;').rp(/>/g, '&gt;').rp(/"/g, '&quot;');
}

/** Restores the original source string's '<' and '>' as entered in
    the document, before the browser processed it as HTML. There is no
    way in an HTML document to distinguish an entity that was entered
    as an entity.
*/
function unescapeHTMLEntities(str) {
    // Process &amp; last so that we don't recursively unescape
    // escaped escape sequences.
    return str.
        rp(/&lt;/g, '<').
        rp(/&gt;/g, '>').
        rp(/&quot;/g, '"').
        rp(/&#39;/g, "'").
        rp(/&ndash;/g, '--').
        rp(/&mdash;/g, '---').
        rp(/&amp;/g, '&');
}


function removeHTMLTags(str) {
    return str.rp(/<.*?>/g, '');
}


/** Turn the argument into a legal URL anchor */
function mangle(text) {
    return encodeURI(text.rp(/\s/g, '').toLowerCase());
}

/** Creates a style sheet containing elements like:

  hn::before { 
    content: counter(h1) "." counter(h2) "." ... counter(hn) " "; 
    counter-increment: hn; 
   } 
*/
function sectionNumberingStylesheet() {
    var s = '';

    for (var i = 1; i <= 6; ++i) {
        s += 'h' + i + '::before {\ncontent:';
        for (var j = 1; j <= i; ++j) {
            s += 'counter(h' + j + ') "' + ((j < i) ? '.' : ' ') + '"';
        }
        s += ';\ncounter-increment: h' + i + ';margin-right:10px}';
    }

    return entag('style', s);
}

/**
   \param node  A node from an HTML DOM

   \return A String that is a very good reconstruction of what the
   original source looked like before the browser tried to correct
   it to legal HTML.
 */
function nodeToMarkdeepSource(node, leaveEscapes) {
    var source = node.innerHTML;

    // Markdown uses <john@bar.com> e-mail syntax, which HTML parsing
    // will try to close by inserting the matching close tags at the end of the
    // document. Remove anything that looks like that and comes *after*
    // the first fallback style.
    source = source.rp(/(?:<style class="fallback">[\s\S]*?<\/style>[\s\S]*)<\/\S+@\S+\.\S+?>/gim, '');
    
    // Remove artificially inserted close tags
    source = source.rp(/<\/h?ttps?:.*>/gi, '');
    
    // Now try to fix the URLs themselves, which will be 
    // transformed like this: <http: casual-effects.com="" markdeep="">
    source = source.rp(/<(https?): (.*?)>/gi, function (match, protocol, list) {

        // Remove any quotes--they wouldn't have been legal in the URL anyway
        var s = '<' + protocol + '://' + list.rp(/=""\s/g, '/');

        if (s.ss(s.length - 3) === '=""') {
            s = s.ss(0, s.length - 3);
        }

        // Remove any lingering quotes (since they
        // wouldn't have been legal in the URL)
        s = s.rp(/"/g, '');

        return s + '>';
    });

    // Remove the "fallback" style tags
    source = source.rp(/<style class=["']fallback["']>.*?<\/style>/gmi, '');

    source = unescapeHTMLEntities(source);

    return source;
}


/** Extracts one diagram from a Markdown string.

    Returns {beforeString, diagramString, alignmentHint, afterString}
    diagramString will be empty if nothing was found. The
    DIAGRAM_MARKER is stripped from the diagramString. 

    alignmentHint may be:
    floatleft  
    floatright
    center
    flushleft

    diagramString does not include the marker characters. 
    If there is a caption, it will appear in the afterString and not be parsed.
*/
function extractDiagram(sourceString) {
    function advance() {
        nextLineBeginning = sourceString.indexOf('\n', lineBeginning) + 1;
        textOnLeft  = textOnLeft  || /\S/.test(sourceString.ss(lineBeginning, lineBeginning + xMin));
        textOnRight = textOnRight || /\S/.test(sourceString.ss(lineBeginning + xMax + 1, nextLineBeginning));
    }

    var noDiagramResult = {beforeString: sourceString, diagramString: '', alignmentHint: '', afterString: ''};

    // Search sourceString for the first rectangle of enclosed
    // DIAGRAM_MARKER characters at least DIAGRAM_START.length wide
    for (var i = sourceString.indexOf(DIAGRAM_START);
         i >= 0; 
         i = sourceString.indexOf(DIAGRAM_START, i + DIAGRAM_START.length)) {

        // Is this a diagram? Try following it around
        
        // Look backwards to find the beginning of the line (or of the string)
        // and measure the start character relative to it
        var lineBeginning = max(0, sourceString.lastIndexOf('\n', i)) + 1;
        var xMin = i - lineBeginning;
        
        // Find the first non-diagram character...or the end of the string
        var j;
        for (j = i + DIAGRAM_START.length; sourceString[j] === DIAGRAM_MARKER; ++j) {}
        var xMax = j - lineBeginning - 1;
        
        // We have a potential hit. Start accumulating a result. If there was anything
        // between the newline and the diagram, move it to the after string for proper alignment.
        var result = {
            beforeString: sourceString.ss(0, lineBeginning), 
            diagramString: '',
            alignmentHint: 'center', 
            afterString: sourceString.ss(lineBeginning, i).rp(/[ \t]+$/, ' ')
        };

        var nextLineBeginning = 0;
        var textOnLeft = false, textOnRight = false;

        advance();
                                    
        // Now, see if the pattern repeats on subsequent lines
        for (var good = true, previousEnding = j; good; ) {
            // Find the next line
            lineBeginning = nextLineBeginning;
            advance();
            if (lineBeginning === 0) {
                // Hit the end of the string before the end of the pattern
                return noDiagramResult; 
            }
            
            if (textOnLeft) {
                // Even if there is text on *both* sides
                result.alignmentHint = 'floatright';
            } else if (textOnRight) {
                result.alignmentHint = 'floatleft';
            }
            
            // See if there are markers at the correct locations on the next line
            if ((sourceString[lineBeginning + xMin] === DIAGRAM_MARKER) && 
                (sourceString[lineBeginning + xMax] === DIAGRAM_MARKER)) {

                // See if there's a complete line of DIAGRAM_MARKER, which would end the diagram
                for (var x = xMin; (x < xMax) && (sourceString[lineBeginning + x] === DIAGRAM_MARKER); ++x) {}
           
                var begin = lineBeginning + xMin;
                var end   = lineBeginning + xMax;

                // Trim any excess whitespace caused by our truncation because Markdown will
                // interpret that as fixed-formatted lines
                result.afterString += sourceString.ss(previousEnding, begin).rp(/^[ \t]*[ \t]/, ' ').rp(/[ \t][ \t]*$/, ' ');
                if (x === xMax) {
                    // We found the last row. Put everything else into
                    // the afterString and return the result.
                
                    result.afterString += sourceString.ss(lineBeginning + xMax + 1);
                    return result;
                } else {
                    // A line of a diagram. Extract everything before
                    // the diagram line started into the string of
                    // content to be placed after the diagram in the
                    // final HTML
                    result.diagramString += sourceString.ss(begin + 1, end) + '\n';
                    previousEnding = end + 1;
                }
            } else {
                // Found an incorrectly delimited line. Abort
                // processing of this potential diagram, which is now
                // known to NOT be a diagram after all.
                good = false;
            }
        } // Iterate over verticals in the potential box
    } // Search for the start

    return noDiagramResult;
}

/** 
    Find the specified delimiterRegExp used as a quote (e.g., *foo*)
    and replace it with the HTML tag and optional attributes.
*/
function replaceMatched(string, delimiterRegExp, tag, attribs) {
    var delimiter = delimiterRegExp.source;
    var flanking = '[^ \\t\\n' + delimiter + ']';
    var pattern  = '(' + delimiter + ')' +
        '(' + flanking + '.*?(\\n.+?)*?)' + 
        delimiter + '(?![A-Za-z0-9])';

    return string.rp(new RegExp(pattern, 'g'), 
                          '<' + tag + (attribs ? ' ' + attribs : '') +
                          '>$2</' + tag + '>');
}
    
/** Maruku ("github")-style table processing */
function replaceTables(s, protect) {
    var TABLE_ROW       = /(?:\n\|?[ \t\S]+?(?:\|[ \t\S]+?)+\|?(?=\n))/.source;
    var TABLE_SEPARATOR = /\n\|? *\:?-+\:?(?: *\| *\:?-+\:?)+ *\|?(?=\n)/.source;
    var TABLE_CAPTION   = /\n[ \t]*\[[^\n\|]+\][ \t]*(?=\n)/.source;
    var TABLE_REGEXP    = new RegExp(TABLE_ROW + TABLE_SEPARATOR + TABLE_ROW + '+(' + TABLE_CAPTION + ')?', 'g');

    function trimTableRowEnds(row) {
        return row.trim().rp(/^\||\|$/g, '');
    }
    
    s = s.rp(TABLE_REGEXP, function (match) {
        // Found a table, actually parse it by rows
        var rowArray = match.split('\n');
        
        var result = '';
        
        // Skip the bogus leading row
        var startRow = (rowArray[0] === '') ? 1 : 0;

        var caption = rowArray[rowArray.length - 1].trim();

        if ((caption.length > 3) && (caption[0] === '[') && (caption[caption.length - 1] === ']')) {
            // Remove the caption from the row array
            rowArray.pop();
            caption = caption.ss(1, caption.length - 1);
        } else {
            caption = undefined;
        }

        // Parse the separator row for left/center/right-indicating colons
        var columnStyle = [];
        trimTableRowEnds(rowArray[startRow + 1]).rp(/:?-+:?/g, function (match) {
            var left = (match[0] === ':');
            var right = (match[match.length - 1] === ':');
            columnStyle.push(protect(' style="text-align:' + ((left && right) ? 'center' : (right ? 'right' : 'left')) + '"'));
        });
        
        var tag = 'th';
        for (var r = startRow; r < rowArray.length; ++r) {
            // Remove leading and trailing whitespace and column delimiters
            var row = trimTableRowEnds(rowArray[r].trim());
            
            result += '<tr>';
            var i = 0;
            result += '<' + tag + columnStyle[0] + '>' + 
                row.rp(/\|/g, function () {
                    ++i;
                    return '</' + tag + '><' + tag + columnStyle[i] + '>';
                }) + '</' + tag + '>';
            
            
            result += '</tr>\n';
            // Skip the header-separator row
            if (r == startRow) { 
                ++r; 
                tag = 'td';
            }
        }
        
        result = entag('table', result, protect('class="table"'));

        if (caption) {
            result = '<div ' + protect('class="tablecaption"') + '>' +  caption + '</div>' + result;
        }

        return result;
    });

    return s;
}


function replaceLists(s, protect) {
    // Identify list blocks:
    // Blank line or line ending in colon, line that starts with 1.,*,+, or -,
    // and then any number of lines until another blank line
    var BLANK_LINES = /^\s*\n/.source;
    
    // Preceding line ending in a colon
    var PREFIX     = /[:,]\s*\n/.source;
    var LIST_BLOCK_REGEXP = 
        new RegExp('(' + PREFIX + '|' + BLANK_LINES + ')' +
                   /((?:[ \t]*(?:\d+\.|-|\+|\*)(?:[ \t]+.+\n\n?)+)+)/.source, 'gm');

    var keepGoing = true;

    var ATTRIBS = {'+': protect('class="plus"'), '-': protect('class="minus"'), '*': protect('class="asterisk"')};
    var NUMBER_ATTRIBS = protect('class="number"');

    // Sometimes the list regexp grabs too much because subsequent
    // lines are indented *less* than the first line. So, if that case
    // is found, re-run the regexp.
    while (keepGoing) {
        keepGoing = false;
        s = s.rp(LIST_BLOCK_REGEXP, function (match, prefix, block) {
            var result = prefix;
            
            // Contains {indentLevel, tag}
            var stack = [];
            var current = {indentLevel: -1};
            
            /* function logStack(stack) {
               var s = '[';
               stack.forEach(function(v) { s += v.indentLevel + ', '; });
               console.log(s.ss(0, s.length - 2) + ']');
               } */
            
            block.split('\n').forEach(function (line) {
                var trimmed     = line.rp(/^\s*/, '');
                
                var indentLevel = line.length - trimmed.length;
                
                // Add a CSS class based on the type of list bullet
                var attribs = ATTRIBS[trimmed[0]];
                var isUnordered = !! attribs; // attribs !== undefined
                attribs = attribs || NUMBER_ATTRIBS;
                var isOrdered   = /^\d+\.[ \t]/.test(trimmed);

                // If not ordered or unordered, we're just looking at a blank line
                
                if (! current) {
                    // Went above top-level indent
                    result += '\n' + line;
                } else if (! isOrdered && ! isUnordered) {
                    // Continued line
                    result += '\n' + current.indentChars + line;
                } else {
                    if (indentLevel !== current.indentLevel) {
                        // Enter or leave indentation level
                        if ((current.indentLevel !== -1) && (indentLevel < current.indentLevel)) {
                            while (current && (indentLevel < current.indentLevel)) {
                                stack.pop();
                                // End the current list and decrease indentation
                                result += '</li></' + current.tag + '>';
                                current = stack[stack.length - 1];
                            }
                        } else {
                            // Start a new list that is more indented
                            current = {indentLevel: indentLevel,
                                       tag:         isOrdered ? 'ol' : 'ul',
                                       indentChars: line.ss(0, indentLevel)};
                            stack.push(current);
                            result += '<' + current.tag + '>';
                        }
                    } else if (current.indentLevel !== -1) {
                        // End previous list item, if there was one
                        result += '</li>';
                    } // Indent level changed
                    
                    if (current) {
                        // Add the list item
                        result += '\n' + current.indentChars + '<li ' + attribs + '>' + trimmed.rp(/^(\d+\.|-|\+|\*) /, '');
                    } else {
                        // Just reached something that is *less* indented than the root--
                        // copy forward and then re-process that list
                        result += '\n' + line;
                        keepGoing = true;
                    }
                }
            }); // For each line

            // Finish the last item and anything else on the stack (if needed)
            for (current = stack.pop(); current; current = stack.pop()) {
                result += '</li></' + current.tag + '>\n';
            }
       
            return result;
        });
    } // while keep going

    return s;
}


/** 
    Identifies schedule lists, which look like:

  date: title
    events

  Where date must contain a day, month, and four-number year and may
  also contain a day of the week.  Note that the date must not be indented
  and the events must be indented.
*/
function replaceScheduleLists(str, protect) {
    // Must open with something other than indentation or a list
    // marker.  There must be a four-digit number somewhere on the
    // line. Exclude lines that begin with an HTML tag...this will
    // avoid parsing headers that have dates in them.
    var BEGINNING = /^(?:[^\|<>\s-\+\*\d].*[12]\d{3}(?!\d).*?|(?:[12]\d{3}(?!\.).*\d.*?)|(?:\d{1,3}(?!\.).*[12]\d{3}(?!\d).*?))/.source;

    // There must be at least one more number in a date, a colon, and then some more text
    var DATE_AND_TITLE = '(' + BEGINNING + '):' + /[ \t]+([^ \t\n].*)\n/.source;

    // The body of the schedule item. It may begin with a blank line and contain
    // multiple paragraphs separated by blank lines...as long as there is indenting
    var EVENTS = /(?:[ \t]*\n)?((?:[ \t]+.+\n(?:[ \t]*\n){0,3})*)/.source;
    var ENTRY = DATE_AND_TITLE + EVENTS;

    var ENTRY_REGEXP = new RegExp(ENTRY, 'gm');

    var rowAttribs = protect('valign="top"');
    var dateTDAttribs = protect('style="width:100px;padding-right:15px" rowspan="2"');
    var eventTDAttribs = protect('style="padding-bottom:25px"');

    var DAY_NAME   = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    var MONTH_NAME = ['jan', 'feb', 'mar', 'apr', 'may', 'jun', 'jul', 'aug', 'sep', 'oct', 'nov', 'dec'];
    var MONTH_FULL_NAME = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

    try {
        var scheduleNumber = 0;
        str = 
            str.rp(new RegExp('(' + ENTRY + '){2,}', 'gm'),
                   function (schedule) {
                       ++scheduleNumber;

                       // Each entry has the form {date:date, title:string, text:string}
                       var entryArray = [];

                       // Now parse the schedule into individual day entries
                       schedule.rp(ENTRY_REGEXP,
                                   function (entry, date, title, events) {
                                       // Remove the day from the date (we'll reconstruct it below). This is actually unnecessary, since we
                                       // explicitly compute the value anyway and the parser is robust to extra characters.
                                       // 
                                       // date = date.rp(/(?:(?:sun|mon|tues|wednes|thurs|fri|satur)day|(?:sun|mon|tue|wed|thu|fri|sat)\.?|(?:su|mo|tu|we|th|fr|sa)),?/gi, '');
                                       
                                       // Parse the date. The
                                       // Javascript Date class's
                                       // parser is useless because it
                                       // is locale dependent
                                       
                                       var year = '', month = '', day = '';
                                       
                                       // DD MM YYYY
                                       var match = date.match(/([0123]?\d)\D+([01]?\d|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\D+([12]\d{3})/i);
                                       
                                       if (match) {
                                           day = match[1]; month = match[2]; year = match[3];
                                       } else {
                                           // YYYY MM DD
                                           match = date.match(/([12]\d{3})\D+([01]?\d|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\D+([0123]?\d)/i); 
                                           if (match) {
                                               day = match[3]; month = match[2]; year = match[1];
                                           } else {
                                               // monthname day year
                                               match = date.match(/(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\D+([0123]?\d)\D+([12]\d{3})/i);
                                               if (match) {
                                                   day = match[2]; month = match[1]; year = match[3];
                                               } else {
                                                   throw "Could not parse date";
                                               }
                                           }
                                       }
                                       
                                       // Reconstruct standardized date format
                                       date = day + ' ' + month + ' ' + year;
                                       
                                       // Detect the month
                                       var monthNumber = parseInt(month) - 1;
                                       if (isNaN(monthNumber)) {
                                           monthNumber = MONTH_NAME.indexOf(month.toLowerCase());
                                       }
                                       
                                       var dateVal = new Date(parseInt(year), monthNumber, parseInt(day));
                                       // Reconstruct the day of the week
                                       date = DAY_NAME[dateVal.getDay()] + '<br/>' + date;

                                       entryArray.push({date: dateVal, 
                                                        title: title,
                                                        text:
                                                        entag('tr',
                                                              entag('td', 
                                                                    '<a ' + protect('name="schedule' + scheduleNumber + '_' + dateVal.getFullYear() + '-' + (dateVal.getMonth() + 1) + '-' + dateVal.getDate() + '"') + '></a>' +
                                                                    date, dateTDAttribs) + 
                                                              entag('td', entag('b', title)), rowAttribs) + 
                                                        entag('tr', entag('td', '\n\n' + events, eventTDAttribs), rowAttribs)});
                                      
                                       return '';
                                   });
                       
                       // Sort by date
                       entryArray.sort(function (a, b) {
                           return a.date.getTime() - b.date.getTime();
                       });

                       var MILLISECONDS_PER_DAY = 1000 * 60 * 60 * 24;
                       var daySpan = (entryArray[entryArray.length - 1].date.getTime() - entryArray[0].date.getTime()) / MILLISECONDS_PER_DAY;
                       
                       var today = new Date();
                       // Move back to midnight
                       today = new Date(today.getFullYear(), today.getMonth(), today.getDate());

                       var calendar = '';
                       // Make a calendar view with links, if suitable
                       if ((daySpan > 14) && (daySpan / entryArray.length < 16)) {
                           var DAY_HEADER_ATTRIBS = protect('colspan="2" width="14%" style="padding-top:5px;text-align:center;font-style:italic"');
                           var DATE_ATTRIBS       = protect('width="1px" height="30px" style="text-align:right;border:1px solid #EEE;border-right:none;"');
                           var FADED_ATTRIBS      = protect('width="1px" height="30px" style="color:#BBB;text-align:right;"');
                           var ENTRY_ATTRIBS      = protect('width="14%" style="border:1px solid #EEE;border-left:none;"');

                           // Find the first day of the first month
                           var date = entryArray[0].date;
                           var index = 0;

                           // Go to the first of the month
                           date = new Date(date.getFullYear(), date.getMonth(), 1);

                           var sameDay = function (d1, d2) {
                               return (Math.abs(d1.getTime() - d2.getTime()) < MILLISECONDS_PER_DAY / 2);
                           }

                           while (date.getTime() < entryArray[entryArray.length - 1].date.getTime()) {

                               // Create the calendar header
                               calendar += '<table ' + protect('class="calendar"') + '>\n' +
                               entag('tr', entag('th', MONTH_FULL_NAME[date.getMonth()] + ' ' + date.getFullYear(), protect('colspan="14"'))) + '<tr>';
                       
                               DAY_NAME.forEach(function (name) {
                                   calendar += entag('td', name, DAY_HEADER_ATTRIBS);
                               });
                               calendar += '</tr>';
                               
                               // Go back into the previous month to reach a Sunday. Check the time at noon
                               // to avoid problems with daylight saving time occuring early in the morning
                               while (new Date(date.getTime() + 12 * 60 * 60 * 1000).getDay() !== 0) { 
                                   date = new Date(date.getTime() - MILLISECONDS_PER_DAY); 
                               }
                               
                               // Insert the days from the previous month
                               if (date.getDate() !== 1) {
                                   calendar += '<tr ' + rowAttribs + '>';
                                   while (date.getDate() !== 1) {
                                       calendar += '<td ' + FADED_ATTRIBS + '>' + date.getDate() + '</td><td>&nbsp;</td>';
                                       date = new Date(date.getTime() + MILLISECONDS_PER_DAY);
                                   }
                               }

                               // Run until the end of the month
                               do {
                                   if (date.getDay() === 0) {
                                       // Sunday, start a row
                                       calendar += '<tr ' + rowAttribs + '>';
                                   }
                                   
                                   // Insert links as needed from entries
                                   // Deal with UTC issues by allowing 1/2 day of error
                                   var entry = entryArray[index];
                                   var attribs = '';
                                   if (sameDay(date, today)) {
                                       attribs = protect(' class="today"');
                                   }
                                   if (entry && sameDay(entry.date, date)) {
                                       calendar += entag('td', entag('b', date.getDate()), DATE_ATTRIBS + attribs) + 
                                           entag('td', 
                                                 entag('a', 
                                                       entry.title, 
                                                       protect('href="#schedule' + scheduleNumber + '_' + date.getFullYear() + '-' + (date.getMonth() + 1) + '-' + date.getDate() + '"')),
                                                 ENTRY_ATTRIBS + attribs);
                                       ++index;
                                   } else {
                                       calendar += '<td ' + DATE_ATTRIBS + attribs + '></a>' + date.getDate() + '</td><td ' + ENTRY_ATTRIBS + attribs + '> &nbsp; </td>';
                                   }
                                   
                                   if (date.getDay() === 6) {
                                       // Saturday, end a row
                                       calendar += '</tr>';
                                   }
                                   
                                   date = new Date(date.getTime() + MILLISECONDS_PER_DAY);
                               } while (date.getDate() > 1);

                               // Finish out the week
                               if (date.getDay() !== 0) {
                                   while (date.getDay() !== 0) {
                                       calendar += '<td ' + FADED_ATTRIBS + '>' + date.getDate() + '</td><td>&nbsp</td>';
                                       date = new Date(date.getTime() + MILLISECONDS_PER_DAY);
                                   }
                                   
                                   calendar += '</tr>';
                               }

                               calendar += '</table><br/>\n';

                               // Go to the first of the (new) month
                               date = new Date(date.getFullYear(), date.getMonth(), 1);
                               
                           } // Until all days covered
                       } // if add calendar

                       // Construct the schedule
                       schedule = '';
                       entryArray.forEach(function (entry) {
                           schedule += entry.text;
                       });

                       return calendar + entag('table', schedule, protect('class="schedule"')) + '\n\n';
                   });
    } catch (ignore) {
        // Maybe this wasn't a schedule after all, since we couldn't parse a date
        console.log(ignore);
    }

    return str;
}


/**
 Term
 :     description, which might be multiple 
       lines and include blanks.

 Next Term

becomes

<dl>
  <dt>Term</dt>
  <dd> description, which might be multiple 
       lines and include blanks.</dd>
  <dt>Next Term</dt>
</dl>
*/
function replaceDefinitionLists(s) {
    var TERM       = /^.+\n:(?=[ \t])/.source;

    // Definition can contain multiple paragraphs
    var DEFINITION = '(\s*\n|[: \t].+\n)+';

    s = s.rp(new RegExp('(' + TERM + DEFINITION + ')+', 'gm'),
             function (block) {

                 // Parse the block
                 var result = '';
                 
                 block.split('\n').forEach(function (line, i) {
                     // What kind of line is this?
                     if (line.trim().length === 0) {
                         // Empty line
                         result += '\n';
                     } else if (! /\s/.test(line[0]) && (line[0] !== ':')) {
                         // Line is not indented: Definition
                         if (i > 0) { result += '</dd>'; }

                         // Leave *two* blanks at the start of a
                         // definition so that subsequent processing
                         // can detect block formatting within it.
                         result += '<dt>\n' + line + '\n</dt>\n<dd>\n\n';
                     } else {
                         // Add the line to the current definition, stripping any single leading ':'
                         if (line[0] === ':') { line = ' ' + line.ss(1); }
                         result += line + '\n';
                     }
                 });

                 return entag('dl', result + '</dd>');
             });

    return s;
}

/** Returns [string, table], where the table maps strings to levels */
function insertTableOfContents(s, protect) {
    // Gather headers for table of contents (TOC). We
    // accumulate a long and short TOC and then choose which
    // to insert at the end.
    var fullTOC = '';
    var shortTOC = '';
    
    // headerCounter[i] is the current counter for header level (i - 1)
    var headerCounter = [0];
    var currentLevel = 0;
    var numAboveLevel1 = 0;

    var table = {};
    s = s.rp(/<h([1-6])>(.*?)<\/h\1>/gi, function (header, level, text) {
        level = parseInt(level)
        text = text.trim();
        // If becoming more nested:
        for (var i = currentLevel; i < level; ++i) { headerCounter[i] = 0; }
        
        // If becoming less nested:
        headerCounter.splice(level, currentLevel - level);
        currentLevel = level;
        
        ++headerCounter[currentLevel - 1];
        
        // Generate a unique name for this element
        var number = headerCounter.join('.');
        var name = 'toc' + number;

        table[removeHTMLTags(text).trim().toLowerCase()] = number;

        // Only insert for the first three levels
        if (level <= 3) {
            // Indent and append (the Array() call generates spaces)
            fullTOC += Array(level).join('&nbsp;&nbsp;') + '<a href="#' + name + '" class="level' + level + '">' + number + '&nbsp; ' + text + '</a><br/>\n';
            
            if (level === 1) {
                shortTOC += ' &middot; <a href="#' + name + '">' + text + '</a>';
            } else {
                ++numAboveLevel1;
            }
        }
        
        return entag('a', '', protect('name="' + name + '"')) + header;
    });

    if (shortTOC.length > 0) {
        // Strip the leading " &middot; "
        shortTOC = shortTOC.ss(10);
    }
    
    var numLevel1 = headerCounter[0];
    var numHeaders = numLevel1 + numAboveLevel1;

    // The location of the first header is indicative of the length of
    // the abstract...as well as where we insert. The first header may be accompanied by
    // <a name> tags, which we want to appear before.
    var firstHeaderLocation = s.regexIndexOf(/((<a\s+\S+><\/a>)\s*)*<h1>/i);

    if (firstHeaderLocation === -1) { firstHeaderLocation = 0; }

    var AFTER_TITLES = '<div class="afterTitles"><\/div>';
    var insertLocation = s.indexOf(AFTER_TITLES);
    if (insertLocation === -1) {
        insertLocation = 0;
    } else {
        insertLocation += AFTER_TITLES.length;
    }


    // Which TOC style should we use?
    var TOC = '';
    if (((numHeaders < 4) && (numLevel1 <= 1)) || (s.length < 2048)) {
        // No TOC; this document is really short
    } else if ((numLevel1 < 7) && (numHeaders / numLevel1 < 2.5)) {
        // We can use the short TOC
        TOC = '<div class="shortTOC">' + shortTOC + '</div>';
    } else if ((firstHeaderLocation === -1) || (firstHeaderLocation / 55 > numHeaders)) {
        // The abstract is long enough to float alongside, and there
        // are not too many levels.        
        // Insert the medium-length TOC floating
        TOC = '<div class="mediumTOC"><center><b>Contents</b></center><p>' + fullTOC + '</p></div>';
    } else {
        // This is a long table of contents or a short abstract
        // Insert a long toc...right before the first header
        insertLocation = firstHeaderLocation;
        TOC = '<div class="longTOC"><div class="tocHeader">Contents</div><p>' + fullTOC + '</p></div>';
    }

    s = s.ss(0, insertLocation) + TOC + s.ss(insertLocation);

    return [s, table];
}


function escapeRegExpCharacters(str) {
    return str.rp(/([\.\[\]\(\)\*\+\?\^\$\\\{\}\|])/g, '\\$1');
}


/** Returns true if there are at least two newlines in each of the arguments */
function isolated(preSpaces, postSpaces) {
    if (preSpaces && postSpaces) {
        preSpaces  = preSpaces.match(/\n/g);
        postSpaces = postSpaces.match(/\n/g);
        return preSpaces && (preSpaces.length > 1) && postSpaces && (postSpaces.length > 1);
    } else {
        return false;
    }
}


/**
    Performs Markdeep processing on str, which must be a string or a
    DOM element.  Returns a string that is the HTML to display for the
    body. The result does not include the header: Markdeep stylesheet
    and script tags for including a math library, or the Markdeep
    signature footer.

    Optional argument elementMode defaults to true. This avoids turning a bold first word into a 
    title or introducing a table of contents. Section captions are unaffected by this argument.
    Set elementMode = false if processing a whole document instead of an internal node.

 */
function markdeepToHTML(str, elementMode) {
    // Map names to the number used for end notes, in the order
    // encountered in the text.
    var endNoteTable = {}, endNoteCount = 0;

    // Reference links
    var referenceLinkTable = {};

    // In the private use area
    var PROTECT_CHARACTER = '\ue010';
    var PROTECT_RADIX     = 36;
    var protectedStringArray = [];

    // Gives 1.7M possible sequences in base 36
    var PROTECT_DIGITS    = 4;

    var PROTECT_REGEXP    = RegExp(PROTECT_CHARACTER + '[0-9a-z]{' + PROTECT_DIGITS + ',' + PROTECT_DIGITS + '}', 'g');

    /** Given an arbitrary string, returns an escaped identifier
        string to temporarily replace it with to prevent Markdeep from
        processing the contents. See expose() */
    function protect(s) {
        var i = (protectedStringArray.push(s) - 1).toString(PROTECT_RADIX);

        while (i.length < PROTECT_DIGITS) {
            i = '0' + i;
        }

        return PROTECT_CHARACTER + i;
    }

    /** Given the escaped identifier string from protect(), returns
        the orginal string. */
    function expose(i) {
        // Strip the escape character and parse, then look up in the
        // dictionary.
        var i = parseInt(i.ss(1), PROTECT_RADIX);
        return protectedStringArray[i];
    }

    /** First-class function to pass to String.replace to protect a
        sequence defined by a regular expression. */
    function protector(match, protectee) {
        return protect(protectee);
    }

    function protectorWithPrefix(match, prefix, protectee) {
        return prefix + protect(protectee);
    }

    function makeHeaderFunc(level) {
        return function (match, header) {
            return '\n<a ' + protect('name="' + mangle(removeHTMLTags(header)) + '"') + 
                '></a>' + entag('h' + level, header) + '\n\n';
        }
    }

    if (elementMode === undefined) { 
        elementMode = true;
    }
    
    if (str.innerHTML !== undefined) {
        str = str.innerHTML;
    }

    // Replace pre-formatted script tags that are used to protect
    // less-than signs, e.g., in std::vector<Value>
    str = str.rp(/<script\s+type\s*=\s*['"]preformatted['"]\s*>([\s\S]*?)<\/script>/gi, '$1');

    function replaceDiagrams(str) {
        var result = extractDiagram(str);
        if (result.diagramString) {
            var CAPTION_REGEXP = /^\n*[ \t]*\[[^\n]+\][ \t]*(?=\n)/;
            //console.log(result.afterString.match(CAPTION_REGEXP));
            result.afterString = result.afterString.rp(CAPTION_REGEXP, function (caption) {
                // Strip whitespace and enclosing brackets from the caption
                caption = caption.trim();
                caption = caption.ss(1, caption.length - 1);
              
                return entag('center', entag('div', caption, protect('class="imagecaption"')));
            });

            var diagramSVG = diagramToSVG(result.diagramString, result.alignmentHint);
            return result.beforeString +
                diagramSVG + '\n' +
                replaceDiagrams(result.afterString);
        } else {
            return str;
        }
    }

    // Prefix a newline so that blocks beginning at the top of the
    // document are processed correctly
    str = '\n\n' + str;

    // CODE FENCES, with styles. Do this before other
    // processing so that their code is protected from further
    // Markdown processing
    var stylizeFence = function (cssClass, symbol) {
        var pattern = new RegExp('\n' + symbol + '{3,}.*\n([\\s\\S]+?)\n' + symbol + '{3,}\n([ \t]*\\[.*\\])?', 'g');
        str = str.rp(pattern, function(match, sourceCode, caption) {
            var result = '\n';
            if (caption) {
                caption = caption.trim();
                result += '<div ' + protect('class="listingcaption ' + cssClass + '"') + '>' + caption.ss(1, caption.length - 1) + '</div>\n';
            }
            var highlighted = hljs.highlightAuto(sourceCode);
            return result + protect(entag('pre', entag('code', highlighted.value), 'class="listing ' + cssClass + '"')) + '\n';
        });
    };
    
    stylizeFence('tilde', '~');
    stylizeFence('backtick', '`');

    // Protect raw <CODE> content
    str = str.rp(/(<code\b.*?<\/code>)/gi, protector);
    
    str = replaceDiagrams(str);

    // Protect SVG blocks (including the ones we just inserted)
    str = str.rp(/<svg( .*?)?>([\s\S]*?)<\/svg>/gi, function (match, attribs, body) {
        return '<svg' + protect(attribs) + '>' + protect(body) + '</svg>';
    });

    // Protect STYLE blocks
    str = str.rp(/<style>([\s\S]*?)<\/style>/gi, function (match, body) {
        return entag('style', protect(body));
    });

    // Protect the very special case of img tags with newlines and
    // breaks in them AND mismatched angle brackets. This happens for
    // gravizo graphs.
    str = str.rp(/<img\s+src=(["'])[\s\S]*?\1\s*>/gi, function (match, quote) {
        // Strip the "<img " and ">", and then protect:
        return "<img " + protect(match.ss(5, match.length - 1)) + ">";
    });

    // INLINE CODE: Surrounded in back ticks on a single line.  Do
    // this before any other processing to protect code blocks
    // from further interference. Don't process back ticks inside
    // of code fences. Allow a single newline, but not wrapping
    // further because that might just pick up quotes used as other
    // punctuation across lines. Explicitly exclude cases where the second
    // quote immediately preceeds a number, e.g., the old `97
    str = str.rp(/(`)(.+?(?:\n.+?)?)`(?!\d)/g, entag('code', '$2'));

    // CODE: Escape angle brackets inside code blocks (including the
    // ones we just introduced), and then protect the blocks
    // themselves
    str = str.rp(/(<code(?: .*?)?>)([\s\S]*?)<\/code>/gi, function (match, open, inlineCode) {
        return protect(open + escapeHTMLEntities(inlineCode) + '</code>');
    });

    // PRE: Protect pre blocks
    str = str.rp(/(<pre\b[\s\S]*?<\/pre>)/gi, protector);

    // Protect raw HTML attributes from processing
    str = str.rp(/(<\w[^ \n<>]*?[ \t]+)(.*?)(?=\/?>)/g, protectorWithPrefix);

    // Temporarily hide $$ MathJax LaTeX blocks from Markdown processing (this must
    // come before single $ block detection below)
    str = str.rp(/(\$\$[\s\S]+?\$\$)/g, protector);

    // Convert LaTeX $ ... $ to MathJax, but verify that this
    // actually looks like math and not just dollar
    // signs. Don't rp double-dollar signs. Do this only
    // outside of protected blocks.
    //
    // Literally: find a non-dollar sign, non-number followed
    // by a dollar sign and a space.  Then, find any number of
    // characters until the same pattern reversed, allowing
    // one punctuation character before the final space. We're
    // trying to exclude things like Canadian 1$ and US $1
    // triggering math mode.

    str = str.rp(/((?:[^\w\d]))\$([ \t][^\$]+?[ \t])\$(?![\w\d])/g, '$1\\($2\\)');

    // Also allow LaTeX of the form $...$ if the close tag is not US$
    // and there are spaces outside of the dollar signs.
    //
    // Test: " $3 or US$2 and 3$, $x$ $y + \n 2x$ or ($z$) $k$. or $2 or $2".match(pattern) = 
    // ["$x$", "$y +  2x$", "$z$", "$k$"];
    str = str.rp(/((?:[^\w\d]))\$(\S(?:[^\$]*?\S(?!US))??)\$(?![\w\d])/g, '$1\\($2\\)');

    // Temporarily hide MathJax LaTeX blocks from Markdown processing
    str = str.rp(/(\\\([\s\S]+?\\\))/g, protector);
    str = str.rp(/(\\begin\{equation\}[\s\S]*?\\end\{equation\})/g, protector);
    str = str.rp(/(\\begin\{eqnarray\}[\s\S]*?\\end\{eqnarray\})/g, protector);
    str = str.rp(/(\\begin\{equation\*\}[\s\S]*?\\end\{equation\*\})/g, protector);

    // Setext-style H1: Text with ======== right under it
    str = str.rp(/(?:^|\n)(.+?)\n[ \t]*={3,}[ \t]*\n/g, makeHeaderFunc(1));
    
    // Setext-style H2: Text with -------- right under it
    str = str.rp(/(?:^|\n)(.+?)\n[ \t]*-{3,}[ \t]*\n/g, makeHeaderFunc(2));

    // ATX-style headers:
    for (var i = 6; i > 0; --i) {
        str = str.rp(new RegExp(/^[ \t]*/.source + '#{' + i + ',' + i +'}(?:[ \t])([^\n#]+)#*[ \t]*\n', 'gm'), 
                 makeHeaderFunc(i));
    }

    // HORIZONTAL RULE: * * *, - - -, _ _ _
    str = str.rp(/\n((?:_[ \t]*){3,}|(?:-[ \t]*){3,}|(?:\*[ \t]*){3,})\s*?\n/g, '\n<hr/>\n');

    var FANCY_QUOTE = protect('class="fancyquote"');

    // FANCY QUOTE in a blockquote:
    // > " .... "
    // >    -- Foo

    str = str.rp(/\n>[ \t]*"(.*(?:\n>.*)*)"[ \t]*(?:\n>[ \t]*)?(\n>[ \t]{2,}\S.*)?\n/g,
                 function (match, quote, author) {
                     return entag('blockquote', 
                                  entag('span',
                                        quote.rp(/\n>/g, '\n'), 
                                        FANCY_QUOTE) + 
                                  (author ? entag('span',
                                                  author.rp(/\n>/g, '\n'),
                                                  protect('class="author"')) : ''),
                                  FANCY_QUOTE);
                });

    // BLOCKQUOTE: > in front of a series of lines
    str = str.rp(/(?:\n>.*){2,}/g, function (match) {
        // Strip the leading '>'
        return entag('blockquote', match.rp(/\n>/g, '\n'));
    });


    // FOOTNOTES/ENDNOTES: [^symbolic name]
    str = str.rp(/\s*\[\^(.*?)\](?!:)/g, function (match, symbolicName) {
        symbolicName = symbolicName.toLowerCase().trim();

        if (! (symbolicName in endNoteTable)) {
            ++endNoteCount;
            endNoteTable[symbolicName] = endNoteCount;
        }

        return '<sup><a ' + protect('href="#endnote-' + symbolicName + '"') + 
            '>' + endNoteTable[symbolicName] + '</a></sup>';
    });

    // CITATIONS: [#symbolicname]
    // The reference:
    str = str.rp(/\[#(.*?)\](?!:)/g, function (match, symbolicName) {
        symbolicName = symbolicName.trim();
        return '[<a ' + protect('href="#citation-' + symbolicName.toLowerCase() + '"') + 
            '>' + symbolicName + '</a>]';
    });

    // The bibliography entry:
    str = str.rp(/\n\[#(.*?)\]:((?:.+?\n?)*)/g, function (match, symbolicName, entry) {
        symbolicName = symbolicName.trim();
        return '<div ' + protect('class="bib"') + '>[<a ' + protect('name="citation-' + symbolicName.toLowerCase() + '"') + 
            '></a><b>' + symbolicName + '</b>] ' + entry + '</div>';
    });

    // TABLES: line with | over line containing only | and -
    // (process before reference links to avoid ambiguity on the captions)
    str = replaceTables(str, protect);

    // REFERENCE-LINKS: [foo][] or [bar][foo] + [foo]: http://foo.com
    str = str.rp(/^\[([^\^#].*?)\]:(.*?)$/gm, function (match, symbolicName, url) {
        referenceLinkTable[symbolicName.toLowerCase().trim()] = url.trim();
        return '';
    });

    // E-MAIL ADDRESS: <foo@bar.baz> or foo@bar.baz
    str = str.rp(/(?:<|(?!<)\b)(\S+@(\S+\.)+?\S{3,}?)(?:$|>|(?=<)|(?=\s)(?!>))/g, function (match, addr) {
        return '<a ' + protect('href="mailto:' + addr + '"') + '>' + addr + '</a>';
    });

    // Common code for formatting images with and without a caption
    var formatImage = function (ignore, url, attribs) {
        // Detect videos
        attribs = attribs || '';
        var img;
        var hash;

        if (/(.mp4|.m4v|.avi|.mpg|.mov)$/i.test(url)) {
            // This is video. Any attributes provided will override the defaults given here
            img = '<video ' + protect('class="markdeep" src="' + url + '"' + attribs + ' width="480px" controls="true"') + '/>';
        } else if (hash = url.match(/^https:\/\/(?:www\.)?youtube.com\/\S*?v=([\w\d-]+)(&.*)?$/i)) {
            // Youtube video
            img = '<iframe ' + protect('class="markdeep" src="https://www.youtube.com/embed/' + hash[1] + '"' + attribs + ' width="480px" height="300px" frameborder="0" allowfullscreen webkitallowfullscreen mozallowfullscreen') + '></iframe>';
        } else if (hash = url.match(/^https:\/\/(?:www\.)?vimeo.com\/\S*?\/([\w\d-]+)$/i)) {
            // Vimeo video
            img = '<iframe ' + protect('class="markdeep" src="https://player.vimeo.com/video/' + hash[1] + '"' + attribs + ' width="480px" height="300px" frameborder="0" allowfullscreen webkitallowfullscreen mozallowfullscreen') + '></iframe>';
        } else {
            // Image
            img = '<img ' + protect('class="markdeep" src="' + url + '"' + attribs) + '/>';

            // Check for width or height (or max-width and max-height). If they exist,
            // link this to the full-size image as well.
            if (/\b(width|height)\b/i.test(attribs)) {
                img = entag('a ', img, protect('href="' + url + '" target="_blank"'));
            }
        }

        return img;
    };

    // Process links before images so that captions can contain links

    // Detect gravizo URLs inside of markdown images and protect them, 
    // which will cause them to be parsed sort-of reasonably. This is
    // a really special case needed to handle the newlines and potential
    // nested parentheses. Use the pattern from http://blog.stevenlevithan.com/archives/regex-recursion
    // (could be extended to multiple nested parens if needed)
    str = str.rp(/\(http:\/\/g.gravizo.com\/g\?((?:[^\(\)]|\([^\(\)]*\))*)\)/gi, function(match, url) {
        return "(http://g.gravizo.com/g?" + encodeURIComponent(url) + ")";
    });

    // LINKS: [text](url)
    str = str.rp(/(^|[^!])\[([^\[\]]+?)\]\(([^\)]+?)\)/g, function (match, pre, text, url) {
        return pre + '<a ' + protect('href="' + url + '"') + '>' + text + '</a>';
    });

    // EMPTY LINKS: [](url)
    str = str.rp(/(^|[^!])\[[ \t]*?\]\(([^\)]+?)\)/g, function (match, pre, url) {
        return pre + '<a ' + protect('href="' + url + '"') + '>' + url + '</a>';
    });

    // SIMPLE IMAGE: ![](url attribs)
    str = str.rp(/(\s*)!\[\]\(([^\)\s]+)([^\)]*?)?\)(\s*)/g, function (match, preSpaces, url, attribs, postSpaces) {
        var img = formatImage(match, url, attribs);

        if (isolated(preSpaces, postSpaces)) {
            // In a block by itself: center
            img = entag('center', img);
        }

        return preSpaces + img + postSpaces;
    });


    // Explicit loop so that the output will be re-processed, preserving spaces between blocks.
    // Note that there is intentionally no global flag on the first regexp since we only want
    // to process the first occurance.
    var loop = true;
    while (loop) {
        loop = false;
        // CAPTIONED IMAGE: ![caption](url attribs)
        str = str.rp(/(\s*)!\[([\s\S]+?)?\]\(([^\)\s]+)([^\)]*?)?\)(\s*)/, function (match, preSpaces, caption, url, attribs, postSpaces) {
            loop = true;
            var divStyle = '';
            
            if (attribs) {
                // Move any width attribute specification to the box itself
                attribs = attribs.rp(/((?:max-)?width)\s*:\s*[^;'"]*/g, function (attribMatch, attrib) {
                    divStyle = attribMatch + ';';
                    return attrib + ':100%';
                });
                
                // Move any width style specification to the box itself
                attribs = attribs.rp(/((?:max-)?width)\s*=\s*('\S+?'|"\S+?")/g, function (attribMatch, attrib, expr) {
                    // Strip the quotes
                    divStyle = attrib + ':' + expr.ss(1, expr.length - 1) + ';';
                    return 'style="width:100%" ';
                });
            }
            
            var img = formatImage(match, url, attribs);
            
            if (isolated(preSpaces, postSpaces)) {
                // In its own block: center
                preSpaces += '<center>';
                postSpaces = '</center>' + postSpaces;
            } else {
                // Embedded: float
                divStyle += 'float:right;margin:4px 0px 0px 25px;'
            }
            
            return preSpaces + 
                entag('div', img + entag('div', caption, protect('class="imagecaption"')),
                      protect('class="image" style="' + divStyle + '"')) + 
                postSpaces;
        });
    } // while replacements made


    // Process these after links, so that URLs with underscores and tildes are protected.

    // STRONG: Must run before italic, since they use the
    // same symbols. **b** __b__
    str = replaceMatched(str, /\*\*/, 'strong', protect('class="asterisk"'));
    str = replaceMatched(str, /__/, 'strong', protect('class="underscore"'));

    // EM (ITALICS): *i* _i_
    str = replaceMatched(str, /\*/, 'em', protect('class="asterisk"'));
    str = replaceMatched(str, /_/, 'em', protect('class="underscore"'));
    
    // STRIKETHROUGH: ~~text~~
    str = str.rp(/\~\~([^~].*?)\~\~/g, entag('del', '$1'));

    // SMART DOUBLE QUOTES: "a -> &ldquo;   z"  -> &rdquo;
    // Allow situations such as "foo"==>"bar" and foo:"bar", but not 3' 9"
    str = str.rp(/(^|[ \t->])(")(?=\w)/gm, '$1&ldquo;');
    str = str.rp(/([A-Za-z\.,:;\?!=<])(")(?=$|\W)/gm, '$1&rdquo;');
    
    // ARROWS:
    str = str.rp(/(\s)==>(\s)/g, '$1&rarr;$2');
    str = str.rp(/(\s)<==(\s)/g, '$1&larr;$2');

    // EM DASH: ---
    // (exclude things that look like table delimiters!)
    str = str.rp(/([^-!\:\|])---([^->\:\|])/g, '$1&mdash;$2');

    // EN DASH: --
    // (exclude things that look like table delimiters!)
    str = str.rp(/([^-!\:\|])--([^->\:\|])/g, '$1&ndash;$2');

    // NUMBER x NUMBER:
    str = str.rp(/(\d+\s?)x(\s?\d+)/g, '$1&times;$2');

    // MINUS: -4 or 2 - 1
    str = str.rp(/([\s\(\[<\|])-(\d)/g, '$1&minus;$2');
    str = str.rp(/(\d) - (\d)/g, '$1 &minus; $2');

    // EXPONENTS: ^1 ^-1 (no decimal places allowed)
    str = str.rp(/\^([-+]?\d+)\b/g, '<sup>$1</sup>');

    // SCHEDULE LISTS: date : title followed by indented content
    str = replaceScheduleLists(str, protect);

    // DEFINITION LISTS: Word followed by a colon list
    // Use <dl><dt>term</dt><dd>definition</dd></dl>
    // https://developer.mozilla.org/en-US/docs/Web/HTML/Element/dl
    //
    // Process these before lists so that lists within definition lists
    // work correctly
    str = replaceDefinitionLists(str);

    // LISTS: lines with -, +, *, or 1.
    str = replaceLists(str, protect);

    // DEGREE: ##-degree
    str = str.rp(/(\d+?)[ \t-]degree(?:s?)/g, '$1&deg;');

    // PARAGRAPH: Newline, any amount of space, newline
    str = str.rp(/\n[\s\n]*?\n/g, '\n\n</p><p>\n\n');

    // Reference links
    str = str.rp(/\[(.+?)\]\[(.*?)\]/g, function (match, text, symbolicName) {
        // Empty symbolic name is replaced by the text
        if (! symbolicName.trim()) {
            symbolicName = text;
        }
        
        symbolicName = symbolicName.toLowerCase().trim();
        return '<a ' + protect('href="' + referenceLinkTable[symbolicName] + '"') + '>' + text + '</a>';
    });


    // End notes
    str = str.rp(/\n\[\^(.*?)\]:((?:.+?\n?)*)/g, function (match, symbolicName, note) {
        symbolicName = symbolicName.toLowerCase().trim();
        
        if (symbolicName in endNoteTable) {
            return '\n<div ' + protect('class="endnote"') + '><a ' + 
                protect('name="endnote-' + symbolicName + '"') + 
                '></a><sup>' + endNoteTable[symbolicName] + '</sup> ' + note + '</div>';
        } else {
            return "\n";
        }
    });
    

    // SECTION LINKS: XXX section, XXX subsection.
    // Do this by rediscovering the headers and then recursively
    // searching for links to them. Process after other
    // forms of links to avoid ambiguity.
    
    var allHeaders = str.match(/<h([1-6])>(.*?)<\/h\1>/gi);
    if (allHeaders) {
        allHeaders.forEach(function (header) {
            header = removeHTMLTags(header.ss(4, header.length - 5)).trim();
            var link = '<a ' + protect('href="#' + mangle(header) + '"') + '>';
            // Search for links to this section
            str = str.rp(RegExp("(" + escapeRegExpCharacters(header) + ")(?=\\ssubsection|\\ssection)", 'gi'),
                         link + "$1</a>"); 
        });
    }

    // TABLE, LISTING, and FIGURE LABEL NUMBERING: Figure [symbol]:  Table [symbol]:  Listing [symbol]
    var refCounter = {};
    // refTable[ref] = number to link to
    var refTable = {};
    str = str.rp(/($|>)\s*(figure|table|listing)\s+\[(.+?)\]:/gim, function (match, prefix, type, ref) {
        type = type.toLowerCase();
        // Increment the counter
        var count = refCounter[type] = (refCounter[type] | 0) + 1;
        var ref = type + '_' + mangle(ref.toLowerCase().trim());

        // Store the reference number
        refTable[ref] = count;
        
        return prefix + entag('a', '', protect('name="' + ref + '"')) + entag('b', type[0].toUpperCase() + type.ss(1) + '&nbsp;' + count + ':', protect('style="font-style:normal;"'));
    });

    // FIGURE, TABLE, and LISTING references:
    str = str.rp(/\b(figure|fig\.|table|tbl\.|listing|lst.)\s+\[(.+?)\]/gi, function (match, _type, ref) {
        // Fix abbreviations
        var type = _type.toLowerCase();
        switch (type) {
        case 'fig.': type = 'figure'; break;
        case 'tbl.': type = 'table'; break;
        case 'lst.': type = 'listing'; break;
        }

        // Clean up the reference
        var ref = type + '_' + mangle(ref.toLowerCase().trim());
        var num = refTable[ref];
        if (num) {
            return '<a ' + protect('href="#' + ref + '"') + '>' + _type + '&nbsp;' + num + '</a>';
        } else {
            return _type + ' ?';
        }
    });

    // URL: <http://baz> or http://baz
    // Must be detected after [link]() processing 
    str = str.rp(/(?:<|(?!<)\b)(\w{3,6}:\/\/.+?)(?:$|>|(?=<)|(?=\s)(?!<))/g, function (match, url) {
        return '<a ' + protect('href="' + url + '" class="url"') + '>' + url + '</a>';
    });

    if (! elementMode) {
        var TITLE_PATTERN = /^\s*(?:<\/p><p>\s*)<strong.*?>([^ \t\*].*?[^ \t\*])<\/strong>[ \t]*\n/.source;
        
        var ALL_SUBTITLES_PATTERN = /([ {4,}\t][ \t]*\S.*\n)*/.source;
        
        // Detect a bold first line and make it into a title; detect indented lines
        // below it and make them subtitles
        str = str.rp(
            new RegExp(TITLE_PATTERN + ALL_SUBTITLES_PATTERN, 'g'),
            function (match, title) {
                title = title.trim();

                // rp + RegExp won't give us the full list of
                // subtitles, only the last one. So, we have to
                // re-process match.
                var subtitles = match.ss(match.indexOf('\n', match.indexOf('</strong>')));
                subtitles = subtitles ? subtitles.rp(/[ \t]*(\S.*?)\n/g, '<div class="subtitle"> $1 </div>\n') : '';
                
                // Remove all tags from the title when inside the <TITLE> tag
                return entag('title', removeHTMLTags(title)) + '<div class="title"> ' + title + 
                    ' </div>\n' + subtitles + '<div class="afterTitles"></div>\n';
            });
    } // if ! noTitles

    // Remove any bogus leading close-paragraph tag inserted by our extra newlines
    str = str.rp(/^\s*<\/p>/, '');

    if (! elementMode) {
        var temp = insertTableOfContents(str, protect);
        str = temp[0];
        var toc = temp[1];
        // SECTION LINKS: Replace sec. [X], section [X], subsection [X]
        str = str.rp(/\b(sec\.|section|subsection)\s\[(.+?)\]/gi, 
                    function (match, prefix, ref) {
                        var link = toc[ref.toLowerCase().trim()];
                        if (link) {
                            return prefix + '  <a ' + protect('href="#toc' + link + '"') + '>' + link + '</a>';  
                        } else {
                            return prefix + ' ?';
                        }
                    });
    }

    // Expose all protected values. We may need to do this
    // recursively, because pre and code blocks can be nested.
    while (str.indexOf(PROTECT_CHARACTER) + 1) {
        str = str.rp(PROTECT_REGEXP, expose);
    }

    return '<section class="md">' + entag('p', str) + '</section>';
}


/**
   Adds whitespace at the end of each line of str, so that all lines
   have equal length
*/
function equalizeLineLengths(str) {
    var lineArray = str.split('\n');
    var longest = 0;
    lineArray.forEach(function(line) {
        longest = max(longest, line.length);
    });

    // Worst case spaces needed for equalizing lengths
    // http://stackoverflow.com/questions/1877475/repeat-character-n-times
    var spaces = Array(longest + 1).join(' ');

    var result = '';
    lineArray.forEach(function(line) {
        // Append the needed number of spaces onto each line, and
        // reconstruct the output with newlines
        result += line + spaces.ss(line.length) + '\n';
    });

    return result;
}

/** Finds the longest common whitespace prefix of all non-empty lines
    and then removes it */
function removeLeadingSpace(str) {
    var lineArray = str.split('\n');

    var minimum = Infinity;
    lineArray.forEach(function (line) {
        if (line.trim() !== '') {
            // This is a non-empty line
            var spaceArray = line.match(/^([ \t]*)/);
            if (spaceArray) {
                minimum = min(minimum, spaceArray[0].length);
            }
        }
    });

    if (minimum === 0) {
        // No leading space
        return str;
    }

    var result = '';
    lineArray.forEach(function(line) {
        // Strip the common spaces
        result += line.ss(minimum) + '\n';
    });

    return result;
}

/** Returns true if this character is a "letter" under the ASCII definition */
function isASCIILetter(c) {
    var code = c.charCodeAt(0);
    return ((code >= 65) && (code <= 90)) || ((code >= 97) && (code <= 122));
}

/** Converts diagramString, which is a Markdeep diagram without the
    surrounding asterisks, to SVG (HTML). 

    alignmentHint is the float alignment desired for the SVG tag,
    which can be 'floatleft', 'floatright', or ''
 */
function diagramToSVG(diagramString, alignmentHint) {
    // Clean up diagramString if line endings are ragged
    diagramString = equalizeLineLengths(diagramString);

    // Temporarily replace 'o' that is surrounded by other text
    // with another character to avoid processing it as a point 
    // decoration. This will be replaced in the final svg and is
    // faster than checking each neighborhood each time.
    var HIDE_O = '\ue004';
    diagramString = diagramString.rp(/([a-z]|[A-Z])o([a-z]|[A-Z])/g, '$1' + HIDE_O + '$2');

    /** Pixels per character */
    var SCALE   = 8;

    /** Multiply Y coordinates by this when generating the final SVG
        result to account for the aspect ratio of text files. This
        MUST be 2 */
    var ASPECT = 2;

    var DIAGONAL_ANGLE = Math.atan(1.0 / ASPECT) * 180 / Math.PI;

    var EPSILON = 1e-6;

    // The order of the following is based on rotation angles
    // and is used for ArrowSet.toSVG
    var ARROW_HEAD_CHARACTERS            = '>v<^';
    var POINT_CHARACTERS                 = 'o*';
    var JUMP_CHARACTERS                  = '()';
    var UNDIRECTED_VERTEX_CHARACTERS     = "+";
    var VERTEX_CHARACTERS                = UNDIRECTED_VERTEX_CHARACTERS + ".'";

    // GRAY[i] is the Unicode block character for (i+1)/4 level gray
    var GRAY_CHARACTERS = '\u2591\u2592\u2593\u2594\u2589';

    // TRI[i] is a right-triangle rotated by 90*i
    var TRI_CHARACTERS  = '\u25E2\u25E3\u25E4\u25E5';

    var DECORATION_CHARACTERS            = ARROW_HEAD_CHARACTERS + POINT_CHARACTERS + JUMP_CHARACTERS + GRAY_CHARACTERS + TRI_CHARACTERS;

    function isUndirectedVertex(c) { return UNDIRECTED_VERTEX_CHARACTERS.indexOf(c) + 1; }
    function isVertex(c)           { return VERTEX_CHARACTERS.indexOf(c) !== -1; }
    function isTopVertex(c)        { return isUndirectedVertex(c) || (c === '.'); }
    function isBottomVertex(c)     { return isUndirectedVertex(c) || (c === "'"); }
    function isVertexOrLeftDecoration(c){ return isVertex(c) || (c === '<') || isPoint(c); }
    function isVertexOrRightDecoration(c){return isVertex(c) || (c === '>') || isPoint(c); }
    function isArrowHead(c)        { return ARROW_HEAD_CHARACTERS.indexOf(c) + 1; }
    function isGray(c)             { return GRAY_CHARACTERS.indexOf(c) + 1; }
    function isTri(c)              { return TRI_CHARACTERS.indexOf(c) + 1; }

    // "D" = Diagonal slash (/), "B" = diagonal Backslash (\)
    // Characters that may appear anywhere on a solid line
    function isSolidHLine(c)       { return (c === '-') || isUndirectedVertex(c) || isJump(c); }
    function isSolidVLineOrJumpOrPoint(c) { return isSolidVLine(c) || isJump(c) || isPoint(c); }
    function isSolidVLine(c)       { return (c === '|') || isUndirectedVertex(c); }
    function isSolidDLine(c)       { return (c === '/') || isUndirectedVertex(c) }
    function isSolidBLine(c)       { return (c === '\\') || isUndirectedVertex(c); }
    function isJump(c)             { return JUMP_CHARACTERS.indexOf(c) + 1; }
    function isPoint(c)            { return POINT_CHARACTERS.indexOf(c) + 1; }
    function isDecoration(c)       { return DECORATION_CHARACTERS.indexOf(c) + 1; }
    function isEmpty(c)            { return c === ' '; }
   
    ///////////////////////////////////////////////////////////////////////////////
    // Math library

    /** Invoke as new Vec2(v) to clone or new Vec2(x, y) to create from coordinates.
        Can also invoke without new for brevity. */
    function Vec2(x, y) {
        // Detect when being run without new
        if (! (this instanceof Vec2)) { return new Vec2(x, y); }

        if (y === undefined) {
            if (x === undefined) { x = y = 0; } 
            else if (x instanceof Vec2) { y = x.y; x = x.x; }
            else { console.error("Vec2 requires one Vec2 or (x, y) as an argument"); }
        }
        this.x = x;
        this.y = y;
        Object.seal(this);
    }

    /** Returns an SVG representation */
    Vec2.prototype.toString = Vec2.prototype.toSVG = 
        function () { return '' + (this.x * SCALE) + ',' + (this.y * SCALE * ASPECT) + ' '; };

    /** The grid is */
    function makeGrid(str) {
        /** Converts a "rectangular" string defined by newlines into 2D
            array of characters. Grids are immutable. */

        /** Returns ' ' for out of bounds values */
        var grid = function(x, y) {
            if (y === undefined) {
                if (x instanceof Vec2) { y = x.y; x = x.x; }
                else { console.error('grid requires either a Vec2 or (x, y)'); }
            }
            
            return ((x >= 0) && (x < grid.width) && (y >= 0) && (y < grid.height)) ?
                str[y * (grid.width + 1) + x] : ' ';
        };

        // Elements are true when consumed
        grid._used   = [];

        grid.width   = str.indexOf('\n');
        grid.height  = str.split('\n').length;
        if (str[str.length - 1] === '\n') { --grid.height; }

        /** Mark this location. Takes a Vec2 or (x, y) */
        grid.setUsed = function (x, y) {
            if (y === undefined) {
                if (x instanceof Vec2) { y = x.y; x = x.x; }
                else { console.error('grid requires either a Vec2 or (x, y)'); }
            }
            if ((x >= 0) && (x < grid.width) && (y >= 0) && (y < grid.height)) {
                // Match the source string indexing
                grid._used[y * (grid.width + 1) + x] = true;
            }
        };
        
        grid.isUsed = function (x, y) {
            if (y === undefined) {
                if (x instanceof Vec2) { y = x.y; x = x.x; }
                else { console.error('grid requires either a Vec2 or (x, y)'); }
            }
            return (this._used[y * (this.width + 1) + x] === true);
        };
        
        /** Returns true if there is a solid vertical line passing through (x, y) */
        grid.isSolidVLineAt = function (x, y) {
            if (y === undefined) { y = x.x; x = x.x; }
            
            var up = grid(x, y - 1);
            var c  = grid(x, y);
            var dn = grid(x, y + 1);
            
            var uprt = grid(x + 1, y - 1);
            var uplt = grid(x - 1, y - 1);
            
            if (isSolidVLine(c)) {
                // Looks like a vertical line...does it continue?
                return (isTopVertex(up)    || (up === '^') || isSolidVLine(up) || isJump(up) ||
                        isBottomVertex(dn) || (dn === 'v') || isSolidVLine(dn) || isJump(dn) ||
                        isPoint(up) || isPoint(dn) || (grid(x, y - 1) === '_') || (uplt === '_') ||
                        (uprt === '_') ||
                        
                        // Special case of 1-high vertical on two curved corners 
                        ((isTopVertex(uplt) || isTopVertex(uprt)) &&
                         (isBottomVertex(grid(x - 1, y + 1)) || isBottomVertex(grid(x + 1, y + 1)))));
                
            } else if (isTopVertex(c) || (c === '^')) {
                // May be the top of a vertical line
                return isSolidVLine(dn) || (isJump(dn) && (c !== '.'));
            } else if (isBottomVertex(c) || (c === 'v')) {
                return isSolidVLine(up) || (isJump(up) && (c !== "'"));
            } else if (isPoint(c)) {
                return isSolidVLine(up) || isSolidVLine(dn);
            } 
            
            return false;
        };
    
    
        /** Returns true if there is a solid middle (---) horizontal line
            passing through (x, y). Ignores underscores. */
        grid.isSolidHLineAt = function (x, y) {
            if (y === undefined) { y = x.x; x = x.x; }
            
            var ltlt = grid(x - 2, y);
            var lt   = grid(x - 1, y);
            var c    = grid(x + 0, y);
            var rt   = grid(x + 1, y);
            var rtrt = grid(x + 2, y);
            
            if (isSolidHLine(c) || (isSolidHLine(lt) && isJump(c))) {
                // Looks like a horizontal line...does it continue? We need three in a row.
                if (isSolidHLine(lt)) {
                    return isSolidHLine(rt) || isVertexOrRightDecoration(rt) || 
                        isSolidHLine(ltlt) || isVertexOrLeftDecoration(ltlt);
                } else if (isVertexOrLeftDecoration(lt)) {
                    return isSolidHLine(rt);
                } else {
                    return isSolidHLine(rt) && (isSolidHLine(rtrt) || isVertexOrRightDecoration(rtrt));
                }

            } else if (c === '<') {
                return isSolidHLine(rt) && isSolidHLine(rtrt)
                
            } else if (c === '>') {
                return isSolidHLine(lt) && isSolidHLine(ltlt);
                
            } else if (isVertex(c)) {
                return ((isSolidHLine(lt) && isSolidHLine(ltlt)) || 
                        (isSolidHLine(rt) && isSolidHLine(rtrt)));
            }
            
            return false;
        };
        
        
        /** Returns true if there is a solid backslash line passing through (x, y) */
        grid.isSolidBLineAt = function (x, y) {
            if (y === undefined) { y = x.x; x = x.x; }
            var c = grid(x, y);
            var lt = grid(x - 1, y - 1);
            var rt = grid(x + 1, y + 1);
            
            if (c === '\\') {
                // Looks like a diagonal line...does it continue? We need two in a row.
                return (isSolidBLine(rt) || isBottomVertex(rt) || isPoint(rt) || (rt === 'v') ||
                        isSolidBLine(lt) || isTopVertex(lt) || isPoint(lt) || (lt === '^') ||
                        (grid(x, y - 1) === '/') || (grid(x, y + 1) === '/') || (rt === '_') || (lt === '_')); 
            } else if (c === '.') {
                return (rt === '\\');
            } else if (c === "'") {
                return (lt === '\\');
            } else if (c === '^') {
                return rt === '\\';
            } else if (c === 'v') {
                return lt === '\\';
            } else if (isVertex(c) || isPoint(c) || (c === '|')) {
                return isSolidBLine(lt) || isSolidBLine(rt);
            }
        };
        

        /** Returns true if there is a solid diagonal line passing through (x, y) */
        grid.isSolidDLineAt = function (x, y) {
            if (y === undefined) { y = x.x; x = x.x; }
            
            var c = grid(x, y);
            var lt = grid(x - 1, y + 1);
            var rt = grid(x + 1, y - 1);
            
            if (c === '/' && ((grid(x, y - 1) === '\\') || (grid(x, y + 1) === '\\'))) {
                // Special case of tiny hexagon corner
                return true;
            } else if (isSolidDLine(c)) {
                // Looks like a diagonal line...does it continue? We need two in a row.
                return (isSolidDLine(rt) || isTopVertex(rt) || isPoint(rt) || (rt === '^') || (rt === '_') ||
                        isSolidDLine(lt) || isBottomVertex(lt) || isPoint(lt) || (lt === 'v') || (lt === '_')); 
            } else if (c === '.') {
                return (lt === '/');
            } else if (c === "'") {
                return (rt === '/');
            } else if (c === '^') {
                return lt === '/';
            } else if (c === 'v') {
                return rt === '/';
            } else if (isVertex(c) || isPoint(c) || (c === '|')) {
                return isSolidDLine(lt) || isSolidDLine(rt);
            }
            return false;
        };
        
        grid.toString = function () { return str; };
        
        return Object.freeze(grid);
    }
    
    
    /** A 1D curve. If C is specified, the result is a bezier with
        that as the tangent control point */
    function Path(A, B, C, D, dashed) {
        if (! ((A instanceof Vec2) && (B instanceof Vec2))) {
            console.error('Path constructor requires at least two Vec2s');
        }
        this.A = A;
        this.B = B;
        if (C) {
            this.C = C;
            if (D) {
                this.D = D;
            } else {
                this.D = C;
            }
        }

        this.dashed = dashed || false;

        Object.freeze(this);
    }

    var _ = Path.prototype;
    _.isVertical = function () {
        return this.B.x === this.A.x;
    };

    _.isHorizontal = function () {
        return this.B.y === this.A.y;
    };

    /** Diagonal lines look like: / See also backDiagonal */
    _.isDiagonal = function () {
        var dx = this.B.x - this.A.x;
        var dy = this.B.y - this.A.y;
        return (Math.abs(dy + dx) < EPSILON);
    };

    _.isBackDiagonal = function () {
        var dx = this.B.x - this.A.x;
        var dy = this.B.y - this.A.y;
        return (Math.abs(dy - dx) < EPSILON);
    };

    _.isCurved = function () {
        return this.C !== undefined;
    };

    /** Does this path have any end at (x, y) */
    _.endsAt = function (x, y) {
        if (y === undefined) { y = x.y; x = x.x; }
        return ((this.A.x === x) && (this.A.y === y)) ||
            ((this.B.x === x) && (this.B.y === y));
    };

    /** Does this path have an up end at (x, y) */
    _.upEndsAt = function (x, y) {
        if (y === undefined) { y = x.y; x = x.x; }
        return this.isVertical() && (this.A.x === x) && (min(this.A.y, this.B.y) === y);
    };

    /** Does this path have an up end at (x, y) */
    _.diagonalUpEndsAt = function (x, y) {
        if (! this.isDiagonal()) { return false; }
        if (y === undefined) { y = x.y; x = x.x; }
        if (this.A.y < this.B.y) {
            return (this.A.x === x) && (this.A.y === y);
        } else {
            return (this.B.x === x) && (this.B.y === y);
        }
    };

    /** Does this path have a down end at (x, y) */
    _.diagonalDownEndsAt = function (x, y) {
        if (! this.isDiagonal()) { return false; }
        if (y === undefined) { y = x.y; x = x.x; }
        if (this.B.y < this.A.y) {
            return (this.A.x === x) && (this.A.y === y);
        } else {
            return (this.B.x === x) && (this.B.y === y);
        }
    };

    /** Does this path have an up end at (x, y) */
    _.backDiagonalUpEndsAt = function (x, y) {
        if (! this.isBackDiagonal()) { return false; }
        if (y === undefined) { y = x.y; x = x.x; }
        if (this.A.y < this.B.y) {
            return (this.A.x === x) && (this.A.y === y);
        } else {
            return (this.B.x === x) && (this.B.y === y);
        }
    };

    /** Does this path have a down end at (x, y) */
    _.backDiagonalDownEndsAt = function (x, y) {
        if (! this.isBackDiagonal()) { return false; }
        if (y === undefined) { y = x.y; x = x.x; }
        if (this.B.y < this.A.y) {
            return (this.A.x === x) && (this.A.y === y);
        } else {
            return (this.B.x === x) && (this.B.y === y);
        }
    };

    /** Does this path have a down end at (x, y) */
    _.downEndsAt = function (x, y) {
        if (y === undefined) { y = x.y; x = x.x; }
        return this.isVertical() && (this.A.x === x) && (max(this.A.y, this.B.y) === y);
    };

    /** Does this path have a left end at (x, y) */
    _.leftEndsAt = function (x, y) {
        if (y === undefined) { y = x.y; x = x.x; }
        return this.isHorizontal() && (this.A.y === y) && (min(this.A.x, this.B.x) === x);
    };

    /** Does this path have a right end at (x, y) */
    _.rightEndsAt = function (x, y) {
        if (y === undefined) { y = x.y; x = x.x; }
        return this.isHorizontal() && (this.A.y === y) && (max(this.A.x, this.B.x) === x);
    };

    _.verticalPassesThrough = function (x, y) {
        if (y === undefined) { y = x.y; x = x.x; }
        return this.isVertical() && 
            (this.A.x === x) && 
            (min(this.A.y, this.B.y) <= y) &&
            (max(this.A.y, this.B.y) >= y);
    }

    _.horizontalPassesThrough = function (x, y) {
        if (y === undefined) { y = x.y; x = x.x; }
        return this.isHorizontal() && 
            (this.A.y === y) && 
            (min(this.A.x, this.B.x) <= x) &&
            (max(this.A.x, this.B.x) >= x);
    }
    
    /** Returns a string suitable for inclusion in an SVG tag */
    _.toSVG = function () {
        var svg = '<path d="M ' + this.A;

        if (this.isCurved()) {
            svg += 'C ' + this.C + this.D + this.B;
        } else {
            svg += 'L ' + this.B;
        }
        svg += '" style="fill:none;"';
        if (this.dashed) {
            svg += ' stroke-dasharray="3,6"';
        }
        svg += '/>';
        return svg;
    };


    /** A group of 1D curves. This was designed so that all of the
        methods can later be implemented in O(1) time, but it
        currently uses O(n) implementations for source code
        simplicity. */
    function PathSet() {
        this._pathArray = [];
    }

    var PS = PathSet.prototype;
    PS.insert = function (path) {
        this._pathArray.push(path);
    };

    /** Returns a new method that returns true if method(x, y) 
        returns true on any element of _pathAray */
    function makeFilterAny(method) {
        return function(x, y) {
            for (var i = 0; i < this._pathArray.length; ++i) {
                if (method.call(this._pathArray[i], x, y)) { return true; }
            }
            return false;
        }
    }

    // True if an up line ends at these coordinates. Recall that the
    // variable _ is bound to the Path prototype still.
    PS.upEndsAt                = makeFilterAny(_.upEndsAt);
    PS.diagonalUpEndsAt        = makeFilterAny(_.diagonalUpEndsAt);
    PS.backDiagonalUpEndsAt    = makeFilterAny(_.backDiagonalUpEndsAt);
    PS.diagonalDownEndsAt      = makeFilterAny(_.diagonalDownEndsAt);
    PS.backDiagonalDownEndsAt  = makeFilterAny(_.backDiagonalDownEndsAt);
    PS.downEndsAt              = makeFilterAny(_.downEndsAt);
    PS.leftEndsAt              = makeFilterAny(_.leftEndsAt);
    PS.rightEndsAt             = makeFilterAny(_.rightEndsAt);
    PS.endsAt                  = makeFilterAny(_.endsAt);
    PS.verticalPassesThrough   = makeFilterAny(_.verticalPassesThrough);
    PS.horizontalPassesThrough = makeFilterAny(_.horizontalPassesThrough);

    /** Returns an SVG string */
    PS.toSVG = function () {
        var svg = '';
        for (var i = 0; i < this._pathArray.length; ++i) {
            svg += this._pathArray[i].toSVG() + '\n';
        }
        return svg;
    };


    function DecorationSet() {
        this._decorationArray = [];
    }

    var DS = DecorationSet.prototype;

    /** insert(x, y, type, <angle>)  
        insert(vec, type, <angle>)

        angle is the angle in degrees to rotate the result */
    DS.insert = function(x, y, type, angle) {
        if (type === undefined) { type = y; y = x.y; x = x.x; }

        if (! isDecoration(type)) {
            console.error('Illegal decoration character: ' + type); 
        }
        var d = {C: Vec2(x, y), type: type, angle:angle || 0};

        // Put arrows at the front and points at the back so that
        // arrows always draw under points

        if (isPoint(type)) {
            this._decorationArray.push(d);
        } else {
            this._decorationArray.unshift(d);
        }
    };


    DS.toSVG = function () {
        var svg = '';
        for (var i = 0; i < this._decorationArray.length; ++i) {
            var decoration = this._decorationArray[i];
            var C = decoration.C;
            
            if (isJump(decoration.type)) {
                // Slide jumps
                var dx = (decoration.type === ')') ? +0.75 : -0.75;
                var up  = Vec2(C.x, C.y - 0.5);
                var dn  = Vec2(C.x, C.y + 0.5);
                var cup = Vec2(C.x + dx, C.y - 0.5);
                var cdn = Vec2(C.x + dx, C.y + 0.5);

                svg += '<path d="M ' + dn + ' C ' + cdn + cup + up + '" style="fill:none;"/>';

            } else if (isPoint(decoration.type)) {

                svg += '<circle cx="' + (C.x * SCALE) + '" cy="' + (C.y * SCALE * ASPECT) +
                       '" r="' + (SCALE - STROKE_WIDTH) + '" class="' + ((decoration.type === '*') ? 'closed' : 'open') + 'dot"/>';
            } else if (isGray(decoration.type)) {
                
                var shade = Math.round((3 - GRAY_CHARACTERS.indexOf(decoration.type)) * 63.75);
                svg += '<rect x="' + ((C.x - 0.5) * SCALE) + '" y="' + ((C.y - 0.5) * SCALE * ASPECT) + '" width="' + SCALE + '" height="' + (SCALE * ASPECT) + '" fill="rgb(' + shade + ',' + shade + ',' + shade +')"/>';

            } else if (isTri(decoration.type)) {
                // 30-60-90 triangle
                var index = TRI_CHARACTERS.indexOf(decoration.type);
                var xs  = 0.5 - (index & 1);
                var ys  = 0.5 - (index >> 1);
                xs *= sign(ys);
                var tip = Vec2(C.x + xs, C.y - ys);
                var up  = Vec2(C.x + xs, C.y + ys);
                var dn  = Vec2(C.x - xs, C.y + ys);
                svg += '<polygon points="' + tip + up + dn + '" style="stroke:none"/>\n';
            } else { // Arrow head
                var tip = Vec2(C.x + 1, C.y);
                var up =  Vec2(C.x - 0.5, C.y - 0.35);
                var dn =  Vec2(C.x - 0.5, C.y + 0.35);
                svg += '<polygon points="' + tip + up + dn + 
                    '"  style="stroke:none" transform="rotate(' + decoration.angle + ',' + C + ')"/>\n';
            }
        }
        return svg;
    };

    ////////////////////////////////////////////////////////////////////////////

    function findPaths(grid, pathSet) {
        // Does the line from A to B contain at least one c?
        function lineContains(A, B, c) {
            var dx = sign(B.x - A.x);
            var dy = sign(B.y - A.y);
            var x, y;

            for (x = A.x, y = A.y; (x !== B.x) || (y !== B.y); x += dx, y += dy) {
                if (grid(x, y) === c) { return true; }
            }

            // Last point
            return (grid(x, y) === c);
        }

        // Find all solid vertical lines. Iterate horizontally
        // so that we never hit the same line twice
        for (var x = 0; x < grid.width; ++x) {
            for (var y = 0; y < grid.height; ++y) {
                if (grid.isSolidVLineAt(x, y)) {
                    // This character begins a vertical line...now, find the end
                    var A = Vec2(x, y);
                    do  { grid.setUsed(x, y); ++y; } while (grid.isSolidVLineAt(x, y));
                    var B = Vec2(x, y - 1);
                    
                    var up = grid(A);
                    var upup = grid(A.x, A.y - 1);

                    if (! isVertex(up) && ((upup === '-') || (upup === '_') || (grid(A.x - 1, A.y - 1) === '_') ||
                                           (grid(A.x + 1, A.y - 1) === '_') || 
                                           isBottomVertex(upup)) || isJump(upup)) {
                        // Stretch up to almost reach the line above (if there is a decoration,
                        // it will finish the gap)
                        A.y -= 0.5;
                    }

                    var dn = grid(B);
                    var dndn = grid(B.x, B.y + 1);
                    if (! isVertex(dn) && ((dndn === '-') || isTopVertex(dndn)) || isJump(dndn) ||
                        (grid(B.x - 1, B.y) === '_') || (grid(B.x + 1, B.y) === '_')) {
                        // Stretch down to almost reach the line below
                        B.y += 0.5;
                    }

                    // Don't insert degenerate lines
                    if ((A.x !== B.x) || (A.y !== B.y)) {
                        pathSet.insert(new Path(A, B));
                    }

                    // Continue the search from the end value y+1
                } 

                // Some very special patterns for the short lines needed on
                // circuit diagrams. Only invoke these if not also on a curve
                //      _  _    
                //    -'    '-
                else if ((grid(x, y) === "'") &&
                    (((grid(x - 1, y) === '-') && (grid(x + 1, y - 1) === '_') &&
                     ! isSolidVLineOrJumpOrPoint(grid(x - 1, y - 1))) ||
                     ((grid(x - 1, y - 1) === '_') && (grid(x + 1, y) === '-') &&
                     ! isSolidVLineOrJumpOrPoint(grid(x + 1, y - 1))))) {
                    pathSet.insert(new Path(Vec2(x, y - 0.5), Vec2(x, y)));
                }

                //    _.-  -._ 
                else if ((grid(x, y) === '.') &&
                         (((grid(x - 1, y) === '_') && (grid(x + 1, y) === '-') && 
                           ! isSolidVLineOrJumpOrPoint(grid(x + 1, y + 1))) ||
                          ((grid(x - 1, y) === '-') && (grid(x + 1, y) === '_') &&
                           ! isSolidVLineOrJumpOrPoint(grid(x - 1, y + 1))))) {
                    pathSet.insert(new Path(Vec2(x, y), Vec2(x, y + 0.5)));
                }

            } // y
        } // x
        
        // Find all solid horizontal lines 
        for (var y = 0; y < grid.height; ++y) {
            for (var x = 0; x < grid.width; ++x) {
                if (grid.isSolidHLineAt(x, y)) {
                    // Begins a line...find the end
                    var A = Vec2(x, y);
                    do { grid.setUsed(x, y); ++x; } while (grid.isSolidHLineAt(x, y));
                    var B = Vec2(x - 1, y);

                    // Detect curves and shorten the edge
                    if ( ! isVertex(grid(A.x - 1, A.y)) && 
                         ((isTopVertex(grid(A)) && isSolidVLineOrJumpOrPoint(grid(A.x - 1, A.y + 1))) ||
                          (isBottomVertex(grid(A)) && isSolidVLineOrJumpOrPoint(grid(A.x - 1, A.y - 1))))) {
                        ++A.x;
                    }

                    if ( ! isVertex(grid(B.x + 1, B.y)) && 
                         ((isTopVertex(grid(B)) && isSolidVLineOrJumpOrPoint(grid(B.x + 1, B.y + 1))) ||
                          (isBottomVertex(grid(B)) && isSolidVLineOrJumpOrPoint(grid(B.x + 1, B.y - 1))))) {
                        --B.x;
                    }

                    // Don't insert degenerate lines
                    if ((A.x !== B.x) || (A.y !== B.y)) {
                        pathSet.insert(new Path(A, B));
                    }
                    // Continue the search from the end x+1
                }
            }
        } // y

        // Find all solid left-to-right downward diagonal lines (BACK DIAGONAL)
        for (var i = -grid.height; i < grid.width; ++i) {
            for (var x = i, y = 0; y < grid.height; ++y, ++x) {
                if (grid.isSolidBLineAt(x, y)) {
                    // Begins a line...find the end
                    var A = Vec2(x, y);
                    do { ++x; ++y; } while (grid.isSolidBLineAt(x, y));
                    var B = Vec2(x - 1, y - 1);

                    // Ensure that the entire line wasn't just vertices
                    if (lineContains(A, B, '\\')) {
                        for (var j = A.x; j <= B.x; ++j) {
                            grid.setUsed(j, A.y + (j - A.x)); 
                        }

                        var top = grid(A);
                        var up = grid(A.x, A.y - 1);
                        var uplt = grid(A.x - 1, A.y - 1);
                        if ((up === '/') || (uplt === '_') || (up === '_') || 
                            (! isVertex(top)  && 
                             (isSolidHLine(uplt) || isSolidVLine(uplt)))) {
                            // Continue half a cell more to connect for:
                            //  ___   ___
                            //  \        \    /      ----     |
                            //   \        \   \        ^      |^
                            A.x -= 0.5; A.y -= 0.5;
                        } else if (isPoint(uplt)) {
                            // Continue 1/4 cell more to connect for:
                            //
                            //  o
                            //   ^
                            //    \
                            A.x -= 0.25; A.y -= 0.25;
                        }
                        
                        var bottom = grid(B);
                        var dnrt = grid(B.x + 1, B.y + 1);
                        if ((grid(B.x, B.y + 1) === '/') || (grid(B.x + 1, B.y) === '_') || 
                            (grid(B.x - 1, B.y) === '_') || 
                            (! isVertex(grid(B)) &&
                             (isSolidHLine(dnrt) || isSolidVLine(dnrt)))) {
                            // Continue half a cell more to connect for:
                            //                       \      \ |
                            //  \       \     \       v      v|
                            //   \__   __\    /      ----     |
                            
                            B.x += 0.5; B.y += 0.5;
                        } else if (isPoint(dnrt)) {
                            // Continue 1/4 cell more to connect for:
                            //
                            //    \
                            //     v
                            //      o
                            
                            B.x += 0.25; B.y += 0.25;
                        }
                        
                        pathSet.insert(new Path(A, B));
                        // Continue the search from the end x+1,y+1
                    } // lineContains
                }
            }
        } // i


        // Find all solid left-to-right upward diagonal lines (DIAGONAL)
        for (var i = -grid.height; i < grid.width; ++i) {
            for (var x = i, y = grid.height - 1; y >= 0; --y, ++x) {
                if (grid.isSolidDLineAt(x, y)) {
                    // Begins a line...find the end
                    var A = Vec2(x, y);
                    do { ++x; --y; } while (grid.isSolidDLineAt(x, y));
                    var B = Vec2(x - 1, y + 1);

                    if (lineContains(A, B, '/')) {
                        // This is definitely a line. Commit the characters on it
                        for (var j = A.x; j <= B.x; ++j) {
                            grid.setUsed(j, A.y - (j - A.x)); 
                        }

                        var up = grid(B.x, B.y - 1);
                        var uprt = grid(B.x + 1, B.y - 1);
                        var bottom = grid(B);
                        if ((up === '\\') || (up === '_') || (uprt === '_') || 
                            (! isVertex(grid(B)) &&
                             (isSolidHLine(uprt) || isSolidVLine(uprt)))) {
                            
                            // Continue half a cell more to connect at:
                            //     __   __  ---     |
                            //    /      /   ^     ^|
                            //   /      /   /     / |
                            
                            B.x += 0.5; B.y -= 0.5;
                        } else if (isPoint(uprt)) {
                            
                            // Continue 1/4 cell more to connect at:
                            //
                            //       o
                            //      ^
                            //     /
                            
                            B.x += 0.25; B.y -= 0.25;
                        }
                        
                        var dnlt = grid(A.x - 1, A.y + 1);
                        var top = grid(A);
                        if ((grid(A.x, A.y + 1) === '\\') || (grid(A.x - 1, A.y) === '_') || (grid(A.x + 1, A.y) === '_') ||
                            (! isVertex(grid(A)) &&
                             (isSolidHLine(dnlt) || isSolidVLine(dnlt)))) {

                            // Continue half a cell more to connect at:
                            //               /     \ |
                            //    /  /      v       v|
                            // __/  /__   ----       | 
                            
                            A.x -= 0.5; A.y += 0.5;
                        } else if (isPoint(dnlt)) {
                            
                            // Continue 1/4 cell more to connect at:
                            //
                            //       /
                            //      v
                            //     o
                            
                            A.x -= 0.25; A.y += 0.25;
                        }
                        pathSet.insert(new Path(A, B));

                        // Continue the search from the end x+1,y-1
                    } // lineContains
                }
            }
        } // y
        
        
        // Now look for curved corners. The syntax constraints require
        // that these can always be identified by looking at three
        // horizontally-adjacent characters.
        for (var y = 0; y < grid.height; ++y) {
            for (var x = 0; x < grid.width; ++x) {
                var c = grid(x, y);

                // Note that because of undirected vertices, the
                // following cases are not exclusive
                if (isTopVertex(c)) {
                    // -.
                    //   |
                    if (isSolidHLine(grid(x - 1, y)) && isSolidVLine(grid(x + 1, y + 1))) {
                        grid.setUsed(x - 1, y); grid.setUsed(x, y); grid.setUsed(x + 1, y + 1);
                        pathSet.insert(new Path(Vec2(x - 1, y), Vec2(x + 1, y + 1), 
                                                Vec2(x + 1.1, y), Vec2(x + 1, y + 1)));
                    }

                    //  .-
                    // |
                    if (isSolidHLine(grid(x + 1, y)) && isSolidVLine(grid(x - 1, y + 1))) {
                        grid.setUsed(x - 1, y + 1); grid.setUsed(x, y); grid.setUsed(x + 1, y);
                        pathSet.insert(new Path(Vec2(x + 1, y), Vec2(x - 1, y + 1), 
                                                Vec2(x - 1.1, y), Vec2(x - 1, y + 1)));
                    }
                }
                
                // Special case patterns:
                //   .  .   .  .    
                //  (  o     )  o
                //   '  .   '  '
                if (((c === ')') || isPoint(c)) && (grid(x - 1, y - 1) === '.') && (grid(x - 1, y + 1) === "\'")) {
                    grid.setUsed(x, y); grid.setUsed(x - 1, y - 1); grid.setUsed(x - 1, y + 1);
                    pathSet.insert(new Path(Vec2(x - 2, y - 1), Vec2(x - 2, y + 1), 
                                            Vec2(x + 0.6, y - 1), Vec2(x + 0.6, y + 1)));
                }

                if (((c === '(') || isPoint(c)) && (grid(x + 1, y - 1) === '.') && (grid(x + 1, y + 1) === "\'")) {
                    grid.setUsed(x, y); grid.setUsed(x + 1, y - 1); grid.setUsed(x + 1, y + 1);
                    pathSet.insert(new Path(Vec2(x + 2, y - 1), Vec2(x + 2, y + 1), 
                                            Vec2(x - 0.6, y - 1), Vec2(x - 0.6, y + 1)));
                }

                if (isBottomVertex(c)) {
                    //   |
                    // -' 
                    if (isSolidHLine(grid(x - 1, y)) && isSolidVLine(grid(x + 1, y - 1))) {
                        grid.setUsed(x - 1, y); grid.setUsed(x, y); grid.setUsed(x + 1, y - 1);
                        pathSet.insert(new Path(Vec2(x - 1, y), Vec2(x + 1, y - 1), 
                                                Vec2(x + 1.1, y), Vec2(x + 1, y - 1)));
                    }

                    // | 
                    //  '-
                    if (isSolidHLine(grid(x + 1, y)) && isSolidVLine(grid(x - 1, y - 1))) {
                        grid.setUsed(x - 1, y - 1); grid.setUsed(x, y); grid.setUsed(x + 1, y);
                        pathSet.insert(new Path(Vec2(x + 1, y), Vec2(x - 1, y - 1),
                                                Vec2(x - 1.1, y), Vec2(x - 1, y - 1)));
                    }
                }
               
            } // for x
        } // for y

        // Find low horizontal lines marked with underscores. These
        // are so simple compared to the other cases that we process
        // them directly here without a helper function. Process these
        // from top to bottom and left to right so that we can read
        // them in a single sweep.
        // 
        // Exclude the special case of double underscores going right
        // into an ASCII character, which could be a source code
        // identifier such as __FILE__ embedded in the diagram.
        for (var y = 0; y < grid.height; ++y) {
            for (var x = 0; x < grid.width - 2; ++x) {
                var lt = grid(x - 1, y);

                if ((grid(x, y) === '_') && (grid(x + 1, y) === '_') && 
                    (! isASCIILetter(grid(x + 2, y)) || (lt === '_')) && 
                    (! isASCIILetter(lt) || (grid(x + 2, y) === '_'))) {

                    var ltlt = grid(x - 2, y);
                    var A = Vec2(x - 0.5, y + 0.5);

                    if ((lt === '|') || (grid(x - 1, y + 1) === '|') ||
                        (lt === '.') || (grid(x - 1, y + 1) === "'")) {
                        // Extend to meet adjacent vertical
                        A.x -= 0.5;

                        // Very special case of overrunning into the side of a curve,
                        // needed for logic gate diagrams
                        if ((lt === '.') && 
                            ((ltlt === '-') ||
                             (ltlt === '.')) &&
                            (grid(x - 2, y + 1) === '(')) {
                            A.x -= 0.5;
                        }
                    } else if (lt === '/') {
                        A.x -= 1.0;
                    }

                    // Detect overrun of a tight double curve
                    if ((lt === '(') && (ltlt === '(') &&
                        (grid(x, y + 1) === "'") && (grid(x, y - 1) === '.')) {
                        A.x += 0.5;
                    }
                    lt = ltlt = undefined;

                    do { grid.setUsed(x, y); ++x; } while (grid(x, y) === '_');

                    var B = Vec2(x - 0.5, y + 0.5);
                    var c = grid(x, y);
                    var rt = grid(x + 1, y);
                    var dn = grid(x, y + 1);

                    if ((c === '|') || (dn === '|') || (c === '.') || (dn === "'")) {
                        // Extend to meet adjacent vertical
                        B.x += 0.5;

                        // Very special case of overrunning into the side of a curve,
                        // needed for logic gate diagrams
                        if ((c === '.') && 
                            ((rt === '-') || (rt === '.')) &&
                            (grid(x + 1, y + 1) === ')')) {
                            B.x += 0.5;
                        }
                    } else if ((c === '\\')) {
                        B.x += 1.0;
                    }

                    // Detect overrun of a tight double curve
                    if ((c === ')') && (rt === ')') && (grid(x - 1, y + 1) === "'") && (grid(x - 1, y - 1) === '.')) {
                        B.x += -0.5;
                    }

                    pathSet.insert(new Path(A, B));
                }
            } // for x
        } // for y
    } // findPaths


    function findDecorations(grid, pathSet, decorationSet) {
        function isEmptyOrVertex(c) { return (c === ' ') || /[^a-zA-Z0-9]|[ov]/.test(c); }
                    
        /** Is the point in the center of these values on a line? Allow points that are vertically
            adjacent but not horizontally--they wouldn't fit anyway, and might be text. */
        function onLine(up, dn, lt, rt) {
            return ((isEmptyOrVertex(dn) || isPoint(dn)) &&
                    (isEmptyOrVertex(up) || isPoint(up)) &&
                    isEmptyOrVertex(rt) &&
                    isEmptyOrVertex(lt));
        }

        for (var x = 0; x < grid.width; ++x) {
            for (var j = 0; j < grid.height; ++j) {
                var c = grid(x, j);
                var y = j;

                if (isJump(c)) {

                    // Ensure that this is really a jump and not a stray character
                    if (pathSet.downEndsAt(x, y - 0.5) &&
                        pathSet.upEndsAt(x, y + 0.5)) {
                        decorationSet.insert(x, y, c);
                        grid.setUsed(x, y);
                    }

                } else if (isPoint(c)) {
                    var up = grid(x, y - 1);
                    var dn = grid(x, y + 1);
                    var lt = grid(x - 1, y);
                    var rt = grid(x + 1, y);

                    if (pathSet.rightEndsAt(x - 1, y) ||   // Must be at the end of a line...
                        pathSet.leftEndsAt(x + 1, y) ||    // or completely isolated NSEW
                        pathSet.downEndsAt(x, y - 1) ||
                        pathSet.upEndsAt(x, y + 1) ||

                        pathSet.upEndsAt(x, y) ||    // For points on vertical lines 
                        pathSet.downEndsAt(x, y) ||  // that are surrounded by other characters

                        onLine(up, dn, lt, rt)) {
                        
                        decorationSet.insert(x, y, c);
                        grid.setUsed(x, y);
                    }
                } else if (isGray(c)) {
                    decorationSet.insert(x, y, c);
                    grid.setUsed(x, y);
                } else if (isTri(c)) {
                    decorationSet.insert(x, y, c);
                    grid.setUsed(x, y);
                } else { // Arrow heads

                    // If we find one, ensure that it is really an
                    // arrow head and not a stray character by looking
                    // for a connecting line.
                    var dx = 0;
                    if ((c === '>') && (pathSet.rightEndsAt(x, y) || 
                                        pathSet.horizontalPassesThrough(x, y))) {
                        if (isPoint(grid(x + 1, y))) {
                            // Back up if connecting to a point so as to not
                            // overlap it
                            dx = -0.5;
                        }
                        decorationSet.insert(x + dx, y, '>', 0);
                        grid.setUsed(x, y);
                    } else if ((c === '<') && (pathSet.leftEndsAt(x, y) ||
                                               pathSet.horizontalPassesThrough(x, y))) {
                        if (isPoint(grid(x - 1, y))) {
                            // Back up if connecting to a point so as to not
                            // overlap it
                            dx = 0.5;
                        }
                        decorationSet.insert(x + dx, y, '>', 180); 
                        grid.setUsed(x, y);
                    } else if (c === '^') {
                        // Because of the aspect ratio, we need to look
                        // in two slots for the end of the previous line
                        if (pathSet.upEndsAt(x, y - 0.5)) {
                            decorationSet.insert(x, y - 0.5, '>', 270); 
                            grid.setUsed(x, y);
                        } else if (pathSet.upEndsAt(x, y)) {
                            decorationSet.insert(x, y, '>', 270);
                            grid.setUsed(x, y);
                        } else if (pathSet.diagonalUpEndsAt(x + 0.5, y - 0.5)) {
                            decorationSet.insert(x + 0.5, y - 0.5, '>', 270 + DIAGONAL_ANGLE);
                            grid.setUsed(x, y);
                        } else if (pathSet.diagonalUpEndsAt(x + 0.25, y - 0.25)) {
                            decorationSet.insert(x + 0.25, y - 0.25, '>', 270 + DIAGONAL_ANGLE);
                            grid.setUsed(x, y);
                        } else if (pathSet.diagonalUpEndsAt(x, y)) {
                            decorationSet.insert(x, y, '>', 270 + DIAGONAL_ANGLE);
                            grid.setUsed(x, y);
                        } else if (pathSet.backDiagonalUpEndsAt(x, y)) {
                            decorationSet.insert(x, y, c, 270 - DIAGONAL_ANGLE);
                            grid.setUsed(x, y);
                        } else if (pathSet.backDiagonalUpEndsAt(x - 0.5, y - 0.5)) {
                            decorationSet.insert(x - 0.5, y - 0.5, c, 270 - DIAGONAL_ANGLE);
                            grid.setUsed(x, y);
                        } else if (pathSet.backDiagonalUpEndsAt(x - 0.25, y - 0.25)) {
                            decorationSet.insert(x - 0.25, y - 0.25, c, 270 - DIAGONAL_ANGLE);
                            grid.setUsed(x, y);
                        } else if (pathSet.verticalPassesThrough(x, y)) {
                            // Only try this if all others failed
                            decorationSet.insert(x, y - 0.5, '>', 270); 
                            grid.setUsed(x, y);
                        }
                    } else if (c === 'v') {
                        if (pathSet.downEndsAt(x, y + 0.5)) {
                            decorationSet.insert(x, y + 0.5, '>', 90); 
                            grid.setUsed(x, y);
                        } else if (pathSet.downEndsAt(x, y)) {
                            decorationSet.insert(x, y, '>', 90);
                            grid.setUsed(x, y);
                        } else if (pathSet.diagonalDownEndsAt(x, y)) {
                            decorationSet.insert(x, y, '>', 90 + DIAGONAL_ANGLE);
                            grid.setUsed(x, y);
                        } else if (pathSet.diagonalDownEndsAt(x - 0.5, y + 0.5)) {
                            decorationSet.insert(x - 0.5, y + 0.5, '>', 90 + DIAGONAL_ANGLE);
                            grid.setUsed(x, y);
                        } else if (pathSet.diagonalDownEndsAt(x - 0.25, y + 0.25)) {
                            decorationSet.insert(x - 0.25, y + 0.25, '>', 90 + DIAGONAL_ANGLE);
                            grid.setUsed(x, y);
                        } else if (pathSet.backDiagonalDownEndsAt(x, y)) {
                            decorationSet.insert(x, y, '>', 90 - DIAGONAL_ANGLE);
                            grid.setUsed(x, y);
                        } else if (pathSet.backDiagonalDownEndsAt(x + 0.5, y + 0.5)) {
                            decorationSet.insert(x + 0.5, y + 0.5, '>', 90 - DIAGONAL_ANGLE);
                            grid.setUsed(x, y);
                        } else if (pathSet.backDiagonalDownEndsAt(x + 0.25, y + 0.25)) {
                            decorationSet.insert(x + 0.25, y + 0.25, '>', 90 - DIAGONAL_ANGLE);
                            grid.setUsed(x, y);
                        } else if (pathSet.verticalPassesThrough(x, y)) {
                            // Only try this if all others failed
                            decorationSet.insert(x, y + 0.5, '>', 90); 
                            grid.setUsed(x, y);
                        }
                    } // arrow heads
                } // decoration type
            } // y
        } // x
    } // findArrowHeads

    //var grid = new Grid(diagramString);
    var grid = makeGrid(diagramString);

    var pathSet = new PathSet();
    var decorationSet = new DecorationSet();

    findPaths(grid, pathSet);
    findDecorations(grid, pathSet, decorationSet);

    var svg = '<svg class="diagram" xmlns="http://www.w3.org/2000/svg" version="1.1" height="' + 
        ((grid.height + 1) * SCALE * ASPECT) + '" width="' + ((grid.width + 1) * SCALE) + '"';

    if (alignmentHint === 'floatleft') {
        svg += ' style="float:left;margin: 15px 30px 15px 0px;"';
    } else if (alignmentHint === 'floatright') {
        svg += ' style="float:right;margin: 15px 0px 15px 30px;"';
    } else if (alignmentHint === 'center') {
        svg += ' style="margin: 0px auto 0px auto;"';
    }

    svg += '><g transform="translate(' + Vec2(1, 1) + ')">\n';

    if (DEBUG_SHOW_GRID) {
        svg += '<g style="opacity:0.1">\n';
        for (var x = 0; x < grid.width; ++x) {
            for (var y = 0; y < grid.height; ++y) {
                svg += '<rect x="' + ((x - 0.5) * SCALE + 1) + '" + y="' + ((y - 0.5) * SCALE * ASPECT + 2) + '" width="' + (SCALE - 2) + '" height="' + (SCALE * ASPECT - 2) + '" style="fill:';
                if (grid.isUsed(x, y)) {
                    svg += 'red;';
                } else if (grid(x, y) === ' ') {
                    svg += 'gray; opacity:0.05';
                } else {
                    svg += 'blue;';
                }
                svg += '"/>\n';
            }
        }
        svg += '</g>\n';
    }
    
    svg += pathSet.toSVG();
    svg += decorationSet.toSVG();

    // Convert any remaining characters
    if (! DEBUG_HIDE_PASSTHROUGH) {
        svg += '<g transform="translate(0,0)">';
        for (var y = 0; y < grid.height; ++y) {
            for (var x = 0; x < grid.width; ++x) {
                var c = grid(x, y);
                if (/[\u2B22\u2B21]/.test(c)) {
                    // Enlarge hexagons so that they fill a grid
                    svg += '<text text-anchor="middle" x="' + (x * SCALE) + '" y="' + (4 + y * SCALE * ASPECT) + '" style="font-size:20.5px">' + escapeHTMLEntities(c) +  '</text>';
                } else if ((c !== ' ') && ! grid.isUsed(x, y)) {
                    svg += '<text text-anchor="middle" x="' + (x * SCALE) + '" y="' + (4 + y * SCALE * ASPECT) + '">' + escapeHTMLEntities(c) +  '</text>';
                } // if
            } // y
        } // x
        svg += '</g>';
    }

    if (DEBUG_SHOW_SOURCE) {
        // Offset the characters a little for easier viewing
        svg += '<g transform="translate(2, 2)">\n';
        for (var x = 0; x < grid.width; ++x) {
            for (var y = 0; y < grid.height; ++y) {
                var c = grid(x, y);
                if (c !== ' ') {
                    svg += '<text text-anchor="middle" x="' + (x * SCALE) + '" y="' + (4 + y * SCALE * ASPECT) + '" style="fill:#F00;font-family:Menlo,monospace;font-size:12px;text-align:center">' + escapeHTMLEntities(c) +  '</text>';
                } // if
            } // y
        } // x
        svg += '</g>';
    } // if

    svg += '</g></svg>';

    svg = svg.rp(new RegExp(HIDE_O, 'g'), 'o');


    return svg;
}


/* xcode.min.js modified */
var HIGHLIGHT_STYLESHEET = 
        "<style>.hljs{display:block;overflow-x:auto;padding:0.5em;background:#fff;color:#000;-webkit-text-size-adjust:none}"+
        ".hljs-comment{color:#006a00}" +
        ".hljs-keyword {color:#02E}" +
        ".hljs-literal,.nginx .hljs-title{color:#aa0d91}" + 
        ".method,.hljs-list .hljs-title,.hljs-tag .hljs-title,.setting .hljs-value,.hljs-winutils,.tex .hljs-command,.http .hljs-title,.hljs-request,.hljs-status,.hljs-name{color:#008}" + 
        ".hljs-envvar,.tex .hljs-special{color:#660}" + 
        ".hljs-string{color:#c41a16}" +
        ".hljs-tag .hljs-value,.hljs-cdata,.hljs-filter .hljs-argument,.hljs-attr_selector,.apache .hljs-cbracket,.hljs-date,.hljs-regexp{color:#080}" + 
        ".hljs-sub .hljs-identifier,.hljs-pi,.hljs-tag,.hljs-tag .hljs-keyword,.hljs-decorator,.ini .hljs-title,.hljs-shebang,.hljs-prompt,.hljs-hexcolor,.hljs-rule .hljs-value,.hljs-symbol,.hljs-symbol .hljs-string,.hljs-number,.css .hljs-function,.hljs-function .hljs-title,.coffeescript .hljs-attribute{color:#A0C}" +
        ".hljs-function .hljs-title{font-weight:bold;color:#000}" + 
        ".hljs-class .hljs-title,.smalltalk .hljs-class,.hljs-type,.hljs-typename,.hljs-tag .hljs-attribute,.hljs-doctype,.hljs-class .hljs-id,.hljs-built_in,.setting,.hljs-params,.clojure .hljs-attribute{color:#5c2699}" +
        ".hljs-variable{color:#3f6e74}" +
        ".css .hljs-tag,.hljs-rule .hljs-property,.hljs-pseudo,.hljs-subst{color:#000}" + 
        ".css .hljs-class,.css .hljs-id{color:#9b703f}" +
        ".hljs-value .hljs-important{color:#ff7700;font-weight:bold}" +
        ".hljs-rule .hljs-keyword{color:#c5af75}" +
        ".hljs-annotation,.apache .hljs-sqbracket,.nginx .hljs-built_in{color:#9b859d}" +
        ".hljs-preprocessor,.hljs-preprocessor *,.hljs-pragma{color:#643820}" +
        ".tex .hljs-formula{background-color:#eee;font-style:italic}" +
        ".diff .hljs-header,.hljs-chunk{color:#808080;font-weight:bold}" +
        ".diff .hljs-change{background-color:#bccff9}" +
        ".hljs-addition{background-color:#baeeba}" +
        ".hljs-deletion{background-color:#ffc8bd}" +
        ".hljs-comment .hljs-doctag{font-weight:bold}" +
        ".method .hljs-id{color:#000}</style>";

function isMarkdeepScriptName(str) { return str.search(/markdeep\S*?\.js$/i) !== -1; }
function toArray(list) { return Array.prototype.slice.call(list); }

// Intentionally uninitialized global variable used to detect
// recursive invocations
if (! window.alreadyProcessedMarkdeep) {
    window.alreadyProcessedMarkdeep = true;

    // Detect the noformat argument to the URL
    var noformat = (window.location.href.search(/\?.*noformat.*/i) !== -1);

    // Export relevant methods
    window.markdeep = Object.freeze({ 
        format:               markdeepToHTML,
        formatDiagram:        diagramToSVG,
        stylesheet:           function() {
            return STYLESHEET + sectionNumberingStylesheet() + HIGHLIGHT_STYLESHEET;
        }
    });

    var mode = option('mode');
    switch (mode) {
    case 'script':
        // Nothing to do
        return;

    case 'html':
    case 'doxygen':
        toArray(document.getElementsByClassName('diagram')).concat(toArray(document.getElementsByTagName('diagram'))).forEach(
            function (element) {
                var src = unescapeHTMLEntities(element.innerHTML);
                // Remove the first and last string (which probably
                // had the pre or diagram tag as part of them) if they are 
                // empty except for whitespace.
                src = src.rp(/(:?^[ \t]*\n)|(:?\n[ \t]*)$/g, '');

                if (mode === 'doxygen') {
                    // Undo Doxygen's &ndash and &mdash, which are impossible to 
                    // detect once the browser has parsed the document
                    src = src.rp(new RegExp('\u2013', 'g'), '--');
                    src = src.rp(new RegExp('\u2014', 'g'), '---');
                    
                    // Undo Doxygen's links within the diagram because they throw off spacing
                    src = src.rp(/<a class="el" .*>(.*)<\/a>/g, '$1');
                }
                element.outerHTML = '<center class="md">' + diagramToSVG(removeLeadingSpace(src), '') + '</center>';
            });
        
        toArray(document.getElementsByClassName('markdeep')).concat(toArray(document.getElementsByTagName('markdeep'))).forEach(
            function (src) {
                var dst = document.createElement('div');
                dst.innerHTML = markdeepToHTML(removeLeadingSpace(unescapeHTMLEntities(src.innerHTML)), true);
                src.parentNode.replaceChild(dst, src);
            });

        // Include our stylesheet even if there are no tags, but not the BODY_STYLESHEET
        document.head.innerHTML = window.markdeep.stylesheet() + document.head.innerHTML;
        return;
    }
    
    // The following is Morgan's massive hack for allowing browsers to
    // directly parse Markdown from what appears to be a text file, but is
    // actually an intentionally malformed HTML file.
    
    // In order to be able to show what source files look like, the
    // noformat argument may be supplied.
     
    
    if (! noformat) {
        // Remove any recursive references to this script so that we
        // don't trigger the cost of recursive *loading*. (The
        // alreadyProcessedMarkdeep variable will prevent recursive
        // *execution*.) We allow other scripts to pass through.
        toArray(document.getElementsByTagName('script')).forEach(function(node) {
            if (isMarkdeepScriptName(node.src)) {
                node.parentNode.removeChild(node);
            }
        });
        
        // Hide the body while formatting
        document.body.style.visibility = 'hidden';
    }
      
    var source = nodeToMarkdeepSource(document.body);

    if (noformat) { 
        // Abort processing
        source = source.rp(/<!-- Markdeep:.+$/gm, '') + MARKDEEP_LINE;
    
        // Escape the <> (not ampersand) that we just added
        source = source.rp(/</g, '&lt;').rp(/>/g, '&gt;');
            
        // Replace the Markdeep line itself so that ?noformat examples have a valid line to copy
        document.body.innerHTML = entag('pre', source);
        return;
    }

    source = unescapeHTMLEntities(source);
    
    // Run markdeep processing after the rest of this file parses
    setTimeout(function() {
        var markdeepHTML = markdeepToHTML(source, false);
        
        // Need MathJax if $$ ... $$, \( ... \), or \begin{
        var needMathJax = option('detectMath') &&
            ((markdeepHTML.search(/(?:\$\$[\s\S]+\$\$)|(?:\\begin{)/m) !== -1) || 
             (markdeepHTML.search(/\\\(.*\\\)/) !== -1));
        
        if (needMathJax) {
            // Custom definitions (NC == \newcommand)
            var MATHJAX_COMMANDS = '$$NC{\\n}{\\hat{n}}NC{\\w}{\\hat{\\omega}}NC{\\wi}{\\w_\\mathrm{i}}NC{\\wo}{\\w_\\mathrm{o}}NC{\\wh}{\\w_\\mathrm{h}}NC{\\Li}{L_\\mathrm{i}}NC{\\Lo}{L_\\mathrm{o}}NC{\\Le}{L_\\mathrm{e}}NC{\\Lr}{L_\\mathrm{r}}NC{\\Lt}{L_\\mathrm{t}}NC{\\O}{\\mathrm{O}}NC{\\degrees}{{^\\circ}}NC{\\T}{\\mathsf{T}}NC{\\mathset}[1]{\\mathbb{#1}}NC{\\Real}{\\mathset{R}}NC{\\Integer}{\\mathset{Z}}NC{\\Boolean}{\\mathset{B}}NC{\\Complex}{\\mathset{C}}$$\n'.rp(/NC/g, '\\newcommand');

            markdeepHTML = '<script type="text/x-mathjax-config">MathJax.Hub.Config({ TeX: { equationNumbers: {autoNumber: "AMS"} } });</script>' + 
                '<span style="display:none">' + MATHJAX_COMMANDS + '</span>\n' + markdeepHTML; 
        }
        
        markdeepHTML += MARKDEEP_FOOTER;
        
        // Replace the document. If using MathJax, include the custom Markdeep definitions
        var longDocument = source.length > 1000;
        var head = BODY_STYLESHEET + STYLESHEET + sectionNumberingStylesheet() + HIGHLIGHT_STYLESHEET;
        if (longDocument) {
            // Add more spacing before the title in a long document
            head += entag('style', 'div.title { padding-top: 40px; } div.afterTitles { height: 15px; }');
        }

        if (window.location.href.search(/\?.*export.*/i) !== -1) {
            // Export mode
            var text = '<meta charset="UTF-8"><meta http-equiv="content-type" content="text/html; charset=UTF-8">' + head + document.head.innerHTML + markdeepHTML;
            if (needMathJax) {
                // Dynamically load mathjax
                text += '<script src="https://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML"></script>';
            }
            document.body.innerHTML = entag('code', escapeHTMLEntities(text));
        } else {
            document.head.innerHTML = '<meta charset="UTF-8"><meta http-equiv="content-type" content="text/html; charset=UTF-8">' + head + document.head.innerHTML;
            document.body.innerHTML = markdeepHTML;
            if (needMathJax) {
                // Dynamically load mathjax
                var script = document.createElement("script");
                script.type = "text/javascript";
                script.src  = "https://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML";
                document.getElementsByTagName("head")[0].appendChild(script);
            }
        }

        document.body.style.visibility = 'visible';
    }, 0);
}

})();

/* The following is highlight.min.js 9.3.0 from https://highlightjs.org
    BSD3 License | git.io/hljslicense */
!function(e){var n="object"==typeof window&&window||"object"==typeof self&&self;"undefined"!=typeof exports?e(exports):n&&(n.hljs=e({}),"function"==typeof define&&define.amd&&define([],function(){return n.hljs}))}(function(e){function n(e){return e.replace(/&/gm,"&amp;").replace(/</gm,"&lt;").replace(/>/gm,"&gt;")}function t(e){return e.nodeName.toLowerCase()}function r(e,n){var t=e&&e.exec(n);return t&&0==t.index}function a(e){return/^(no-?highlight|plain|text)$/i.test(e)}function i(e){var n,t,r,i=e.className+" ";if(i+=e.parentNode?e.parentNode.className:"",t=/\blang(?:uage)?-([\w-]+)\b/i.exec(i))return w(t[1])?t[1]:"no-highlight";for(i=i.split(/\s+/),n=0,r=i.length;r>n;n++)if(w(i[n])||a(i[n]))return i[n]}function o(e,n){var t,r={};for(t in e)r[t]=e[t];if(n)for(t in n)r[t]=n[t];return r}function u(e){var n=[];return function r(e,a){for(var i=e.firstChild;i;i=i.nextSibling)3==i.nodeType?a+=i.nodeValue.length:1==i.nodeType&&(n.push({event:"start",offset:a,node:i}),a=r(i,a),t(i).match(/br|hr|img|input/)||n.push({event:"stop",offset:a,node:i}));return a}(e,0),n}function c(e,r,a){function i(){return e.length&&r.length?e[0].offset!=r[0].offset?e[0].offset<r[0].offset?e:r:"start"==r[0].event?e:r:e.length?e:r}function o(e){function r(e){return" "+e.nodeName+'="'+n(e.value)+'"'}f+="<"+t(e)+Array.prototype.map.call(e.attributes,r).join("")+">"}function u(e){f+="</"+t(e)+">"}function c(e){("start"==e.event?o:u)(e.node)}for(var s=0,f="",l=[];e.length||r.length;){var g=i();if(f+=n(a.substr(s,g[0].offset-s)),s=g[0].offset,g==e){l.reverse().forEach(u);do c(g.splice(0,1)[0]),g=i();while(g==e&&g.length&&g[0].offset==s);l.reverse().forEach(o)}else"start"==g[0].event?l.push(g[0].node):l.pop(),c(g.splice(0,1)[0])}return f+n(a.substr(s))}function s(e){function n(e){return e&&e.source||e}function t(t,r){return new RegExp(n(t),"m"+(e.cI?"i":"")+(r?"g":""))}function r(a,i){if(!a.compiled){if(a.compiled=!0,a.k=a.k||a.bK,a.k){var u={},c=function(n,t){e.cI&&(t=t.toLowerCase()),t.split(" ").forEach(function(e){var t=e.split("|");u[t[0]]=[n,t[1]?Number(t[1]):1]})};"string"==typeof a.k?c("keyword",a.k):Object.keys(a.k).forEach(function(e){c(e,a.k[e])}),a.k=u}a.lR=t(a.l||/\w+/,!0),i&&(a.bK&&(a.b="\\b("+a.bK.split(" ").join("|")+")\\b"),a.b||(a.b=/\B|\b/),a.bR=t(a.b),a.e||a.eW||(a.e=/\B|\b/),a.e&&(a.eR=t(a.e)),a.tE=n(a.e)||"",a.eW&&i.tE&&(a.tE+=(a.e?"|":"")+i.tE)),a.i&&(a.iR=t(a.i)),void 0===a.r&&(a.r=1),a.c||(a.c=[]);var s=[];a.c.forEach(function(e){e.v?e.v.forEach(function(n){s.push(o(e,n))}):s.push("self"==e?a:e)}),a.c=s,a.c.forEach(function(e){r(e,a)}),a.starts&&r(a.starts,i);var f=a.c.map(function(e){return e.bK?"\\.?("+e.b+")\\.?":e.b}).concat([a.tE,a.i]).map(n).filter(Boolean);a.t=f.length?t(f.join("|"),!0):{exec:function(){return null}}}}r(e)}function f(e,t,a,i){function o(e,n){for(var t=0;t<n.c.length;t++)if(r(n.c[t].bR,e))return n.c[t]}function u(e,n){if(r(e.eR,n)){for(;e.endsParent&&e.parent;)e=e.parent;return e}return e.eW?u(e.parent,n):void 0}function c(e,n){return!a&&r(n.iR,e)}function g(e,n){var t=N.cI?n[0].toLowerCase():n[0];return e.k.hasOwnProperty(t)&&e.k[t]}function p(e,n,t,r){var a=r?"":E.classPrefix,i='<span class="'+a,o=t?"":"</span>";return i+=e+'">',i+n+o}function h(){if(!k.k)return n(M);var e="",t=0;k.lR.lastIndex=0;for(var r=k.lR.exec(M);r;){e+=n(M.substr(t,r.index-t));var a=g(k,r);a?(B+=a[1],e+=p(a[0],n(r[0]))):e+=n(r[0]),t=k.lR.lastIndex,r=k.lR.exec(M)}return e+n(M.substr(t))}function d(){var e="string"==typeof k.sL;if(e&&!R[k.sL])return n(M);var t=e?f(k.sL,M,!0,y[k.sL]):l(M,k.sL.length?k.sL:void 0);return k.r>0&&(B+=t.r),e&&(y[k.sL]=t.top),p(t.language,t.value,!1,!0)}function b(){L+=void 0!==k.sL?d():h(),M=""}function v(e,n){L+=e.cN?p(e.cN,"",!0):"",k=Object.create(e,{parent:{value:k}})}function m(e,n){if(M+=e,void 0===n)return b(),0;var t=o(n,k);if(t)return t.skip?M+=n:(t.eB&&(M+=n),b(),t.rB||t.eB||(M=n)),v(t,n),t.rB?0:n.length;var r=u(k,n);if(r){var a=k;a.skip?M+=n:(a.rE||a.eE||(M+=n),b(),a.eE&&(M=n));do k.cN&&(L+="</span>"),k.skip||(B+=k.r),k=k.parent;while(k!=r.parent);return r.starts&&v(r.starts,""),a.rE?0:n.length}if(c(n,k))throw new Error('Illegal lexeme "'+n+'" for mode "'+(k.cN||"<unnamed>")+'"');return M+=n,n.length||1}var N=w(e);if(!N)throw new Error('Unknown language: "'+e+'"');s(N);var x,k=i||N,y={},L="";for(x=k;x!=N;x=x.parent)x.cN&&(L=p(x.cN,"",!0)+L);var M="",B=0;try{for(var C,j,I=0;;){if(k.t.lastIndex=I,C=k.t.exec(t),!C)break;j=m(t.substr(I,C.index-I),C[0]),I=C.index+j}for(m(t.substr(I)),x=k;x.parent;x=x.parent)x.cN&&(L+="</span>");return{r:B,value:L,language:e,top:k}}catch(O){if(-1!=O.message.indexOf("Illegal"))return{r:0,value:n(t)};throw O}}function l(e,t){t=t||E.languages||Object.keys(R);var r={r:0,value:n(e)},a=r;return t.filter(w).forEach(function(n){var t=f(n,e,!1);t.language=n,t.r>a.r&&(a=t),t.r>r.r&&(a=r,r=t)}),a.language&&(r.second_best=a),r}function g(e){return E.tabReplace&&(e=e.replace(/^((<[^>]+>|\t)+)/gm,function(e,n){return n.replace(/\t/g,E.tabReplace)})),E.useBR&&(e=e.replace(/\n/g,"<br>")),e}function p(e,n,t){var r=n?x[n]:t,a=[e.trim()];return e.match(/\bhljs\b/)||a.push("hljs"),-1===e.indexOf(r)&&a.push(r),a.join(" ").trim()}function h(e){var n=i(e);if(!a(n)){var t;E.useBR?(t=document.createElementNS("http://www.w3.org/1999/xhtml","div"),t.innerHTML=e.innerHTML.replace(/\n/g,"").replace(/<br[ \/]*>/g,"\n")):t=e;var r=t.textContent,o=n?f(n,r,!0):l(r),s=u(t);if(s.length){var h=document.createElementNS("http://www.w3.org/1999/xhtml","div");h.innerHTML=o.value,o.value=c(s,u(h),r)}o.value=g(o.value),e.innerHTML=o.value,e.className=p(e.className,n,o.language),e.result={language:o.language,re:o.r},o.second_best&&(e.second_best={language:o.second_best.language,re:o.second_best.r})}}function d(e){E=o(E,e)}function b(){if(!b.called){b.called=!0;var e=document.querySelectorAll("pre code");Array.prototype.forEach.call(e,h)}}function v(){addEventListener("DOMContentLoaded",b,!1),addEventListener("load",b,!1)}function m(n,t){var r=R[n]=t(e);r.aliases&&r.aliases.forEach(function(e){x[e]=n})}function N(){return Object.keys(R)}function w(e){return e=(e||"").toLowerCase(),R[e]||R[x[e]]}var E={classPrefix:"hljs-",tabReplace:null,useBR:!1,languages:void 0},R={},x={};return e.highlight=f,e.highlightAuto=l,e.fixMarkup=g,e.highlightBlock=h,e.configure=d,e.initHighlighting=b,e.initHighlightingOnLoad=v,e.registerLanguage=m,e.listLanguages=N,e.getLanguage=w,e.inherit=o,e.IR="[a-zA-Z]\\w*",e.UIR="[a-zA-Z_]\\w*",e.NR="\\b\\d+(\\.\\d+)?",e.CNR="(-?)(\\b0[xX][a-fA-F0-9]+|(\\b\\d+(\\.\\d*)?|\\.\\d+)([eE][-+]?\\d+)?)",e.BNR="\\b(0b[01]+)",e.RSR="!|!=|!==|%|%=|&|&&|&=|\\*|\\*=|\\+|\\+=|,|-|-=|/=|/|:|;|<<|<<=|<=|<|===|==|=|>>>=|>>=|>=|>>>|>>|>|\\?|\\[|\\{|\\(|\\^|\\^=|\\||\\|=|\\|\\||~",e.BE={b:"\\\\[\\s\\S]",r:0},e.ASM={cN:"string",b:"'",e:"'",i:"\\n",c:[e.BE]},e.QSM={cN:"string",b:'"',e:'"',i:"\\n",c:[e.BE]},e.PWM={b:/\b(a|an|the|are|I'm|isn't|don't|doesn't|won't|but|just|should|pretty|simply|enough|gonna|going|wtf|so|such|will|you|your|like)\b/},e.C=function(n,t,r){var a=e.inherit({cN:"comment",b:n,e:t,c:[]},r||{});return a.c.push(e.PWM),a.c.push({cN:"doctag",b:"(?:TODO|FIXME|NOTE|BUG|XXX):",r:0}),a},e.CLCM=e.C("//","$"),e.CBCM=e.C("/\\*","\\*/"),e.HCM=e.C("#","$"),e.NM={cN:"number",b:e.NR,r:0},e.CNM={cN:"number",b:e.CNR,r:0},e.BNM={cN:"number",b:e.BNR,r:0},e.CSSNM={cN:"number",b:e.NR+"(%|em|ex|ch|rem|vw|vh|vmin|vmax|cm|mm|in|pt|pc|px|deg|grad|rad|turn|s|ms|Hz|kHz|dpi|dpcm|dppx)?",r:0},e.RM={cN:"regexp",b:/\//,e:/\/[gimuy]*/,i:/\n/,c:[e.BE,{b:/\[/,e:/\]/,r:0,c:[e.BE]}]},e.TM={cN:"title",b:e.IR,r:0},e.UTM={cN:"title",b:e.UIR,r:0},e.METHOD_GUARD={b:"\\.\\s*"+e.UIR,r:0},e});hljs.registerLanguage("bash",function(e){var t={cN:"variable",v:[{b:/\$[\w\d#@][\w\d_]*/},{b:/\$\{(.*?)}/}]},s={cN:"string",b:/"/,e:/"/,c:[e.BE,t,{cN:"variable",b:/\$\(/,e:/\)/,c:[e.BE]}]},a={cN:"string",b:/'/,e:/'/};return{aliases:["sh","zsh"],l:/-?[a-z\.]+/,k:{keyword:"if then else elif fi for while in do done case esac function",literal:"true false",built_in:"break cd continue eval exec exit export getopts hash pwd readonly return shift test times trap umask unset alias bind builtin caller command declare echo enable help let local logout mapfile printf read readarray source type typeset ulimit unalias set shopt autoload bg bindkey bye cap chdir clone comparguments compcall compctl compdescribe compfiles compgroups compquote comptags comptry compvalues dirs disable disown echotc echoti emulate fc fg float functions getcap getln history integer jobs kill limit log noglob popd print pushd pushln rehash sched setcap setopt stat suspend ttyctl unfunction unhash unlimit unsetopt vared wait whence where which zcompile zformat zftp zle zmodload zparseopts zprof zpty zregexparse zsocket zstyle ztcp",_:"-ne -eq -lt -gt -f -d -e -s -l -a"},c:[{cN:"meta",b:/^#![^\n]+sh\s*$/,r:10},{cN:"function",b:/\w[\w\d_]*\s*\(\s*\)\s*\{/,rB:!0,c:[e.inherit(e.TM,{b:/\w[\w\d_]*/})],r:0},e.HCM,s,a,t]}});hljs.registerLanguage("xml",function(s){var e="[A-Za-z0-9\\._:-]+",t={eW:!0,i:/</,r:0,c:[{cN:"attr",b:e,r:0},{b:/=\s*/,r:0,c:[{cN:"string",endsParent:!0,v:[{b:/"/,e:/"/},{b:/'/,e:/'/},{b:/[^\s"'=<>`]+/}]}]}]};return{aliases:["html","xhtml","rss","atom","xsl","plist"],cI:!0,c:[{cN:"meta",b:"<!DOCTYPE",e:">",r:10,c:[{b:"\\[",e:"\\]"}]},s.C("<!--","-->",{r:10}),{b:"<\\!\\[CDATA\\[",e:"\\]\\]>",r:10},{b:/<\?(php)?/,e:/\?>/,sL:"php",c:[{b:"/\\*",e:"\\*/",skip:!0}]},{cN:"tag",b:"<style(?=\\s|>|$)",e:">",k:{name:"style"},c:[t],starts:{e:"</style>",rE:!0,sL:["css","xml"]}},{cN:"tag",b:"<script(?=\\s|>|$)",e:">",k:{name:"script"},c:[t],starts:{e:"</script>",rE:!0,sL:["actionscript","javascript","handlebars","xml"]}},{cN:"meta",v:[{b:/<\?xml/,e:/\?>/,r:10},{b:/<\?\w+/,e:/\?>/}]},{cN:"tag",b:"</?",e:"/?>",c:[{cN:"name",b:/[^\/><\s]+/,r:0},t]}]}});hljs.registerLanguage("apache",function(e){var r={cN:"number",b:"[\\$%]\\d+"};return{aliases:["apacheconf"],cI:!0,c:[e.HCM,{cN:"section",b:"</?",e:">"},{cN:"attribute",b:/\w+/,r:0,k:{nomarkup:"order deny allow setenv rewriterule rewriteengine rewritecond documentroot sethandler errordocument loadmodule options header listen serverroot servername"},starts:{e:/$/,r:0,k:{literal:"on off all"},c:[{cN:"meta",b:"\\s\\[",e:"\\]$"},{cN:"variable",b:"[\\$%]\\{",e:"\\}",c:["self",r]},r,e.QSM]}}],i:/\S/}});hljs.registerLanguage("java",function(e){var t=e.UIR+"(<"+e.UIR+"(\\s*,\\s*"+e.UIR+")*>)?",a="false synchronized int abstract float private char boolean static null if const for true while long strictfp finally protected import native final void enum else break transient catch instanceof byte super volatile case assert short package default double public try this switch continue throws protected public private module requires exports",r="\\b(0[bB]([01]+[01_]+[01]+|[01]+)|0[xX]([a-fA-F0-9]+[a-fA-F0-9_]+[a-fA-F0-9]+|[a-fA-F0-9]+)|(([\\d]+[\\d_]+[\\d]+|[\\d]+)(\\.([\\d]+[\\d_]+[\\d]+|[\\d]+))?|\\.([\\d]+[\\d_]+[\\d]+|[\\d]+))([eE][-+]?\\d+)?)[lLfF]?",s={cN:"number",b:r,r:0};return{aliases:["jsp"],k:a,i:/<\/|#/,c:[e.C("/\\*\\*","\\*/",{r:0,c:[{b:/\w+@/,r:0},{cN:"doctag",b:"@[A-Za-z]+"}]}),e.CLCM,e.CBCM,e.ASM,e.QSM,{cN:"class",bK:"class interface",e:/[{;=]/,eE:!0,k:"class interface",i:/[:"\[\]]/,c:[{bK:"extends implements"},e.UTM]},{bK:"new throw return else",r:0},{cN:"function",b:"("+t+"\\s+)+"+e.UIR+"\\s*\\(",rB:!0,e:/[{;=]/,eE:!0,k:a,c:[{b:e.UIR+"\\s*\\(",rB:!0,r:0,c:[e.UTM]},{cN:"params",b:/\(/,e:/\)/,k:a,r:0,c:[e.ASM,e.QSM,e.CNM,e.CBCM]},e.CLCM,e.CBCM]},s,{cN:"meta",b:"@[A-Za-z]+"}]}});hljs.registerLanguage("perl",function(e){var t="getpwent getservent quotemeta msgrcv scalar kill dbmclose undef lc ma syswrite tr send umask sysopen shmwrite vec qx utime local oct semctl localtime readpipe do return format read sprintf dbmopen pop getpgrp not getpwnam rewinddir qqfileno qw endprotoent wait sethostent bless s|0 opendir continue each sleep endgrent shutdown dump chomp connect getsockname die socketpair close flock exists index shmgetsub for endpwent redo lstat msgctl setpgrp abs exit select print ref gethostbyaddr unshift fcntl syscall goto getnetbyaddr join gmtime symlink semget splice x|0 getpeername recv log setsockopt cos last reverse gethostbyname getgrnam study formline endhostent times chop length gethostent getnetent pack getprotoent getservbyname rand mkdir pos chmod y|0 substr endnetent printf next open msgsnd readdir use unlink getsockopt getpriority rindex wantarray hex system getservbyport endservent int chr untie rmdir prototype tell listen fork shmread ucfirst setprotoent else sysseek link getgrgid shmctl waitpid unpack getnetbyname reset chdir grep split require caller lcfirst until warn while values shift telldir getpwuid my getprotobynumber delete and sort uc defined srand accept package seekdir getprotobyname semop our rename seek if q|0 chroot sysread setpwent no crypt getc chown sqrt write setnetent setpriority foreach tie sin msgget map stat getlogin unless elsif truncate exec keys glob tied closedirioctl socket readlink eval xor readline binmode setservent eof ord bind alarm pipe atan2 getgrent exp time push setgrent gt lt or ne m|0 break given say state when",r={cN:"subst",b:"[$@]\\{",e:"\\}",k:t},s={b:"->{",e:"}"},n={v:[{b:/\$\d/},{b:/[\$%@](\^\w\b|#\w+(::\w+)*|{\w+}|\w+(::\w*)*)/},{b:/[\$%@][^\s\w{]/,r:0}]},i=[e.BE,r,n],o=[n,e.HCM,e.C("^\\=\\w","\\=cut",{eW:!0}),s,{cN:"string",c:i,v:[{b:"q[qwxr]?\\s*\\(",e:"\\)",r:5},{b:"q[qwxr]?\\s*\\[",e:"\\]",r:5},{b:"q[qwxr]?\\s*\\{",e:"\\}",r:5},{b:"q[qwxr]?\\s*\\|",e:"\\|",r:5},{b:"q[qwxr]?\\s*\\<",e:"\\>",r:5},{b:"qw\\s+q",e:"q",r:5},{b:"'",e:"'",c:[e.BE]},{b:'"',e:'"'},{b:"`",e:"`",c:[e.BE]},{b:"{\\w+}",c:[],r:0},{b:"-?\\w+\\s*\\=\\>",c:[],r:0}]},{cN:"number",b:"(\\b0[0-7_]+)|(\\b0x[0-9a-fA-F_]+)|(\\b[1-9][0-9_]*(\\.[0-9_]+)?)|[0_]\\b",r:0},{b:"(\\/\\/|"+e.RSR+"|\\b(split|return|print|reverse|grep)\\b)\\s*",k:"split return print reverse grep",r:0,c:[e.HCM,{cN:"regexp",b:"(s|tr|y)/(\\\\.|[^/])*/(\\\\.|[^/])*/[a-z]*",r:10},{cN:"regexp",b:"(m|qr)?/",e:"/[a-z]*",c:[e.BE],r:0}]},{cN:"function",bK:"sub",e:"(\\s*\\(.*?\\))?[;{]",eE:!0,r:5,c:[e.TM]},{b:"-\\w\\b",r:0},{b:"^__DATA__$",e:"^__END__$",sL:"mojolicious",c:[{b:"^@@.*",e:"$",cN:"comment"}]}];return r.c=o,s.c=o,{aliases:["pl","pm"],l:/[\w\.]+/,k:t,c:o}});hljs.registerLanguage("css",function(e){var c="[a-zA-Z-][a-zA-Z0-9_-]*",t={b:/[A-Z\_\.\-]+\s*:/,rB:!0,e:";",eW:!0,c:[{cN:"attribute",b:/\S/,e:":",eE:!0,starts:{eW:!0,eE:!0,c:[{b:/[\w-]+\(/,rB:!0,c:[{cN:"built_in",b:/[\w-]+/},{b:/\(/,e:/\)/,c:[e.ASM,e.QSM]}]},e.CSSNM,e.QSM,e.ASM,e.CBCM,{cN:"number",b:"#[0-9A-Fa-f]+"},{cN:"meta",b:"!important"}]}}]};return{cI:!0,i:/[=\/|'\$]/,c:[e.CBCM,{cN:"selector-id",b:/#[A-Za-z0-9_-]+/},{cN:"selector-class",b:/\.[A-Za-z0-9_-]+/},{cN:"selector-attr",b:/\[/,e:/\]/,i:"$"},{cN:"selector-pseudo",b:/:(:)?[a-zA-Z0-9\_\-\+\(\)"'.]+/},{b:"@(font-face|page)",l:"[a-z-]+",k:"font-face page"},{b:"@",e:"[{;]",i:/:/,c:[{cN:"keyword",b:/\w+/},{b:/\s/,eW:!0,eE:!0,r:0,c:[e.ASM,e.QSM,e.CSSNM]}]},{cN:"selector-tag",b:c,r:0},{b:"{",e:"}",i:/\S/,c:[e.CBCM,t]}]}});hljs.registerLanguage("ruby",function(e){var r="[a-zA-Z_]\\w*[!?=]?|[-+~]\\@|<<|>>|=~|===?|<=>|[<>]=?|\\*\\*|[-/+%^&*~`|]|\\[\\]=?",b={keyword:"and then defined module in return redo if BEGIN retry end for self when next until do begin unless END rescue else break undef not super class case require yield alias while ensure elsif or include attr_reader attr_writer attr_accessor",literal:"true false nil"},c={cN:"doctag",b:"@[A-Za-z]+"},a={b:"#<",e:">"},s=[e.C("#","$",{c:[c]}),e.C("^\\=begin","^\\=end",{c:[c],r:10}),e.C("^__END__","\\n$")],n={cN:"subst",b:"#\\{",e:"}",k:b},t={cN:"string",c:[e.BE,n],v:[{b:/'/,e:/'/},{b:/"/,e:/"/},{b:/`/,e:/`/},{b:"%[qQwWx]?\\(",e:"\\)"},{b:"%[qQwWx]?\\[",e:"\\]"},{b:"%[qQwWx]?{",e:"}"},{b:"%[qQwWx]?<",e:">"},{b:"%[qQwWx]?/",e:"/"},{b:"%[qQwWx]?%",e:"%"},{b:"%[qQwWx]?-",e:"-"},{b:"%[qQwWx]?\\|",e:"\\|"},{b:/\B\?(\\\d{1,3}|\\x[A-Fa-f0-9]{1,2}|\\u[A-Fa-f0-9]{4}|\\?\S)\b/}]},i={cN:"params",b:"\\(",e:"\\)",endsParent:!0,k:b},d=[t,a,{cN:"class",bK:"class module",e:"$|;",i:/=/,c:[e.inherit(e.TM,{b:"[A-Za-z_]\\w*(::\\w+)*(\\?|\\!)?"}),{b:"<\\s*",c:[{b:"("+e.IR+"::)?"+e.IR}]}].concat(s)},{cN:"function",bK:"def",e:"$|;",c:[e.inherit(e.TM,{b:r}),i].concat(s)},{b:e.IR+"::"},{cN:"symbol",b:e.UIR+"(\\!|\\?)?:",r:0},{cN:"symbol",b:":(?!\\s)",c:[t,{b:r}],r:0},{cN:"number",b:"(\\b0[0-7_]+)|(\\b0x[0-9a-fA-F_]+)|(\\b[1-9][0-9_]*(\\.[0-9_]+)?)|[0_]\\b",r:0},{b:"(\\$\\W)|((\\$|\\@\\@?)(\\w+))"},{cN:"params",b:/\|/,e:/\|/,k:b},{b:"("+e.RSR+")\\s*",c:[a,{cN:"regexp",c:[e.BE,n],i:/\n/,v:[{b:"/",e:"/[a-z]*"},{b:"%r{",e:"}[a-z]*"},{b:"%r\\(",e:"\\)[a-z]*"},{b:"%r!",e:"![a-z]*"},{b:"%r\\[",e:"\\][a-z]*"}]}].concat(s),r:0}].concat(s);n.c=d,i.c=d;var l="[>?]>",o="[\\w#]+\\(\\w+\\):\\d+:\\d+>",u="(\\w+-)?\\d+\\.\\d+\\.\\d(p\\d+)?[^>]+>",w=[{b:/^\s*=>/,starts:{e:"$",c:d}},{cN:"meta",b:"^("+l+"|"+o+"|"+u+")",starts:{e:"$",c:d}}];return{aliases:["rb","gemspec","podspec","thor","irb"],k:b,i:/\/\*/,c:s.concat(w).concat(d)}});hljs.registerLanguage("coffeescript",function(e){var c={keyword:"in if for while finally new do return else break catch instanceof throw try this switch continue typeof delete debugger super then unless until loop of by when and or is isnt not",literal:"true false null undefined yes no on off",built_in:"npm require console print module global window document"},n="[A-Za-z$_][0-9A-Za-z$_]*",r={cN:"subst",b:/#\{/,e:/}/,k:c},s=[e.BNM,e.inherit(e.CNM,{starts:{e:"(\\s*/)?",r:0}}),{cN:"string",v:[{b:/'''/,e:/'''/,c:[e.BE]},{b:/'/,e:/'/,c:[e.BE]},{b:/"""/,e:/"""/,c:[e.BE,r]},{b:/"/,e:/"/,c:[e.BE,r]}]},{cN:"regexp",v:[{b:"///",e:"///",c:[r,e.HCM]},{b:"//[gim]*",r:0},{b:/\/(?![ *])(\\\/|.)*?\/[gim]*(?=\W|$)/}]},{b:"@"+n},{b:"`",e:"`",eB:!0,eE:!0,sL:"javascript"}];r.c=s;var i=e.inherit(e.TM,{b:n}),t="(\\(.*\\))?\\s*\\B[-=]>",o={cN:"params",b:"\\([^\\(]",rB:!0,c:[{b:/\(/,e:/\)/,k:c,c:["self"].concat(s)}]};return{aliases:["coffee","cson","iced"],k:c,i:/\/\*/,c:s.concat([e.C("###","###"),e.HCM,{cN:"function",b:"^\\s*"+n+"\\s*=\\s*"+t,e:"[-=]>",rB:!0,c:[i,o]},{b:/[:\(,=]\s*/,r:0,c:[{cN:"function",b:t,e:"[-=]>",rB:!0,c:[o]}]},{cN:"class",bK:"class",e:"$",i:/[:="\[\]]/,c:[{bK:"extends",eW:!0,i:/[:="\[\]]/,c:[i]},i]},{b:n+":",e:":",rB:!0,rE:!0,r:0}])}});hljs.registerLanguage("http",function(e){var t="HTTP/[0-9\\.]+";return{aliases:["https"],i:"\\S",c:[{b:"^"+t,e:"$",c:[{cN:"number",b:"\\b\\d{3}\\b"}]},{b:"^[A-Z]+ (.*?) "+t+"$",rB:!0,e:"$",c:[{cN:"string",b:" ",e:" ",eB:!0,eE:!0},{b:t},{cN:"keyword",b:"[A-Z]+"}]},{cN:"attribute",b:"^\\w",e:": ",eE:!0,i:"\\n|\\s|=",starts:{e:"$",r:0}},{b:"\\n\\n",starts:{sL:[],eW:!0}}]}});hljs.registerLanguage("makefile",function(e){var a={cN:"variable",b:/\$\(/,e:/\)/,c:[e.BE]};return{aliases:["mk","mak"],c:[e.HCM,{b:/^\w+\s*\W*=/,rB:!0,r:0,starts:{e:/\s*\W*=/,eE:!0,starts:{e:/$/,r:0,c:[a]}}},{cN:"section",b:/^[\w]+:\s*$/},{cN:"meta",b:/^\.PHONY:/,e:/$/,k:{"meta-keyword":".PHONY"},l:/[\.\w]+/},{b:/^\t+/,e:/$/,r:0,c:[e.QSM,a]}]}});hljs.registerLanguage("cs",function(e){var r={keyword:"abstract as base bool break byte case catch char checked const continue decimal dynamic default delegate do double else enum event explicit extern finally fixed float for foreach goto if implicit in int interface internal is lock long when object operator out override params private protected public readonly ref sbyte sealed short sizeof stackalloc static string struct switch this try typeof uint ulong unchecked unsafe ushort using virtual volatile void while async protected public private internal ascending descending from get group into join let orderby partial select set value var where yield",literal:"null false true"},t=e.IR+"(<"+e.IR+">)?(\\[\\])?";return{aliases:["csharp"],k:r,i:/::/,c:[e.C("///","$",{rB:!0,c:[{cN:"doctag",v:[{b:"///",r:0},{b:"<!--|-->"},{b:"</?",e:">"}]}]}),e.CLCM,e.CBCM,{cN:"meta",b:"#",e:"$",k:{"meta-keyword":"if else elif endif define undef warning error line region endregion pragma checksum"}},{cN:"string",b:'@"',e:'"',c:[{b:'""'}]},e.ASM,e.QSM,e.CNM,{bK:"class interface",e:/[{;=]/,i:/[^\s:]/,c:[e.TM,e.CLCM,e.CBCM]},{bK:"namespace",e:/[{;=]/,i:/[^\s:]/,c:[e.inherit(e.TM,{b:"[a-zA-Z](\\.?\\w)*"}),e.CLCM,e.CBCM]},{bK:"new return throw await",r:0},{cN:"function",b:"("+t+"\\s+)+"+e.IR+"\\s*\\(",rB:!0,e:/[{;=]/,eE:!0,k:r,c:[{b:e.IR+"\\s*\\(",rB:!0,c:[e.TM],r:0},{cN:"params",b:/\(/,e:/\)/,eB:!0,eE:!0,k:r,r:0,c:[e.ASM,e.QSM,e.CNM,e.CBCM]},e.CLCM,e.CBCM]}]}});hljs.registerLanguage("sql",function(e){var t=e.C("--","$");return{cI:!0,i:/[<>{}*#]/,c:[{bK:"begin end start commit rollback savepoint lock alter create drop rename call delete do handler insert load replace select truncate update set show pragma grant merge describe use explain help declare prepare execute deallocate release unlock purge reset change stop analyze cache flush optimize repair kill install uninstall checksum restore check backup revoke",e:/;/,eW:!0,l:/[\w\.]+/,k:{keyword:"",literal:"true false null",built_in:"array bigint binary bit blob boolean char character date dec decimal float int int8 integer interval number numeric real record serial serial8 smallint text varchar varying void"},c:[{cN:"string",b:"'",e:"'",c:[e.BE,{b:"''"}]},{cN:"string",b:'"',e:'"',c:[e.BE,{b:'""'}]},{cN:"string",b:"`",e:"`",c:[e.BE]},e.CNM,e.CBCM,t]},e.CBCM,t]}});hljs.registerLanguage("python",function(e){var r={cN:"meta",b:/^(>>>|\.\.\.) /},b={cN:"string",c:[e.BE],v:[{b:/(u|b)?r?'''/,e:/'''/,c:[r],r:10},{b:/(u|b)?r?"""/,e:/"""/,c:[r],r:10},{b:/(u|r|ur)'/,e:/'/,r:10},{b:/(u|r|ur)"/,e:/"/,r:10},{b:/(b|br)'/,e:/'/},{b:/(b|br)"/,e:/"/},e.ASM,e.QSM]},a={cN:"number",r:0,v:[{b:e.BNR+"[lLjJ]?"},{b:"\\b(0o[0-7]+)[lLjJ]?"},{b:e.CNR+"[lLjJ]?"}]},l={cN:"params",b:/\(/,e:/\)/,c:["self",r,a,b]};return{aliases:["py","gyp"],k:{keyword:"and elif is global as in if from raise for except finally print import pass return exec else break not with class assert yield try while continue del or def lambda async await nonlocal|10 None True False",built_in:"Ellipsis NotImplemented"},i:/(<\/|->|\?)/,c:[r,a,b,e.HCM,{v:[{cN:"function",bK:"def",r:10},{cN:"class",bK:"class"}],e:/:/,i:/[${=;\n,]/,c:[e.UTM,l,{b:/->/,eW:!0,k:"None"}]},{cN:"meta",b:/^[\t ]*@/,e:/$/},{b:/\b(print|exec)\(/}]}});hljs.registerLanguage("objectivec",function(e){var t={cN:"built_in",b:"(AV|CA|CF|CG|CI|MK|MP|NS|UI|XC)\\w+"},i={keyword:"int float while char export sizeof typedef const struct for union unsigned long volatile static bool mutable if do return goto void enum else break extern asm case short default double register explicit signed typename this switch continue wchar_t inline readonly assign readwrite self @synchronized id typeof nonatomic super unichar IBOutlet IBAction strong weak copy in out inout bycopy byref oneway __strong __weak __block __autoreleasing @private @protected @public @try @property @end @throw @catch @finally @autoreleasepool @synthesize @dynamic @selector @optional @required",literal:"false true FALSE TRUE nil YES NO NULL",built_in:"BOOL dispatch_once_t dispatch_queue_t dispatch_sync dispatch_async dispatch_once"},n=/[a-zA-Z@][a-zA-Z0-9_]*/,o="@interface @class @protocol @implementation";return{aliases:["mm","objc","obj-c"],k:i,l:n,i:"</",c:[t,e.CLCM,e.CBCM,e.CNM,e.QSM,{cN:"string",v:[{b:'@"',e:'"',i:"\\n",c:[e.BE]},{b:"'",e:"[^\\\\]'",i:"[^\\\\][^']"}]},{cN:"meta",b:"#",e:"$",c:[{cN:"meta-string",v:[{b:'"',e:'"'},{b:"<",e:">"}]}]},{cN:"class",b:"("+o.split(" ").join("|")+")\\b",e:"({|$)",eE:!0,k:o,l:n,c:[e.UTM]},{b:"\\."+e.UIR,r:0}]}});hljs.registerLanguage("php",function(e){var c={b:"\\$+[a-zA-Z_-ÿ][a-zA-Z0-9_-ÿ]*"},a={cN:"meta",b:/<\?(php)?|\?>/},i={cN:"string",c:[e.BE,a],v:[{b:'b"',e:'"'},{b:"b'",e:"'"},e.inherit(e.ASM,{i:null}),e.inherit(e.QSM,{i:null})]},t={v:[e.BNM,e.CNM]};return{aliases:["php3","php4","php5","php6"],cI:!0,k:"and include_once list abstract global private echo interface as static endswitch array null if endwhile or const for endforeach self var while isset public protected exit foreach throw elseif include __FILE__ empty require_once do xor return parent clone use __CLASS__ __LINE__ else break print eval new catch __METHOD__ case exception default die require __FUNCTION__ enddeclare final try switch continue endfor endif declare unset true false trait goto instanceof insteadof __DIR__ __NAMESPACE__ yield finally",c:[e.HCM,e.C("//","$",{c:[a]}),e.C("/\\*","\\*/",{c:[{cN:"doctag",b:"@[A-Za-z]+"}]}),e.C("__halt_compiler.+?;",!1,{eW:!0,k:"__halt_compiler",l:e.UIR}),{cN:"string",b:/<<<['"]?\w+['"]?$/,e:/^\w+;?$/,c:[e.BE,{cN:"subst",v:[{b:/\$\w+/},{b:/\{\$/,e:/\}/}]}]},a,c,{b:/(::|->)+[a-zA-Z_\x7f-\xff][a-zA-Z0-9_\x7f-\xff]*/},{cN:"function",bK:"function",e:/[;{]/,eE:!0,i:"\\$|\\[|%",c:[e.UTM,{cN:"params",b:"\\(",e:"\\)",c:["self",c,e.CBCM,i,t]}]},{cN:"class",bK:"class interface",e:"{",eE:!0,i:/[:\(\$"]/,c:[{bK:"extends implements"},e.UTM]},{bK:"namespace",e:";",i:/[\.']/,c:[e.UTM]},{bK:"use",e:";",c:[e.UTM]},{b:"=>"},i,t]}});hljs.registerLanguage("javascript",function(e){return{aliases:["js","jsx"],k:{keyword:"in of if for while finally var new function do return void else break catch instanceof with throw case default try this switch continue typeof delete let yield const export super debugger as async await static import from as",literal:"true false null undefined NaN Infinity",built_in:"eval isFinite isNaN parseFloat parseInt decodeURI decodeURIComponent encodeURI encodeURIComponent escape unescape Object Function Boolean Error EvalError InternalError RangeError ReferenceError StopIteration SyntaxError TypeError URIError Number Math Date String RegExp Array Float32Array Float64Array Int16Array Int32Array Int8Array Uint16Array Uint32Array Uint8Array Uint8ClampedArray ArrayBuffer DataView JSON Intl arguments require module console window document Symbol Set Map WeakSet WeakMap Proxy Reflect Promise"},c:[{cN:"meta",r:10,b:/^\s*['"]use (strict|asm)['"]/},{cN:"meta",b:/^#!/,e:/$/},e.ASM,e.QSM,{cN:"string",b:"`",e:"`",c:[e.BE,{cN:"subst",b:"\\$\\{",e:"\\}"}]},e.CLCM,e.CBCM,{cN:"number",v:[{b:"\\b(0[bB][01]+)"},{b:"\\b(0[oO][0-7]+)"},{b:e.CNR}],r:0},{b:"("+e.RSR+"|\\b(case|return|throw)\\b)\\s*",k:"return throw case",c:[e.CLCM,e.CBCM,e.RM,{b:/</,e:/(\/\w+|\w+\/)>/,sL:"xml",c:[{b:/<\w+\s*\/>/,skip:!0},{b:/<\w+/,e:/(\/\w+|\w+\/)>/,skip:!0,c:["self"]}]}],r:0},{cN:"function",bK:"function",e:/\{/,eE:!0,c:[e.inherit(e.TM,{b:/[A-Za-z$_][0-9A-Za-z$_]*/}),{cN:"params",b:/\(/,e:/\)/,eB:!0,eE:!0,c:[e.CLCM,e.CBCM]}],i:/\[|%/},{b:/\$[(.]/},e.METHOD_GUARD,{cN:"class",bK:"class",e:/[{;=]/,eE:!0,i:/[:"\[\]]/,c:[{bK:"extends"},e.UTM]},{bK:"constructor",e:/\{/,eE:!0}],i:/#(?!!)/}});hljs.registerLanguage("json",function(e){var i={literal:"true false null"},n=[e.QSM,e.CNM],r={e:",",eW:!0,eE:!0,c:n,k:i},t={b:"{",e:"}",c:[{cN:"attr",b:/"/,e:/"/,c:[e.BE],i:"\\n"},e.inherit(r,{b:/:/})],i:"\\S"},c={b:"\\[",e:"\\]",c:[e.inherit(r)],i:"\\S"};return n.splice(n.length,0,t,c),{c:n,k:i,i:"\\S"}});hljs.registerLanguage("cpp",function(t){var e={cN:"keyword",b:"\\b[a-z\\d_]*_t\\b"},r={cN:"string",v:[t.inherit(t.QSM,{b:'((u8?|U)|L)?"'}),{b:'(u8?|U)?R"',e:'"',c:[t.BE]},{b:"'\\\\?.",e:"'",i:"."}]},i={cN:"number",v:[{b:"\\b(\\d+(\\.\\d*)?|\\.\\d+)(u|U|l|L|ul|UL|f|F)"},{b:t.CNR}],r:0},s={cN:"meta",b:"#",e:"$",k:{"meta-keyword":"if else elif endif define undef warning error line pragma ifdef ifndef"},c:[{b:/\\\n/,r:0},{bK:"include",e:"$",k:{"meta-keyword":"include"},c:[t.inherit(r,{cN:"meta-string"}),{cN:"meta-string",b:"<",e:">",i:"\\n"}]},r,t.CLCM,t.CBCM]},a=t.IR+"\\s*\\(",c={keyword:"int float while private char catch export virtual operator sizeof dynamic_cast|10 typedef const_cast|10 const struct for static_cast|10 union namespace unsigned long volatile static protected bool template mutable if public friend do goto auto void enum else break extern using class asm case typeid short reinterpret_cast|10 default double register explicit signed typename try this switch continue inline delete alignof constexpr decltype noexcept static_assert thread_local restrict _Bool complex _Complex _Imaginary atomic_bool atomic_char atomic_schar atomic_uchar atomic_short atomic_ushort atomic_int atomic_uint atomic_long atomic_ulong atomic_llong atomic_ullong",built_in:"std string cin cout cerr clog stdin stdout stderr stringstream istringstream ostringstream auto_ptr deque list queue stack vector map set bitset multiset multimap unordered_set unordered_map unordered_multiset unordered_multimap array shared_ptr abort abs acos asin atan2 atan calloc ceil cosh cos exit exp fabs floor fmod fprintf fputs free frexp fscanf isalnum isalpha iscntrl isdigit isgraph islower isprint ispunct isspace isupper isxdigit tolower toupper labs ldexp log10 log malloc realloc memchr memcmp memcpy memset modf pow printf putchar puts scanf sinh sin snprintf sprintf sqrt sscanf strcat strchr strcmp strcpy strcspn strlen strncat strncmp strncpy strpbrk strrchr strspn strstr tanh tan vfprintf vprintf vsprintf endl initializer_list unique_ptr",literal:"true false nullptr NULL"},n=[e,t.CLCM,t.CBCM,i,r];return{aliases:["c","cc","h","c++","h++","hpp"],k:c,i:"</",c:n.concat([s,{b:"\\b(deque|list|queue|stack|vector|map|set|bitset|multiset|multimap|unordered_map|unordered_set|unordered_multiset|unordered_multimap|array)\\s*<",e:">",k:c,c:["self",e]},{b:t.IR+"::",k:c},{v:[{b:/=/,e:/;/},{b:/\(/,e:/\)/},{bK:"new throw return else",e:/;/}],k:c,c:n.concat([{b:/\(/,e:/\)/,c:n.concat(["self"]),r:0}]),r:0},{cN:"function",b:"("+t.IR+"[\\*&\\s]+)+"+a,rB:!0,e:/[{;=]/,eE:!0,k:c,i:/[^\w\s\*&]/,c:[{b:a,rB:!0,c:[t.TM],r:0},{cN:"params",b:/\(/,e:/\)/,k:c,r:0,c:[t.CLCM,t.CBCM,r,i]},t.CLCM,t.CBCM,s]}])}});

// To reduce script size in the minified file, I cut these keywords from less-frequently used languages:
//abort absolute acc acce accep accept access accessed accessible account acos action activate add addtime admin administer advanced advise aes_decrypt aes_encrypt after agent aggregate ali alia alias allocate allow alter always analyze ancillary and any anydata anydataset anyschema anytype apply archive archived archivelog are as asc ascii asin assembly assertion associate asynchronous at atan atn2 attr attri attrib attribu attribut attribute attributes audit authenticated authentication authid authors auto autoallocate autodblink autoextend automatic availability avg backup badfile basicfile before begin beginning benchmark between bfile bfile_base big bigfile bin binary_double binary_float binlog bit_and bit_count bit_length bit_or bit_xor bitmap blob_base block blocksize body both bound buffer_cache buffer_pool build bulk by byte byteordermark bytes c cache caching call calling cancel capacity cascade cascaded case cast catalog category ceil ceiling chain change changed char_base char_length character_length characters characterset charindex charset charsetform charsetid check checksum checksum_agg child choose chr chunk class cleanup clear client clob clob_base clone close cluster_id cluster_probability cluster_set clustering coalesce coercibility col collate collation collect colu colum column column_value columns columns_updated comment commit compact compatibility compiled complete composite_limit compound compress compute concat concat_ws concurrent confirm conn connec connect connect_by_iscycle connect_by_isleaf connect_by_root connect_time connection consider consistent constant constraint constraints constructor container content contents context contributors controlfile conv convert convert_tz corr corr_k corr_s corresponding corruption cos cost count count_big counted covar_pop covar_samp cpu_per_call cpu_per_session crc32 create creation critical cross cube cume_dist curdate current current_date current_time current_timestamp current_user cursor curtime customdatum cycle d data database databases datafile datafiles datalength date_add date_cache date_format date_sub dateadd datediff datefromparts datename datepart datetime2fromparts day day_to_second dayname dayofmonth dayofweek dayofyear days db_role_change dbtimezone ddl deallocate declare decode decompose decrement decrypt deduplicate def defa defau defaul default defaults deferred defi defin define degrees delayed delegate delete delete_all delimited demand dense_rank depth dequeue des_decrypt des_encrypt des_key_file desc descr descri describ describe descriptor deterministic diagnostics difference dimension direct_load directory disable disable_all disallow disassociate discardfile disconnect diskgroup distinct distinctrow distribute distributed div do document domain dotnet double downgrade drop dumpfile duplicate duration e each edition editionable editions element ellipsis else elsif elt empty enable enable_all enclosed encode encoding encrypt end end-exec endian enforced engine engines enqueue enterprise entityescaping eomonth error errors escaped evalname evaluate event eventdata events except exception exceptions exchange exclude excluding execu execut execute exempt exists exit exp expire explain export export_set extended extent external external_1 external_2 externally extract f failed failed_login_attempts failover failure far fast feature_set feature_value fetch field fields file file_name_convert filesystem_like_logging final finish first first_value fixed flash_cache flashback floor flush following follows for forall force form forma format found found_rows freelist freelists freepools fresh from from_base64 from_days ftp full function g general generated get get_format get_lock getdate getutcdate global global_name globally go goto grant grants greatest group group_concat group_id grouping grouping_id groups gtid_subtract guarantee guard handler hash hashkeys having hea head headi headin heading heap help hex hierarchy high high_priority hosts hour http i id ident_current ident_incr ident_seed identified identity idle_time if ifnull ignore iif ilike ilm immediate import in include including increment index indexes indexing indextype indicator indices inet6_aton inet6_ntoa inet_aton inet_ntoa infile initial initialized initially initrans inmemory inner innodb input insert install instance instantiable instr interface interleaved intersect into invalidate invisible is is_free_lock is_ipv4 is_ipv4_compat is_not is_not_null is_used_lock isdate isnull isolation iterate java join json json_exists k keep keep_duplicates key keys kill l language large last last_day last_insert_id last_value lax lcase lead leading least leaves left len lenght length less level levels library like like2 like4 likec limit lines link list listagg little ln load load_file lob lobs local localtime localtimestamp locate locator lock locked log log10 log2 logfile logfiles logging logical logical_reads_per_call logoff logon logs long loop low low_priority lower lpad lrtrim ltrim m main make_set makedate maketime managed management manual map mapping mask master master_pos_wait match matched materialized max maxextents maximize maxinstances maxlen maxlogfiles maxloghistory maxlogmembers maxsize maxtrans md5 measures median medium member memcompress memory merge microsecond mid migration min minextents minimum mining minus minute minvalue missing mod mode model modification modify module monitoring month months mount move movement multiset mutex n name name_const names nan national native natural nav nchar nclob nested never new newline next nextval no no_write_to_binlog noarchivelog noaudit nobadfile nocheck nocompress nocopy nocycle nodelay nodiscardfile noentityescaping noguarantee nokeep nologfile nomapping nomaxvalue nominimize nominvalue nomonitoring none noneditionable nonschema noorder nopr nopro noprom nopromp noprompt norely noresetlogs noreverse normal norowdependencies noschemacheck noswitch not nothing notice notrim novalidate now nowait nth_value nullif nulls num numb numbe nvarchar nvarchar2 object ocicoll ocidate ocidatetime ociduration ociinterval ociloblocator ocinumber ociref ocirefcursor ocirowid ocistring ocitype oct octet_length of off offline offset oid oidindex old on online only opaque open operations operator optimal optimize option optionally or oracle oracle_date oradata ord ordaudio orddicom orddoc order ordimage ordinality ordvideo organization orlany orlvary out outer outfile outline output over overflow overriding p package pad parallel parallel_enable parameters parent parse partial partition partitions pascal passing password password_grace_time password_lock_time password_reuse_max password_reuse_time password_verify_function patch path patindex pctincrease pctthreshold pctused pctversion percent percent_rank percentile_cont percentile_disc performance period period_add period_diff permanent physical pi pipe pipelined pivot pluggable plugin policy position post_transaction pow power pragma prebuilt precedes preceding precision prediction prediction_cost prediction_details prediction_probability prediction_set prepare present preserve prior priority private private_sga privileges procedural procedure procedure_analyze processlist profiles project prompt protection public publishingservername purge quarter query quick quiesce quota quotename radians raise rand range rank raw read reads readsize rebuild record records recover recovery recursive recycle redo reduced ref reference referenced references referencing refresh regexp_like register regr_avgx regr_avgy regr_count regr_intercept regr_r2 regr_slope regr_sxx regr_sxy reject rekey relational relative relaylog release release_lock relies_on relocate rely rem remainder rename repair repeat replace replicate replication required reset resetlogs resize resource respect restore restricted result result_cache resumable resume retention return returning returns reuse reverse revoke right rlike role roles rollback rolling rollup round row row_count rowdependencies rowid rownum rows rtrim rules safe salt sample save savepoint sb1 sb2 sb4 scan schema schemacheck scn scope scroll sdo_georaster sdo_topo_geometry search sec_to_time second section securefile security seed segment select self sequence sequential serializable server servererror session session_user sessions_per_user set sets settings sha sha1 sha2 share shared shared_pool short show shrink shutdown si_averagecolor si_colorhistogram si_featurelist si_positionalcolor si_stillimage si_texture siblings sid sign sin size size_t sizes skip slave sleep smalldatetimefromparts smallfile snapshot some soname sort soundex source space sparse spfile split sql sql_big_result sql_buffer_result sql_cache sql_calc_found_rows sql_small_result sql_variant_property sqlcode sqldata sqlerror sqlname sqlstate sqrt square standalone standby start starting startup statement static statistics stats_binomial_test stats_crosstab stats_ks_test stats_mode stats_mw_test stats_one_way_anova stats_t_test_ stats_t_test_indep stats_t_test_one stats_t_test_paired stats_wsr_test status std stddev stddev_pop stddev_samp stdev stop storage store stored str str_to_date straight_join strcmp strict string struct stuff style subdate subpartition subpartitions substitutable substr substring subtime subtring_index subtype success sum suspend switch switchoffset switchover sync synchronous synonym sys sys_xmlagg sysasm sysaux sysdate sysdatetimeoffset sysdba sysoper system system_user sysutcdatetime t table tables tablespace tan tdo template temporary terminated tertiary_weights test than then thread through tier ties time time_format time_zone timediff timefromparts timeout timestamp timestampadd timestampdiff timezone_abbr timezone_minute timezone_region to to_base64 to_date to_days to_seconds todatetimeoffset trace tracking transaction transactional translate translation treat trigger trigger_nestlevel triggers trim truncate try_cast try_convert try_parse type ub1 ub2 ub4 ucase unarchived unbounded uncompress under undo unhex unicode uniform uninstall union unique unix_timestamp unknown unlimited unlock unpivot unrecoverable unsafe unsigned until untrusted unusable unused update updated upgrade upped upper upsert url urowid usable usage use use_stored_outlines user user_data user_resources users using utc_date utc_timestamp uuid uuid_short validate validate_password_strength validation valist value values var var_samp varcharc vari varia variab variabl variable variables variance varp varraw varrawc varray verify version versions view virtual visible void wait wallet warning warnings week weekday weekofyear wellformed when whene whenev wheneve whenever where while whitespace with within without work wrapped xdb xml xmlagg xmlattributes xmlcast xmlcolattval xmlelement xmlexists xmlforest xmlindex xmlnamespaces xmlpi xmlquery xmlroot xmlschema xmlserialize xmltable xmltype xor year year_to_month years yearweek
