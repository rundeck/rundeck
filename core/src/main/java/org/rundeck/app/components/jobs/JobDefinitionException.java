/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.app.components.jobs;

public class JobDefinitionException
        extends Exception
{
    public JobDefinitionException() {
        super();
    }

    public JobDefinitionException(final String message) {
        super(message);
    }

    public JobDefinitionException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public JobDefinitionException(final Throwable cause) {
        super(cause);
    }

    protected JobDefinitionException(
            final String message,
            final Throwable cause,
            final boolean enableSuppression,
            final boolean writableStackTrace
    )
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
