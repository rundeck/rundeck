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


import org.junit.Test
import rundeck.Option

import static org.junit.Assert.*

class EditOptsControllerTests  {

    @Test
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
            assertNull option.valuesList
            assertNull option.realValuesUrl
            assertNull option.regex
            assertNull option.defaultValue
        }
    @Test
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
            assertNotNull option.optionValues
            assertEquals 3,option.optionValues.size()
            assertTrue option.optionValues.contains('a')
            assertTrue option.optionValues.contains('b')
            assertTrue option.optionValues.contains('c')
            assertNotNull option.valuesList
            assertNull option.realValuesUrl
            assertNull option.regex
            assertNull option.defaultValue
        }
    @Test
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
            assertNull option.valuesList
            assertNotNull option.realValuesUrl
            assertEquals 'http://test.com', option.realValuesUrl.toExternalForm()
            assertNull option.regex
            assertNull option.defaultValue
        }
    @Test
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
            assertNull option.valuesList
            assertNotNull option.realValuesUrl
            assertEquals 'http://test.com', option.realValuesUrl.toExternalForm()
            assertNull option.regex
            assertNull option.defaultValue
        }
    @Test
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
            assertNull option.valuesList
            assertNotNull option.realValuesUrl
            assertEquals 'http://test.com', option.realValuesUrl.toExternalForm()
            assertNotNull option.regex
            assertEquals 'testregex',option.regex
            assertNull option.defaultValue
        }
    @Test
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
            assertNotNull option.optionValues
            assertNull option.realValuesUrl
            assertNull option.regex
            assertNull option.defaultValue
            assertFalse option.secureInput
            assertFalse option.secureExposed
        }
    @Test
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
            assertNotNull option.optionValues
            assertNull option.realValuesUrl
            assertNull option.regex
            assertNull option.defaultValue
            assertTrue option.secureInput
            assertFalse option.secureExposed
        }
    @Test
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
            assertNotNull option.optionValues
            assertNull option.realValuesUrl
            assertNull option.regex
            assertNull option.defaultValue
            assertTrue option.secureInput
            assertTrue option.secureExposed
        }

        //test on existing Option content
    @Test
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
            assertNotNull test2.optionValues
            assertEquals 3, test2.optionValues.size()
            assertTrue test2.optionValues.contains('a')
            assertTrue test2.optionValues.contains('b')
            assertTrue test2.optionValues.contains('c')
            assertNotNull test2.valuesList

        }

    @Test
    void test_setOptionFromParams_sort_options() {
        EditOptsController ctrl = new EditOptsController()
        Option test1 = new Option()
        ctrl._setOptionFromParams(test1,[name:'optname',description:'a description',valuesType:'url',enforcedType:'regex',valuesUrl:'http://test.com',regex:'testregex'])

        Option test2 = ctrl._setOptionFromParams(test1,[name:'optname',description:'a description2',defaultValue:'a',valuesType:'list',valuesList:'c,b,a', sortValues: 'true'])
        assertNotNull test2
        assertEquals 'optname',test2.name
        assertEquals 'a description2',test2.description
        assertEquals 'a',test2.defaultValue
        assertFalse test2.required
        assertNull test2.realValuesUrl
        assertNotNull test2.optionValues
        assertEquals 3, test2.optionValues.size()
        assertEquals "[a, b, c]" , test2.optionValues.toString()
        assertNotNull test2.valuesList

    }
    @Test
    void test_setOptionFromParams_sort_numeric_options() {
        EditOptsController ctrl = new EditOptsController()
        Option test1 = new Option()
        ctrl._setOptionFromParams(test1,[name:'optname',description:'a description',valuesType:'url',enforcedType:'regex',valuesUrl:'http://test.com',regex:'testregex'])

        Option test2 = ctrl._setOptionFromParams(test1,[name:'optname',description:'a description2',defaultValue:'a',valuesType:'list',valuesList:'33.3,22,44.0,11.0,3,1,2', sortValues: 'true'])
        assertNotNull test2
        assertEquals 'optname',test2.name
        assertEquals 'a description2',test2.description
        assertEquals 'a',test2.defaultValue
        assertFalse test2.required
        assertNull test2.realValuesUrl
        assertNotNull test2.optionValues
        assertEquals 7, test2.optionValues.size()
        assertEquals "[1, 2, 3, 11.0, 22, 33.3, 44.0]" , test2.optionValues.toString()
        assertNotNull test2.valuesList

    }
    @Test
    void test_setOptionFromParams_nosort_options() {
        EditOptsController ctrl = new EditOptsController()
        Option test1 = new Option()
        ctrl._setOptionFromParams(test1,[name:'optname',description:'a description',valuesType:'url',enforcedType:'regex',valuesUrl:'http://test.com',regex:'testregex'])

        Option test2 = ctrl._setOptionFromParams(test1,[name:'optname',description:'a description2',defaultValue:'a',valuesType:'list',valuesList:'c,b,a', sortValues: 'false'])
        assertNotNull test2
        assertEquals 'optname',test2.name
        assertEquals 'a description2',test2.description
        assertEquals 'a',test2.defaultValue
        assertFalse test2.required
        assertNull test2.realValuesUrl
        assertNotNull test2.optionValues
        assertEquals 3, test2.optionValues.size()
        assertEquals "[c, b, a]" , test2.optionValues.toString()
        assertNotNull test2.valuesList

    }
    @Test
    void test_setOptionFromParams_options_delimiters() {
        EditOptsController ctrl = new EditOptsController()
        Option test1 = new Option()
        ctrl._setOptionFromParams(test1,[name:'optname',description:'a description',valuesType:'url',enforcedType:'regex',valuesUrl:'http://test.com',regex:'testregex'])

        Option test2 = ctrl._setOptionFromParams(test1,[name:'optname',description:'a description2',defaultValue:'a',valuesType:'list',valuesList:'a|b|c|d|f|g', sortValues: 'false', valuesListDelimiter: "|"])
        assertNotNull test2
        assertEquals 'optname',test2.name
        assertEquals 'a description2',test2.description
        assertEquals 'a',test2.defaultValue
        assertFalse test2.required
        assertNull test2.realValuesUrl
        assertNotNull test2.optionValues
        assertEquals 6, test2.optionValues.size()
        assertTrue test2.optionValues.contains('a')
        assertTrue test2.optionValues.contains('b')
        assertTrue test2.optionValues.contains('c')
        assertNotNull test2.valuesList

    }
}
