/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.dispatcher

import spock.lang.Specification

/**
 * @author greg
 * @since 5/9/17
 */
class DataContextUtilsSpec extends Specification {

    def testReplaceDataReferences() {
        when:
        String input = null
        then:
        //null input, null data
        DataContextUtils.replaceDataReferencesInString(input, null) == null

        //null data
        DataContextUtils.replaceDataReferencesInString('test ${test.key1}', null) == 'test ${test.key1}'
        DataContextUtils.replaceDataReferencesInString('${test2.key2}', null) == '${test2.key2}'

        when:
        Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();
        then:
        //null input, data
        DataContextUtils.replaceDataReferencesInString((String) null, dataContext) == null

        //empty data
        DataContextUtils.replaceDataReferencesInString('test ${test.key1}', dataContext) == 'test ${test.key1}'
        DataContextUtils.replaceDataReferencesInString('${test2.key2}', dataContext) == '${test2.key2}'

        when:
        //add context but no data for the keys
        dataContext.put('test', new HashMap<String, String>());
        then:
        DataContextUtils.replaceDataReferencesInString('test ${test.key1}', dataContext) == 'test ${test.key1}'
        DataContextUtils.replaceDataReferencesInString('${test2.key2}', dataContext) == '${test2.key2}'

        when:
        //put in null value
        dataContext.get('test').put('key1', null);
        then:
        DataContextUtils.replaceDataReferencesInString('test ${test.key1}', dataContext) == 'test ${test.key1}'
        DataContextUtils.replaceDataReferencesInString('${test2.key2}', dataContext) == '${test2.key2}'

        when:
        //put in some data
        dataContext.get('test').put('key1', '123');
        then:
        DataContextUtils.replaceDataReferencesInString('test ${test.key1}', dataContext) == 'test 123'

        when:
        //test null value for context
        dataContext.get('test').put('key1', '123');
        dataContext.put('test2', null);
        then:
        DataContextUtils.replaceDataReferencesInString('test ${test2.key1}', dataContext) == 'test ${test2.key1}'

        when:
        //test null value for data
        dataContext.get('test').put('key2', null);
        then:
        DataContextUtils.replaceDataReferencesInString('test ${test.key2}', dataContext) == 'test ${test.key2}'
    }

    def testReplaceDataReferencesArray() {
        when:
        Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();
        String[] arr1 = null;
        then:
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext) == null
    }

    def testReplaceDataReferencesArray2() {
        when:
        Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();
        String[] arr1 = [];
        then:
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext) != null
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext).length == 0
    }

    def testReplaceDataReferencesArray3() {
        when:
        Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();
        String[] arr1 = [ 'a' ];
        then:
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext) != null
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext).length == 1
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext)[0] == 'a'
    }

    def testReplaceDataReferencesArray4() {
        when:
        Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();
        String[] arr1 = ['a', 'test ${test.key1}'];
        then:
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext) != null
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext).length == 2
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext)[0] == 'a'
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext)[1] == 'test ${test.key1}'
    }

    def testReplaceDataReferencesArray5() {
        when:
        Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();
        dataContext.put('test', new HashMap<String, String>());
        String[] arr1 = ['a', 'test ${test.key1}'];
        then:
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext) != null
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext).length == 2
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext)[0] == 'a'
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext)[1] == 'test ${test.key1}'
    }

    def testReplaceDataReferencesArray6() {
        when:
        Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();
        dataContext.put('test', new HashMap<String, String>());
        dataContext.get('test').put('key1', null);
        String[] arr1 = ['a', 'test ${test.key1}'];
        then:
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext) != null
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext).length == 2
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext)[0] == 'a'
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext)[1] == 'test ${test.key1}'
    }

    def testReplaceDataReferencesArray7() {
        when:
        Map<String, Map<String, String>> dataContext = new HashMap<String, Map<String, String>>();
        dataContext.put('test', new HashMap<String, String>());
        dataContext.get('test').put('key1', '123');
        String[] arr1 = ['a', 'test ${test.key1}'];
        then:
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext) != null
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext).length == 2
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext)[0] == 'a'
        DataContextUtils.replaceDataReferencesInArray(arr1, dataContext)[1] == 'test 123'

    }

    def testMerge() {
        given:
        final HashMap<String, Map<String, String>> origContext = new HashMap<String, Map<String, String>>();
        final HashMap<String, String> test1data = new HashMap<String, String>();
        test1data.put('key1', 'value1');
        origContext.put('test1', test1data);
        final HashMap<String, String> test2data = new HashMap<String, String>();
        test2data.put('key2', 'value2');
        origContext.put('test2', test2data);


        final HashMap<String, Map<String, String>> newContext = new HashMap<String, Map<String, String>>();
        final HashMap<String, String> test3data = new HashMap<String, String>();
        test3data.put('key1', 'value2');
        newContext.put('test1', test3data);
        when:
        final Map<String, Map<String, String>> result = DataContextUtils.merge(origContext, newContext);

        then:
        result != null
        result.size() == 2
        result.get('test1') != null
        result.get('test1').size() == 1
        result.get('test1').get('key1') != null
        result.get('test1').get('key1') == 'value2'
        result.get('test2') != null
        result.get('test2').size() == 1
        result.get('test2').get('key2') != null
        result.get('test2').get('key2') == 'value2'
    }
}
