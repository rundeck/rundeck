package com.dtolabs.rundeck.core.execution.impl.local;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.apache.tools.ant.taskdefs.Redirector;
import org.apache.tools.ant.util.LineOrientedOutputStreamRedirector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CustomExecTask extends ExecTask {
    ExecuteStreamHandler executeStreamHandler;

    protected ExecuteStreamHandler createHandler() throws BuildException {
        redirector.createHandler();
        this.executeStreamHandler = new CustomPumpStreamHandler(redirector);
        return this.executeStreamHandler;
    }

    public void log(String msg, int msgLevel) {
        if(this.executeStreamHandler != null) {
            CustomExecTask.CustomPumpStreamHandler p = (CustomExecTask.CustomPumpStreamHandler) this.executeStreamHandler;
            CustomExecTask.CustomLineOrientedOutputStream outputStream = (CustomExecTask.CustomLineOrientedOutputStream) p.getOutputStream();
            CustomExecTask.CustomLineOrientedOutputStream errorStream = (CustomExecTask.CustomLineOrientedOutputStream) p.getErrorStream();

            int outputStreamLength = outputStream != null ? outputStream.baos.size() : 0;
            int errorStreamLength = errorStream != null ? errorStream.baos.size() : 0;
            byte[] a = msg.getBytes();

            if (outputStreamLength > 0 &&
                    errorStreamLength > 0 &&
                    a.length == outputStreamLength + errorStreamLength) {

                String out = outputStream.baos.toString();
                String err = errorStream.baos.toString();

                if (msg.startsWith(out) && msg.endsWith(err)) {
                    super.log(out, msgLevel);
                    super.log(err, msgLevel);
                    return;
                } else if (msg.startsWith(err) && msg.endsWith(out)) {
                    super.log(err, msgLevel);
                    super.log(out, msgLevel);
                    return;
                }
            }
        }

        super.log(msg, msgLevel);
    }

    public static final class CustomPumpStreamHandler extends PumpStreamHandler{
        public CustomPumpStreamHandler(Redirector redirector) {
            super(new CustomLineOrientedOutputStream(redirector.getOutputStream()),
                    new CustomLineOrientedOutputStream(redirector.getErrorStream()),
                    redirector.getInputStream(), true);
        }

        protected OutputStream getOutputStream(){
            return this.getOut();
        }

        protected OutputStream getErrorStream(){
            return this.getErr();
        }
    }

    public static final class CustomLineOrientedOutputStream extends LineOrientedOutputStreamRedirector {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        public CustomLineOrientedOutputStream(OutputStream stream) {
            super(stream);
        }

        @Override
        protected void processLine(byte[] b) throws IOException {
            baos = new ByteArrayOutputStream();
            baos.write(b);
            this.doProcessLine(b);
        }

        private void doProcessLine(byte[] b) throws IOException {
            super.processLine(b);
        }
    }
}
