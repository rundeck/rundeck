/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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

package rundeck.codecs

import org.junit.Test

import static org.junit.Assert.*

import rundeck.codecs.SanitizedHTMLCodec

/**
 * SanitizedHTMLCodecTest is ...
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-11-19
 */

class SanitizedHTMLCodecTest {
    @Test
    void testAHref(){
        assertEquals('<a href="http://test.com" rel="nofollow">a</a>', SanitizedHTMLCodec.encode('<a href="http://test.com">a</a>'))
    }
    @Test
    void testAHrefJavascript(){
        assertEquals('a', SanitizedHTMLCodec.encode('<a href="javascript://alert(1)">a</a>'))
    }
    @Test
    void testAOnclick(){
        assertEquals('<a href="http://test.com" rel="nofollow">a</a>', SanitizedHTMLCodec.encode('<a href="http://test.com" onclick="alert(1)">a</a>'))
    }
    @Test
    void testScript(){
        assertEquals('', SanitizedHTMLCodec.encode('<script>alert(1)</script>'))
    }

    // PS-1683: style attribute must be CSS-guarded, not passed through verbatim
    @Test
    void testTdStyleMaliciousCssStripped(){
        assertEquals(
            '<table><tbody><tr><td style="width:100vw;height:100vh">x</td></tr></tbody></table>',
            SanitizedHTMLCodec.encode(
                '<td style="position:fixed;inset:0;width:100vw;height:100vh;background:url(\'//attacker/leak\')">x</td>'
            )
        )
    }

    @Test
    void testSvgRectStyleMaliciousCssStripped(){
        assertEquals(
            '<svg><rect fill="red"></rect></svg>',
            SanitizedHTMLCodec.encode(
                '<svg><rect style="position:fixed;inset:0;background:url(\'//attacker/leak\')" fill="red"/></svg>'
            )
        )
    }

    @Test
    void testTdStyleNormalCssStillWorks(){
        assertEquals(
            '<table><tbody><tr><td style="color:red;font-weight:bold;text-align:center">ok</td></tr></tbody></table>',
            SanitizedHTMLCodec.encode('<td style="color:red;font-weight:bold;text-align:center">ok</td>')
        )
    }

    // RUN-5000: WorkflowStrategy plugins (e.g. node-first) declare a STATIC_TEXT/text-html
    // "info" property whose value is a help table. A prior bug (RUN-4866) could persist that
    // table into saved job config, and the read-only Job Definition view had no STATIC_TEXT
    // handling, so it fell through to plain HTML-escaping instead of rendering a table. The
    // fix routes STATIC_TEXT/text-html values through this same sanitizer rather than through
    // plain escaping or raw/unescaped output -- these tests cover that codec usage directly.

    @Test
    void testNodeFirstWorkflowStrategyInfoTableIsPreserved(){
        // Verbatim defaultValue of NodeFirstWorkflowStrategy#info
        String info = '<table>\n' +
                      '    <tr><td>1.</td><td class="text-info">NodeA</td> <td>step 1</td></tr>\n' +
                      '    <tr><td>2.</td><td class="text-info">"</td> <td>step 2</td></tr>\n' +
                      '    <tr><td>3.</td><td class="text-info">"</td> <td>step 3</td></tr>\n' +
                      '    <tr><td>4.</td><td class="text-muted">NodeB</td> <td>step 1</td></tr>\n' +
                      '    <tr><td>5.</td><td class="text-muted">"</td> <td>step 2</td></tr>\n' +
                      '    <tr><td>6.</td><td class="text-muted">"</td> <td>step 3</td></tr>\n' +
                      '</table>'

        String result = SanitizedHTMLCodec.encode(info)

        assertTrue('table structure must survive sanitization, not be escaped', result.contains('<table>'))
        assertFalse('output must not be HTML-escaped literal tag text', result.contains('&lt;table&gt;'))
        assertEquals('all 18 data cells must survive', 18, (result =~ /<td[ >]/).count)
        assertTrue('cell text content must survive', result.contains('NodeA'))
        assertTrue('cell text content must survive', result.contains('step 1'))
        assertTrue('td class attribute must survive (already-allowed per testTdStyleNormalCssStillWorks)', result.contains('class="text-info"'))
    }

    @Test
    void testStaticTextValueWithInjectedScriptTagIsStripped(){
        // Simulates a persisted STATIC_TEXT value that is no longer the pristine plugin
        // default (the actual data this codepath renders is persisted job config, not a
        // trusted plugin descriptor default -- see RUN-5000), containing an injected script.
        String tampered = '<table><tr><td>1.</td><td class="text-info">NodeA<script>alert(document.cookie)</script></td></tr></table>'

        String result = SanitizedHTMLCodec.encode(tampered)

        assertFalse('script tag must be stripped, not passed through raw', result.toLowerCase().contains('<script'))
        assertFalse('script tag must be stripped, not passed through raw', result.toLowerCase().contains('alert('))
        assertTrue('safe surrounding content must still render as a live table', result.contains('<table>'))
        assertTrue('safe cell text must survive', result.contains('NodeA'))
    }
}
