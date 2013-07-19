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

import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.naming.InvalidNameException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class HostnameVerifyingTrustManagerTest {

    protected HostnameVerifyingTrustManager trustManager;
    protected X509TrustManager realTrustManager;

    @Before
    public void setup() {
        realTrustManager = Mockito.mock(X509TrustManager.class);
        trustManager = new HostnameVerifyingTrustManager(realTrustManager);
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
        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(certificate.getSubjectDN()).thenReturn(principal);
        X509Certificate[] chain = { certificate };
        String authType = "type";
        String host = "host";
        Mockito.when(principal.getName()).thenReturn(String.format("CN=%s", host));

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
    public void testCheckServerTrustedFailsCNExtraction() throws Exception {
        X509Certificate certificate = Mockito.mock(X509Certificate.class);
        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(certificate.getSubjectDN()).thenReturn(principal);
        X509Certificate[] chain = { certificate };
        String authType = "type";
        String host = "host";
        Mockito.when(principal.getName()).thenReturn("invalid");

        HostnameVerifyingSSLSocketFactory.setTargetHost(host);

        try {
            trustManager.checkServerTrusted(chain, authType);
            Assert.fail("Expected hostname verification to fail.");
        }
        catch (CertificateException e) {
            Assert.assertTrue("Expected cause to be instanceof InvalidNameException", e.getCause() instanceof InvalidNameException);
        }

        Mockito.verifyZeroInteractions(realTrustManager);
    }
    
    @Test
    public void testCheckServerTrustedFailsCNVerification() throws Exception {
        X509Certificate certificate = Mockito.mock(X509Certificate.class);
        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(certificate.getSubjectDN()).thenReturn(principal);
        X509Certificate[] chain = { certificate };
        String authType = "type";
        String host = "host";
        Mockito.when(principal.getName()).thenReturn("CN=otherhost");

        HostnameVerifyingSSLSocketFactory.setTargetHost(host);

        try {
            trustManager.checkServerTrusted(chain, authType);
            Assert.fail("Expected hostname verification to fail.");
        }
        catch (CertificateException e) {
            Assert.assertNull("Expected no underlying cause", e.getCause());
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
