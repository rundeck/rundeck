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

package rundeck.codecs

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.CoreHtmlNodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlNodeRendererFactory;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;

/*
 * MarkdownCodec.java
 * 
 * User: greg
 * Created: Nov 29, 2007 12:33:11 PM
 * $Id: MarkdownCodec.groovy 357 2007-11-29 21:10:38Z gschueler $
 */

 /**
  * MarkdownCodec converts markdown text to html, using {@link HtmlRenderer}
  */
class MarkdownCodec {
    static List<Extension> extensions = Arrays.asList(TablesExtension.create());
    static Parser parser = Parser.builder()
            .extensions(Arrays.asList(TablesExtension.create())).build();
    static HtmlRenderer renderer = HtmlRenderer.builder()
            .extensions(extensions)
            .escapeHtml(true)
            .nodeRendererFactory({ HtmlNodeRendererContext context -> new HtmlCommentStrippingRenderer(context) } as HtmlNodeRendererFactory)
            .build();

    static String decodeStr (String str){
        Node doc = parser.parse(str);
        return SanitizedHTMLCodec.encode("<article class=\"markdown-body\">" + renderer.render(doc) + "</article>");
    }
    static decode = { str ->
        return decodeStr(str)
    }

    /**
     * Renders {@link HtmlBlock} and {@link HtmlInline} nodes so that raw HTML comments
     * (e.g. directive comments such as {@code <!-- fgcolor: #ffffff -->}) never leak into
     * the output as visible escaped text (see RUN-4945).
     * <p>
     * Security invariant: for any {@link HtmlBlock} or {@link HtmlInline} node, this renderer
     * either (a) emits nothing at all, when the node's literal consists solely of one or more
     * complete HTML comments plus whitespace, or (b) delegates entirely to
     * {@link CoreHtmlNodeRenderer}, which escapes the literal because
     * {@link HtmlNodeRendererContext#shouldEscapeHtml()} is {@code true} (RUN-4543). There is no
     * third branch that could emit raw, unescaped HTML.
     */
    @groovy.transform.CompileStatic
    static class HtmlCommentStrippingRenderer implements NodeRenderer {
        private final CoreHtmlNodeRenderer coreRenderer

        HtmlCommentStrippingRenderer(HtmlNodeRendererContext context) {
            this.coreRenderer = new CoreHtmlNodeRenderer(context)
        }

        @Override
        Set<Class<? extends Node>> getNodeTypes() {
            return [HtmlBlock, HtmlInline] as Set
        }

        @Override
        void render(Node node) {
            String literal
            if (node instanceof HtmlBlock) {
                literal = ((HtmlBlock) node).literal
            } else if (node instanceof HtmlInline) {
                literal = ((HtmlInline) node).literal
            } else {
                // Unreachable: the renderer map only dispatches the types from getNodeTypes().
                return
            }

            if (isCommentOnly(literal)) {
                // Emit nothing: the whole literal is HTML comment(s) and whitespace.
                return
            }

            // Delegate to the core renderer so the literal is escaped exactly as it
            // would be without this customization (escapeHtml(true) stays authoritative).
            coreRenderer.render(node)
        }

        /**
         * Returns {@code true} only when the trimmed literal is non-empty and consists solely of
         * one or more complete {@code <!-- ... -->} HTML comments separated by optional whitespace,
         * with no other characters present.
         * <p>
         * This is implemented as a plain index scan (no backtracking regex) so that any content
         * outside of a comment -- such as a stray tag following a comment on the same line --
         * correctly causes the whole literal to fall back to escaping instead of being dropped.
         */
        static boolean isCommentOnly(String literal) {
            if (literal == null) {
                return false
            }
            String text = literal.trim()
            if (text.isEmpty()) {
                return false
            }
            int index = 0
            int length = text.length()
            boolean foundAtLeastOneComment = false
            while (index < length) {
                while (index < length && Character.isWhitespace(text.charAt(index))) {
                    index++
                }
                if (index >= length) {
                    break
                }
                if (!text.startsWith('<!--', index)) {
                    return false
                }
                int closeIndex = text.indexOf('-->', index + 4)
                if (closeIndex == -1) {
                    return false
                }
                index = closeIndex + 3
                foundAtLeastOneComment = true
            }
            return foundAtLeastOneComment
        }
    }
}
