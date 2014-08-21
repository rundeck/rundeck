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

import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder

import javax.servlet.http.HttpSession

/**
 * <p>HMacSynchronizerTokensHolder manages HMAC based request tokens within a session.  It extends {@link SynchronizerTokensHolder} to enable it
 * to be used in place of the existing grails &lt;g:form useToken="true"...&gt; mechanism.
 * </p>
 *
 * <p>An instance of this class must be stored in the user's session via the {@link HMacSynchronizerTokensHolder#store(javax.servlet.http.HttpSession, org.rundeck.web.infosec.HMacSynchronizerTokensManager, java.util.List)} method <b>before</b> a grails
 * &lt;g:form useToken="true"...&gt; tag is invoked, so that this tokens holder is used instead of the default implementation.
 * </p>
 *
 * @author Greg Schueler <greg@simplifyops.com>
 * @since 2014-08-20
 */
class HMacSynchronizerTokensHolder extends SynchronizerTokensHolder implements Serializable{

    public static final String HOLDER = "TOKENS_HOLDER"
    public static final String TOKEN_KEY = "TOKEN_KEY"
    public static final String TOKEN_TIMESTAMP = "TOKEN_TIMESTAMP"
    public static final long DEFAULT_DURATION= Long.getLong(HMacSynchronizerTokensHolder.class.name + '.DEFAULT_DURATION', 15 * 60 * 1000L)

    String sessionID
    List<String> sessionData
    HMacSynchronizerTokensManager manager
    long tokenDuration=DEFAULT_DURATION
    protected Map<String,Long> currentTokens = [:]
    protected Map<String,Set<String>> urlTokens = [:]

    String generateToken(Long timestamp, String url) {
        def token = manager.generateToken(timestamp, sessionID, sessionData+[url])
        currentTokens.put(token,timestamp)
        if(!urlTokens.containsKey(url)){
            urlTokens.put(url,new HashSet<String>([token]))
        }else{
            urlTokens.get(url).add(token)
        }
        return token
    }

    @Override
    String generateToken(String url) {
        return generateToken(System.currentTimeMillis() + tokenDuration,url)
    }

    boolean isValid(Long timestamp, String url, String token) {
        return currentTokens.containsKey(token) && manager.validToken(token, timestamp, sessionID, sessionData+[url])
    }
    @Override
    boolean isValid(String url, String token) {
        return urlTokens.containsKey(url) && urlTokens.get(url).contains(token) && isValid(currentTokens.get(token), url, token)
    }

    HMacSynchronizerTokensHolder(HMacSynchronizerTokensManager manager,String sessionID, List<String> sessionData) {
        this.manager=manager
        this.sessionID = sessionID
        this.sessionData = sessionData
    }
    void resetToken(String token) {
        currentTokens.remove(token)
    }

    @Override
    void resetToken(String url, String token) {
        resetToken(token)
        if(urlTokens.containsKey(url)){
            urlTokens.get(url).remove(token)
        }
    }

    @Override
    protected Set<UUID> getTokens(String url) {
        throw new IllegalArgumentException('Should not be invoked')
    }

    @Override
    boolean isEmpty() {
        return currentTokens.isEmpty()
    }

    static HMacSynchronizerTokensHolder store(HttpSession session, HMacSynchronizerTokensManager manager, List<String> data) {
        def found= session.getAttribute(SynchronizerTokensHolder.HOLDER)
        HMacSynchronizerTokensHolder tokensHolder
        if (!found || !(found instanceof HMacSynchronizerTokensHolder)) {
            tokensHolder = new HMacSynchronizerTokensHolder(manager,session.id,data)
            session.setAttribute(SynchronizerTokensHolder.HOLDER, tokensHolder)
        }else{
            tokensHolder=found
        }
        return tokensHolder
    }
}
