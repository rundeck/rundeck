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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Rejects Git hosts that resolve to loopback, link-local, private, or unique-local addresses.
 * The exception message does not include the resolved address; that detail is written to the log.
 */
@CompileStatic
class InternalAddressGuard {

    static final String HOST_NOT_ALLOWED = 'The Git host is not allowed.'

    private static final Logger LOG = LoggerFactory.getLogger(InternalAddressGuard)

    /**
     * Resolves {@code host} and rejects it when any address is internal.
     *
     * @param host DNS name or literal address
     * @throws IOException when a resolved address is blocked
     * @throws UnknownHostException when the name cannot be resolved
     */
    static void assertPublicHost(String host) throws IOException {
        if (host == null || host.isEmpty()) {
            throw new IOException(HOST_NOT_ALLOWED)
        }
        InetAddress[] addresses = InetAddress.getAllByName(host)
        for (InetAddress address : addresses) {
            if (isBlocked(address)) {
                LOG.warn("Blocked Git host {} resolved to {}", host, address.hostAddress)
                throw new IOException(HOST_NOT_ALLOWED)
            }
        }
    }

    /**
     * @param address resolved address, including IPv4-mapped IPv6
     * @return {@code true} when the address must not be used as a Git remote
     */
    static boolean isBlocked(InetAddress address) {
        InetAddress unwrapped = unwrapMappedIpv4(address)
        if (unwrapped.isAnyLocalAddress()
                || unwrapped.isLoopbackAddress()
                || unwrapped.isLinkLocalAddress()
                || unwrapped.isSiteLocalAddress()) {
            return true
        }
        return isUniqueLocalIpv6(unwrapped)
    }

    private static boolean isUniqueLocalIpv6(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return false
        }
        byte[] bytes = address.getAddress()
        int first = bytes[0] & 0xff
        return (first & 0xfe) == 0xfc
    }

    private static InetAddress unwrapMappedIpv4(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return address
        }
        byte[] bytes = address.getAddress()
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != (byte) 0) {
                return address
            }
        }
        if (bytes[10] != (byte) 0xff || bytes[11] != (byte) 0xff) {
            return address
        }
        byte[] ipv4 = new byte[4]
        System.arraycopy(bytes, 12, ipv4, 0, 4)
        try {
            return InetAddress.getByAddress(ipv4)
        } catch (UnknownHostException ignored) {
            return address
        }
    }
}
