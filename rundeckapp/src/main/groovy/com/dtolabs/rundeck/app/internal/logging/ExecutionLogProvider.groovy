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

import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogReader
import com.dtolabs.rundeck.core.logging.StreamingLogWriter

/*
 * ExecutionLogProvider.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 1/22/13 6:41 PM
 * 
 */
public interface ExecutionLogProvider {
    StreamingLogWriter getLogWriter(String id, String key, LogLevel threshold, Map<String, String> defaultMeta)
    StreamingLogReader getLogReader(String id, String key)
    URL getURLForKey(String key)
}
