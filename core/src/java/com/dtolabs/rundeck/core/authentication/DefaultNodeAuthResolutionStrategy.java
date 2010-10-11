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
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.CoreException;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import org.apache.log4j.Category;

/**
 * Default implementation for a {@link INodeAuthResolutionStrategy} interface.
 * This supports a key-based authentication strategy.
 */
public class DefaultNodeAuthResolutionStrategy implements INodeAuthResolutionStrategy {
    public static final Category logger = Category.getInstance(DefaultNodeAuthResolutionStrategy.class);

    final private IPropertyLookup frameworkPropertyLookup;

    /**
     * Default constructor
     * @param framework The {@link Framework} singleton
     */
    public DefaultNodeAuthResolutionStrategy(Framework framework) {
        frameworkPropertyLookup = framework.getPropertyLookup();
    }

    /**
     * Factory method
     * @param framework The {@link Framework} singleton
     * @return Returns a new instance
     */
    public static INodeAuthResolutionStrategy create(Framework framework) {
        return new DefaultNodeAuthResolutionStrategy(framework);
    }


    /**
     * Predicate returning if this strategy is key-based
     * @param nodeEntry  a {@link INodeEntry}
     * @return returns true
     */
    public boolean isKeyBasedAuthentication(INodeEntry nodeEntry) {
        return true;
    }

    /**
     * Predicate returning if this strategy is password-based
     * @param nodeEntry  a {@link INodeEntry}
     * @return Always returns false (for now)
     */
    public boolean isPasswordBasedAuthentication(INodeEntry nodeEntry) {
        return false;
    }

    public String fetchUsername(INodeEntry nodeEntry) {
        if (null != nodeEntry.extractUserName()) {
            return nodeEntry.extractUserName();
        }
        /**
         * NodeEntry did not contain any info about the username.
         * Default to the framework.ssh.user value
         */
        String sshUser = frameworkPropertyLookup.getProperty(Constants.SSH_USER_PROP);
        if (null == sshUser || "".equals(sshUser.trim())) {
            throw new CoreException("Could not set a default for the remote connection username. Reason: " +
                    Constants.SSH_USER_PROP +
                    " property not defined or is blank in framework.properties");
        }
        logger.debug("DefaultNodeAuthResolutionStrategy fetching username from the " +
                "framework.properties setting: "+Constants.SSH_USER_PROP);
        return sshUser;
    }

    /**
     * Fetches the password value for this Node.
     *
     * @param nodeEntry The {@link com.dtolabs.rundeck.core.common.INodeEntry} representing the remote node
     * @return This implmentation returns null.
     */
    public String fetchPassword(INodeEntry nodeEntry) {
        logger.warn("DefaultNodeAuthResolutionStrategy unexpectedly asked to " +
                "fetch password for node " + nodeEntry.getNodename()+". Returning null for password.");        
        return null;
    }
}
