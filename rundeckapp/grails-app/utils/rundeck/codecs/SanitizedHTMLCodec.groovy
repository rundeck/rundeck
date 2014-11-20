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

import org.owasp.html.PolicyFactory
import org.owasp.html.Sanitizers

/**
 * Sanitize HTML using owasp sanitizer and allow basic HTML elements.
 * @author Greg Schueler <greg@simplifyops.com>
 * @since 2014-11-19
 */
class SanitizedHTMLCodec {
    static final PolicyFactory POLICY = Sanitizers.BLOCKS.and(Sanitizers.FORMATTING).and(Sanitizers.IMAGES).and(Sanitizers.LINKS)
    static encode = {  str ->
        return POLICY.sanitize(str.toString())
    }
}
