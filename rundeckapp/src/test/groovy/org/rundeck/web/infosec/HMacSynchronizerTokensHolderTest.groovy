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

import org.junit.Test

import static org.junit.Assert.*

/**
 * HMacSynchronizerTokensHolderTest is ...
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-12-03
 */

class HMacSynchronizerTokensHolderTest  {

    @Test
    void testIsEmpty(){
        HMacSynchronizerTokensHolder holder = createHolder('123', ['abc', 'def'])
        assertTrue(holder.isEmpty())
        def token=holder.generateToken(123L,'/my/url')
        assertFalse(holder.isEmpty())
        holder.resetToken(token)
        assertTrue(holder.isEmpty())
    }

    @Test
    void testGenerateToken(){
        HMacSynchronizerTokensHolder holder = createHolder('123', ['abc', 'def'])
        String token = holder.generateToken(123L, '/my/url')
        assertNotNull(token)
    }

    @Test
    void testGenerateTokenUrl(){
        HMacSynchronizerTokensHolder holder = createHolder('123', ['abc', 'def'])
        String token = holder.generateToken('/my/url')
        assertNotNull(token)
    }

    @Test
    void testExpiredTimestamp(){
        HMacSynchronizerTokensHolder holder = createHolder('123', ['abc', 'def'])
        def token = holder.generateToken(System.currentTimeMillis() - 10_000L, '/my/url')
        //expired
        assertFalse(holder.isValid('/my/url',token))
    }

    @Test
    void testValidTimestamp(){
        HMacSynchronizerTokensHolder holder = createHolder('123', ['abc', 'def'])
        def token = holder.generateToken(System.currentTimeMillis() + 10_000L, '/my/url')
        //not expired
        assertTrue(holder.isValid('/my/url',token))
    }

    @Test
    void testValidUrlToken(){
        HMacSynchronizerTokensHolder holder = createHolder('123', ['abc', 'def'])
        def token = holder.generateToken('/my/url')
        //not expired
        assertTrue(holder.isValid('/my/url',token))
    }

    @Test
    void testUniqueForSameTimestamp(){
        HMacSynchronizerTokensHolder holder = createHolder('123', ['abc', 'def'])
        def token1 = holder.generateToken(123L, '/my/url')
        def token2 = holder.generateToken(123L, '/my/url')
        assertFalse("should not be equal", token1==token2)
    }

    @Test
    void testResetToken(){
        HMacSynchronizerTokensHolder holder = createHolder('123', ['abc', 'def'])
        def token = holder.generateToken( '/my/url')
        assertTrue(holder.isValid('/my/url', token))

        holder.resetToken(token)
        assertFalse(holder.isValid('/my/url', token))
    }

    @Test
    void testIncorrectURL(){
        HMacSynchronizerTokensHolder holder = createHolder('123', ['abc', 'def'])
        def token = holder.generateToken( '/my/url')
        assertTrue(holder.isValid('/my/url', token))
        assertFalse(holder.isValid('/not/my/url', token))
        assertFalse(holder.isValid(null, token))
    }

    protected static HMacSynchronizerTokensHolder createHolder(String sessionId, ArrayList<String> data) {
        def manager = new HMacSynchronizerTokensManager()
        manager.init()
        def holder = new HMacSynchronizerTokensHolder(manager, sessionId, data)
        holder
    }
}
