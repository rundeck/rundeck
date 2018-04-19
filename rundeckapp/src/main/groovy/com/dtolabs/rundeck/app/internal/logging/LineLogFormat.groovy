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

package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogEvent

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 5/21/13
 * Time: 6:05 PM
 */
public interface LineLogFormat {
    FormatItem parseLine(String line)
    long seekBackwards(File file, int count)
    static interface FormatItem {
        boolean getFileEnd()
        boolean getLineComplete()
        boolean getFileStart()
        boolean isInvalid()
        LogEvent getEntry()
        String getPartial()
    }
}
