/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.dtolabs.rundeck.core.utils;


/**
 * Exception throwable from an IPropertyLookup
 */
public class PropertyLookupException extends RuntimeException {

    /**
     * Constructor
     *
     * @param msg       Exception message
     * @param exception Exception cause.
     */
    public PropertyLookupException(final String msg, final Throwable exception) {
        super(msg, exception);
    }

    /**
     * Constructor
     *
     * @param msg Exception message.
     */
    public PropertyLookupException(final String msg) {
        super(msg);
    }
}
