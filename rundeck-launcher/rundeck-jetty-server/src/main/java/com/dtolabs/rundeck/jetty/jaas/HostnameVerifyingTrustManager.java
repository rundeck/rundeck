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

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.ssl.HostnameVerifier;

/**
 * A wrapper around an existing X509TrustManager that verifies the server
 * certificate against the remote host.
 * 
 * @author Kim Ho <a href="mailto:kim.ho@salesforce.com">kim.ho@salesforce.com</a>
 */
public class HostnameVerifyingTrustManager implements X509TrustManager {

    protected X509TrustManager realTrustManager;
    protected HostnameVerifier verifier; 

    public HostnameVerifyingTrustManager(TrustManager trustManager) {
        if (!(trustManager instanceof X509TrustManager)) {
            throw new IllegalArgumentException(
                                               String.format("Expected trustManager to be of type X509TrustManager but was [%s]",
                                                             trustManager.getClass()));
        }
        this.verifier = HostnameVerifier.STRICT;
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
            String host = HostnameVerifyingSSLSocketFactory.getTargetHost();
            try {
                verifier.check(host, serverCert);
            }
            catch (SSLException e) {
                throw new CertificateException(e);
            }
        }
        realTrustManager.checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return realTrustManager.getAcceptedIssuers();
    }
}
