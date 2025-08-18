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

/*
* CLIUtils.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 31, 2010 8:43:34 AM
* $Id$
*/
package com.dtolabs.rundeck.core.cli;

import com.dtolabs.rundeck.core.utils.Converter;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * CLIUtils provides utility functions
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class CLIUtils {
    /**
     * Create an appropriately quoted argline to use given the command (script path) and argument strings.
     *
     * @param scriptpath path to command or script
     * @param args       arguments to pass to the command
     *
     * @return a String of the command followed by the arguments, where each item which has spaces is appropriately
     *         quoted.  Pre-quoted items are not changed during "unsafe" quoting.
     *
     *         At this point in time, default behavior is "unsafe" quoting.
     */
    public static String generateArgline(final String scriptpath, final String[] args) {
        return generateArgline(scriptpath, args, " ", false);
    }

    /**
     * Create an appropriately quoted argline to use given the command (script path) and argument strings.
     *
     * @param scriptpath path to command or script
     * @param args       arguments to pass to the command
     * @param unsafe     whether to use backwards-compatible, known-insecure quoting
     *
     * @return a String of the command followed by the arguments, where each item which has spaces is appropriately
     *         quoted.  Pre-quoted items are not changed during "unsafe" quoting.
     */
    public static String generateArgline(final String scriptpath, final String[] args, final Boolean unsafe) {
        return generateArgline(scriptpath, args, " ", unsafe);
    }

    /**
     * Create an appropriately quoted argline to use given the command (script path) and argument strings.
     *
     * @param scriptpath path to command or script
     * @param args       arguments to pass to the command
     * @param separator  character to use to separate arguments
     * @param unsafe     whether to use backwards-compatible, known-insecure quoting
     *
     * @return a String of the command followed by the arguments, where each item which has spaces is appropriately
     *         quoted.  Pre-quoted items are not changed during "unsafe" quoting.
     */
    public static String generateArgline(final String scriptpath, final String[] args, final String separator, final Boolean unsafe) {
        final StringBuilder sb = new StringBuilder();
        final ArrayList<String> list = new ArrayList<String>();
        if (null != scriptpath) {
            list.add(scriptpath);
        }
        if (null != args) {
            list.addAll(Arrays.asList(args));
        }
        for (final String arg : list) {
            if(null==arg){
                continue;
            }
            if (sb.length() > 0) {
                sb.append(separator);
            }
            if(unsafe) {
                /* DEPRECATED SECURITY RISK: Exists for backwards compatibility only. */
                if (arg.indexOf(" ") >= 0 && !(0 == arg.indexOf("'") && (arg.length() - 1) == arg.lastIndexOf("'"))) {
                    sb.append("'").append(arg).append("'");
                } else {
                    sb.append(arg);
                }
            } else {
                quoteUnixShellArg(sb, arg);
            }
        }
        return sb.toString();
    }

    /**
     * @return true if the string contains any whitespace
     * @param arg string
     */
    public static boolean containsSpace(String arg) {
        return StringUtils.containsAny(arg, " ");
    }
    /**
     * @return true if the string contains any whitespace
     * @param arg string
     */
    public static boolean containsQuote(String arg) {
        return StringUtils.containsAny(arg, "'");
    }

    /**
     * evaluates to true if a string contains a space
     */
    public static final Predicate<String> stringContainsWhitespacePredicate = CLIUtils::containsSpace;
    /**
     * evaluates to true if a string contains a quote
     */
    public static final Predicate<String> stringContainsQuotePredicate = CLIUtils::containsQuote;
    public static String quoteUnixShellArg(String arg) {
        StringBuilder stringBuilder = new StringBuilder();
        quoteUnixShellArg(stringBuilder, arg);
        return stringBuilder.toString();
    }

    public static String quoteWindowsCMDArg(String arg) {
        StringBuilder stringBuilder = new StringBuilder();
        quoteWindowsCMDArg(stringBuilder, arg);
        return stringBuilder.toString();
    }

    private static void quoteWindowsCMDArg(StringBuilder sb, String arg) {
        if (StringUtils.containsNone(arg, WINDOWS_CMD_CHARS) &&
                StringUtils.containsNone(arg, WINDOWS_WS_CHARS) &&
                StringUtils.containsNone(arg, " ")) {
            if (arg != null) {
                sb.append(arg);
            }
            return;
        }
        sb.append("'");
        sb.append(arg.replace("'", "'\"'\"'"));
        sb.append("'");
    }

    private static void quoteUnixShellArg(StringBuilder sb, String arg) {
        if (StringUtils.containsNone(arg, UNIX_SHELL_CHARS) &&
                StringUtils.containsNone(arg, WS_CHARS) &&
                StringUtils.containsNone(arg, " ")) {
            if (arg != null) {
                sb.append(arg);
            }
            return;
        }
        sb.append("'");
        sb.append(arg.replace("'", "'\"'\"'"));
        sb.append("'");
    }

    public static Converter<String, String> characterEscapeForOperatingSystem(String type) {
        Converter<String, String> defaultConverter = UNIX_SHELL_ESCAPE;
        if ("unix".equalsIgnoreCase(type)) {
            return UNIX_SHELL_ESCAPE;
            //TODO: windows
        } else {
            return defaultConverter;
        }
    }

    public static Converter<String, String> argumentQuoteForOperatingSystem(String type) {
        return argumentQuoteForOperatingSystem(type, null);
    }

    /**
     * Create a converter for quoting arguments for the given operating system type and command interpreter.
     *
     * @param type               "unix" or "windows"
     * @param commandInterpreter "cmd" for Windows cmd.exe, or null for default
     * @return a Converter that quotes arguments appropriately for the specified OS and command interpreter
     */
    public static Converter<String, String> argumentQuoteForOperatingSystem(String type, String commandInterpreter) {
        Converter<String, String> defaultConverter = UNIX_ARGUMENT_QUOTE;
        if ("unix".equalsIgnoreCase(type)) {
            return UNIX_ARGUMENT_QUOTE;
        }else if("windows".equalsIgnoreCase(type)){
            if("cmd".equalsIgnoreCase(commandInterpreter)){
                return WINDOWS_CMD_ESCAPE;
            }
            return WINDOWS_ARGUMENT_QUOTE;
        }else {
            return defaultConverter;
        }
    }

    /**
     * Converter that can escape shell-special characters
     */
    public static final Converter<String, String> UNIX_ARGUMENT_QUOTE= new Converter<String, String>() {
        public String convert(String s) {
            return quoteUnixShellArg(s);
        }
    };

    /**
     * Converter that can quote arguments for Windows
     * This is not the same as escaping special characters, it quotes the entire argument
     */
    public static final Converter<String, String> WINDOWS_ARGUMENT_QUOTE= new Converter<String, String>() {
        public String convert(String s) {
            return quoteWindowsCMDArg(s);
        }
    };

    /**
     * Converter that can escape shell-special characters
     */
    public static final Converter<String,String> UNIX_SHELL_ESCAPE =new Converter<String, String>() {
        public String convert(String s) {
            return escapeUnixShellChars(s);
        }
    };

    /**
     * Converter that can escape Windows CMD special characters
     */
    public static final Converter<String,String> WINDOWS_CMD_ESCAPE =new Converter<String, String>() {
        public String convert(String s) {
            return escapeWindowsCMDChars(s);
        }
    };

    public static String escapeUnixShellChars(String str) {
        StringBuilder stringBuilder = new StringBuilder();
        escapeUnixShellChars(stringBuilder, str, UNIX_SHELL_CHARS);
        return stringBuilder.toString();
    }

    public static String escapeUnixShellChars(String str, String shellChars) {
        StringBuilder stringBuilder = new StringBuilder();
        escapeUnixShellChars(stringBuilder, str, shellChars);
        return stringBuilder.toString();
    }

    public static String escapeWindowsCMDChars(String str) {
        StringBuilder stringBuilder = new StringBuilder();
        escapeWindowsCMDChars(stringBuilder, str);
        return stringBuilder.toString();
    }

    public static final String UNIX_SHELL_CHARS = "\"';{}()&$\\|*?><`";
    public static final String WINDOWS_CMD_CHARS = "&|<>^%!;,()\\/:*?\"<>|$`'\"{}";
    public static final String UNIX_SHELL_CHARS_NO_QUOTES = ";{}()&$\\|*?><";
    /**
     * non-space whitespace
     */
    private static final String WS_CHARS = "\n\r\t";
    private static final String WINDOWS_WS_CHARS = "\n\r\t\0";

    /**
     * Escape special characters in a string for use in a Windows CMD command line.
     *
     * @param sb  StringBuilder to append the escaped string to
     * @param str String to escape
     */
    public static void escapeWindowsCMDChars(StringBuilder sb, String str) {
        if (str != null) {
            for (char c : str.toCharArray()) {
                if (WINDOWS_CMD_CHARS.indexOf(c) >= 0) {
                    sb.append('^');
                }
                sb.append(c);
            }
        }
    }

    public static void escapeUnixShellChars(StringBuilder out, String str, final String unixShellChars) {
        if (StringUtils.containsNone(str, unixShellChars)) {
            if (str != null) {
                out.append(str);
            }
            return;
        }
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (unixShellChars.indexOf(c) >= 0) {
                out.append('\\');
            }else if(WS_CHARS.indexOf(c)>=0){
                out.append('\\');
                if(c==CharUtils.CR){
                    out.append('r');
                }else if(c==CharUtils.LF){
                    out.append('n');
                }else if(c=='\t'){
                    out.append('t');
                }
                continue;
            }
            out.append(c);
        }
    }
}
