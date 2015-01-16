package com.dtolabs.rundeck.core.utils;

import org.apache.commons.lang.text.StrMatcher;

import java.util.*;

/**
 * Tokenizer for strings delimited by spaces, allowing quoted strings with either single or double quotes, and escaped
 * quote values within those strings by doubling the quote character.  Delimiters are not returned in the tokens, and
 * runs of delimiters can be quelled.  All chars in a quoted section are returned, even blanks.
 * Implements {@link Iterable} and {@link Iterator}.
 *
 */
public class QuotedStringTokenizer implements Iterator<String>, Iterable<String> {
    private char[] string;
    private int pos;
    private Queue<String> buffer;
    private StrMatcher delimiterMatcher;
    private StrMatcher quoteMatcher;
    private StrMatcher whitespaceMatcher;
    private boolean quashDelimiters;

    public QuotedStringTokenizer(String string) {
        this(string.toCharArray(), 0);
    }

    public QuotedStringTokenizer(char[] chars, int pos) {
        this.string = chars;
        this.pos = pos;
        buffer = new ArrayDeque<String>();
        delimiterMatcher = StrMatcher.trimMatcher();
        quoteMatcher = StrMatcher.quoteMatcher();
        whitespaceMatcher = StrMatcher.trimMatcher();
        quashDelimiters=true;
        readNext();
    }

    public static String[] tokenizeToArray(String string) {
        List<String> strings = collectTokens(string);
        return strings.toArray(new String[strings.size()]);
    }

    public static List<String> tokenizeToList(String string) {
        return collectTokens(string);
    }

    public static Iterable<String> tokenize(String string) {
        return new QuotedStringTokenizer(string);
    }

    private static List<String> collectTokens(String string) {
        ArrayList<String> strings = new ArrayList<String>();
        for (String s : new QuotedStringTokenizer(string)) {
            strings.add(s);
        }
        return strings;
    }


    @Override
    public boolean hasNext() {
        return !buffer.isEmpty();
    }

    @Override
    public String next() {
        String remove = buffer.remove();
        readNext();
        return remove;
    }

    private void readNext() {
        pos = readNextToken(string, pos, buffer);
    }

    private int readNextToken(char[] chars, int pos, Collection<String> tokens) {
        if (pos >= chars.length) {
            return -1;
        }
        int ws = whitespaceMatcher.isMatch(chars, pos);
        if (ws > 0) {
            pos += ws;
        }
        if (pos >= chars.length) {
            return -1;
        }
        int delim = delimiterMatcher.isMatch(chars, pos);
        if (delim > 0) {
            if (quashDelimiters) {
                pos = consumeDelimiters(chars, pos, delim);
            } else {
                addToken(buffer, "");
                return pos + delim;
            }
        }
        int quote = quoteMatcher.isMatch(chars, pos);
        return readQuotedToken(chars, pos, tokens, quote);
    }

    private int consumeDelimiters(char[] chars, int start, int delim) {
        while (delim > 0 && start < chars.length - delim) {
            start += delim;
            delim = delimiterMatcher.isMatch(chars, start);
        }
        return start;
    }

    private int readQuotedToken(char[] chars, int start, Collection<String> tokens, int quotesize) {
        int pos = start;
        StringBuilder tchars = new StringBuilder();
        boolean quoting = quotesize > 0;
        if (quoting) {
            pos += quotesize;
        }
        while (pos < chars.length) {
            if (quoting) {
                if (charsMatch(chars, start, pos, quotesize)) {
                    //matches the quoting char

                    //if next token is the same quote, it is an escaped quote
                    if (charsMatch(chars, start, pos + quotesize, quotesize)) {
                        //token the quote
                        tchars.append(new String(chars, pos, quotesize));
                        pos += 2 * quotesize;
                        continue;
                    }
                    //end of quoting
                    quoting = false;
                    pos += quotesize;
                    continue;
                }
                //append char
                tchars.append(chars[pos++]);
            } else {
                int delim = delimiterMatcher.isMatch(chars, pos);
                if (delim > 0) {
                    if (quashDelimiters) {
                        pos = consumeDelimiters(chars, pos, delim);
                        addToken(tokens, tchars.toString());
                        return pos;
                    } else {
                        addToken(tokens, tchars.toString());
                        return pos + delim;
                    }
                }

                if (quotesize > 0 && charsMatch(chars, start, pos, quotesize)) {
                    //new quote
                    quoting = true;
                    pos += quotesize;
                    continue;
                }
                //append char
                tchars.append(chars[pos++]);
            }
        }
        addToken(tokens, tchars.toString());
        return pos;
    }

    /**
     * @return true if two sequences of chars match within the array.
     *
     * @param chars char set
     * @param pos1  position 1
     * @param pos2  position 2
     * @param len2  length to compare
     *
     */
    private boolean charsMatch(char[] chars, int pos1, int pos2, int len2) {
        return charsMatch(chars, pos1, len2, pos2, len2);
    }

    /**
     * @return true if two sequences of chars match within the array.
     *
     * @param chars char set
     * @param pos1 pos 1
     * @param len1 length 1
     * @param pos2 pos 2
     * @param len2 length 2
     *
     */
    private boolean charsMatch(char[] chars, int pos1, int len1, int pos2, int len2) {
        if (len1 != len2) {
            return false;
        }
        if (pos1 + len1 > chars.length || pos2 + len2 > chars.length) {
            return false;
        }
        for (int i = 0; i < len1; i++) {
            if (chars[pos1 + i] != chars[pos2 + i]) {
                return false;
            }
        }
        return true;
    }

    private void addToken(Collection<String> buffer, String token) {
        buffer.add(token);
    }

    @Override
    public void remove() {
    }

    @Override
    public Iterator<String> iterator() {
        return this;
    }
}
