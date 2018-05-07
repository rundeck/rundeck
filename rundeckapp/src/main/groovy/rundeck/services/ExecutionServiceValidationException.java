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

package rundeck.services;

import java.util.Map;

/**
 * $INTERFACE is ... User: greg Date: 10/21/13 Time: 10:36 AM
 */
public class ExecutionServiceValidationException extends ExecutionServiceException {

    Map<String, String> options;
    Map<String, String> errors;

    public ExecutionServiceValidationException() {
    }

    public ExecutionServiceValidationException(String s, Map<String, String> options, Map<String, String> errors) {
        super(s);
        this.options = options;
        this.errors = errors;
    }

    public ExecutionServiceValidationException(String s, Map<String, String> options, Map<String, String> errors,
            Throwable throwable) {
        super(s, throwable);
        this.options = options;
        this.errors = errors;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
