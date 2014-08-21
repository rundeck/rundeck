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

import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder

import javax.servlet.http.HttpSession

/**
 * HMacSynchronizerTokensHolder is ...
 * @author Greg Schueler <greg@simplifyops.com>
 * @since 2014-08-20
 */
class HMacSynchronizerTokensHolder  {

    public static final String HOLDER = "TOKENS_HOLDER"
    public static final String TOKEN_KEY = "TOKEN_KEY"
    public static final String TOKEN_TIMESTAMP = "TOKEN_TIMESTAMP"
    String sessionID
    List<String> sessionData
    HMacSynchronizerTokensManager manager
    protected Set<String> currentTokens = []
    boolean isValid(Long timestamp, String token) {
        return currentTokens.contains(token) && manager.validToken(token, timestamp, sessionID, sessionData)
    }
    String generateToken(Long timestamp) {
        def token = manager.generateToken(timestamp, sessionID, sessionData)
        currentTokens.add(token)
        return token
    }

    HMacSynchronizerTokensHolder(HMacSynchronizerTokensManager manager,String sessionID, List<String> sessionData) {
        this.manager=manager
        this.sessionID = sessionID
        this.sessionData = sessionData
    }

    void resetToken(String token) {
        currentTokens.remove(token)
    }

    static HMacSynchronizerTokensHolder store(HttpSession session, HMacSynchronizerTokensManager manager, List<String> data) {
        HMacSynchronizerTokensHolder tokensHolder = session.getAttribute(HOLDER)
        if (!tokensHolder || !(tokensHolder instanceof HMacSynchronizerTokensHolder)) {
            tokensHolder = new HMacSynchronizerTokensHolder(manager,session.id,data)
            session.setAttribute(HOLDER, tokensHolder)
        }
        return tokensHolder
    }
}
