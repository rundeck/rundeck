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

package com.dtolabs.rundeck.plugins.scm;

/**
 * Result of Export action
 */
public interface ScmExportResult {
    /**
     * @return true if export was successful, false otherwise
     */
    public boolean isSuccess();

    /**
     * @return true if an error occurred, false otherwise
     */
    public boolean isError();

    /**
     * @return basic result message
     */
    public String getMessage();

    /**
     * @return extended result message
     */
    public String getExtendedMessage();

    /**
     * @return Id associated with export, if any
     */
    public String getId();
    /**
     * @return info for the exported commit, to synch with import status
     */
    ScmCommitInfo getCommit();
}
