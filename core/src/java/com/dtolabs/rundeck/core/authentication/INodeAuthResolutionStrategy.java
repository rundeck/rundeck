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

package com.dtolabs.rundeck.core.authentication;

import com.dtolabs.rundeck.core.common.INodeEntry;

/**
 * Abstracts the fetching of username, passwords (and in the future keys) for
 * remote client connections.
 */
public interface INodeAuthResolutionStrategy {
  /**
     * Predicate returning if this strategy is key-based
     * @param nodeEntry  a {@link INodeEntry}
     * @return returns true if it uses keys
     */
    boolean isKeyBasedAuthentication(INodeEntry nodeEntry);
  /**
     * Predicate returning if this strategy is password-based
     * @param nodeEntry  a {@link INodeEntry}
     * @return returns True if it uses password
     */
    boolean isPasswordBasedAuthentication(INodeEntry nodeEntry);

    /**
     * Fetches the password value for this Node.
     * @param nodeEntry The {@link INodeEntry} representing the remote node
     * @return A clear text password
     */
    String fetchPassword(INodeEntry nodeEntry);

    /**
     * Fetches the username value for this Node
     * @param nodeEntry The {@link INodeEntry} representing the remote node
     * @return The username
     */
    String fetchUsername(INodeEntry nodeEntry);
}
