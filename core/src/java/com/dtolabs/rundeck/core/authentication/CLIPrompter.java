/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * CLIPrompter.java
 * 
 * User: greg
 * Created: Feb 10, 2005 12:39:17 PM
 * $Id: CLIPrompter.java 1079 2008-02-05 04:53:32Z ahonor $
 */
package com.dtolabs.rundeck.core.authentication;


import java.io.*;
import java.util.Arrays;


/**
 * CLIPrompter prompts for username and password via an input and output stream (typically STDIN and STDOUT).
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 1079 $
 */
public class CLIPrompter implements UserInfoPrompter {
    InputStream in;
    PrintStream out;

    public CLIPrompter(InputStream in, OutputStream out) {
        this.in = in;
        this.out = new PrintStream(out);
    }


    public IUserInfo prompt(String defaultUsername) throws IOException {
        //prompt for username and password

        final String username = readUsername(in, defaultUsername);
        final char[] read = readPasswd(in, username);
        final String password;
        if (null == read) {
            throw new IllegalStateException("read password was null");
        } else {
            password = new String(read);
        }
        return new IUserInfo() {
            public String getPassword() {
                return password;
            }

            public String getUsername() {
                return username;
            }
        };
    }

    public String readUsername(InputStream in, String defaultUserName) throws IOException {
        out.print("Username" + (defaultUserName != null ? " [" + defaultUserName + "]" : "") + ": ");
        String user = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        while (user == null) {
            String l = br.readLine();
            if ("".equals(l) && defaultUserName != null) {
                user = defaultUserName;
            } else if (!"".equals(l)) {
                user = l;
            }
        }

        return user;
    }

    /**
     * Reads user password from given input stream.
     * <a href="http://java.sun.com/j2se/1.4.2/docs/guide/security/jce/JCERefGuide.html#PBEEx">reference</a>
     */
    public char[] readPasswd(InputStream in, String username) throws IOException {
        if (in == null) {
            throw new IllegalArgumentException("got a null InputStream");
        }
        out.print(username + "'s Password: ");
        char[] lineBuffer;
        char[] buf;
        int i;

        buf = lineBuffer = new char[128];

        int room = buf.length;
        int offset = 0;
        int c;

        boolean done = false;
        while (!done) {
            switch (c = in.read()) {
                case -1:
                case '\n':
                    done = true;
                    break;

                case '\r':
                    int c2 = in.read();
                    if (c2 != '\n' && c2 != -1) {
                        if (!(in instanceof PushbackInputStream)) {
                            in = new PushbackInputStream(in);
                        }
                        ((PushbackInputStream) in).unread(c2);
                    } else {
                        done = true;
                        break;
                    }

                default:
                    if (--room < 0) {
                        buf = new char[offset + 128];
                        room = buf.length - offset - 1;
                        System.arraycopy(lineBuffer, 0, buf, 0, offset);
                        Arrays.fill(lineBuffer, ' ');
                        lineBuffer = buf;
                    }
                    buf[offset++] = (char) c;
                    break;
            }
        }

        if (offset == 0) {
            return null;
        }

        char[] ret = new char[offset];
        System.arraycopy(buf, 0, ret, 0, offset);
        Arrays.fill(buf, ' ');

        return ret;
    }
}
