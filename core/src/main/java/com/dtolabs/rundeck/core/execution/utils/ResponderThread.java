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
* ResponderThread.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/21/11 10:26 AM
*
*/
package com.dtolabs.rundeck.core.execution.utils;

import com.dtolabs.rundeck.core.utils.PartialLineBuffer;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.regex.Pattern;

/**
 * ResponderThread interacts with an input and outputstream, using a {@link Responder} instance to define the
 * interaction.  It looks for lines on the input stream that match the "inputSuccess" regular expression (e.g. password
 * prompt). If it detects the "inputFailure" regular expression, it fails. If successful, it writes the "inputString" to
 * the output stream.
 * <p/>
 * After writing to the output stream, It then looks for a certain "responseSuccess" regular expression. If successful
 * it completes successfully. If it detects a "responseFailure" regular expression in the output, it fails.
 * <p/>
 * If a {@link ResultHandler} is set, it will call the handleResult method after the response logic.
 * <p/>
 * If it is stopped with the {@link #stopResponder()} method, then all further response interactions cease, and no
 * result handler is called.
 * <p/>
 * Example: wait for "[sudo] password for user: ", write a password, and fail on "try again" response:
 * <p/>
 * <ul> <li>inputSuccessPattern: '^\[sudo\] password for .+: '</li> <li>responseFailurePattern: '^.*try again.*'</li>
 * <li>failOnResponseThreshold: false</li> <li>InputMaxLines: 12</li> <li>inputString: 'password'</li> </ul>
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ResponderThread extends Thread implements ResponderStopper {
    static Logger logger = Logger.getLogger(ResponderThread.class.getName());
    private Responder responder;
    private OutputStream outputStream;
    private InputStreamReader reader;
    private boolean success;
    private boolean failed;
    private String failureReason;
    private ResultHandler resultHandler;
    volatile boolean stopped = false;

    public ResponderThread(final Responder responder, final InputStream inputStream, final OutputStream outputStream,
                           final ResultHandler resultHandler) {
        this.responder = responder;
        this.outputStream = outputStream;
        reader = new InputStreamReader(inputStream);
        this.resultHandler = resultHandler;
    }

    static String[] STEP_DESCS = {
        "waiting for input prompt",
        "writing response",
        "waiting for response"
    };

    public void run() {
        logger.debug("started responder thread");
        runResponder();
        if (null != resultHandler && !stopped) {
            resultHandler.handleResult(success, failureReason);
        }
    }

    public void stopResponder() {
        stopped = true;
        this.interrupt();
    }

    public boolean isResponderStopped() {
        return stopped;
    }

    private void runResponder() {
        int step = 0;
        try {
            //look for input pattern
            final PartialLineBuffer buffer = new PartialLineBuffer();
            if (null != responder.getInputSuccessPattern() || null != responder.getInputFailurePattern()) {
                logger.debug("Awaiting input: " + responder.getInputSuccessPattern() + ";" + responder
                    .getInputFailurePattern());
                boolean detected;
                try {
                    detected = detect(responder.getInputSuccessPattern(),
                        responder.getInputFailurePattern(),
                        responder.getInputMaxTimeout(),
                        responder.getInputMaxLines(), reader, this, buffer);
                    logger.debug("Success detected? " + detected);
                    if (stopped) {
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
                    }else if (responder.isFailOnInputTimeoutThreshold() && e.getType() == ThresholdType.milliseconds) {
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

            if(null!=responder.getInputString()){
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
                        responder.getResponseMaxLines(), reader, this, buffer);
                    if (stopped) {
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
                          final int maxLines, final InputStreamReader reader, final ResponderStopper thread,
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
        while (System.currentTimeMillis() < start + timeout && linecount < maxLines && !thread.isResponderStopped()) {
            //check buffer first, it may already contain data
            String line = buffer.readLine();
            while (null != line) {
                logger.debug("read line: " + line);
                start = System.currentTimeMillis();
                linecount++;
                if (null != success && success.matcher(line).matches()) {
                    return true;
                } else if (null != failure && failure.matcher(line).matches()) {
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
                    return true;
                } else if (null != failure && failure.matcher(line).matches()) {
                    return false;
                }
            }

            if (!reader.ready()) {
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    //ignore
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
        if (linecount >= maxLines) {
            throw new ThreshholdException(maxLines, ThresholdType.lines);
        } else if (System.currentTimeMillis() >= start + timeout) {
            throw new ThreshholdException(timeout, ThresholdType.milliseconds);
        } else {
            //
            return false;
        }
    }
    /**
     * Threshold type
     */
    static enum ThresholdType{
        milliseconds,
        lines,
    }
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

    public boolean isSuccess() {
        return success;
    }

    public String getFailureReason() {
        return failureReason;
    }

}
