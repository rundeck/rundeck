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

package rundeck.controllers

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.Execution
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.WorkflowStep

@TestFor(EditOptsController)
@Mock([ScheduledExecution, Option, Workflow, WorkflowStep, CommandExec, Execution])
class EditOptsControllerTests  {

    void test_setOptionFromParamstestEmpty(){
        EditOptsController ctrl = new EditOptsController()
            Option inputOption = new Option()
            final Map testmap = [:]
            Option option = ctrl._setOptionFromParams(inputOption,testmap)

            assertNotNull(option)
            //test properties
            assertNull option.name
            assertNull option.description
            assertFalse option.required
            assertFalse option.enforced
            assertNull option.values
            assertNull option.valuesList
            assertNull option.realValuesUrl
            assertNull option.regex
            assertNull option.defaultValue
        }

        void test_setOptionFromParamstestValuesTypeList() {
            EditOptsController ctrl = new EditOptsController()
            //use valuesType='list', enforcedType='none'
            Option inputOption = new Option()
            final Map testmap = [name:'testopt',description:'a description',valuesType:'list',enforcedType:'none',valuesList:'a,b,c',valuesUrl:'http://test.com',regex:'testregex']
            Option option = ctrl._setOptionFromParams(inputOption,testmap)

            assertNotNull(option)
            assertFalse(option.errors.hasErrors())
            //test properties
            assertEquals 'testopt', option.name
            assertEquals 'a description', option.description
            assertFalse option.required
            assertFalse option.enforced
            assertNotNull option.values
            assertEquals 3,option.values.size()
            assertTrue option.values.contains('a')
            assertTrue option.values.contains('b')
            assertTrue option.values.contains('c')
            assertNull option.valuesList
            assertNull option.realValuesUrl
            assertNull option.regex
            assertNull option.defaultValue
        }

    void test_setOptionFromParams_testValuesTypeUrl() {
        EditOptsController ctrl = new EditOptsController()
            //use valuesType='url', enforcedType='none'
            Option inputOption = new Option()
            final Map testmap = [name:'testopt',description:'a description',valuesType:'url',enforcedType:'none',valuesList:'a,b,c',valuesUrl:'http://test.com',regex:'testregex']
            Option option = ctrl._setOptionFromParams(inputOption,testmap)

            assertNotNull(option)
            //test properties
            assertEquals 'testopt', option.name
            assertEquals 'a description', option.description
            assertFalse option.required
            assertFalse option.enforced
            assertNull option.values
            assertNull option.valuesList
            assertNotNull option.realValuesUrl
            assertEquals 'http://test.com', option.realValuesUrl.toExternalForm()
            assertNull option.regex
            assertNull option.defaultValue
        }

    void test_setOptionFromParams_testEnforcedType() {
        EditOptsController ctrl = new EditOptsController()
            //use valuesType='url', enforcedType='enforced'
            Option inputOption = new Option()
            final Map testmap = [name:'testopt',description:'a description',valuesType:'url',enforcedType:'enforced',valuesList:'a,b,c',valuesUrl:'http://test.com',regex:'testregex']
            Option option = ctrl._setOptionFromParams(inputOption,testmap)

            assertNotNull(option)
            //test properties
            assertEquals 'testopt', option.name
            assertEquals 'a description', option.description
            assertFalse option.required
            assertTrue option.enforced
            assertNull option.values
            assertNull option.valuesList
            assertNotNull option.realValuesUrl
            assertEquals 'http://test.com', option.realValuesUrl.toExternalForm()
            assertNull option.regex
            assertNull option.defaultValue
        }

