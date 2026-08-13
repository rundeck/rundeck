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
}
