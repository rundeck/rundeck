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

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin

/**
 * SanitizedHTMLCodecTest is ...
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-11-19
 */
@TestMixin(GrailsUnitTestMixin)
class SanitizedHTMLCodecTest {
    void testAHref(){
        Assert.assertEquals('<a href="http://test.com" rel="nofollow">a</a>', SanitizedHTMLCodec.encode('<a href="http://test.com">a</a>'))
    }
    void testAHrefJavascript(){
        Assert.assertEquals('a', SanitizedHTMLCodec.encode('<a href="javascript://alert(1)">a</a>'))
    }
    void testAOnclick(){
        Assert.assertEquals('<a href="http://test.com" rel="nofollow">a</a>', SanitizedHTMLCodec.encode('<a href="http://test.com" onclick="alert(1)">a</a>'))
    }
    void testScript(){
        Assert.assertEquals('', SanitizedHTMLCodec.encode('<script>alert(1)</script>'))
    }
}
