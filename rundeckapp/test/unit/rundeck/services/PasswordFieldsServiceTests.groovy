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

package rundeck.services

import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import grails.test.mixin.*

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(PasswordFieldsService)
class PasswordFieldsServiceTests {

    private final String SECRET = "secret"

    static passwordField = [
            getName: { "password" },
            getRenderingOptions: {
                [
                        "displayType": StringRenderingConstants.DisplayType.PASSWORD,
                ]
            }
    ] as Property
    static passwordField2 = [
            getName: { "password2" },
            getRenderingOptions: {
                [
                        "displayType": "PASSWORD",
                ]
            }
    ] as Property

    static textField = [
            getName: { "textField" },
            getRenderingOptions: {[:]}
    ] as Property

    static noPasswordFieldDescription = [
            getName: { "noPasswordDescription" },
            getTitle: { "No Password" },
            getDescription: {"No Password Description" },
            getProperties: { [textField] },
            getPropertyMapping: { [:] },
            getFwkPropertyMapping: { [:] }
    ] as Description

    static withPasswordFieldDescription = [
            getName: { "withPasswordDescription" },
            getTitle: { "With Password" },
            getDescription: {"With Password Description" },
            getProperties: { [textField, passwordField] },
            getPropertyMapping: { [:] },
            getFwkPropertyMapping: { [:] }
    ] as Description

    static withPasswordFieldDescriptionText = [
            getName: { "withPasswordDescription2" },
            getTitle: { "With Password2" },
            getDescription: {"With Password Description2" },
            getProperties: { [textField, passwordField2] },
            getPropertyMapping: { [:] },
            getFwkPropertyMapping: { [:] }
    ] as Description

    def configurationUnknownType = [
            [
                    "type": "foobar",
                    "props": props("simple=text", "password=secret", "textField=a test field")
            ]
    ]


    private def genTwoSimilarTypes() {
        def i=0
        [
            [
                    "type" : "withPasswordDescription",
                    "props": props("password=secret_set1", "textField=a test field")
            ],
            [
                    "type" : "noPasswordFieldDescription",
                    "props": props("password=secret", "textField=a test field")
            ],
            [
                    "type" : "withPasswordDescription",
                    "props": props("password=secret_set2", "textField=a test field")
            ],
        ].collect { [config: it, index: i++] }
    }

    private def genMissingType() {
        def i=0
        [
                [
                        "type" : "withPasswordDescription",
                        "props": props("password=secret_set1", "textField=a test field")
                ],
                [
                        "type" : "foobar",
                        "props": props("password=secret", "textField=a test field")
                ],
                [
                        "type" : "withPasswordDescription",
                        "props": props("password=secret_set2", "textField=a test field")
                ],
        ].collect { [config: it, index: i++] }
    }

    private def genConfiguration() {
        def i=0
        [
                [
                        "type": "withPasswordDescription",
                        "props": props("simple=text", "password=secret", "textField=a test field")
                ],
                [
                        "type": "noPasswordFieldDescription",
                        "props": props("simple=text", "password=secret", "textField=a test field")
                ]
        ].collect { [config: it, index: i++] }
    }
    private def genConfiguration2() {
        def i=0
        [
                [
                        "type": "withPasswordDescription2",
                        "props": props("simple=text", "password2=secret", "textField=a test field")
                ],
                [
                        "type": "noPasswordFieldDescription",
                        "props": props("simple=text", "password=secret", "textField=a test field")
                ]
        ].collect { [config: it, index: i++] }
    }

    private def genMissingPropField() {
        def i=0
        [
                [
                        "type": "withPasswordDescription",
                        "props": props("simple=text", "textfield=a test field")
                ]
        ].collect { [config: it, index: i++] }
    }

    private def genMultiConfiguration(int max=1) {
        def list=[]
        for(int j=0;j<max;j++){
            list<<[
                    config: [
                            "type": "withPasswordDescription",
                            "props": props("simple=text", "password=secret", "textField=a test field")
                    ],
                    index: j
            ]
        }
        return list
    }

    void testPasswordResetFields() {
        service.reset()
        assertEquals(0, service.tracking())
    }

    void testTrackNull() {
        service.track([null], noPasswordFieldDescription)
    }

    void testPasswordIdentifyPasswordFieldsEmptyList() {
        int count = service.track(genConfiguration()*.config)
        assertEquals(0, count)
        assertEquals(0, service.tracking())
    }

