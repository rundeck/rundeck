/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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

package com.dtolabs.rundeck.core.utils;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Arrays;


/**
 */
public class TestOptsUtil extends TestCase {

    public TestOptsUtil(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestOptsUtil.class);
    }

    public void testBurstSpace() {
        String[] args = OptsUtil.burst("-arg1 one -whitespaces 'string with white space'");
        assertEquals("unexpected: " + args[0], "-arg1", args[0]);
        assertEquals("unexpected: " + args[1], "one", args[1]);
        assertEquals("unexpected: " + args[2], "-whitespaces", args[2]);
        assertEquals("unexpected: " + args[3], "string with white space", args[3]);
    }
    public void testBurstDoubleQuoteSpace(){
        String[] args = OptsUtil.burst("-arg1 one -whitespaces \"string with white space\"");
        assertEquals("unexpected: " + args[0], "-arg1", args[0]);
        assertEquals("unexpected: " + args[1], "one", args[1]);
        assertEquals("unexpected: " + args[2], "-whitespaces", args[2]);
        assertEquals("unexpected: " + args[3], "string with white space", args[3]);


    }

    public void testBurstDoubleQuoteExtraSpace() {

        String[] args = OptsUtil.burst("-whitespaces     \"string    with   white space\" -arg1 one   ");
        assertEquals("wrong: " + Arrays.asList(args), 4, args.length);
        assertEquals("unexpected: " + args[0], "-whitespaces", args[0]);
        assertEquals("unexpected: " + args[1], "string    with   white space", args[1]);
        assertEquals("unexpected: " + args[2], "-arg1", args[2]);
        assertEquals("unexpected: " + args[3], "one", args[3]);

    }
    public void testBurstDoubleQuoteExtraWhiteSpace() {

        String[] args = OptsUtil.burst("-whitespaces\t     \"string  \r\n  with   white space\" -arg1\r\n one   ");
        assertEquals("wrong: " + Arrays.asList(args), 4, args.length);
        assertEquals("unexpected: " + args[0], "-whitespaces", args[0]);
        assertEquals("unexpected: " + args[1], "string  \r\n  with   white space", args[1]);
        assertEquals("unexpected: " + args[2], "-arg1", args[2]);
        assertEquals("unexpected: " + args[3], "one", args[3]);

    }

    public void testBurstContainsQuote() {
        String[] args = OptsUtil.burst("-whitespaces \"string with 'single' quote\"");
        assertEquals("unexpected: " + args[0], "-whitespaces", args[0]);
        assertEquals("unexpected: " + args[1], "string with 'single' quote", args[1]);

    }

    public void testBurstEscapedDoubleQuote() {
        String[] args = OptsUtil.burst("-whitespaces \"string with \"\"escaped\"\" quote\"");
        assertEquals("unexpected: " + args[0], "-whitespaces", args[0]);
        assertEquals("unexpected: " + args[1], "string with \"escaped\" quote", args[1]);

    }

    public void testBurstEscapedSingleQuote() {
        String[] args = OptsUtil.burst("-whitespaces 'string with ''escaped'' quote'");
        assertEquals("unexpected: " + args[0], "-whitespaces", args[0]);
        assertEquals("unexpected: " + args[1], "string with 'escaped' quote", args[1]);

    }

    public void testBurstQuotedDoubleQuote() {
        String[] args = OptsUtil.burst("-whitespaces 'string with \"escaped\" quote'");
        assertEquals("unexpected: " + args[0], "-whitespaces", args[0]);
        assertEquals("unexpected: " + args[1], "string with \"escaped\" quote", args[1]);

    }

    public void testBurstQuotedEscapedQuote() {
        String[] args = OptsUtil.burst("-whitespaces 'string with \"escape''d\" quote'");
        assertEquals("unexpected: " + args[0], "-whitespaces", args[0]);
        assertEquals("unexpected: " + args[1], "string with \"escape'd\" quote", args[1]);

    }

    public void testBurstQuotedBlank() {
        String[] args = OptsUtil.burst("-blank ''");
        assertEquals("unexpected: " + args[0], "-blank", args[0]);
        assertEquals("unexpected: " + args[1], "", args[1]);

    }

    public void testBurstDoubleQuotedBlank() {
        String[] args = OptsUtil.burst("-blank \"\"");
        assertEquals("unexpected: " + args[0], "-blank", args[0]);
        assertEquals("unexpected: " + args[1], "", args[1]);
    }

    public void testJoin() {

        assertEquals("-arg1 one -whitespaces \"string with white space\"",
                OptsUtil.join(
                        new String[]{
                                "-arg1",
                                "one",
                                "-whitespaces",
                                "string with white space"
                        }
                )
        );

        assertEquals("-whitespaces \"string with white space\" -arg1 one",
                OptsUtil.join(new String[]{
                        "-whitespaces",
                        "string with white space",
                        "-arg1",
                        "one"
                })

        );

        assertEquals("-whitespaces \"string    with   white space\" -arg1 one",
                OptsUtil.join(new String[]{
                        "-whitespaces",
                        "string    with   white space",
                        "-arg1",
                        "one",
                })
        );


        assertEquals("-whitespaces \"string with 'single' quote\"",
                OptsUtil.join(new String[]{
                        "-whitespaces",
                        "string with 'single' quote"
                })
        );

        assertEquals("-whitespaces \"string with \"\"escaped\"\" quote\"",
                OptsUtil.join(new String[]{
                        "-whitespaces",
                        "string with \"escaped\" quote",
                })
        );
        assertEquals("-whitespaces \"string with 'escaped' quote\"",
                OptsUtil.join(new String[]{
                        "-whitespaces",
                        "string with 'escaped' quote",
                })
        );
        assertEquals("-whitespaces \"'escaped'\"",
                OptsUtil.join(new String[]{
                        "-whitespaces",
                        "'escaped'",
                })
        );
        assertEquals("-whitespaces \"esc'aped\"",
                OptsUtil.join(new String[]{
                        "-whitespaces",
                        "esc'aped",
                })
        );
        assertEquals("-whitespaces \"esc\"\"aped\"",
                OptsUtil.join(new String[]{
                        "-whitespaces",
                        "esc\"aped",
                })
        );
    }
    public void testJoinBlank(){
        assertEquals("-abc \"\"",
                OptsUtil.join(new String[]{
                        "-abc",
                        "",
                })
        );
    }

}

