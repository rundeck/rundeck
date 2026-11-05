/*
 * Copyright 2026 PagerDuty, Inc.
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

package com.dtolabs.rundeck.core.common;

/**
 * Coordinates deferred project deletion for one persistent project row.
 */
public interface ProjectDeleteCoordinator {

    /**
     * Try to acquire cleanup ownership.
     *
     * @param projectName project name
     * @param projectId persistent project row identifier
     * @return an opaque fencing token, or {@code null} when another cleanup owns the row
     */
    String tryAcquire(String projectName, long projectId);

    /**
     * Test whether a claim still owns cleanup.
     *
     * @param projectName project name
     * @param projectId persistent project row identifier
     * @param claimToken opaque token returned by {@link #tryAcquire(String, long)}
     * @return true when the claim is current
     */
    boolean isOwner(String projectName, long projectId, String claimToken);

    /**
     * Release cleanup ownership if the supplied token is still current.
     *
     * @param projectName project name
     * @param projectId persistent project row identifier
     * @param claimToken opaque token returned by {@link #tryAcquire(String, long)}
     */
    void release(String projectName, long projectId, String claimToken);
}
