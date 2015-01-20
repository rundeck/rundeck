/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
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
* ResponderTask.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/21/11 10:26 AM
*
*/
package com.dtolabs.rundeck.core.execution.utils;

import com.dtolabs.rundeck.core.utils.PartialLineBuffer;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;


/**
 * ResponderTask interacts with an input and outputstream, using a {@link Responder} instance to define the interaction.
 * It looks for lines on the input stream that match the "inputSuccess" regular expression (e.g. password prompt). If it
 * detects the "inputFailure" regular expression, it fails. If successful, it writes the "inputString" to the output
 * stream.
 * <br>
 * After writing to the output stream, It then looks for a certain "responseSuccess" regular expression. If successful
 * it completes successfully. If it detects a "responseFailure" regular expression in the output, it fails.
 * <br>
 * If a {@link ResultHandler} is set, it will call the handleResult method after the response logic.
 * <br>
 * If it the thread running the ResponderTask is interrupted, then the process will stop as soon as it is detected.
 * <br>
 * Implements {@link Callable} so it can be submitted to a {@link java.util.concurrent.ExecutorService}.
 * <br>
 * <br>
 * <br>
 * Example: wait for "[sudo] password for user: ", write a password, and fail on "try again" response:
 * <br>
 * <ul> <li>inputSuccessPattern: '^\[sudo\] password for .+: '</li> <li>responseFailurePattern: '^.*try again.*'</li>
 * <li>failOnResponseThreshold: false</li> <li>InputMaxLines: 12</li> <li>inputString: 'password'</li> </ul>
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ResponderTask implements Callable<ResponderTask.ResponderResult> {
    static Logger logger = Logger.getLogger(ResponderTask.class.getName());
    private Responder responder;
    private OutputStream outputStream;
    private InputStreamReader reader;
    private boolean success;
    private boolean failed;
    private String failureReason;
    private ResultHandler resultHandler;
    private PartialLineBuffer partialLineBuffer;

    /**
     * Create a ResponderTask with a responder, io streams, and result handler which can be null.
     * @param responder responder
     * @param inputStream input
     * @param outputStream output
     * @param resultHandler handler
     */
    public ResponderTask(final Responder responder, final InputStream inputStream, final OutputStream outputStream,
                         final ResultHandler resultHandler) {
        this(responder, new InputStreamReader(inputStream), outputStream, resultHandler, new PartialLineBuffer());
    }

    /**
     * Internal constructor
     */
    private ResponderTask(final Responder responder,
                          final InputStreamReader reader,
                          final OutputStream outputStream,
                          final ResultHandler resultHandler, final PartialLineBuffer buffer) {
        this.responder = responder;
        this.outputStream = outputStream;
        this.reader = reader;
        this.resultHandler = resultHandler;
        this.partialLineBuffer = buffer;
    }

    static String[] STEP_DESCS = {
        "waiting for input prompt",
        "writing response",
        "waiting for response"
    };

    public ResponderResult call() throws Exception {
        logger.debug("started responder thread");
        runResponder();
        if (null != resultHandler && !Thread.currentThread().isInterrupted()) {
            resultHandler.handleResult(success, failureReason);
        }
        return new ResponderResult(responder, failureReason, success, Thread.interrupted());
    }

    private void runResponder() {
        int step = 0;
        try {
            //look for input pattern
            if (null != responder.getInputSuccessPattern() || null != responder.getInputFailurePattern()) {
                logger.debug("Awaiting input: " + responder.getInputSuccessPattern() + ";" + responder
                    .getInputFailurePattern());
                boolean detected;
                try {
                    detected = detect(responder.getInputSuccessPattern(),
                                      responder.getInputFailurePattern(),
                                      responder.getInputMaxTimeout(),
                                      responder.getInputMaxLines(), reader, partialLineBuffer);
                    logger.debug("Success detected? " + detected);
                    if (Thread.currentThread().isInterrupted()) {
                        logger.debug("interrupted");
                        return;
                    }
                    if (!detected) {
                        fail(step, "Expected input was not seen");
                        return;
                    }
                } catch (ThreshholdException e) {
                    if (responder.isFailOnInputLinesThreshold() && e.getType() == ThresholdType.lines) {
                        logger.debug("Threshold met " + reason(e));
                        fail(step, reason(e));
                        return;
                    } else if (responder.isFailOnInputTimeoutThreshold() && e.getType() == ThresholdType.milliseconds) {
                        logger.debug("Threshold met " + reason(e));
                        fail(step, reason(e));
                        return;
                    } else if (responder.isSuccessOnInputThreshold()) {
                        success = true;
                        return;
                    }
                }
            }
            step++;

            if (null != responder.getInputString()) {
                logger.debug("Writing to output");
                //write responseString
                outputStream.write(responder.getInputString().getBytes());
                logger.debug("Wrote to output");
            }
            step++;

            if (null != responder.getResponseSuccessPattern() || null != responder.getResponseFailurePattern()) {
                //detect success/failure response
                boolean succeeded = false;
                try {
                    logger.debug("Awaiting response: " + responder.getResponseSuccessPattern() + ", " + responder
                        .getResponseFailurePattern());
                    succeeded = detect(responder.getResponseSuccessPattern(),
                                       responder.getResponseFailurePattern(),
                                       responder.getResponseMaxTimeout(),
                                       responder.getResponseMaxLines(), reader, partialLineBuffer);
                    if (Thread.currentThread().isInterrupted()) {
                        logger.debug("interrupted");
                        return;
                    }
                    success = succeeded;
                    if (!succeeded) {
                        fail(step, "Did not see the correct response");
                    }
                    logger.debug("Success detected? " + succeeded);
                    return;
                } catch (ThreshholdException e) {
                    if (responder.isFailOnResponseThreshold()) {
                        logger.debug("Threshold met " + reason(e));
                        fail(step, reason(e));
                        return;
                    }
                }
            }
            success = true;
        } catch (IOException e) {
            logger.debug("IOException " + e.getMessage(), e);
            fail(step, e.getMessage());
            e.printStackTrace();
        }
    }

    private void fail(final int step, final String reason) {
        success = false;
        failed = true;
        failureReason =
            "Failed " + (step < STEP_DESCS.length ? STEP_DESCS[step] : "?") + ": " + reason;
    }

    private String reason(final ThreshholdException e) {
        return "Expected input was not seen in " + e.getValue() + " " + e
            .getType();
    }

    public boolean isFailed() {
        return failed;
    }

    /**
     * Set up a chained ResponderTask, where the new instance will use the same input/output streams and buffers.
     * However they must both be invoked individually, use {@link #createSequence(Responder,
     * ResponderTask.ResultHandler)} to set up a sequential invocation of
     * another responder.
     */
    private ResponderTask chainResponder(final Responder responder, final ResultHandler resultHandler) {
        return new ResponderTask(responder, reader, outputStream, resultHandler, partialLineBuffer);
    }

    /**
     * Create a Callable that will execute another responder if this one is successful, with the same resultHandler for the
     * second one.
     * @param responder  responder
     * @return sequence
     */
    public Callable<ResponderResult> createSequence(final Responder responder) {
        return createSequence(responder, resultHandler);
    }
    /**
     * Create a Callable that will execute another responder if this one is successful, with a specified resultHandler for the
     * second one.
     *
     * @param responder  responder
     * @param resultHandler handler
     * @return sequence
     */
    public Callable<ResponderResult> createSequence(final Responder responder, final ResultHandler resultHandler) {
        return new Sequence<ResponderResult>(this, this.chainResponder(responder, resultHandler));
    }

    /**
     * Handles result of responder thread run
     */
    public static interface ResultHandler {
        /**
         * Will be called at the end of the thread
         *
         * @param success if the responder was successful
         * @param reason  failure reason if the responder was not successful
         */
        public void handleResult(boolean success, String reason);
    }

    /**
     * Look for the detect pattern in the input, if seen return true.  If the failure pattern is detected, return false.
     * If a max timeout or max number of lines to read is exceeded, throw threshhold error.
     */
    static boolean detect(final String detectPattern, final String failurePattern, final long timeout,
                          final int maxLines, final InputStreamReader reader,
                          final PartialLineBuffer buffer) throws IOException, ThreshholdException {
        if (null == detectPattern && null == failurePattern) {
            throw new IllegalArgumentException("detectPattern or failurePattern required");
        }
        long start = System.currentTimeMillis();
        int linecount = 0;
        final Pattern success;
        if (null != detectPattern) {
            success = Pattern.compile(detectPattern);
        } else {
            success = null;
        }
        final Pattern failure;
        if (null != failurePattern) {
            failure = Pattern.compile(failurePattern);
        } else {
            failure = null;
        }
        outer:
        while (System.currentTimeMillis() < start + timeout && linecount < maxLines && !Thread.currentThread()
            .isInterrupted()) {
            //check buffer first, it may already contain data
            String line = buffer.readLine();
            while (null != line) {
                logger.debug("read line: " + line);
                start = System.currentTimeMillis();
                linecount++;
                if (null != success && success.matcher(line).matches()) {
                    logger.debug("success matched");
                    return true;
                } else if (null != failure && failure.matcher(line).matches()) {
                    logger.debug("failure matched");
                    return false;
                }
                if (linecount >= maxLines) {
                    break outer;
                }
                line = buffer.readLine();
            }
            line = buffer.getPartialLine();
            if (null != line) {
                logger.debug("read partial: " + line);
                if (null != success && success.matcher(line).matches()) {
                    logger.debug("success matched partial");
                    buffer.clearPartial();
                    return true;
                } else if (null != failure && failure.matcher(line).matches()) {
                    logger.debug("failure matched partial");
                    buffer.clearPartial();
                    return false;
                }else {
                    buffer.unmarkPartial();
                }
            }

            if (!reader.ready()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                continue;
            }
            final int c = buffer.read(reader);
            if (c < 0) {
                //end of stream
                logger.debug("end of input");
            } else if (c > 0) {
                logger.debug("read " + c);
            }

        }
        if (Thread.currentThread().isInterrupted()) {
            logger.debug("detect interrupted");
        }
        if (linecount >= maxLines) {
            logger.debug("max input lines");
            throw new ThreshholdException(maxLines, ThresholdType.lines);
        } else if (System.currentTimeMillis() >= start + timeout) {
            logger.debug("max timeout");
            throw new ThreshholdException(timeout, ThresholdType.milliseconds);
        } else {
            //
            return false;
        }
    }

    /**
     * Threshold type
     */
    static enum ThresholdType {
        milliseconds,
        lines,
    }

    /**
     * Indicates than a threshold was reached
     */
    static final class ThreshholdException extends Exception {
        private Object value;
        private ThresholdType type;

        ThreshholdException(final Object value, final ThresholdType type) {
            this.value = value;
            this.type = type;
        }

        ThreshholdException(final String s, final Object value, final ThresholdType type) {
            super(s);
            this.value = value;
            this.type = type;
        }

        public Object getValue() {
            return value;
        }

        public ThresholdType getType() {
            return type;
        }
    }

    /**
     * Success/failure result interface
     */
    public static interface SuccessResult {
        public boolean isSuccess();
    }

    /**
     * Creates a callable by executing the first step, and only if successful executing the next step.
     */
    public static class Sequence<T extends SuccessResult> implements Callable<T> {
        private Callable<T> step1;
        private Callable<T> step2;

        public Sequence(final Callable<T> step1,
                        final Callable<T> step2) {
            this.step1 = step1;
            this.step2 = step2;
        }

        public T call() throws Exception {
            final T result1 = step1.call();
            if (result1.isSuccess() && !Thread.currentThread().isInterrupted()) {
                return step2.call();
            } else {
                return result1;
            }
        }
    }

    /**
     * Result from a responder execution, which contains success, the original Responder, a failure reason if
     * unsuccessful and a boolean indicating if the process was interrupted.
     */
    public static final class ResponderResult implements SuccessResult {
        private String failureReason;
        private boolean success;
        private boolean interrupted;
        private Responder responder;

        ResponderResult(final Responder responder,
                        final String failureReason,
                        final boolean success,
                        final boolean interrupted) {
            this.responder = responder;
            this.failureReason = failureReason;
            this.success = success;
            this.interrupted = interrupted;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isInterrupted() {
            return interrupted;
        }

        public Responder getResponder() {
            return responder;
        }

        @Override
        public String toString() {
            return "ResponderResult{" +
                   "failureReason='" + failureReason + '\'' +
                   ", success=" + success +
                   ", interrupted=" + interrupted +
                   ", responder=" + responder +
                   '}';
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public String getFailureReason() {
        return failureReason;
    }

}
