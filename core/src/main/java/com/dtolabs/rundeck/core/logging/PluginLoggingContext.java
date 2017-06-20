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

package com.dtolabs.rundeck.core.logging;

import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.data.DataContext;
import com.dtolabs.rundeck.core.data.MultiDataContext;
import com.dtolabs.rundeck.core.execution.ExecutionLogger;
import com.dtolabs.rundeck.core.execution.workflow.SharedOutputContext;

/**
 * Context for LoggingFilter plugins
 *
 * @author greg
 * @since 5/11/17
 */
public interface PluginLoggingContext extends ExecutionLogger {

    /**
     * @return context for emitting new data
     */
    public SharedOutputContext getOutputContext();

    /**
     * Return data context set
     *
     * @return map of data contexts keyed by name
     */
    public DataContext getDataContext();

    /**
     * @return the scoped context data keyed by scope
     */
    public MultiDataContext<ContextView, DataContext> getSharedDataContext();

    /**
     * @return the data context in the private scope
     */
    public DataContext getPrivateDataContext();
}
