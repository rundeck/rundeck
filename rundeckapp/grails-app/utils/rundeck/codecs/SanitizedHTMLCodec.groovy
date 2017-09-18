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

import org.owasp.html.ElementPolicy
import org.owasp.html.HtmlChangeListener
import org.owasp.html.HtmlPolicyBuilder
import org.owasp.html.HtmlStreamEventProcessor
import org.owasp.html.HtmlStreamEventReceiver
import org.owasp.html.PolicyFactory
import org.owasp.html.Sanitizers

/**
 * Sanitize HTML using owasp sanitizer and allow basic HTML elements.
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-11-19
 */
class SanitizedHTMLCodec {
    def grailsApplication

    static class AutoClosingEventReceiver implements HtmlStreamEventReceiver {
        @Delegate
        HtmlStreamEventReceiver sink
        Set<String> autoCloseTags = []


        @Override
        void openTag(final String elementName, final List<String> attrs) {
            sink.openTag(elementName, attrs)
            if (autoCloseTags.contains(elementName.toLowerCase())) {
                sink.closeTag(elementName)
            }
        }
    }
    static class AutoClosingEventProcessor implements HtmlStreamEventProcessor {
        Set<String> autoCloseTags = []

        @Override
        HtmlStreamEventReceiver wrap(final HtmlStreamEventReceiver sink) {
            return new AutoClosingEventReceiver(sink:sink,autoCloseTags: autoCloseTags)
        }
    }

    public static final PolicyFactory SVG = new HtmlPolicyBuilder()
            .allowElements(
            'svg',
            'g',
            'path',
            'polygon',
            'circle',
            'rect',
            'text')
            .allowAttributes('text-anchor', 'x', 'y','style')
            .onElements('text')
            .allowAttributes('transform','style')
            .onElements('g')
            .allowAttributes('cx','cy','r','class')
            .onElements('circle')
            .allowAttributes('points','style','transform')
            .onElements('polygon')
            .allowAttributes('d','style')
            .onElements('path')
            .allowAttributes('class','xmlns','version','height','width','style')
            .onElements('svg')
            .allowAttributes('x','y','height','width','style','fill')
            .onElements('rect')
            .withPreprocessor(new AutoClosingEventProcessor(autoCloseTags: ['circle','polygon','path','rect']))
            .toFactory();
    static final PolicyFactory POLICY =
            Sanitizers.BLOCKS.
                    and(Sanitizers.FORMATTING).
                    and(Sanitizers.IMAGES).
                    and(Sanitizers.LINKS).
                    and(SVG).
                    and(new HtmlPolicyBuilder()
                                .allowElements(
                            'section',
                            'p',
                            'i',
                            'b',
                            'div',
                            'article',
                            'a',
                            'ul','ol','li',
                            'pre', 'code',
                            'table', 'tr', 'td', 'tbody', 'th',
                            'span',
                            'h1',
                            'h2',
                            'h3',
                            'h4',
                            'dd','dl','dt',
                            'hr'
                            )
                                .allowAttributes("name").onElements("a")
                        //allow 'class' attribute on these elements
                                .allowAttributes('class').onElements(
                            'section',
                            'p',
                            'i',
                            'em',
                            'strong',
                            'b',
                            'div',
                            'article',
                            'a',
                            'pre', 'code',
                            'ul','ol','li',
                            'table', 'tr', 'td', 'th', 'tbody',
                            'span',
                            'h1',
                            'h2',
                            'h3',
                            'h4',
                            'dd','dl','dt',
                            'hr'
                    ).allowAttributes('style').onElements(
                            'td','th',
                    ).allowAttributes('colspan').onElements(
                            'th',
                    )

                                .toFactory()
                    )

    static debugLog = { str ->
        log.info(str)
    }

    static changeListener = new HtmlChangeListener() {
        @Override
        void discardedTag(final Object t, final String s) {
            debugLog("HTML Sanitizer audit: Discarding tag: " + s)
        }

        @Override
        void discardedAttributes(final Object t, final String s, final String... strings) {
            debugLog("HTML Sanitizer audit: Discarding attrs for tag: " + s + ": " +
                             "attrs: " + (strings as List)
            )
        }
    }
    static encode = { str ->
        return POLICY.sanitize(str.toString(), changeListener, null)
    }
}
