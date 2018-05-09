package org.rundeck.util.logback

import ch.qos.logback.core.OutputStreamAppender
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream
import groovy.transform.CompileStatic


/**
 * Append to System.out/System.err. Uses the original value of System.out/System.err PrintStream when {@link ThreadBoundOutputStream}
 * has been installed to System.out/System.err, to avoid logging to whatever sink output stream
 * may be installed for the current thread.
 * @param < E >
 */
@CompileStatic
class TrueConsoleAppender<E> extends OutputStreamAppender<E> {

    String target = "System.out"


    @Override
    void start() {
        if (this.target == 'System.out') {
            this.outputStream = lazyOut()
        } else {
            this.outputStream = lazyErr()
        }
        super.start()
    }

    private static OutputStream lazyOut() {
        new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                ThreadBoundOutputStream.getOrigSystemOut(System.out).write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                ThreadBoundOutputStream.getOrigSystemOut(System.out).write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                ThreadBoundOutputStream.getOrigSystemOut(System.out).write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                ThreadBoundOutputStream.getOrigSystemOut(System.out).flush();
            }
        }
    }

    private static OutputStream lazyErr() {
        new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                ThreadBoundOutputStream.getOrigSystemErr(System.err).write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                ThreadBoundOutputStream.getOrigSystemErr(System.err).write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                ThreadBoundOutputStream.getOrigSystemErr(System.err).write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                ThreadBoundOutputStream.getOrigSystemErr(System.err).flush();
            }
        }
    }
}