    void test_setOptionFromParams_testEnforcedType2() {
        EditOptsController ctrl = new EditOptsController()
            //use valuesType='url', enforcedType='regex'
            Option inputOption = new Option()
            final Map testmap = [name:'testopt',description:'a description',valuesType:'url',enforcedType:'regex',valuesList:'a,b,c',valuesUrl:'http://test.com',regex:'testregex']
            Option option = ctrl._setOptionFromParams(inputOption,testmap)

            assertNotNull(option)
            //test properties
            assertEquals 'testopt', option.name
            assertEquals 'a description', option.description
            assertFalse option.required
            assertFalse option.enforced
            assertNull option.values
            assertNull option.valuesList
            assertNotNull option.realValuesUrl
            assertEquals 'http://test.com', option.realValuesUrl.toExternalForm()
            assertNotNull option.regex
            assertEquals 'testregex',option.regex
            assertNull option.defaultValue
        }

    void test_setOptionFromParams_testInputTypePlain() {
        EditOptsController ctrl = new EditOptsController()
            //use inputType='plain'
            Option inputOption = new Option()
            final Map testmap = [name:'testopt',description:'a description',valuesList:'a,b,c',inputType:'plain']
            Option option = ctrl._setOptionFromParams(inputOption,testmap)

            assertNotNull(option)
            //test properties
            assertEquals 'testopt', option.name
            assertEquals 'a description', option.description
            assertFalse option.required
            assertFalse option.enforced
            assertNotNull option.values
            assertNull option.realValuesUrl
            assertNull option.regex
            assertNull option.defaultValue
            assertFalse option.secureInput
            assertFalse option.secureExposed
        }

    void test_setOptionFromParams_testInputTypeSecure() {
        EditOptsController ctrl = new EditOptsController()
            //use inputType='secure'
            Option inputOption = new Option()
            final Map testmap = [name:'testopt',description:'a description',valuesList:'a,b,c',inputType:'secure']
            Option option = ctrl._setOptionFromParams(inputOption,testmap)

            assertNotNull(option)
            //test properties
            assertEquals 'testopt', option.name
            assertEquals 'a description', option.description
            assertFalse option.required
            assertFalse option.enforced
            assertNotNull option.values
            assertNull option.realValuesUrl
            assertNull option.regex
            assertNull option.defaultValue
            assertTrue option.secureInput
            assertFalse option.secureExposed
        }

    void test_setOptionFromParams_testInputTypeSecureExposed() {
        EditOptsController ctrl = new EditOptsController()
            //use inputType='secureExposed'
            Option inputOption = new Option()
            final Map testmap = [name:'testopt',description:'a description',valuesList:'a,b,c',inputType:'secureExposed']
            Option option = ctrl._setOptionFromParams(inputOption,testmap)

            assertNotNull(option)
            //test properties
            assertEquals 'testopt', option.name
            assertEquals 'a description', option.description
            assertFalse option.required
            assertFalse option.enforced
            assertNotNull option.values
            assertNull option.realValuesUrl
            assertNull option.regex
            assertNull option.defaultValue
            assertTrue option.secureInput
            assertTrue option.secureExposed
        }

        //test on existing Option content

    void test_setOptionFromParams_test() {
        EditOptsController ctrl = new EditOptsController()
            Option test1 = new Option()
            ctrl._setOptionFromParams(test1,[name:'optname',description:'a description',valuesType:'url',enforcedType:'regex',valuesUrl:'http://test.com',regex:'testregex'])

            Option test2 = ctrl._setOptionFromParams(test1,[name:'optname',description:'a description2',defaultValue:'a',valuesType:'list',enforcedType:'enforced',valuesList:'a,b,c',regex:'testregex'])
            assertNotNull test2
            assertEquals 'optname',test2.name
            assertEquals 'a description2',test2.description
            assertEquals 'a',test2.defaultValue
            assertTrue test2.enforced
            assertFalse test2.required
            assertNull test2.realValuesUrl
            assertNull test2.regex
            assertNotNull test2.values
            assertEquals 3, test2.values.size()
            assertTrue test2.values.contains('a')
            assertTrue test2.values.contains('b')
            assertTrue test2.values.contains('c')
            assertNull test2.valuesList

        }





}
