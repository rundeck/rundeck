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

package com.dtolabs.rundeck.core.dispatcher;

import org.apache.commons.collections.Predicate;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

/**
 * Reader that filters text to replace delimited tokens with values, the default delimiters are '@', and the default
 * allowed token characters are alphanumeric plus punctuation characters: "+-._:"
 */
public class ReplaceTokenReader extends FilterReader {
    public static final char DEFAULT_TOKEN_START = '@';
    public static final char DEFAULT_TOKEN_END = '@';
    private Map<String, String> tokens;
    boolean blankIfMissing;
    char tokenStart = DEFAULT_TOKEN_START;
    char tokenEnd = DEFAULT_TOKEN_END;
    private Predicate tokenCharPredicate;

    static class Buf {
        StringBuilder buffer = new StringBuilder();
        int index = -1;

        int length() {
            return index > -1
                   ? buffer.length() - index
                   : -1;
        }

        boolean avail() {
            return length() > 0;
        }

        public int read() {
            int val = -1;
            if (index > -1 && index < buffer.length()) {
                val = buffer.charAt(index++);
                if (!avail()) {
                    buffer.setLength(0);
                    index = -1;
                }
            }
            return val;
        }

        public void append(final char read) {
            buffer.append((char) read);
            if (index < 0) {
                index = 0;
            }
        }

        public void reset() {
            buffer.setLength(0);
            index = -1;
        }

        public void append(final String value) {
            buffer.append(value);
            if (index < 0) {
                index = 0;
            }
        }

        public void append(final Buf buf) {
            buffer.append(buf.buffer);
            if (index < 0) {
                index = 0;
            }
        }

        public String readAll() {
            String substring = buffer.substring(index);
            reset();
            return substring;
        }
    }

    public ReplaceTokenReader(
            Reader reader, Map<String, String> tokens, boolean blankIfMissing, char tokenStart,
            char tokenEnd
    )
    {
        super(reader);
        this.tokens = tokens;
        this.blankIfMissing = blankIfMissing;
        this.tokenStart = tokenStart;
        this.tokenEnd = tokenEnd;
        readBuffer = new Buf();
        tokenBuffer = new Buf();
        tokenCharPredicate = DEFAULT_ALLOWED_PREDICATE;
    }

    public static final Predicate DEFAULT_ALLOWED_PREDICATE = new Predicate() {
        @Override
        public boolean evaluate(Object o) {
            Character c = (Character) o;
            return !Character.isWhitespace(c);
        }
    };


    private boolean escaped;
    private Buf readBuffer;
    private Buf tokenBuffer;

    @Override
    public int read(char[] chars, int offset, int len) throws IOException {
        int count=0;
        while (count < len && (count + offset) < chars.length) {
            int read = read();
            if (read != -1) {
                chars[count + offset] = (char) read;
                count++;
            } else if(count==0){
                return -1;
            }else{
                break;
            }
        }
        return count;
    }

    @Override
    public int read() throws IOException {
        //return buffered content that has no tokens
        if (readBuffer.avail()) {
            return readBuffer.read();
        }
        int read = -1;
        //re-read from buffered token chars
        if (tokenBuffer.avail()) {
            read = tokenBuffer.read();
        } else {
            read = super.read();
        }
        if (escaped) {
            escaped = false;
            return read;
        }
        if (read == tokenStart) {
            readBuffer.append((char) read);
            do {
                read = super.read();
                if (read == tokenStart) {
                    if (readBuffer.length() == 1) {
                        //return a single start token
                        return read();
                    }
                }
                if (read == tokenEnd) {
                    //eat tokenStart
                    readBuffer.read();
                    String key = readBuffer.readAll();
                    readBuffer.append(substitution(key));
                    return read();
                } else if (read != -1) {
                    readBuffer.append((char) read);
                    if (readBuffer.length() > 1 && !tokenCharPredicate.evaluate((char) read)) {
                        //not an allowed token character
                        //simply replace the content, and continue
                        return read();
                    }
                }
            } while (read != -1);

            //return buffered content that has no tokens
            if (readBuffer.avail()) {
                return readBuffer.read();
            }
        }
        return read;
    }


    private String substitution(String key) {
        if (null != tokens.get(key)) {
            return tokens.get(key);
        } else if (blankIfMissing) {
            return "";
        } else {
            return tokenStart + key + tokenEnd;
        }
    }


    public Predicate getTokenCharPredicate() {
        return tokenCharPredicate;
    }

    public void setTokenCharPredicate(Predicate tokenCharPredicate) {
        this.tokenCharPredicate = tokenCharPredicate;
    }
}
