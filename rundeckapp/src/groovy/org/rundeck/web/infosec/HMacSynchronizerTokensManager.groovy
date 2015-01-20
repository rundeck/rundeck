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

package org.rundeck.web.infosec

import com.dtolabs.rundeck.util.MacUtils
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.plugins.codecs.HexCodec
import org.springframework.beans.factory.InitializingBean

import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * HMacSynchonizerTokensManager is ...
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-08-20
 */
class HMacSynchronizerTokensManager implements InitializingBean, Serializable {
    private static final long serialVersionUID = 1L;
    static final Logger logger = Logger.getLogger(HMacSynchronizerTokensManager.class)
    String algorithm = "HmacSHA256"
    private SecretKey secretKey

    void init() {
        if (!algorithm) {
            throw new IllegalStateException("algorithm not set")
        }
        KeyGenerator kg = KeyGenerator.getInstance(algorithm);
        secretKey = kg.generateKey()
    }
    /**
     * Generate a request token
     * @param expiry expiration time in milliseconds
     * @return Map of [TOKEN: String, TIMESTAMP: Long]
     */
    public String generateToken(String nonce, String sessionId, List<String> data) {
        String token = ('' +  nonce + '/' + sessionId + (data ? '/' + (data.join('/')) : ''))
        String digest = HexCodec.encode(MacUtils.digest(algorithm, secretKey, token))
        digest
    }

    @Override
    void afterPropertiesSet() throws Exception {
        init()
    }

    boolean validToken(String testToken, String nonce, long timestamp, String sessionId, List<String> data) {
        //validate token/nonce
        String test = generateToken( nonce, sessionId, data)
        def valid = false
        if (test != testToken) {
            logger.debug("request token invalid for session ${sessionId} (expected (${test}, " +
                                 "but was ${testToken}")
        } else if (System.currentTimeMillis() > timestamp) {
            logger.debug("request token has expired by ${System.currentTimeMillis() - timestamp} " +
                                 "ms")
        } else {
            valid = true
        }
        return valid
    }
}
