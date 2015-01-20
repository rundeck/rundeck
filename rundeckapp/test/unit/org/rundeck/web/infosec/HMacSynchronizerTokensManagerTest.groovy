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

import grails.test.mixin.support.GrailsUnitTestMixin;

/**
 * HMacSynchronizerTokensManagerTest is ...
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-12-03
 */

@TestMixin(GrailsUnitTestMixin)
class HMacSynchronizerTokensManagerTest {
    void testInit() {
        def manager = new HMacSynchronizerTokensManager()
        manager.init()
    }

    void testInitRequiresAlgorithm() {

        def manager = new HMacSynchronizerTokensManager()
        manager.algorithm = null
        try {
            manager.init()
            fail()
        } catch (IllegalStateException e) {
        }
    }

    void testValidToken() {

        def manager = new HMacSynchronizerTokensManager()
        manager.init()
        def token = manager.generateToken('abcd', '123', ['def', 'ghi'])
        assertEquals(true, manager.validToken(token, 'abcd', System.currentTimeMillis() +
                10_000L, '123', ['def', 'ghi']))
    }

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

    void testWrongNonce() {
        def manager = new HMacSynchronizerTokensManager()
        manager.init()
        def token = manager.generateToken('abcd', '123', ['def', 'ghi'])
        assertEquals(false, manager.validToken(token, 'abcd-wrong', System.currentTimeMillis() +
                10_000L, '123', ['def', 'ghi']))
    }

    void testWrongSessionId() {

        def manager = new HMacSynchronizerTokensManager()
        manager.init()
        def token = manager.generateToken('abcd', '123', ['def', 'ghi'])
        assertEquals(false, manager.validToken(token, 'abcd', System.currentTimeMillis() +
                10_000L, '123-wrong', ['def', 'ghi']))
    }

    void testWrongData() {

        def manager = new HMacSynchronizerTokensManager()
        manager.init()
        def token = manager.generateToken('abcd', '123', ['def', 'ghi'])
        assertEquals(false, manager.validToken(token, 'abcd', System.currentTimeMillis() +
                10_000L, '123', ['def', 'ghi', 'extra']))
    }

    void testWrongData2() {

        def manager = new HMacSynchronizerTokensManager()
        manager.init()
        def token = manager.generateToken('abcd', '123', ['def', 'ghi'])
        assertEquals(false, manager.validToken(token, 'abcd', System.currentTimeMillis() +
                10_000L, '123', ['def'/*, 'ghi'*/]))
    }

    void testWrongData3() {

        def manager = new HMacSynchronizerTokensManager()
        manager.init()
        def token = manager.generateToken('abcd', '123', ['def', 'ghi'])
        assertEquals(false, manager.validToken(token, 'abcd', System.currentTimeMillis() +
                10_000L, '123', ['def-wrong', 'ghi']))
    }
}
