package com.dtolabs.rundeck.core.tasks.net;

import com.jcraft.jsch.SocketFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class BindAddressSocketFactory implements SocketFactory {
    private String bindAddress;
    private Long timeout;

    public BindAddressSocketFactory(String bindAddress, Long timeout) {
        this.bindAddress = bindAddress;
        this.timeout = timeout;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket socket = new Socket();
        socket.bind(new InetSocketAddress(bindAddress, 0));
        socket.connect(new InetSocketAddress(host, port), timeout.intValue());
        return socket;
    }

    @Override
    public InputStream getInputStream(Socket socket) throws IOException {
        return socket.getInputStream();
    }

    @Override
    public OutputStream getOutputStream(Socket socket) throws IOException {
        return socket.getOutputStream();
    }
}
