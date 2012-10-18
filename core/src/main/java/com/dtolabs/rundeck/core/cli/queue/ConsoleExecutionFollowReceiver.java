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
class ConsoleExecutionFollowReceiver implements ExecutionFollowReceiver {
    int percent;
    long time;
    int count;
    private final long averageDuration;
    boolean quiet;
    boolean hashmark;
    PrintStream out;
    BaseLogger logger;
    String tickMark = "#";
    String tickMarkExtra = ".";

    public ConsoleExecutionFollowReceiver(long averageDuration, boolean quiet, boolean hashmark, PrintStream out,
                                          BaseLogger logger) {
        this.averageDuration = averageDuration;
        percent = 0;
        time = 0;
        count = 0;
        this.hashmark = hashmark;
        this.quiet = quiet;
        this.out = out;
        this.logger = logger;
    }

    public boolean receiveFollowStatus(long offset, long totalSize, long duration) {
        if (quiet && hashmark && averageDuration >= 0 && percent > -1) {
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
            }
        } else if (quiet && hashmark && averageDuration < 0) {
            //indeterminate estimate, mark every 15 seconds
            if (time <= 0 || time > 0 && System.currentTimeMillis() - time > 15 * 1000) {
                time = System.currentTimeMillis();
                count++;
                out.print(".");
                if (count > 19) {
                    out.println();
                    count = 0;
                }
            }
        }
        return true;
    }

    public boolean receiveLogEntry(String timeStr, String loglevel, String user, String command,
                                   String nodeName, String message) {
        if (!quiet) {
            logger.log(message);
        }
        return true;
    }
}
