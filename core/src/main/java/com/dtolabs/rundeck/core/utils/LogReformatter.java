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

/*
* FormattedPrefixGenerator.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: May 26, 2010 11:19:43 AM
* $Id$
*/
package com.dtolabs.rundeck.core.utils;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * LogReformatter can produce a formatted log message using a set of context data.  The available formatting strings
 * are:
 * <br>
 * %user,%node,%command,%level,%message. Each string maps to the same string in the input context, e.g. %user to "user".
 * The special "%message" context item will be replaced with the input message.
 * <br>
 * Example format string:  <code>[%user@%node %command][%level] %message</code>
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class LogReformatter implements Reformatter {
    private Map<String, String> data;
    private MapGenerator<String, String> generator;
    final private MessageFormat messageFormat;

    private LogReformatter(final String format) {
        String s="{4}";
        if(null!=format){
            s = format.replaceAll("%user", "{0}");
            s = s.replaceAll("%node", "{1}");
            s = s.replaceAll("%command", "{2}");
            s = s.replaceAll("%level", "{3}");
            s = s.replaceAll("%message", "{4}");
        }
        this.messageFormat = new MessageFormat(s);
    }

    public String getTail() {
        return "";
    }

    public String getHead() {
        return "";
    }

    /**
     * Create a LogFormatter with specified format and static context data.
     *
     * @param format the format string
     * @param data   the data
     */
    public LogReformatter(final String format, final Map<String, String> data) {
        this(format);
        this.data = data;
    }

    /**
     * Create a LogFormatter with specified format and object to generate context data
     *
     * @param format    the format string
     * @param generator generator of the data
     */
    public LogReformatter(final String format, final MapGenerator<String, String> generator) {
        this(format);
        this.generator = generator;
    }

    /**
     * Combines the context data, and the local static or dynamic context data, and reformats the message
     *
     * @param context input data
     * @param message message string
     *
     * @return reformatted message string
     */
    public String reformat(final Map<String, String> context, final String message) {
        final HashMap<String, String> tokens = new HashMap<String, String>();
        if (null != context) {
            tokens.putAll(context);
        }
        if (null != generator) {
            tokens.putAll(generator.getMap());
        } else if(null!=data) {
            tokens.putAll(data);
        }
        final String[] arr = {
            null!= tokens.get("user")? tokens.get("user"):"",
            null != tokens.get("node") ? tokens.get("node") : "",
            null != tokens.get("command") ? tokens.get("command") : "",
            tokens.get("level"),
            message
        };
        synchronized (messageFormat){
            return messageFormat.format(arr);
        }
    }
}
