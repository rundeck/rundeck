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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * Socket factory that generates wrapped X509TrustManagers that perform hostname cn verification.
 * 
 * @author Kim Ho <a href="mailto:kim.ho@salesforce.com">kim.ho@salesforce.com</a>
 */ 
public class HostnameVerifyingSSLSocketFactory extends SSLSocketFactory {

	public static final String SSL_ALGORITHM = "TLS";
	
	protected static String host;
	
	protected SSLSocketFactory realSocketFactory;

	public HostnameVerifyingSSLSocketFactory() {
		try {
			TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			factory.init((KeyStore) null);

			TrustManager[] trustManagers = (TrustManager[]) factory.getTrustManagers();
			for (int i = 0; i < trustManagers.length; i++) {
				trustManagers[i] = wrapTrustManager(trustManagers[i]);
			}

			SSLContext sc = SSLContext.getInstance(SSL_ALGORITHM);
			sc.init(null, trustManagers, null);
			realSocketFactory = sc.getSocketFactory();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	protected TrustManager wrapTrustManager(TrustManager trustManager) {
		return new HostnameVerifyingTrustManager(trustManager);
	}

	public static SocketFactory getDefault() {
		return new HostnameVerifyingSSLSocketFactory();
	}

	@Override
	public Socket createSocket() throws IOException {
		return realSocketFactory.createSocket();
	}

	@Override
	public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
		return realSocketFactory.createSocket(host, port);
	}

	@Override
	public Socket createSocket(InetAddress host, int port) throws IOException {
		return realSocketFactory.createSocket(host, port);
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException,
			UnknownHostException {
		return realSocketFactory.createSocket(host, port, localHost, localPort);
	}

	@Override
	public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
			throws IOException {
		return realSocketFactory.createSocket(address, port, localAddress, localPort);
	}

	@Override
	public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
		return realSocketFactory.createSocket(s, host, port, autoClose);
	}

	@Override
	public String[] getDefaultCipherSuites() {
		return realSocketFactory.getDefaultCipherSuites();
	}

	@Override
	public String[] getSupportedCipherSuites() {
		return realSocketFactory.getSupportedCipherSuites();
	}

	public static String getTargetHost() {
		return host;
	}

    public static void setTargetHost(String host) {
        HostnameVerifyingSSLSocketFactory.host = host;
    }
}
