package org.rundeck.plugin.scriptnodestep

import groovy.transform.CompileStatic

import java.util.regex.Pattern

/**
 * Utility for encoding bash Strings
 * @see <a href="https://www.gnu.org/software/bash/manual/html_node/Shell-Parameters.html">Bash Shell Parameters</a>
 */
@CompileStatic
class BashShellUtil implements ShellUtil {
    static final String Q = '\''
    static final String BS = '\\'

    /**
     * Special chars except single quote
     */
    static final Pattern ESCAPED_CHARS = Pattern.compile(
        '[ #^&*()\\[\\]{}?<>;"`!~$' + Pattern.quote(BS) + ']'
    )

    /**
     * white space chars to use ANSI escape sequences
     */
    static final Pattern WS_CHARS = Pattern.compile(
        '[\t\n\r\b]'
    )

    static final Map<String, String> ANSI = [
        '\t': '\\t',
        '\n': '\\n',
        '\r': '\\r',
        '\b': '\\b',
    ]
    /**
     * Quote a string for use in a bash variable value
     * @param value the value
     * @return the encoded value
     */
    String quote(final String value) {
        //split on single quote, escape special chars, escape whitespace, join with escaped single quote
        value.split(Pattern.quote(Q)).
            collect { escapeChars(it) }.
            collect { escapeWs(it) }.
            join(BS + Q)
    }

    static String escapeChars(String it) {
        it.replaceAll(ESCAPED_CHARS, '\\\\$0')
    }

    static String escapeWs(String it) {
        it.replaceAll(WS_CHARS) { String[] args ->
            '$' + Q + ANSI[args[0]] + Q
        }
    }

    void appendEnvLine(
        String name,
        String value,
        StringBuilder sb
    ) {
        sb.append(name).append("=").append(quote(value)).append(";");
    }
}