    void testTrackDescription() {
        int count = service.track(genConfiguration()*.config, noPasswordFieldDescription)
        assertEquals(0, count)
        assertEquals(0, service.tracking())
    }

    void testTrackDescriptionPassword() {
        int count = service.track(genConfiguration()*.config, withPasswordFieldDescription)
        assertEquals(1, count)
        assertEquals(1, service.tracking())
    }

    void testTrackDescriptionPasswordText() {
        int count = service.track(genConfiguration2()*.config, withPasswordFieldDescriptionText)
        assertEquals(1, count)
        assertEquals(1, service.tracking())
    }

    void testAdjustFields() {
        def cnf = genMultiConfiguration(12)
        service.track(cnf*.config, withPasswordFieldDescription,noPasswordFieldDescription)
        assertEquals(12, service.tracking())
        assertEquals(1, service.adjust([2]))
        assertEquals(11, service.tracking())
    }

    void testTrackRemoveWithoutPassword() {
        def cnf = genConfiguration()
        service.track(cnf*.config, noPasswordFieldDescription)
        // index = [1,2]
        cnf = cnf.subList(0,1)
        // index = [1]
        assertEquals(0, service.adjust([1]))  // not tracking
        service.untrack(cnf, noPasswordFieldDescription)
        assertEquals(0, service.tracking())
    }

    void testTrackRemoveWithPasswordKeptInFinalArray() {
        def cnf = genConfiguration()
        service.track(cnf*.config, withPasswordFieldDescription, noPasswordFieldDescription)
        cnf = cnf.subList(0,1) // index = [1,2]=>[1] remove last element.

        def adjusted = service.adjust([1])
        assertEquals(0, adjusted)

        service.untrack(cnf, withPasswordFieldDescription, noPasswordFieldDescription)
        assertPassword("secret", cnf[0].config)
        assertEquals(0, service.tracking())
    }

    void testTrackRemoveWithPasswordRemovedInFinalArray() {

        def cnf = genConfiguration()
        service.track(cnf*.config, withPasswordFieldDescription, noPasswordFieldDescription)
        cnf = cnf.subList(1,2) // index = [1,2]=>[2] dropping the first configuration, which is the one with a password we're tracking
        assertPassword("secret", cnf[0].config) // b/c untracked in noPasswordFieldDescription

        def adjusted = service.adjust([2])
        service.untrack(cnf, withPasswordFieldDescription, noPasswordFieldDescription)

        assertEquals(1, adjusted)
        assertPassword("secret", cnf[0].config) // b/c untracked in noPasswordFieldDescription
        assertEquals(0, service.tracking())
    }

    void testTrackMultipleDescription() {
        def cnf = genConfiguration()

        int count = service.track(cnf*.config, noPasswordFieldDescription, noPasswordFieldDescription)
        assertEquals(0, count)
        assertEquals(0, service.tracking())
    }

    void testTrackDescriptionWithUnknownConfigurationType() {
        int count = service.track(configurationUnknownType, withPasswordFieldDescription)
        assertEquals(0, count)
        assertEquals(0, service.tracking())
    }

    void testTrackDescriptionWithPasswordField() {
        def cnf = genConfiguration()

        Properties original = props("simple=text", "password=secret", "textField=a test field")
        int count = service.track(cnf*.config, withPasswordFieldDescription)
        assertEquals(1, count)
        assertEquals(1, service.tracking())

        assertFalse(cnf[0].config.props["password"].equals(service.fields["password"]?.original))
        assertFalse("password should not equal 'secret'", cnf[0].config.props["password"] == SECRET)
        assertFalse("Expected properties to be modified for withPasswordFieldDescription", original.equals(cnf[0].config.props))

    }

    /**
     * Input is not set for a password field
     */
    void testTrackDescriptionWithPasswordFieldNoValue() {
        def cnf = genConfiguration()
        cnf[0].config.props.remove('password')

        Properties original = props("simple=text", "textField=a test field")
        int count = service.track(cnf*.config, withPasswordFieldDescription)
        assertEquals(0, count)
        assertEquals(0, service.tracking())

        assertNull("password should not be set", cnf[0].config.props["password"])
    }

    void testUntrackDescriptionWithPasswordFieldNullArguments() {
        for(arg in [null, [null]]) {
            def cnf = genConfiguration()
            service.track(cnf*.config, withPasswordFieldDescription)
            service.untrack(arg, withPasswordFieldDescription)
        }
    }
    void testUntrackNullValueFields() {
        def cnf = genConfiguration()
        service.track(cnf*.config, withPasswordFieldDescription)
        cnf[0].config.props.remove('password')
        service.untrack(cnf, withPasswordFieldDescription)
        assertNull(cnf[0].config.props.password)
    }

