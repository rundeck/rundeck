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

package com.dtolabs.rundeck.core.utils;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.Iterator;

/**
 */
@RunWith(JUnit4.class)
public class QuotedStringTokenizerTest {

    @Test
    public void simple() {
        assertTokenized("abc", "abc");
    }

    @Test
    public void delimiter() {
        assertTokenized("abc def", "abc", "def");
    }

    @Test
    public void quoted() {
        assertTokenized("\"abc\"", "abc");
    }

    @Test
    public void quotedSpace() {
        assertTokenized("\"abc def\"", "abc def");
    }

    @Test
    public void quotedEscaped() {
        assertTokenized("\"ab\"\"c def\"", "ab\"c def");
    }

    @Test
    public void quotedSingle() {
        assertTokenized("'abc'", "abc");
    }

    @Test
    public void quotedSingleSpaced() {
        assertTokenized("'abc def'", "abc def");
    }

    @Test
    public void quotedSingleEscaped() {
        assertTokenized("'a''bc def'", "a'bc def");
    }

    @Test
    public void mixed() {
        assertTokenized("abc \"123 45\" elbow 'sized' 'shrimp food' \"scurry 'donut' slef\"", "abc", "123 45",
                "elbow", "sized", "shrimp food", "scurry 'donut' slef");
    }

    @Test
    public void quotedBlank() {
        assertTokenized("abc \"\" def '' xyz", "abc", "", "def", "", "xyz");
    }

    @Test
    public void quotedExtraSpaces() {
        assertTokenized("-whitespaces     \"string    with   white space\" -arg1 one   ",
                "-whitespaces", "string    with   white space", "-arg1", "one");
    }

    @Test
    public void tokenizeToList() {
        Assert.assertEquals(Arrays.asList("-whitespaces", "string    with   white space", "-arg1", "one"),
                QuotedStringTokenizer.tokenizeToList("-whitespaces     \"string    with   white space\" -arg1 one   "));
    }

    @Test
    public void tokenizeToArray() {
        assertArrayEquals(new String[]{"-whitespaces", "string    with   white space", "-arg1", "one"},
                QuotedStringTokenizer.tokenizeToArray("-whitespaces     \"string    with   white space\" -arg1 one   " +
                        ""));
    }


    @Test
    public void tokenize() {
        Iterable<String> tokenize = QuotedStringTokenizer.tokenize("-whitespaces     \"string    with   white space\"" +
                " -arg1 one   ");
        Iterator<String> iterator = tokenize.iterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals("-whitespaces", iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals("string    with   white space", iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals("-arg1", iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals("one", iterator.next());
        Assert.assertFalse(iterator.hasNext());

    }

    private void assertArrayEquals(String[] expected, String[] strings1) {
        Assert.assertEquals(expected.length, strings1.length);
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals("wrong value at " + i, expected[i], strings1[i]);
        }
    }

    public void assertTokenized(String tokens, String... expected) {
        assertArrayEquals(expected, QuotedStringTokenizer.tokenizeToArray(tokens));
    }

}
