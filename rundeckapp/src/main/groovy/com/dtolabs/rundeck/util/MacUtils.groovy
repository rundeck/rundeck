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

package com.dtolabs.rundeck.util

import javax.crypto.Mac
import javax.crypto.SecretKey
import java.security.MessageDigest

/**
 * MacUtils is ...
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-08-19
 */
class MacUtils {

    // Digest byte[], any list/array or string into a byte[]
    static byte[] digest(String algorithm, SecretKey sk, data) {
        if (data == null) {
            return null
        }

        def md = Mac.getInstance(algorithm)
        md.init(sk);
        def src
        if (data instanceof Byte[] || data instanceof byte[]) {
            src = data
        } else if (data instanceof List || data.getClass().isArray()) {
            src = new byte[data.size()]
            data.eachWithIndex { v, i -> src[i] = v }
        } else {
            src = data.toString().getBytes("UTF-8")
        }
        md.update(src) // This probably needs to use the thread's Locale encoding
        return md.doFinal()
    }
}
