/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/*
* ConsoleExecutionFollowReceiver.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 10/18/12 12:16 PM
* 
*/
package com.dtolabs.rundeck.core.cli.queue;

import com.dtolabs.rundeck.core.dispatcher.ExecutionFollowReceiver;
import com.dtolabs.rundeck.core.execution.BaseLogger;

import java.io.PrintStream;


/**
 * ConsoleExecutionFollowReceiver is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ConsoleExecutionFollowReceiver implements ExecutionFollowReceiver {
    public static final long DEFAULT_INDETERMINATE_DELAY = 10 * 1000;
    int percent;
    long time;
    int count;
    private final long averageDuration;
    Mode mode;
    PrintStream out;
    BaseLogger logger;
    String tickMark = "#";
    String tickMarkExtra = ".";

    public ConsoleExecutionFollowReceiver(long averageDuration, Mode mode, PrintStream out, BaseLogger logger) {
        this.averageDuration = averageDuration;
        percent = 0;
        time = 0;
        count = 0;
        this.out = out;
        this.logger = logger;
        this.mode=mode;
    }
    /**
     * Execution follow mode
     */
    public static enum Mode{
        output,
        progress,
        quiet
    }

    public boolean receiveFollowStatus(long offset, long totalSize, long duration) {
        if (mode == Mode.progress && averageDuration >= 0 && percent > -1) {
            /*
            prints a # every 5% of avg duration,
            after exceeding avg duration, prints a . every 10%
             */
            int mark = Math.round(((float) duration / averageDuration) * 100.0f);
            int tick = 5;
            if (mark > percent) {
                int z=0;
                int x=0;
                if(mark<=100){
                    int diff = mark - percent + (percent % tick);
                    z = diff / tick;
                }
                if(mark>100 && percent<100){
                    //remainder of normal tickmarks up to 100%
                    int diff = 100 - percent + (percent % tick);
                    z = diff / tick;
                    percent=100;
                }
                if (mark > 100) {
                    //extra tickmarks after 100%
                    tick = 10;
                    int diff = mark - percent + (percent % tick);
                    x = diff / tick;
                }
                for (int i = 0; i < z; i++) {
                    out.print(tickMark);
                }
                for (int i = 0; i < x; i++) {
                    out.print(tickMarkExtra);
                }
                percent = mark;
                out.flush();
            }
        } else if (mode==Mode.progress && averageDuration < 0) {
            //indeterminate estimate, mark every 15 seconds
            long mark = System.currentTimeMillis();
            if (time <= 0 || time > 0 && (mark - time) > DEFAULT_INDETERMINATE_DELAY) {
                time = mark;
                count++;
                out.print(".");
                if (count > 19) {
                    out.println();
                    count = 0;
                }
                out.flush();
            }
        }
        return true;
    }

    public boolean receiveLogEntry(String timeStr, String loglevel, String user, String command,
                                   String nodeName, String message) {
        if (mode==Mode.output || null==mode) {
            logger.log(message);
        }
        return true;
    }
}
