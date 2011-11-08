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

package com.dtolabs.rundeck.core.common;

/**
 * This RunTimeException subclass is typically thrown when a resource is unavailable in the framework
 * or the framework is misconfigured due to strange user deployment.
 * Any class that throws this should provide an existsXXX() method allowing the programmer
 * to check to see if the resource is available or ready for use.
 */
public class FrameworkResourceException extends RuntimeException {
    private String message;
    private IFrameworkResource resource;

    FrameworkResourceException(final String message, final IFrameworkResource resource) {
        super(message);
        this.message = message;
        this.resource = resource;
    }

    FrameworkResourceException(final String message, final FrameworkResource resource, final Throwable cause) {
        super(message, cause);
        this.message = message;
        this.resource = resource;
    }

    public IFrameworkResource getResource() {
        return resource;
    }


    public String toString() {
        return "FrameworkException{" +
                "message='" + message + "'" +
                ", resource='" + resource + "'" +
                "}";
    }
}
