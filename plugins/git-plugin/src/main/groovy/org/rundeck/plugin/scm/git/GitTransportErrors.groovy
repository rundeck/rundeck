/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.plugin.scm.git

import groovy.transform.CompileStatic
import org.eclipse.jgit.errors.TransportException

import javax.net.ssl.SSLException

/**
 * Chooses the Git transport text that is safe to return to the UI and API.
 * Connection details stay in the server log.
 */
@CompileStatic
class GitTransportErrors {

    static final String GENERIC_ACCESS_MESSAGE =
            'Could not access the Git repository. See the server log for details.'

    /**
     * @param error failure from a Git transport call
     * @param branchName remote branch name, used to keep "branch not found" messages
     * @return a replacement message, or {@code null} when the original message is safe to show
     */
    static String userFacing(Throwable error, String branchName) {
        String policy = policyMessage(error)
        if (policy != null) {
            return policy
        }
        if (branchName != null && BaseGitPlugin.isMissingRemoteBranch(error, branchName)) {
            return null
        }
        if (isSensitiveTransportFailure(error)) {
            return GENERIC_ACCESS_MESSAGE
        }
        return null
    }

    /**
     * @param error failure from a Git transport call
     * @return {@code true} when the message can disclose a remote response or a port-scan result
     */
    static boolean isSensitiveTransportFailure(Throwable error) {
        Throwable current = error
        while (current != null) {
            if (current instanceof TransportException
                    || current instanceof org.eclipse.jgit.api.errors.TransportException
                    || current instanceof ConnectException
                    || current instanceof UnknownHostException
                    || current instanceof SocketException
                    || current instanceof SocketTimeoutException
                    || current instanceof SSLException) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private static String policyMessage(Throwable error) {
        Throwable current = error
        while (current != null) {
            String message = current.message
            if (message == InternalAddressGuard.HOST_NOT_ALLOWED || isSchemeDenial(message)) {
                return message
            }
            current = current.cause
        }
        return null
    }

    private static boolean isSchemeDenial(String message) {
        return message != null && message.startsWith("Git URL scheme '") && message.endsWith("' is not allowed.")
    }
}
