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

package com.dtolabs.rundeck.core.plugins;

import lombok.*;

import java.util.List;

/**
 * Defines a validation state for a plugin
 */
@Builder
@ToString()
@Data
public class PluginValidation {
    /**
     * The validation state
     */
    private State state;
    /**
     * Validation messages if it is not a valid state
     */
    @Singular
    private final List<String> messages;

    /**
     * Validity states
     */
    public static enum State {
        /**
         * Valid state
         */
        VALID(true),
        /**
         * Incompatible state
         */
        INCOMPATIBLE(false),
        /**
         * Invalid state
         */
        INVALID(false);
        /**
         * True if the state is valid
         */
        @Getter
        private boolean valid;

        State(boolean validity) {
            this.valid = validity;
        }

        /**
         * Returns the state with greater precedence of this state or the other
         *
         * @param other other state
         * @return state with precedence
         */
        State or(State other) {
            return this.ordinal() > other.ordinal() ? this : other;
        }
    }
}
