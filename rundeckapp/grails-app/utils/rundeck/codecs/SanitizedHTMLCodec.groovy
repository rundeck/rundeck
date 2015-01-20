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

import org.owasp.html.HtmlChangeListener
import org.owasp.html.HtmlPolicyBuilder
import org.owasp.html.PolicyFactory
import org.owasp.html.Sanitizers

/**
 * Sanitize HTML using owasp sanitizer and allow basic HTML elements.
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-11-19
 */
class SanitizedHTMLCodec {
    def grailsApplication
    static final PolicyFactory POLICY =
        Sanitizers.BLOCKS.
                and(Sanitizers.FORMATTING).
                and(Sanitizers.IMAGES).
                and(Sanitizers.LINKS).
                and(new HtmlPolicyBuilder().
                            //allow 'class' attribute on these elements
                            allowElements('em', 'p', 'i', 'b', 'div', 'a', 'span', 'h1', 'h2',
                                          'h3', 'h4', 'pre', 'code').
                            allowAttributes('class').onElements('p', 'i', 'b', 'div', 'a',
                                                                'span', 'h1', 'h2', 'h3', 'h4',
                                                                'pre', 'code').
                            toFactory()
                )

    static debugLog = { str ->
        log.debug(str)
    }

    static changeListener = new HtmlChangeListener() {
        @Override
        void discardedTag(final Object t, final String s) {
            debugLog("HTML Sanitizer audit: Discarding tag: " + s)
        }

        @Override
        void discardedAttributes(final Object t, final String s, final String... strings) {
            debugLog("HTML Sanitizer audit: Discarding attrs for tag: " + s + ": " +
                             "attrs: " + (strings as List))
        }
    }
    static encode = { str ->
        return POLICY.sanitize(str.toString(), changeListener, null)
    }
}
