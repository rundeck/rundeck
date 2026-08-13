/*
 * Copyright 2026 SimplifyOps Inc, <http://simplifyops.com>
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

/**
 * MarkdownCodecTest covers PS-1683: raw inline HTML in markdown must be
 * escaped rather than passed through as live markup, while normal
 * markdown syntax keeps rendering.
 */
class MarkdownCodecTest {
    @Test
    void testNormalMarkdownStillRenders(){
        assertEquals(
            '<article class="markdown-body"><p><strong>bold</strong></p>\n</article>',
            MarkdownCodec.decodeStr('**bold**')
        )
    }

    @Test
    void testRawHtmlInMarkdownIsEscaped(){
        assertEquals(
            '<article class="markdown-body"><p>some text</p>\n' +
            '<p>&lt;td style&#61;&#34;position:fixed;inset:0;background:url(&#39;//attacker/leak&#39;)&#34;&gt;x&lt;/td&gt;</p>\n</article>',
            MarkdownCodec.decodeStr(
                'some text\n\n<td style="position:fixed;inset:0;background:url(\'//attacker/leak\')">x</td>'
            )
        )
    }
}
