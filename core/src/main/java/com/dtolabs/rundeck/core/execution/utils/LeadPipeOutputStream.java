/*
 * Copyright 2012 DTO Solutions, Inc. (http://dtosolutions.com)
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
* LeadPipeOutputStream.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 1/12/12 9:44 AM
* 
*/
package com.dtolabs.rundeck.core.execution.utils;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.util.*;

/**
 * LeadPipeOutputStream is a PipedOutputStream that allows the thread reading from the PipedInputStream to die or finish
 * without completing, and does not throw any IOExceptions if this occurs.  It can be used when the reader thread does
 * not need to read all of the data, which otherwise would cause an IOException if a writer attempts to write to the
 * PipedOutputStream and the reading thread is not alive. Once the reader thread finishes or dies, then further writes
 * to this LeadPipeOutputStream will return without doing anything.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class LeadPipeOutputStream extends PipedOutputStream {
    private volatile boolean broken = false;

    @Override
    public void write(int i) throws IOException {
        if (!broken) {
            try {
                super.write(i);
            } catch (IOException e) {
                if (e.getMessage().equals("Read end dead") || e.getMessage().equals("Pipe closed") || e.getMessage()
                    .equals(
                        "Pipe broken")) {
                    //quell
                    broken = true;
                } else {
                    throw e;
                }
            }
        }
    }

    @Override
    public void write(byte[] bytes, int i, int i1) throws IOException {
        if (!broken) {
            try {
                super.write(bytes, i, i1);
            } catch (IOException e) {
                if (e.getMessage().equals("Read end dead") || e.getMessage().equals("Pipe closed") || e.getMessage()
                    .equals(
                        "Pipe broken")) {
                    //quell
                    broken = true;
                } else {
                    throw e;
                }
            }
        }
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        if (!broken) {
            try {
                super.write(bytes);
            } catch (IOException e) {
                if (e.getMessage().equals("Read end dead") || e.getMessage().equals("Pipe closed") || e.getMessage()
                    .equals(
                        "Pipe broken")) {
                    //quell
                    broken = true;
                } else {
                    throw e;
                }
            }
        }
    }
}
