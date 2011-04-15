/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
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

/*
* NodeExecutorResultImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/31/11 6:54 PM
* 
*/
package com.dtolabs.rundeck.core.execution.service;

/**
 * NodeExecutorResultImpl simple implementation of {@link NodeExecutorResult}
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodeExecutorResultImpl implements NodeExecutorResult {
    private int resultCode;
    private boolean success;

    public NodeExecutorResultImpl(final int resultCode, final boolean success) {
        this.resultCode = resultCode;
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getResultCode() {
        return resultCode;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final NodeExecutorResultImpl that = (NodeExecutorResultImpl) o;

        if (resultCode != that.resultCode) {
            return false;
        }
        if (success != that.success) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = resultCode;
        result = 31 * result + (success ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "NodeExecutorResultImpl{" +
               "resultCode=" + resultCode +
               ", success=" + success +
               '}';
    }
}
