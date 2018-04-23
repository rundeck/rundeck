/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

import com.dtolabs.rundeck.core.execution.workflow.ContextStack;

import java.io.IOException;
import java.util.Optional;

/**
 * Stack based override of the log writer sink for the current and child threads, each child thread gets a copy of the stack
 * @author greg
 * @since 5/10/17
 */
public class OverridableStreamingLogWriter extends FilterStreamingLogWriter {
    private final InheritableThreadLocal<ContextStack<Optional<StreamingLogWriter>>> override = new
            InheritableThreadLocal<ContextStack<Optional<StreamingLogWriter>>>() {
                @Override
                protected ContextStack<Optional<StreamingLogWriter>> initialValue() {
                    return new ContextStack<>();
                }

                @Override
                protected ContextStack<Optional<StreamingLogWriter>> childValue(
                        final ContextStack<Optional<StreamingLogWriter>> parentValue
                )
                {
                    return new ContextStack<>(parentValue.stack());
                }
            };

    public OverridableStreamingLogWriter(final StreamingLogWriter writer) {
        super(writer);
    }

    @Override
    public void openStream() throws IOException {
        if (getOverride() != null) {
            getOverride().openStream();
            return;
        }
        super.openStream();
    }

    @Override
    public void addEvent(final LogEvent event) {
        if (getOverride() != null) {
            getOverride().addEvent(event);
            return;
        }
        super.addEvent(event);
    }

    @Override
    public void close() {
        if (getOverride() != null) {
            getOverride().close();
            return;
        }
        super.close();
    }

    public StreamingLogWriter getOverride() {
        if (override.get().size() > 0) {
            return override.get().peek().orElse(null);
        } else {
            return null;
        }
    }

    /**
     * Set the writer to use
     *
     * @param writer writer
     */
    public void setOverride(StreamingLogWriter writer) {
        override.get().push(Optional.ofNullable(writer));
    }

    /**
     * Push no value onto the stack
     */
    public void pushEmpty() {
        setOverride(null);
    }

    /**
     * Remove the overriding writer, if any
     *
     * @return overriding writer, or null
     */
    public StreamingLogWriter removeOverride() {
        StreamingLogWriter previous = override.get().pop().orElse(null);
        return previous;
    }
}
