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

import java.net.InetAddress;
import java.net.Socket;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class HostnameVerifyingSSLSocketFactoryTest {

	protected HostnameVerifyingSSLSocketFactory factory;
	protected SSLSocketFactory realSocketFactory;

	@Before
	public void setup() {
		factory = new HostnameVerifyingSSLSocketFactory();
		realSocketFactory = Mockito.mock(SSLSocketFactory.class);
		factory.realSocketFactory = realSocketFactory;
	}

	@Test
	public void testWrapTrustManager() {
		TrustManager trustManager = Mockito.mock(X509TrustManager.class);
		HostnameVerifyingTrustManager result = (HostnameVerifyingTrustManager) factory.wrapTrustManager(trustManager);
		Assert.assertEquals("Expected real trust manager on returned trust manager to be the mock", trustManager,
				result.realTrustManager);
	}

	@Test
	public void testGetDefaultReturnsInstanceOfHostnameVerifyingSSLSocketFactory() {
		SocketFactory factory = HostnameVerifyingSSLSocketFactory.getDefault();
		Assert.assertTrue("Expected getDefault() to return an instance of HostnameVerifyingSSLSocketFactory",
				factory instanceof HostnameVerifyingSSLSocketFactory);
	}

	@Test
	public void testCreateSocket_String_int() throws Exception {
		Socket socket = Mockito.mock(Socket.class);
		String host = "host";
		int port = 123;

		Mockito.when(realSocketFactory.createSocket(Mockito.anyString(), Mockito.anyInt())).thenReturn(socket);
		Socket result = factory.createSocket(host, port);

		Assert.assertSame("Expected realSocketFactory to generate socket", socket, result);
		Mockito.verify(realSocketFactory, Mockito.times(1)).createSocket(Mockito.eq(host), Mockito.eq(port));
	}

	@Test
	public void testCreateSocket_InetAddress_int() throws Exception {
		Socket socket = Mockito.mock(Socket.class);
		int port = 123;
		InetAddress address = Mockito.mock(InetAddress.class);

		Mockito.when(realSocketFactory.createSocket(Mockito.any(InetAddress.class), Mockito.anyInt())).thenReturn(
				socket);
		Socket result = factory.createSocket(address, port);

		Assert.assertSame("Expected realSocketFactory to generate socket", socket, result);
		Mockito.verify(realSocketFactory, Mockito.times(1)).createSocket(Mockito.eq(address), Mockito.eq(port));
	}

	@Test
	public void testCreateSocket_String_int_InetAddress_int() throws Exception {
		Socket socket = Mockito.mock(Socket.class);
		String host = "host";
		int port = 123;
		InetAddress address = Mockito.mock(InetAddress.class);

		Mockito.when(
				realSocketFactory.createSocket(Mockito.anyString(), Mockito.anyInt(), Mockito.any(InetAddress.class),
						Mockito.anyInt())).thenReturn(socket);
		Socket result = factory.createSocket(host, port, address, port);

		Assert.assertSame("Expected realSocketFactory to generate socket", socket, result);
		Mockito.verify(realSocketFactory, Mockito.times(1)).createSocket(Mockito.eq(host), Mockito.eq(port),
				Mockito.eq(address), Mockito.eq(port));
	}

	@Test
	public void testCreateSocket_InetAddress_int_InetAddress_int() throws Exception {
		Socket socket = Mockito.mock(Socket.class);
		int port = 123;
		InetAddress host1 = Mockito.mock(InetAddress.class);
		InetAddress host2 = Mockito.mock(InetAddress.class);

		Mockito.when(
				realSocketFactory.createSocket(Mockito.any(InetAddress.class), Mockito.anyInt(),
						Mockito.any(InetAddress.class), Mockito.anyInt())).thenReturn(socket);
		Socket result = factory.createSocket(host1, port, host2, port);

		Assert.assertSame("Expected realSocketFactory to generate socket", socket, result);
		Mockito.verify(realSocketFactory, Mockito.times(1)).createSocket(Mockito.eq(host1), Mockito.eq(port),
				Mockito.eq(host2), Mockito.eq(port));
	}

	@Test
	public void testCreateSocket_Socket_String_int_bool() throws Exception {
		Socket socket = Mockito.mock(Socket.class);
		Socket originalSocket = Mockito.mock(Socket.class);
		String host = "host";
		int port = 123;
		boolean autoClose = true;

		Mockito.when(
				realSocketFactory.createSocket(Mockito.any(Socket.class), Mockito.anyString(), Mockito.anyInt(),
						Mockito.anyBoolean())).thenReturn(socket);
		Socket result = factory.createSocket(originalSocket, host, port, autoClose);

		Assert.assertSame("Expected realSocketFactory to generate socket", socket, result);
		Mockito.verify(realSocketFactory, Mockito.times(1)).createSocket(Mockito.eq(originalSocket), Mockito.eq(host),
				Mockito.eq(port), Mockito.eq(autoClose));
	}

	@Test
	public void testGetDefaultCipherSuites() {
		String[] ciphers = { "cipher1", "cipher2" };
		Mockito.when(realSocketFactory.getDefaultCipherSuites()).thenReturn(ciphers);

		Assert.assertSame("Expected default ciphers to be returned from realSocketFactory", ciphers,
				factory.getDefaultCipherSuites());
	}

	@Test
	public void testGetSupportedCipherSuites() {
		String[] ciphers = { "cipher1", "cipher2" };
		Mockito.when(realSocketFactory.getSupportedCipherSuites()).thenReturn(ciphers);

		Assert.assertSame("Expected supported ciphers to be returned from realSocketFactory", ciphers,
				factory.getSupportedCipherSuites());
	}

	@Test
	public void testTargetHost() {
		String host = "host";
		HostnameVerifyingSSLSocketFactory.setTargetHost(host);
		Assert.assertEquals("Expected same host value to be returned", host, HostnameVerifyingSSLSocketFactory.getTargetHost());
	}
}
