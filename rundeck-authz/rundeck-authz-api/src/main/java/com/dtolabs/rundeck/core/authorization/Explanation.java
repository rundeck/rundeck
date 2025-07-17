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

package com.dtolabs.rundeck.core.authorization;

/**
 * Explanation provides a way to encapsulate the result of an authorization decision with a Code defining the
 * decision and a description string.
 */
public interface Explanation {

    /**
     * Enumeration of possible codes for authorization explanations. Ordering matters when resolving multiple
     * authorization decisions, lower ordinal values take precedence.
     */
    enum Code {
        GRANTED_OVERRIDE,
        REJECTED_DENIED,
        GRANTED,
        REJECTED
    };
    
    Code getCode();

    /**
     * Create an explanation with the given code and explanation string.
     *
     * @param code        the code for the explanation
     * @param description the explanation string
     * @return an Explanation instance
     */
    static Explanation with(Code code, String description) {
        return new Explanation() {
            @Override
            public Code getCode() {
                return code;
            }

            @Override
            public String toString() {
                return description + " => " + code;
            }
        };
    }
}
