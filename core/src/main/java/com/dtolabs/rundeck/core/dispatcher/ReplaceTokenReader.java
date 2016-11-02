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
import java.util.Arrays;
import java.util.Map;

/**
 * Reader that filters text to replace delimited tokens with values, the default delimiters are '@', and the default
 * allowed token characters are alphanumeric plus punctuation characters: "+-._:"
 */
public class ReplaceTokenReader extends FilterReader {
    public static final char DEFAULT_TOKEN_START = '@';
    public static final char DEFAULT_TOKEN_END = '@';
    public static final char DEFAULT_ESCAPE = '\\';
    private Map<String, String> tokens;
    boolean blankIfMissing;
    char tokenStart = DEFAULT_TOKEN_START;
    char tokenEnd = DEFAULT_TOKEN_END;
    char tokenEsc = DEFAULT_ESCAPE;
    private Predicate tokenCharPredicate;


    public ReplaceTokenReader(Reader reader, Map<String, String> tokens, boolean blankIfMissing, char tokenStart,
            char tokenEnd) {
        this(reader, tokens, blankIfMissing, tokenStart, tokenEnd, DEFAULT_ESCAPE);

    }

    public ReplaceTokenReader(
            Reader reader, Map<String, String> tokens, boolean blankIfMissing, char tokenStart,
            char tokenEnd, char tokenEsc
    )
    {
        super(reader);
        this.tokens = tokens;
        this.blankIfMissing = blankIfMissing;
        this.tokenStart = tokenStart;
        this.tokenEnd = tokenEnd;
        this.tokenEsc = tokenEsc;
        replaceBuffer = new StringBuilder();
        readBuffer = new StringBuilder();
        tokenBuffer = new StringBuilder();
        replaceBufferIndex = -1;
        readBufferIndex = -1;
        tokenBufferIndex = -1;
        tokenCharPredicate = DEFAULT_ALLOWED_PREDICATE;
    }

    public static final Predicate DEFAULT_ALLOWED_PREDICATE = new Predicate() {
        @Override
        public boolean evaluate(Object o) {
            Character c = (Character) o;
            return !Character.isWhitespace(c);
        }
    };


    private StringBuilder replaceBuffer;
    private int replaceBufferIndex;
    private int readBufferIndex;
    private int tokenBufferIndex;
    private boolean escaped;
    private StringBuilder readBuffer;
    private StringBuilder tokenBuffer;

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
        //return replacement text
        if (replaceBufferIndex >= 0 && replaceBufferIndex < replaceBuffer.length()) {
            return replaceBuffer.charAt(replaceBufferIndex++);
        }
        //return buffered content that has no tokens
        if (readBufferIndex >= 0 && readBufferIndex < readBuffer.length()) {
            return readBuffer.charAt(readBufferIndex++);
        }
        int read = -1;
        //re-read from buffered token chars
        if (tokenBufferIndex >= 0 && tokenBufferIndex < tokenBuffer.length()) {
            read = tokenBuffer.charAt(tokenBufferIndex++);
        } else {
            read = super.read();
        }
        if (escaped && read == tokenStart) {
            escaped = false;
            return read;
        } else if (escaped) {
            escaped = false;
            readBuffer.append((char) read);
            if (readBufferIndex < 0) {
                readBufferIndex = 0;
            }
            return tokenEsc;
        }
        if (read == tokenEsc) {
            escaped = true;
            return read();
        }
        if (read == tokenStart) {
            readBuffer.append((char) read);
            if (readBufferIndex < 0) {
                readBufferIndex = 0;
            }
            do {
                read = super.read();
                if (read == tokenStart) {
                    if (readBuffer.length() == 1) {
                        //start token followed by start token
                        tokenBuffer.append((char) read);
                        if (tokenBufferIndex < 0) {
                            tokenBufferIndex = 0;
                        }
                        //restart as token
                        replaceBuffer.setLength(0);
                        replaceBuffer.append(readBuffer);
                        replaceBufferIndex = 0;
                        readBuffer.setLength(0);
                        readBufferIndex = -1;
                        return read();
                    }
                }
                if (read == tokenEnd) {
                    //end of replacement
                    String key = readBuffer.substring(1);
                    replaceBuffer.setLength(0);
                    readBuffer.setLength(0);
                    appendTokenSubstitute(key, replaceBuffer);
                    replaceBufferIndex = 0;
                    readBufferIndex = -1;
                    return read();
                } else if(read!=-1) {
                    readBuffer.append((char) read);
                    if(readBufferIndex<0){
                        readBufferIndex=0;
                    }
                    if (readBuffer.length() > 1 && !tokenCharPredicate.evaluate((char) read)) {
                        //not an allowed token character
                        //simply replace the content, and continue
                        replaceBuffer.setLength(0);
                        replaceBuffer.append(readBuffer);
                        replaceBufferIndex=0;
                        readBuffer.setLength(0);
                        readBufferIndex=-1;
                        return read();
                    }
                }
            } while (read != -1);

            //return buffered content that has no tokens
            if (readBufferIndex >= 0 && readBufferIndex < readBuffer.length()) {
                return readBuffer.charAt(readBufferIndex++);
            }
        }
        return read;
    }


    private void appendTokenSubstitute(String key, StringBuilder replaceBuffer) {
        if (null != tokens.get(key)) {
            replaceBuffer.append(tokens.get(key));
        } else if (blankIfMissing) {
            replaceBuffer.append("");
        } else {
            replaceBuffer.append(tokenStart).append(key).append(tokenEnd);
        }
    }

    public Predicate getTokenCharPredicate() {
        return tokenCharPredicate;
    }

    public void setTokenCharPredicate(Predicate tokenCharPredicate) {
        this.tokenCharPredicate = tokenCharPredicate;
    }
}
