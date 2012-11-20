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
* GeneratedScriptImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/20/12 11:47 AM
* 
*/
package com.dtolabs.rundeck.plugins.step.util;

import com.dtolabs.rundeck.plugins.step.GeneratedScript;

import java.util.*;


/**
 * GeneratedScriptImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class GeneratedScriptImpl implements GeneratedScript {
    private String script;
    private String[] args;
    private String[] command;

    GeneratedScriptImpl(final String script, final String[] args) {
        this.script = script;
        this.args = args;
    }

    GeneratedScriptImpl(final String[] command) {
        this.command = command;
    }

    public String getScript() {
        return script;
    }

    public String[] getArgs() {
        return args;
    }

    public String[] getCommand() {
        return command;
    }

    public static GeneratedScript script(final String script, final String[] args) {
        return new GeneratedScriptImpl(script, args);
    }

    public static GeneratedScript command(final String... command) {
        return new GeneratedScriptImpl(command);
    }
}
