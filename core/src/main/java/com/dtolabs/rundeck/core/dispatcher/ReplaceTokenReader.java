package com.dtolabs.rundeck.core.dispatcher;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

/**
 * $INTERFACE is ... User: greg Date: 7/1/13 Time: 3:37 PM
 */
public class ReplaceTokenReader extends FilterReader {
    public static final char DEFAULT_TOKEN_START = '@';
    public static final char DEFAULT_TOKEN_END = '@';
    private Map<String, String> tokens;
    boolean blankIfMissing;
    char tokenStart = DEFAULT_TOKEN_START;
    char tokenEnd = DEFAULT_TOKEN_END;

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
    }

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
}
