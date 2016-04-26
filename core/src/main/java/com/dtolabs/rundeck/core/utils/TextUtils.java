package com.dtolabs.rundeck.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Join/split and escape/unescape strings
 */
public class TextUtils {
    /**
     * Escape the input string using the escape delimiter for the given special chars
     *
     * @param input   input string
     * @param echar   escape char
     * @param special all chars that should be escaped, the escape char itself will be automatically included
     *
     * @return escaped string
     */
    public static String escape(String input, char echar, char[] special) {
        StringBuilder sb = new StringBuilder();
        for (Character character : special) {
            sb.append(character);
        }
        sb.append(echar);
        String s = Matcher.quoteReplacement(new String(new char[]{echar}));
        return input.replaceAll("[" + Pattern.quote(sb.toString()) + "]", s + "$0");
    }

    /**
     * Unescape the input string by removing the escape char for allowed chars
     *
     * @param input        input string
     * @param echar        escape char
     * @param allowEscaped all chars that can be escaped by the escape char
     *
     * @return unescaped string
     */
    public static String unescape(String input, char echar, char[] allowEscaped) {
        return unescape(input, echar, allowEscaped, new char[0]).unescaped;
    }

    /**
     * Unescape the input string by removing the escape char for allowed chars
     *
     * @param input        input string
     * @param echar        escape char
     * @param allowEscaped all chars that can be escaped by the escape char
     * @param delimiter    all chars that indicate parsing should stop
     *
     * @return partial unescape result
     */
    public static Partial unescape(String input, char echar, char[] allowEscaped, char[] delimiter) {

        StringBuilder component = new StringBuilder();
        boolean escaped = false;
        Character doneDelimiter = null;
        boolean done = false;
        String allowed = String.valueOf(allowEscaped != null ? allowEscaped : new char[0]) +
                         echar +
                         String.valueOf(delimiter != null ? delimiter : new char[0]);
        char[] array = input.toCharArray();
        int i;
        for (i = 0; i < array.length && !done; i++) {
            char c = array[i];
            if (c == echar) {
                if (escaped) {
                    component.append(echar);
                    escaped = false;
                } else {
                    escaped = true;
                }
            } else if (allowed.indexOf(c) >= 0) {
                if (escaped) {
                    component.append(c);
                    escaped = false;
                } else {
                    boolean delimFound = false;
                    if (null != delimiter) {
                        for (final char aDelimiter : delimiter) {
                            if (c == aDelimiter) {
                                delimFound = true;
                                doneDelimiter = aDelimiter;
                                done = true;
                                break;
                            }
                        }
                    }
                    if (!delimFound) {
                        //append
                        component.append(c);
                    }
                }
            } else {
                if (escaped) {
                    component.append(echar);
                    escaped = false;
                }

                component.append(c);
            }
        }
        String rest = i <= array.length - 1 ? new String(array, i, array.length - i) : null;

        return new Partial(component.toString(), doneDelimiter, rest);
    }

    /**
     * A partial unescape result
     */
    public static class Partial {
        private String unescaped;
        private Character delimited;
        private String remaining;

        public Partial(final String unescaped, final Character delimited, final String remaining) {
            this.unescaped = unescaped;
            this.delimited = delimited;
            this.remaining = remaining;
        }

        /**
         * @return unescaped portion found
         */
        public String getUnescaped() {
            return unescaped;
        }


        /**
         * @return delimiter found after unescaped portion, or null
         */
        public Character getDelimited() {
            return delimited;
        }

        /**
         * @return remaining unescaped text after delimiter, or null
         */
        public String getRemaining() {
            return remaining;
        }
    }

    /**
     * Join an array of strings with the given separator, without escaping
     *
     * @param input     input string
     * @param separator separator
     *
     * @return joined string
     */
    public static String join(String[] input, char separator) {
        //simple case, no escaping
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : input) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append(separator);
            }
            stringBuilder.append(s);
        }
        return stringBuilder.toString();
    }

    /**
     * Join an array of strings with the given separator, escape char, and other special chars for escaping
     *
     * @param input     input string
     * @param separator separator
     * @param echar     escape char
     * @param special   all special chars not necessarily including the echar or separator
     *
     * @return joined string
     */
    public static String joinEscaped(String[] input, char separator, char echar, char[] special) {
        StringBuilder sb = new StringBuilder();
        char[] schars = new char[(special != null ? special.length : 0) + 1];
        if (special != null && special.length > 0) {
            System.arraycopy(special, 0, schars, 1, special.length);
        }
        schars[0] = separator;
        for (String s : input) {
            if (sb.length() > 0) {
                sb.append(separator);
            }
            sb.append(escape(s, echar, schars));
        }
        return sb.toString();
    }

    /**
     * Split the input on the given separator char, and unescape each portion using the escape char and special chars
     *
     * @param input     input string
     * @param separator char separating each component
     * @param echar     escape char
     * @param special   chars that are escaped
     *
     * @return results
     */
    public static String[] splitUnescape(String input, char separator, char echar, char[] special) {
        return splitUnescape(input, new char[]{separator}, echar, special);
    }

    /**
     * Split the input on the given separator char, and unescape each portion using the escape char and special chars
     *
     * @param input      input string with components separated by the the separator chars
     * @param separators chars separating each component
     * @param echar      escape char
     * @param special    additional chars that are escaped, does not need to include echar or separators
     *
     * @return the unescaped components of the input
     */
    public static String[] splitUnescape(String input, char[] separators, char echar, char[] special) {
        List<String> components = new ArrayList<>();
        boolean done = false;
        String remaining = input;
        while (!done) {
            Partial result = unescape(remaining, echar, special, separators);
            if (result.getUnescaped() != null) {
                components.add(result.unescaped);
            }
            if (result.getRemaining() != null) {
                remaining = result.getRemaining();
            } else {
                done = true;
            }
        }
        return components.toArray(new String[components.size()]);

    }

}
