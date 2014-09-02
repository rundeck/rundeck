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
import org.springframework.web.context.ServletContextAware

import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.servlet.ServletContext

/**
 * HMacSynchonizerTokensManager is ...
 * @author Greg Schueler <greg@simplifyops.com>
 * @since 2014-08-20
 */
class HMacSynchronizerTokensManager implements ServletContextAware, InitializingBean {
    static final Logger logger = Logger.getLogger(HMacSynchronizerTokensManager.class)
    String algorithm = "HmacSHA256"
    private SecretKey secretKey
    ServletContext servletContext

    void init() {
        KeyGenerator kg = KeyGenerator.getInstance(algorithm);
        secretKey = kg.generateKey()
    }
    /**
     * Generate a request token
     * @param expiry expiration time in milliseconds
     * @return Map of [TOKEN: String, TIMESTAMP: Long]
     */
    public String generateToken(Long timestamp, String sessionId, List<String> data) {
        String token = ('' + timestamp + '/' + sessionId + (data ? '/' + (data.join('/')) : ''))
        String nonce = HexCodec.encode(MacUtils.digest(algorithm, secretKey, token))
        nonce
    }

    @Override
    void afterPropertiesSet() throws Exception {
        if (!algorithm) {
            throw new IllegalStateException("algorithm not set")
        }
        init()
    }

    boolean validToken(String testToken, long timestamp, String sessionId, List<String> data) {
            //validate token/nonce
            String test = generateToken(timestamp,sessionId,data)
            def valid = false
            if (test != testToken) {
                logger.debug("request token invalid for session ${sessionId} (expected (${test}, but was ${testToken}")
            } else if (System.currentTimeMillis() > timestamp) {
                logger.debug("request token has expired by ${System.currentTimeMillis() - timestamp} ms")
            } else {
                valid = true
            }
            return valid
    }
}
