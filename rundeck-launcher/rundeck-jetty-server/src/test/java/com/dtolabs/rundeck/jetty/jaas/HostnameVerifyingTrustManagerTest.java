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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class HostnameVerifyingTrustManagerTest {

    protected HostnameVerifyingTrustManager trustManager;
    protected X509TrustManager realTrustManager;
    protected HostnameVerifier verifier;

    @Before
    public void setup() {
        realTrustManager = Mockito.mock(X509TrustManager.class);
        verifier = Mockito.mock(HostnameVerifier.class);
        trustManager = new HostnameVerifyingTrustManager(realTrustManager);
        trustManager.verifier = verifier;
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInitializeWithNonX509TrustManager() {
        trustManager = new HostnameVerifyingTrustManager(Mockito.mock(TrustManager.class));
    }

    @Test
    public void testCheckClientTrusted() throws Exception {
        X509Certificate[] chain = { null };
        String authType = "type";
        trustManager.checkClientTrusted(chain, authType);

        Mockito.verify(realTrustManager, Mockito.times(1)).checkClientTrusted(Mockito.same(chain),
                                                                              Mockito.same(authType));
    }

    @Test
    public void testCheckServerTrusted() throws Exception {
        X509Certificate certificate = Mockito.mock(X509Certificate.class);
        X509Certificate[] chain = { certificate };
        String authType = "type";
        String host = "host";
        Mockito.doNothing().when(verifier).check(Mockito.eq(host), Mockito.same(certificate));

        HostnameVerifyingSSLSocketFactory.setTargetHost(host);

        trustManager.checkServerTrusted(chain, authType);

        Mockito.verify(realTrustManager, Mockito.times(1)).checkServerTrusted(Mockito.same(chain),
                                                                              Mockito.same(authType));
    }

    @Test
    public void testCheckServerTrustedEmptyChain() throws Exception {
        X509Certificate[] chain = {};
        String authType = "type";

        trustManager.checkServerTrusted(chain, authType);

        Mockito.verify(realTrustManager, Mockito.times(1)).checkServerTrusted(Mockito.same(chain),
                                                                              Mockito.same(authType));
    }
    
    @Test
    public void testCheckServerTrustedFailsVerification() throws Exception {
        X509Certificate certificate = Mockito.mock(X509Certificate.class);
        X509Certificate[] chain = { certificate };
        String authType = "type";
        String host = "host";
        SSLException root = new SSLException("Invalid");
        Mockito.doThrow(root).when(verifier).check(Mockito.eq(host), Mockito.same(certificate));

        HostnameVerifyingSSLSocketFactory.setTargetHost(host);

        try {
            trustManager.checkServerTrusted(chain, authType);
            Assert.fail("Expected hostname verification to fail.");
        }
        catch (CertificateException e) {
            Assert.assertSame("Expected validation exception to be thrown as root cause.", root, e.getCause());
        }

        Mockito.verifyZeroInteractions(realTrustManager);
    }

    @Test
    public void testGetAcceptedIssuers() {
        X509Certificate certificate = Mockito.mock(X509Certificate.class);
        X509Certificate[] chain = { certificate };
        Mockito.when(realTrustManager.getAcceptedIssuers()).thenReturn(chain);

        Assert.assertSame("Expected accepted issuers call to be delegated to actual trust manager", chain,
                          trustManager.getAcceptedIssuers());
    }
}
