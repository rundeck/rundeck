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
    private Map<String, String> tokens;
    boolean blankIfMissing;
    char tokenStart = DEFAULT_TOKEN_START;
    char tokenEnd = DEFAULT_TOKEN_END;
    private Predicate tokenCharPredicate;

    public ReplaceTokenReader(Reader reader, Map<String, String> tokens, boolean blankIfMissing) {
        this(reader, tokens, blankIfMissing, DEFAULT_TOKEN_START, DEFAULT_TOKEN_END);
    }

    public ReplaceTokenReader(Reader reader, Map<String, String> tokens, boolean blankIfMissing, char tokenStart,
            char tokenEnd) {
        super(reader);
        this.tokens = tokens;
        this.blankIfMissing = blankIfMissing;
        this.tokenStart = tokenStart;
        this.tokenEnd = tokenEnd;
        replaceBuffer = new StringBuilder();
        readBuffer = new StringBuilder();
        replaceBufferIndex = -1;
        readBufferIndex = -1;
        tokenCharPredicate = DEFAULT_ALLOWED_PREDICATE;
    }

    private static final char[] ALLOWED_CHARS = ".+-_:".toCharArray();
    static {
        Arrays.sort(ALLOWED_CHARS);
    }
    public static final Predicate DEFAULT_ALLOWED_PREDICATE = new Predicate() {
        @Override
        public boolean evaluate(Object o) {
            Character c = (Character) o;
            return Character.isLetterOrDigit((int)c) || Arrays.binarySearch(ALLOWED_CHARS, c) >= 0;
        }
    };


    private StringBuilder replaceBuffer;
    private int replaceBufferIndex;
    private int readBufferIndex;
    private StringBuilder readBuffer;

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
        int read = super.read();
        if (read == tokenStart) {
            readBuffer.append((char) read);
            if (readBufferIndex < 0) {
                readBufferIndex = 0;
            }
            do {
                read = super.read();
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
