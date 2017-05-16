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

import java.io.IOException;

/**
 * Can override the log writer sink for the current and child threads
 * @author greg
 * @since 5/10/17
 */
public class OverridableStreamingLogWriter extends FilterStreamingLogWriter {
    private final InheritableThreadLocal<StreamingLogWriter> override = new InheritableThreadLocal<>();

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
        return override.get();
    }

    /**
     * Set the writer to use
     *
     * @param writer writer
     */
    public void setOverride(StreamingLogWriter writer) {
        override.set(writer);
    }

    /**
     * Remove the overriding writer, if any
     *
     * @return overriding writer, or null
     */
    public StreamingLogWriter removeOverride() {
        StreamingLogWriter previous = override.get();
        override.remove();
        return previous;
    }
}
