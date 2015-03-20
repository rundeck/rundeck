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
* Responder.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/21/11 10:16 AM
*
*/
package com.dtolabs.rundeck.core.execution.utils;

/**
 * Responder defines a pattern of response to some input from a stream.
 * <br>
 * A Responder defines up to four different regular expressions:
 * <br>
 * <ul> <li> input success pattern: pattern to look for before responding that indicates response should proceed</li>
 * <li> input failure pattern: pattern to look for before responding that indicates failure</li> <li> response success
 * pattern: pattern to look for after responding that indicates success</li> <li> response failure pattern: pattern to
 * look for after responding that indicates failure</li> </ul>
 * <br>
 * It also defines some other heuristics:
 * <br>
 * <ul> <li>inputMaxLines: maximum number of input lines to use to match for input pattern.  If exceeded, then "input
 * threshhold" breached.</li> <li>inputMaxTimeout: maximum time to wait while detecting new input.  If exceeded, then
 * "input threshhold" breached.</li> <li>failOnInputThreshold: if true, fail if "input threshold" breached, otherwise,
 * ignore it.</li> <li>responseMaxLines: maximum number of input lines to use to match for response pattern.  If
 * exceeded, then "response threshhold" breached.</li> <li>responseMaxTimeout: maximum time to wait while detecting
 * response pattern.  If exceeded, then "response threshhold" breached.</li> <li>failOnResponseThreshold: if true, fail
 * if "response threshold" breached, otherwise, ignore it.</li> </ul>
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface Responder {
    /**
     * @return a regex to detect input prompt
     */
    public String getInputSuccessPattern();

    /**
     * @return a regex to detect input prompt failure
     */
    public String getInputFailurePattern();

    /**
     * @return threshold max lines to read detecting input pattern
     */
    public int getInputMaxLines();

    /**
     * @return threshold max timeout detecting input pattern
     */
    public long getInputMaxTimeout();

    /**
     * @return true if input max lines threshold indicates failure
     */
    public boolean isFailOnInputLinesThreshold();
    
    /**
     * @return true if input max timeout threshold indicates failure
     */
    public boolean isFailOnInputTimeoutThreshold();

    /**
     * @return a regex to detect response to input was successful
     */
    public String getResponseSuccessPattern();

    /**
     * @return a regex to detect response to input was failure
     */
    public String getResponseFailurePattern();

    /**
     * @return threshold max lines to read detecting response pattern
     */
    public int getResponseMaxLines();

    /**
     * @return threshold max timeout detecting response pattern
     */
    public long getResponseMaxTimeout();

    /**
     * @return true if response threshold indicates failure
     */
    public boolean isFailOnResponseThreshold();

    /**
     * @return input string bytes to send after successful input pattern (including any newline characters as necessary)
     */
    public byte[] getInputBytes();

    /**
     * @return true if input threshold indicates success
     */
    public boolean isSuccessOnInputThreshold();
}
