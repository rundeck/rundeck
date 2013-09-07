/*
 * Copyright 2012 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* TestINodeEntryComparator.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 1/6/12 10:19 AM
* 
*/
package com.dtolabs.rundeck.core.execution.dispatch;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import junit.framework.TestCase;

import java.util.*;

/**
 * TestINodeEntryComparator is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestINodeEntryComparator extends TestCase {

    public void testCompareDefaultProperty() throws Exception {
        final NodeEntryImpl node1 = new NodeEntryImpl("abc");
        final NodeEntryImpl node2 = new NodeEntryImpl("def");
        final NodeEntryImpl node3 = new NodeEntryImpl("ghi");
        {//ascending
            final INodeEntryComparator comparator = new INodeEntryComparator(null);
            final TreeSet<INodeEntry> sorted = new TreeSet<INodeEntry>(comparator);
            sorted.add(node1);
            sorted.add(node2);
            sorted.add(node3);
            final ArrayList<INodeEntry> expected = new ArrayList<INodeEntry>();
            expected.add(node1);
            expected.add(node2);
            expected.add(node3);
            final ArrayList<INodeEntry> seen = new ArrayList<INodeEntry>(sorted);
            assertEquals(expected, seen);
        }
    }

    public void testCompareRankProperty() throws Exception {
        final NodeEntryImpl node1 = new NodeEntryImpl("abc");
        node1.setAttribute("rank", "789");

        final NodeEntryImpl node2 = new NodeEntryImpl("def");
        node2.setAttribute("rank", "456");

        final NodeEntryImpl node3 = new NodeEntryImpl("ghi");
        node3.setAttribute("rank", "123");
        {//ascending
            final INodeEntryComparator comparator = new INodeEntryComparator("rank");
            final TreeSet<INodeEntry> sorted = new TreeSet<INodeEntry>(comparator);
            sorted.add(node1);
            sorted.add(node2);
            sorted.add(node3);
            final ArrayList<INodeEntry> expected = new ArrayList<INodeEntry>();
            expected.add(node3);
            expected.add(node2);
            expected.add(node1);
            final ArrayList<INodeEntry> seen = new ArrayList<INodeEntry>(sorted);
            assertEquals(expected, seen);
        }
    }

    /**
     * test that numeric values are compared numerically, not alphanumerically
     */
    public void testCompareRankPropertyNumeric() throws Exception {
        final NodeEntryImpl node1 = new NodeEntryImpl("abc");
        node1.setAttribute("rank", "123");

        final NodeEntryImpl node2 = new NodeEntryImpl("def");
        node2.setAttribute("rank", "00456");

        final NodeEntryImpl node3 = new NodeEntryImpl("ghi");
        node3.setAttribute("rank", "789");

        final NodeEntryImpl node4 = new NodeEntryImpl("jkl");
        node3.setAttribute("rank", "101112");

        final NodeEntryImpl node5 = new NodeEntryImpl("mno");
        node3.setAttribute("rank", "131415");
        {//ascending
            final INodeEntryComparator comparator = new INodeEntryComparator("rank");
            final TreeSet<INodeEntry> sorted = new TreeSet<INodeEntry>(comparator);
            sorted.add(node1);
            sorted.add(node2);
            sorted.add(node3);
            sorted.add(node4);
            sorted.add(node5);
            final ArrayList<INodeEntry> expected = new ArrayList<INodeEntry>();
            expected.add(node1);
            expected.add(node2);
            expected.add(node3);
            expected.add(node4);
            expected.add(node5);
            final ArrayList<INodeEntry> seen = new ArrayList<INodeEntry>(sorted);
            assertEquals(expected, seen);
        }
    }

    /**
     * test that equivalent numeric values are compared alphanumerically
     */
    public void testCompareRankPropertyNumeric2() throws Exception {
        final NodeEntryImpl node1 = new NodeEntryImpl("abc");
        node1.setAttribute("rank", "123");

        final NodeEntryImpl node2 = new NodeEntryImpl("def");
        node2.setAttribute("rank", "00456");

        final NodeEntryImpl node3 = new NodeEntryImpl("ghi");
        node3.setAttribute("rank", "0456");

        final NodeEntryImpl node4 = new NodeEntryImpl("jkl");
        node4.setAttribute("rank", "456");

        {//ascending
            final INodeEntryComparator comparator = new INodeEntryComparator("rank");
            final TreeSet<INodeEntry> sorted = new TreeSet<INodeEntry>(comparator);
            sorted.add(node1);
            sorted.add(node2);
            sorted.add(node3);
            sorted.add(node4);
            final ArrayList<INodeEntry> expected = new ArrayList<INodeEntry>();
            expected.add(node1);
            expected.add(node2);
            expected.add(node3);
            expected.add(node4);
            final ArrayList<INodeEntry> seen = new ArrayList<INodeEntry>(sorted);
            assertEquals(expected, seen);
        }

    }

    public void testCompareRankPropertyNulls() throws Exception {
        final NodeEntryImpl node1 = new NodeEntryImpl("abc");

        final NodeEntryImpl node2 = new NodeEntryImpl("def");

        final NodeEntryImpl node3 = new NodeEntryImpl("ghi");
        {//ascending
            final INodeEntryComparator comparator = new INodeEntryComparator("rank");
            final TreeSet<INodeEntry> sorted = new TreeSet<INodeEntry>(comparator);
            sorted.add(node1);
            sorted.add(node2);
            sorted.add(node3);
            final ArrayList<INodeEntry> expected = new ArrayList<INodeEntry>();
            expected.add(node1);
            expected.add(node2);
            expected.add(node3);
            final ArrayList<INodeEntry> seen = new ArrayList<INodeEntry>(sorted);
            assertEquals(expected, seen);
        }
    }

    public void testCompareEqualRankPropertyTreeSet() throws Exception {
        final NodeEntryImpl node1 = new NodeEntryImpl("abc");
        node1.setAttribute("rank", "1");

        final NodeEntryImpl node2 = new NodeEntryImpl("def");

        final NodeEntryImpl node3 = new NodeEntryImpl("ghi");
        node3.setAttribute("rank", "1");

        final NodeEntryImpl node4 = new NodeEntryImpl("jkl");

        final NodeEntryImpl node5 = new NodeEntryImpl("mno");
        node5.setAttribute("rank", "5");
        {//ascending
            final INodeEntryComparator comparator = new INodeEntryComparator("rank");
            final TreeSet<INodeEntry> sorted = new TreeSet<INodeEntry>(comparator);
            sorted.add(node1);
            sorted.add(node2);
            sorted.add(node3);
            sorted.add(node4);
            sorted.add(node5);
            final ArrayList<INodeEntry> expected = new ArrayList<INodeEntry>();
            expected.add(node1);
            expected.add(node3);
            expected.add(node5);
            expected.add(node2);
            expected.add(node4);
            final ArrayList<INodeEntry> seen = new ArrayList<INodeEntry>(sorted);
            assertEquals(expected, seen);
        }
    }
    public void testCompareEqualRankPropertyCollectionsSort() throws Exception {
        final NodeEntryImpl node1 = new NodeEntryImpl("abc");
        node1.setAttribute("rank", "1");

        final NodeEntryImpl node2 = new NodeEntryImpl("def");

        final NodeEntryImpl node3 = new NodeEntryImpl("ghi");
        node3.setAttribute("rank", "1");

        final NodeEntryImpl node4 = new NodeEntryImpl("jkl");

        final NodeEntryImpl node5 = new NodeEntryImpl("mno");
        node5.setAttribute("rank", "5");
        final INodeEntryComparator comparator = new INodeEntryComparator("rank");

        final ArrayList<INodeEntry> sorted = new ArrayList<INodeEntry>();
        sorted.add(node1);
        sorted.add(node2);
        sorted.add(node3);
        sorted.add(node4);
        sorted.add(node5);
        Collections.sort(sorted, comparator);
        final ArrayList<INodeEntry> expected = new ArrayList<INodeEntry>();
        expected.add(node1);
        expected.add(node3);
        expected.add(node5);
        expected.add(node2);
        expected.add(node4);
        final ArrayList<INodeEntry> seen = new ArrayList<INodeEntry>(sorted);
        assertEquals(expected, seen);
    }

    public void testCompareRankPropertyMixedNulls() throws Exception {
        final NodeEntryImpl node1 = new NodeEntryImpl("abc");
        node1.setAttribute("rank", "1");

        final NodeEntryImpl node2 = new NodeEntryImpl("def");

        final NodeEntryImpl node3 = new NodeEntryImpl("ghi");
        node3.setAttribute("rank", "3");

        final NodeEntryImpl node4 = new NodeEntryImpl("jkl");

        final NodeEntryImpl node5 = new NodeEntryImpl("mno");
        node5.setAttribute("rank", "5");
        {//ascending
            final INodeEntryComparator comparator = new INodeEntryComparator("rank");
            final TreeSet<INodeEntry> sorted = new TreeSet<INodeEntry>(comparator);
            sorted.add(node1);
            sorted.add(node2);
            sorted.add(node3);
            sorted.add(node4);
            sorted.add(node5);
            final ArrayList<INodeEntry> expected = new ArrayList<INodeEntry>();
            expected.add(node1);
            expected.add(node3);
            expected.add(node5);
            expected.add(node2);
            expected.add(node4);
            final ArrayList<INodeEntry> seen = new ArrayList<INodeEntry>(sorted);
            assertEquals(expected, seen);
        }
    }
}
