/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
* DeleteJobResultImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 9/26/12 3:56 PM
* 
*/
package com.dtolabs.client.services;

import com.dtolabs.rundeck.core.dispatcher.DeleteJobResult;

import java.util.*;


/**
 * DeleteJobResultImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class DeleteJobResultImpl implements DeleteJobResult {
    private boolean successful;
    private String message;
    private String id;
    private String errorCode;

    private DeleteJobResultImpl(boolean successful, String message, String id, String errorCode) {
        this.successful = successful;
        this.message = message;
        this.id = id;
        this.errorCode = errorCode;
    }

    public static DeleteJobResultImpl createDeleteJobResultImpl(boolean successful,
                                                                String message,
                                                                String id,
                                                                String errorCode) {
        return new DeleteJobResultImpl(successful, message, id, errorCode);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