    void testUntrackDescriptionWithPasswordField() {
        def cnf = genConfiguration()

        service.track(cnf*.config, withPasswordFieldDescription)
        service.untrack(cnf, withPasswordFieldDescription)
        assertPassword(SECRET, cnf[0].config)
        assertEquals(0, service.tracking())
    }

    void testDetectChangeAndDiscardOriginalValue() {
        def cnf = genConfiguration()
        service.track(cnf*.config, withPasswordFieldDescription)

        cnf[0].config.props["password"] = "NEW_PASSWORD"
        int cnt = service.untrack(cnf, withPasswordFieldDescription)
        assertEquals(1, cnt)
        assertPassword("NEW_PASSWORD", cnf[0].config)
        assertEquals(0, service.tracking())
    }

    void testMultipleConfigs() {
        def cnf = genTwoSimilarTypes()
        service.track(cnf*.config, withPasswordFieldDescription)

        setPassword("NEW_PASSWORD1", cnf[0].config)
        setPassword("NEW_PASSWORD2", cnf[2].config)

        int cnt = service.untrack(cnf, withPasswordFieldDescription)

        assertEquals(2, cnt)
        assertPassword("NEW_PASSWORD1", cnf[0].config)
        assertPassword("NEW_PASSWORD2", cnf[2].config)
        assertEquals(0, service.tracking())
    }

    void testThreeConfigsInsertionEditOnInsertion() {
        def cnf = genTwoSimilarTypes()
        def cnfTwoResources = cnf.subList(0,1)
        service.track(cnfTwoResources*.config, withPasswordFieldDescription)

        cnf[2].config.props["password"] = "NEW_PASSWORD2"
        int cnt = service.untrack(cnf, withPasswordFieldDescription)

        assertEquals(1, cnt)
        assertPassword("secret_set1", cnf[0].config)
        assertPassword("NEW_PASSWORD2", cnf[2].config)
        assertEquals(0, service.tracking())
    }

    void testThreeConfigsInsertionEditOnExisting() {
        def cnf = genTwoSimilarTypes()
        def cnfTwoResources = cnf.subList(0,1)
        service.track(cnfTwoResources*.config, withPasswordFieldDescription)

        cnf[0].config.props["password"] = "NEW_PASSWORD2"
        int cnt = service.untrack(cnf, withPasswordFieldDescription)

        assertEquals(1, cnt)
        assertPassword("NEW_PASSWORD2", cnf[0].config)
        assertPassword("secret_set2", cnf[2].config)
        assertEquals(0, service.tracking())
    }

    void testThreeConfigsRemoveEditOnExisting() {
        def cnf = genTwoSimilarTypes()
        service.track(cnf*.config, withPasswordFieldDescription)

        cnf[0].config.props["password"] = "NEW_PASSWORD2"

        def cnfTwoResources = cnf.subList(0, 1)
        int cnt = service.untrack(cnfTwoResources, withPasswordFieldDescription)

        assertEquals(1, cnt)
        assertPassword("NEW_PASSWORD2", cnfTwoResources[0].config)
        assertNull(cnfTwoResources[2])
        assertEquals(1, service.tracking())
    }

    void testThreeConfigsMissingTypeExisting() {
        def cnf = genTwoSimilarTypes()
        service.track(cnf*.config, withPasswordFieldDescription)

        setPassword("NEW_PASSWORD2", cnf[2].config)  // set password on 3rd resource.

        int cnt = service.untrack(cnf, withPasswordFieldDescription)

        assertEquals(2, cnt)
        assertPassword("NEW_PASSWORD2", cnf[2].config)
        assertEquals(0, service.tracking())
    }

    void testAbsentPasswordField() {
        def cnf = genMissingPropField()
        service.track(cnf*.config, withPasswordFieldDescription)

        service.untrack(cnf, withPasswordFieldDescription)

        assertTrue("No exception should be thrown if password field value is not present.", true )
    }

    void setPassword(String password, def resource) {
        resource.props["password"] = password
    }

    void assertPassword(String password, def resource) {
        assertEquals(password, resource.props["password"])
    }

    static Properties props(String... s) {
        Properties p = new Properties()
        p.load(new StringReader(s.join("\n")))
        return p
    }
}
