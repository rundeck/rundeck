package rundeck
/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

//import grails.test.GrailsUnitTestCase

import groovy.mock.interceptor.MockFor
import org.junit.Test
import rundeck.*
import rundeck.controllers.ScheduledExecutionController
import rundeck.services.FrameworkService
import rundeck.services.ScheduledExecutionService

import static org.junit.Assert.*

/*
* rundeck.ScheduledExecutionServiceTests.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jul 29, 2010 4:38:24 PM
* $Id$
*/
public class ScheduledExecutionServiceTests {


    private void assertParseParamNotifications(ArrayList<Map<String, Object>> expected, Map<String, Object> params) {
        def result = ScheduledExecutionService.parseParamNotifications(params)
        assertNotNull(result)
        assertEquals(expected, result)
    }
    @Test
    public void testParseParamNotificationsSuccess() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME,
                        type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                        configuration:[recipients: 'c@example.com,d@example.com']]],
                [(ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL): 'true', 
                        (ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS): 'c@example.com,d@example.com']
        )
    }
    @Test
    public void testParseParamNotificationsSuccess_subject() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME,
                        type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                        configuration:[recipients: 'c@example.com,d@example.com',
                                subject:'blah'
                        ]]],
                [(ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL): 'true',
                        (ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS): 'c@example.com,d@example.com',
                        (ScheduledExecutionController.NOTIFY_SUCCESS_SUBJECT): 'blah']
        )
    }
    @Test
    public void testParseParamNotificationsSuccessUrl() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME,
                        type: ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE,
                        content: 'http://blah.com']],
                [(ScheduledExecutionController.NOTIFY_ONSUCCESS_URL): 'true',
                        (ScheduledExecutionController.NOTIFY_SUCCESS_URL): 'http://blah.com']
        )
    }
    @Test
    public void testParseParamNotificationsFailure() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME,
                        type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                        configuration: [recipients:'c@example.com,d@example.com']]],
                [(ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL): 'true',
                        (ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS): 'c@example.com,d@example.com']
        )
    }
    @Test
    public void testParseParamNotificationsFailure_subject() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME,
                        type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                        configuration: [recipients: 'c@example.com,d@example.com', subject:
                                'elf']]],
                [(ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL): 'true',
                        (ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS): 'c@example.com,d@example.com',
                        (ScheduledExecutionController.NOTIFY_FAILURE_SUBJECT): 'elf']
        )
    }
    @Test
    public void testParseParamNotificationsFailureUrl() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME,
                        type: ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE,
                        content: 'http://blah.com']],
                [(ScheduledExecutionController.NOTIFY_ONFAILURE_URL): 'true',
                        (ScheduledExecutionController.NOTIFY_FAILURE_URL): 'http://blah.com']
        )
    }
    @Test
    public void testParseParamNotificationsRetryableFailure() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME,
                        type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                        configuration: [recipients:'c@example.com,d@example.com']]],
                [(ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_EMAIL): 'true',
                        (ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_RECIPIENTS): 'c@example.com,d@example.com']
        )
    }
    @Test
    public void testParseParamNotificationsRetryableFailure_subject() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME,
                        type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                        configuration: [recipients: 'c@example.com,d@example.com', subject:
                                'elf']]],
                [(ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_EMAIL): 'true',
                        (ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_RECIPIENTS): 'c@example.com,d@example.com',
                        (ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_SUBJECT): 'elf']
        )
    }
    @Test
    public void testParseParamNotificationsRetryableFailureUrl() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME,
                        type: ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE,
                        content: 'http://blah.com']],
                [(ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_URL): 'true',
                        (ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_URL): 'http://blah.com']
        )
    }
    @Test
    public void testParseParamNotificationsStart() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME,
                        type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                        configuration: [recipients: 'c@example.com,d@example.com']]],
                [(ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL): 'true',
                        (ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS): 'c@example.com,d@example.com']
        )
    }
    @Test
    public void testParseParamNotificationsStart_subject() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME,
                        type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                        configuration: [recipients: 'c@example.com,d@example.com',
                                subject: 'rango'
                        ]]],
                [(ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL): 'true',
                        (ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS): 'c@example.com,d@example.com',
                        (ScheduledExecutionController.NOTIFY_FAILURE_SUBJECT): 'rango']
        )
    }
    @Test
    public void testParseParamNotificationsStartUrl() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONSTART_TRIGGER_NAME,
                        type: ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE,
                        content: 'http://blah.com']],
                [(ScheduledExecutionController.NOTIFY_ONSTART_URL): 'true',
                        (ScheduledExecutionController.NOTIFY_START_URL): 'http://blah.com']
        )
    }
    @Test
    public void testParseParamNotificationsSuccessPluginEnabled() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'plugin1', configuration: [:]]],
                [
                        notifyPlugin: [
                                (ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME): [
                                        type: 'plugin1',
                                        enabled: [
                                                'plugin1': 'true'
                                        ],
                                        'plugin1': [
                                                config: [:]
                                        ]
                                ]
                        ],
                ]
        )
    }
    @Test
    public void testParseParamNotificationsFailurePluginEnabled() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'plugin1', configuration: [:]]],
                [
                        notifyPlugin: [
                                (ScheduledExecutionController.ONFAILURE_TRIGGER_NAME): [
                                        type: 'plugin1',
                                        enabled: [
                                                'plugin1': 'true'
                                        ],
                                        'plugin1': [
                                                config: [:]
                                        ]
                                ]
                        ],
                ]
        )
    }
    @Test
    public void testParseParamNotificationsRetryableFailurePluginEnabled() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME, type: 'plugin1', configuration: [:]]],
                [
                        notifyPlugin: [
                                (ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME): [
                                        type: 'plugin1',
                                        enabled: [
                                                'plugin1': 'true'
                                        ],
                                        'plugin1': [
                                                config: [:]
                                        ]
                                ]
                        ],
                ]
        )
    }
    @Test
    public void testParseParamNotificationsStartPluginEnabled() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONSTART_TRIGGER_NAME, type: 'plugin1', configuration: [:]]],
                [
                        notifyPlugin: [
                                (ScheduledExecutionController.ONSTART_TRIGGER_NAME): [
                                        type: 'plugin1',
                                        enabled: [
                                                'plugin1': 'true'
                                        ],
                                        'plugin1': [
                                                config: [:]
                                        ]
                                ]
                        ],
                ]
        )
    }
    @Test
    public void testParseParamNotificationsSuccessPluginDisabled() {
        assertParseParamNotifications(
                [],
                [
                        notifyPlugin: [
                                (ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME): [
                                        type: 'plugin1',
                                        enabled: [
                                                'plugin1': 'false'
                                        ],
                                        'plugin1': [
                                                config: [:]
                                        ]
                                ]
                        ],
                ]
        )
    }
    @Test
    public void testParseParamNotificationsSuccessPluginConfiguration() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'plugin1', configuration: [a:'b',c:'def']]],
                [
                        notifyPlugin: [
                                (ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME): [
                                        type: 'plugin1',
                                        enabled: [
                                                'plugin1': 'true'
                                        ],
                                        'plugin1': [
                                                config: [a:'b',c:'def']
                                        ]
                                ]
                        ],
                ]
        )
    }
    @Test
    public void testParseParamNotificationsSuccessPluginMultiple() {
        assertParseParamNotifications(
                [
                        [eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'plugin1', configuration: [a:'b',c:'def']],
                        [eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'plugin2', configuration: [g: 'h', i: 'jkl']]
                ],
                [
                        notifyPlugin: [
                                (ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME): [
                                        type: ['plugin1','plugin2'],
                                        enabled: [
                                                'plugin1': 'true',
                                                'plugin2': 'true'
                                        ],
                                        'plugin1': [
                                                config: [a:'b',c:'def']
                                        ],
                                        'plugin2': [
                                                config: [g:'h',i:'jkl']
                                        ]
                                ]
                        ],
                ]
        )
    }
    @Test
    public void testParseParamEmailNotificationsAttachedFile() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME,
                  type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                  configuration:[recipients: 'c@example.com,d@example.com', attachLog: true, attachLogInFile: true, attachLogInline: false]]],
                [(ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL): 'true',
                 (ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS): 'c@example.com,d@example.com',
                 (ScheduledExecutionController.NOTIFY_SUCCESS_ATTACH):'true',
                 (ScheduledExecutionController.NOTIFY_SUCCESS_ATTACH_TYPE):'file'
                ]
        )
    }
    @Test
    public void testParseParamEmailNotificationsAttachedInline() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME,
                  type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                  configuration:[recipients: 'c@example.com,d@example.com', attachLog: true, attachLogInFile: false, attachLogInline: true]]],
                [(ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL): 'true',
                 (ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS): 'c@example.com,d@example.com',
                 (ScheduledExecutionController.NOTIFY_SUCCESS_ATTACH):'true',
                 (ScheduledExecutionController.NOTIFY_SUCCESS_ATTACH_TYPE):'inline'
                ]
        )
    }
}
