/*
 * Copyright 2026 PagerDuty, Inc. (http://pagerduty.com)
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

package org.rundeck.app.http

import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Rejects notification/webhook destination hosts that resolve to loopback, link-local,
 * private (RFC1918), or unique-local (IPv6) addresses, so that a project-level job author
 * cannot use a job notification (e.g. the built-in "Send Webhook" plugin) to make the
 * Rundeck server itself issue requests against internal-only network destinations such as
 * cloud metadata services or other internal-only services.
 *
 * Mirrors {@code org.rundeck.plugin.scm.git.InternalAddressGuard}, which already applies
 * this same restriction to the SCM git plugin's own outbound HTTP(S) connections. The
 * exception message intentionally does not include the resolved address; that detail is
 * written to the log only.
 */
@CompileStatic
class WebhookDestinationGuard {

    static final String HOST_NOT_ALLOWED = 'The notification destination host is not allowed.'

    private static final Logger LOG = LoggerFactory.getLogger(WebhookDestinationGuard)

    /**
     * Resolves {@code host} and rejects it when any resolved address is internal.
     *
     * @param host DNS name or literal address
     * @throws IOException when a resolved address is blocked, or the host cannot be resolved
     */
    static void assertPublicHost(String host) throws IOException {
        if (host == null || host.isEmpty()) {
            throw new IOException(HOST_NOT_ALLOWED)
        }
        InetAddress[] addresses
        try {
            addresses = InetAddress.getAllByName(host)
        } catch (UnknownHostException e) {
            throw new IOException("Could not resolve notification destination host.", e)
        }
        for (InetAddress address : addresses) {
            if (isBlocked(address)) {
                LOG.warn("Blocked notification webhook destination {} resolved to {}", host, address.hostAddress)
                throw new IOException(HOST_NOT_ALLOWED)
            }
        }
    }

    /**
     * @param address resolved address, including IPv4-mapped IPv6
     * @return {@code true} when the address must not be used as a notification destination
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
