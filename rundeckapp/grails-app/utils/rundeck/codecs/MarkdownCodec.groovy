package rundeck.codecs
/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.markdownj.MarkdownProcessor

/*
 * MarkdownCodec.java
 * 
 * User: greg
 * Created: Nov 29, 2007 12:33:11 PM
 * $Id: MarkdownCodec.groovy 357 2007-11-29 21:10:38Z gschueler $
 */

 /**
  * MarkdownCodec converts markdown text to html, using {@link MarkdownProcessor}
  */
class MarkdownCodec {
    static MarkdownProcessor p = new MarkdownProcessor();
    static String decodeStr (String str){
        return SanitizedHTMLCodec.encode(p.markdown(str))
    }
    static decode = { str ->
        return decodeStr(str)
    }
}
