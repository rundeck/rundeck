

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

package rundeck

import grails.test.hibernate.HibernateSpec
import org.junit.Test

import static org.junit.Assert.assertEquals

class OptionTest {

    @Test
    void testSortIndexShouldDetermineOrder() {
        
        def options = [
                new Option(name: 'abc-4', defaultValue: '12', sortIndex: 4),
                new Option(name: 'bcd-2', defaultValue: '12', sortIndex: 2),
                new Option(name: 'cde-3', defaultValue: '12', sortIndex: 3),
                new Option(name: 'def-1', defaultValue: '12', sortIndex: 1),
        ] as TreeSet
        assertEquals(['def-1', 'bcd-2', 'cde-3', 'abc-4'], options*.name)
    }

    @Test
    void testNullSortIndexShouldUseNameToDetermineOrder() {
        
        def options = [
                new Option(name: 'def-4', defaultValue: '12',),
                new Option(name: 'abc-1', defaultValue: '12', ),
                new Option(name: 'cde-3', defaultValue: '12', ),
                new Option(name: 'bcd-2', defaultValue: '12', ),
        ] as TreeSet
        assertEquals(['abc-1','bcd-2','cde-3','def-4'], options*.name)
    }

    @Test
    void testMixedSortShouldUseSortIndexFirstThenName() {
        
        def options = [
                new Option(name: 'def-4', defaultValue: '12',),
                new Option(name: 'abc-2', defaultValue: '12', sortIndex: 1),
                new Option(name: 'cde-3', defaultValue: '12',),
                new Option(name: 'bcd-1', defaultValue: '12',sortIndex: 0),
        ] as TreeSet
        assertEquals(['bcd-1', 'abc-2', 'cde-3', 'def-4'], options*.name)
    }

    @Test
    void testToMapPreservesSortIndex(){
        assertEquals 0,new Option(name: 'bcd-1', sortIndex: 0).toMap().sortIndex
        assertEquals 1,new Option(name: 'bcd-1', sortIndex: 1).toMap().sortIndex
        assertEquals null,new Option(name: 'bcd-1',).toMap().sortIndex
    }
    @Test
    void testfromMapPreservesSortIndex(){
        assertEquals 0,Option.fromMap('test',[sortIndex: 0]).sortIndex
        assertEquals 1,Option.fromMap('test',[sortIndex: 1]).sortIndex
        assertEquals null, Option.fromMap('test', [sortIndex: null]).sortIndex
    }
}
