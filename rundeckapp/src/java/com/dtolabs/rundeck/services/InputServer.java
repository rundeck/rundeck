/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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

/*
 * CorrelationServer.java
 * 
 * User: greg
 * Created: Jun 23, 2005 3:08:56 PM
 * $Id: CorrelationServer.java 5688 2006-01-14 20:00:28Z connary_scott $
 */
package com.dtolabs.rundeck.services;


import com.dtolabs.shared.reports.Constants;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.net.SocketNode;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;


/**
 * CorrelationServer is a small daemon thread which listens on a specific port, and uses {@link SocketNode} to log
 * incoming Log4j events.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 5688 $
 */
public class InputServer {
    public final Logger log = Logger.getLogger(InputServer.class);
    private static Logger commonLogger = LogManager.getLoggerRepository().getLogger("com.dtolabs.rundeck.log.common");
    private volatile boolean finished = false;
    private Thread serverThread;

    private int port;

    /**
     * Create a new InputServer configured to listen at the port.
     *
     * @param port
     */
    public InputServer(int port) {
        this.port = port;
    }

    private class worker implements Runnable {
        private ServerSocket serverSocket;

        private worker(ServerSocket serverSocket) throws IOException {
            this.serverSocket = serverSocket;
        }

        public void run() {

            while (!finished) {
                try {
                    if (Thread.interrupted()) {
                        continue;//check interrupt before waiting for socket accept
                    }
                    Socket socket = serverSocket.accept();
                    MDC.put(Constants.MDC_REMOTE_HOST_KEY, socket.getInetAddress().getHostName());
                    new Thread(new SocketNode(socket,
                                              LogManager.getLoggerRepository())).start();
                    MDC.remove(Constants.MDC_REMOTE_HOST_KEY);

                } catch (SocketTimeoutException e) {
                    //accept() method timed out.
                } catch (Exception e) {
                    log.info("Exception during correlation server thread: " + e.getMessage(), e);
                }
            }
            try {
                serverSocket.close();
            } catch (IOException e) {

            }
        }
    }

    /**
     * Begin the server on the given port if it has not already been started.
     *
     * @param port
     *
     * @throws IOException
     */
    public synchronized void begin() throws IOException {
        if (null == serverThread) {
            finished = false;
            log.info("Listening on port " + port);
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(5000);
            serverThread = new Thread(new worker(serverSocket));
            serverThread.start();
        }
    }

    /**
     * interrupt the server and finish.
     */
    public synchronized void finish() {
        finished = true;
        if (null != serverThread) {
            serverThread.interrupt();
        }
        serverThread = null;
    }
}
