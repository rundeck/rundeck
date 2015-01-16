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
* ThreadBoundOutputStream.java
* 
*/
package com.dtolabs.rundeck.core.utils;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;


/**
 * ThreadBoundOutputStream allows a different OutputStream to be used for the current Thread and any child threads when
 * necessary, otherwise the default OutputStream is used.  This is useful for replacing System.err or System.out to
 * capture output in a multi-threaded system, allowing using different sink OutputStreams for each Thread (and their
 * sub-threads). <br> Convenience methods {@link #bindSystemOut()} and {@link #bindSystemErr()} allow easy replacement
 * of the System.out and System.err printstreams, and access to the ThreadBoundOutputStream at any time after doing so.
 * <br> Setting the correct OutputStream should be done with {@link #installThreadStream(java.io.OutputStream)}, and
 * removed with {@link #removeThreadStream()}. <br> Example code which replaces System.out and sets a two different
 * FileOutputStreams as the sinks for System.out for multpile threads:
 * <pre>
 * public static void main(String[] args) throws FileNotFoundException {
 *     //bind to System.out, or retrieve already bound instance
 *     final ThreadBoundOutputStream newout = ThreadBoundOutputStream.bindSystemOut();
 *
 *     //start another thread which redirect System.out to a file
 *     Thread t1 = new Thread(new Runnable(){
 *         public void run() {
 *             try {
 *                 newout.installThreadStream(new FileOutputStream("thread1.output"));
 *                 method1("thread1"); //writes to the thread1.output file
 *             } catch (FileNotFoundException e) { }
 *         }
 *     });
 * 
 *     t1.start();
 *     method1("main"); //prints to original System.out
 * }
 * public static void method1(String thread) {
 *     //pre-existing code which writes to System.out
 *     System.out.println("This output will be written to the correct outputstream in thread: " + thread);
 * }
 * </pre>
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 452 $
 */
public class ThreadBoundOutputStream extends FilterOutputStream {

    private InheritableThreadLocal<OutputStream> inheritOutputStream = new InheritableThreadLocal<OutputStream>();

    private final OutputStream sink;

    /**
     * Create a ThreadBoundOutputStream with a particular OutputStream as the default write destination.
     *
     * @param stream default sink
     */
    public ThreadBoundOutputStream(final OutputStream stream) {
        super(stream);
        this.sink=stream;
    }


    /**
     * Install an outputstream for the current thread and any child threads
     *
     * @param stream the new output stream
     */
    public void installThreadStream(final OutputStream stream) {
        if(stream==this){
            return;
        }
        inheritOutputStream.set(stream);
    }

    /**
     * Remove the custom stream for the current thread.
     * @return thread bound OutputStream or null if none exists
     */
    public OutputStream removeThreadStream() {
        final OutputStream orig = inheritOutputStream.get();
        inheritOutputStream.set(null);
        return orig;
    }


    public OutputStream getThreadStream() {
        final OutputStream localOutputStream = getThreadLocalOutputStream();
        if(null!=localOutputStream){
            return localOutputStream;
        }else {
            return sink;
        }
    }
    private OutputStream getThreadLocalOutputStream() {
        return inheritOutputStream.get();
    }


    public void write(final int i) throws IOException {
        final OutputStream out = getThreadLocalOutputStream();
        if (out == null || out == this) {
            super.write(i);
        } else {
            out.write(i);
        }
    }

    private static PrintStream origSystemOut;
    private static ThreadBoundPrintStream boundOutPrint;

    /**
     * Bind the System.out PrintStream to a ThreadBoundOutputStream and return it. If System.out is already bound,
     * returns the pre-bound ThreadBoundOutputStream. The binding can be removed with {@link #unbindSystemOut()}.
     *
     * @return A ThreadBoundOutputStream bound to System.out
     */
    public static synchronized ThreadBoundOutputStream bindSystemOut() {
        if (null== boundOutPrint) {
            origSystemOut = System.out;
            final ThreadBoundOutputStream boundOut = new ThreadBoundOutputStream(origSystemOut);
            boundOutPrint = new ThreadBoundPrintStream(boundOut);
            System.setOut(boundOutPrint);
            return boundOut;
        } else {
            return boundOutPrint.getThreadBoundOutputStream();
        }
    }

    /**
     * Resets the System.out printstream to the original PrintStream prior to the last call to {@link #bindSystemOut()}.
     * WARNING: you should only call unbindSystemOut if you know that no other threads are depending on the System.out
     * to be bound.Use the {@link #removeThreadStream()} method to remove any OutputStream bound to a particular thread
     * with a ThreadBoundOutputStream.
     */
    public static synchronized void unbindSystemOut() {
        if (null != origSystemOut) {
            System.setOut(origSystemOut);
        }
    }

    private static PrintStream origSystemErr;
    private static ThreadBoundPrintStream boundErrPrint;

    /**
     * Bind the System.err PrintStream to a ThreadBoundOutputStream and return it. If System.err is already bound,
     * returns the pre-bound ThreadBoundOutputStream. The binding can be removed with {@link #unbindSystemErr()}.
     *
     * @return A ThreadBoundOutputStream bound to System.err
     */
    public static synchronized ThreadBoundOutputStream bindSystemErr() {
        if (null== boundErrPrint) {
            origSystemErr = System.err;
            final ThreadBoundOutputStream newErr = new ThreadBoundOutputStream(origSystemErr);
            boundErrPrint = new ThreadBoundPrintStream(newErr);
            System.setErr(boundErrPrint);
            return newErr;
        } else {
            return boundErrPrint.getThreadBoundOutputStream();
        }
    }

    /**
     * Resets the System.out printstream to the original PrintStream prior to the last call to {@link #bindSystemOut()}.
     * WARNING: you should only call unbindSystemOut if you know that no other threads are depending on the System.out
     * to be bound.  Use the {@link #removeThreadStream()} method to remove any OutputStream bound to a particular
     * thread with a ThreadBoundOutputStream.
     */
    public static synchronized void unbindSystemErr() {
        if (null != origSystemErr) {
            System.setErr(origSystemErr);
        }
    }

    /**
     * Return the original OutputStream sink
     * @return the OutputStream
     */
    public OutputStream getSink() {
        return sink;
    }
}
