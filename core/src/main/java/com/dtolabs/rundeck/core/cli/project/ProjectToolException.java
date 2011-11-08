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

package com.dtolabs.rundeck.core.cli.project;

/**
 * thrown by rd-project actions
 */
public class ProjectToolException extends RuntimeException {
    public ProjectToolException(String message) {
        super(message);
    }

    public ProjectToolException(String message, Throwable t) {
        super(message, t);
    }

    public ProjectToolException(Throwable t) {
        super(t);
    }

}
