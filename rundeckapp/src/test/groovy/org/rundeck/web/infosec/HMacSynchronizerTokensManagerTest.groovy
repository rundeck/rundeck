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

import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail;

/**
 * HMacSynchronizerTokensManagerTest is ...
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-12-03
 */

class HMacSynchronizerTokensManagerTest {

    @Test
    void testInit() {
        def manager = new HMacSynchronizerTokensManager()
        manager.init()
    }

    @Test
    void testInitRequiresAlgorithm() {

        def manager = new HMacSynchronizerTokensManager()
        manager.algorithm = null
        try {
            manager.init()
            fail()
        } catch (IllegalStateException e) {
        }
    }

    @Test
    void testValidToken() {

        def manager = new HMacSynchronizerTokensManager()
        manager.init()
        def token = manager.generateToken('abcd', '123', ['def', 'ghi'])
        assertEquals(true, manager.validToken(token, 'abcd', System.currentTimeMillis() +
                10_000L, '123', ['def', 'ghi']))
    }

    @Test
    void testExpiredToken() {
        def manager = new HMacSynchronizerTokensManager()
        manager.init()
        def token = manager.generateToken('abcd', '123', ['def', 'ghi'])
        assertEquals(false, manager.validToken(token,
                                               'abcd',
                                               System.currentTimeMillis() - 10_000L,
                                               '123',
                                               ['def', 'ghi']
        ))
    }

    @Test
    void testWrongNonce() {
        def manager = new HMacSynchronizerTokensManager()
        manager.init()
        def token = manager.generateToken('abcd', '123', ['def', 'ghi'])
        assertEquals(false, manager.validToken(token, 'abcd-wrong', System.currentTimeMillis() +
                10_000L, '123', ['def', 'ghi']))
    }

    @Test
    void testWrongSessionId() {

        def manager = new HMacSynchronizerTokensManager()
        manager.init()
        def token = manager.generateToken('abcd', '123', ['def', 'ghi'])
        assertEquals(false, manager.validToken(token, 'abcd', System.currentTimeMillis() +
                10_000L, '123-wrong', ['def', 'ghi']))
    }

    @Test
    void testWrongData() {

        def manager = new HMacSynchronizerTokensManager()
        manager.init()
        def token = manager.generateToken('abcd', '123', ['def', 'ghi'])
        assertEquals(false, manager.validToken(token, 'abcd', System.currentTimeMillis() +
                10_000L, '123', ['def', 'ghi', 'extra']))
    }

    @Test
    void testWrongData2() {

        def manager = new HMacSynchronizerTokensManager()
        manager.init()
        def token = manager.generateToken('abcd', '123', ['def', 'ghi'])
        assertEquals(false, manager.validToken(token, 'abcd', System.currentTimeMillis() +
                10_000L, '123', ['def'/*, 'ghi'*/]))
    }

    @Test
    void testWrongData3() {

        def manager = new HMacSynchronizerTokensManager()
        manager.init()
        def token = manager.generateToken('abcd', '123', ['def', 'ghi'])
        assertEquals(false, manager.validToken(token, 'abcd', System.currentTimeMillis() +
                10_000L, '123', ['def-wrong', 'ghi']))
    }
}
