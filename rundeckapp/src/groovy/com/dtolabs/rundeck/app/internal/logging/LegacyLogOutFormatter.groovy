package com.dtolabs.rundeck.app.internal.logging
import com.dtolabs.rundeck.core.utils.Reformatter
/*
 * Copyright 2013 DTO Labs, Inc. (http://dtolabs.com)
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
 * LegacyLogOutFormatter.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 1/22/13 10:21 PM
 * 
 */

class LegacyLogOutFormatter implements Reformatter {
    def public static List<String> EXEC_FORMAT_SEQUENCE = ['time', 'level', 'user', 'module', 'command', 'node', 'context']
    public LegacyLogOutFormatter() {

    }




    public String getHead() {
        return "";
    }

    public String getTail() {
        return '^^^END^^^';
    }

    /**
     * Expects 'time': formatted time, 'level': log level, and others in {@link #EXEC_FORMAT_SEQUENCE}
     * @param context data
     * @param message message
     *
     * @return
     */
    @Override
    String reformat(Map<String, String> context, String message) {
        def String dDate = context.time;
        String dMesg = message?:'';
        while (dMesg.endsWith('\r')) {
            dMesg = dMesg.substring(0, dMesg.length() - 1)
        }
        StringBuffer sb = new StringBuffer()
        sb.append('^^^')
        //date
        sb.append(dDate).append('|')
        //level
        sb.append(context.level).append("|")

        //sequence
        if (context) {
            for (def i = 2; i < EXEC_FORMAT_SEQUENCE.size(); i++) {
                if (null == context[EXEC_FORMAT_SEQUENCE[i]]) {
                    sb.append('|')
                } else {
                    sb.append(context[EXEC_FORMAT_SEQUENCE[i]]).append('|')
                }
            }
        }
        //mesg
        sb.append(dMesg)
        //end
        sb.append('^^^')

        return sb.toString()
    }
}
