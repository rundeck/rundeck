/*
 * Copyright 2013 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.jetty.jaas;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * A wrapper around an existing X509TrustManager that verifies the server
 * certificate against the remote host.
 * 
 * @author Kim Ho <kim.ho@salesforce.com>
 */
public class HostnameVerifyingTrustManager implements X509TrustManager {

    protected X509TrustManager realTrustManager;

    public HostnameVerifyingTrustManager(TrustManager trustManager) {
        if (!(trustManager instanceof X509TrustManager)) {
            throw new IllegalArgumentException(
                                               String.format("Expected trustManager to be of type X509TrustManager but was [%s]",
                                                             trustManager.getClass()));
        }
        this.realTrustManager = (X509TrustManager) trustManager;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        realTrustManager.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain.length > 0) {
            X509Certificate serverCert = chain[0];
            try {
                String host = HostnameVerifyingSSLSocketFactory.getTargetHost();
                String dn = serverCert.getSubjectDN().getName();
                String cn = extractCnFromDn(dn);
                if (!cn.equalsIgnoreCase(host)) {
                    throw new CertificateException(String.format("[%s] does not match certificate subject [%s]", host,
                                                                 cn));
                }
            }
            catch (InvalidNameException e) {
                throw new CertificateException("Error processing certificate for subject.", e);
            }
        }
        realTrustManager.checkServerTrusted(chain, authType);
    }

    protected String extractCnFromDn(String dn) throws InvalidNameException {
        LdapName name = new LdapName(dn);
        for (Rdn rdn : name.getRdns()) {
            if (rdn.getType().equalsIgnoreCase("CN")) {
                return (String) rdn.getValue();
            }
        }
        throw new InvalidNameException("Could not find CN");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return realTrustManager.getAcceptedIssuers();
    }
}
