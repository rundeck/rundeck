/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.schedule;

/**
 * @author greg
 * @since 8/11/17
 */
public class JobScheduleFailure extends Exception {
    public JobScheduleFailure() {
    }

    public JobScheduleFailure(final String message) {
        super(message);
    }

    public JobScheduleFailure(final String message, final Throwable cause) {
        super(message, cause);
    }

    public JobScheduleFailure(final Throwable cause) {
        super(cause);
    }

    public JobScheduleFailure(
            final String message,
            final Throwable cause,
            final boolean enableSuppression,
            final boolean writableStackTrace
    )
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
