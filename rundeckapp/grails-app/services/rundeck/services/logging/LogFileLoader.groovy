/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package rundeck.services.logging

import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileLoader
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState
import groovy.transform.CompileStatic

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 12/4/13
 * Time: 2:47 PM
 */
@CompileStatic
class LogFileLoader implements ExecutionFileLoader {
    ExecutionFileState state
    String errorCode
    List<String> errorData
    File file
    long retryBackoff
    InputStream stream
}
