package com.dtolabs.rundeck.core.logging;

/*
 * Copyright 2013 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
 * LogLevel.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 1/22/13 6:52 PM
 * 
 */
public enum LogLevel {
    ERROR,
    WARN,
    NORMAL,
    VERBOSE,
    DEBUG,
    OTHER;

    public boolean belowThreshold(LogLevel threshold) {
        return this.compareTo(threshold) <= 0;
    }

    public static LogLevel looseValueOf(String value, LogLevel defLevel) {
        if (null == value || "".equals(value)) {
            return defLevel;
        }
        if (value.equals("SEVERE") || value.equals("ERR")) {
            return LogLevel.ERROR;
        } else if (value.equals("INFO")) {
            return LogLevel.NORMAL;
        } else if (value.equals("CONFIG")) {
            return LogLevel.DEBUG;
        } else {
            try {
                return LogLevel.valueOf(value);
            } catch (IllegalArgumentException e) {
                return defLevel;
            }
        }
    }
}
