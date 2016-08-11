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

import java.util.Date;
import java.util.Map;

/**
 * SCM commit information
 */
public interface ScmCommitInfo {
    /**
     * @return Map representation of the data, if available
     */
    Map asMap();

    /**
     * @return Commit ID as a string
     */
    String getCommitId();

    /**
     * @return Commit message
     */
    String getMessage();

    /**
     * @return author
     */
    String getAuthor();

    /**
     * @return commit date
     */
    Date getDate();
}
