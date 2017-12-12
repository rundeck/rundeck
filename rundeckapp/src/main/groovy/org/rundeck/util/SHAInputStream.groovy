/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.util

import org.apache.commons.fileupload.util.Streams

import java.security.DigestInputStream
import java.security.MessageDigest

/**
 * Calculates SHA256 on all data read from the underlying input stream
 * @author greg
 * @since 2/22/17
 */
class SHAInputStream extends DigestInputStream {

    SHAInputStream(final InputStream var1) {
        super(var1, MessageDigest.getInstance("SHA-256"))
    }

    /**
     * Calculates the SHA of all read bytes, will reset the digest
     * @return
     */
    byte[] getSHABytes() {
        digest.digest()
    }

    /**
     * Calculates the SHA of all read bytes, will reset the digest
     * @return
     */
    String getSHAString() {
        String.format("%064x", new BigInteger(1, getSHABytes()))
    }

    /**
     * Consume all data from the underlying input stream and calculate SHA
     * @return
     */
    String consumeAndGetSHAString() {
        Streams.copy(this, null, false)
        return getSHAString()
    }

    /**
     * Utility method to consume all data from an input stream in and return the SHA
     * @param input
     * @return
     */
    static String getSHAString(InputStream input) {
        new SHAInputStream(input).consumeAndGetSHAString()
    }
}
