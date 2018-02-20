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
            .build();

    static String decodeStr (String str){
        Node doc = parser.parse(str);
        return SanitizedHTMLCodec.encode("<article class=\"markdown-body\">" + renderer.render(doc) + "</article>");
    }
    static decode = { str ->
        return decodeStr(str)
    }
}
