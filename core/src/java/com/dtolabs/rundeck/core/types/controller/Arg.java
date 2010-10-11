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

package com.dtolabs.rundeck.core.types.controller;

/**
 * Data type very similar to Ant's own arg type used in the ExecTask
 */
public class Arg {
    /**
     * default no-arg constructor *
     */
    public Arg() {
    }

    /**
     * Parameterized constructor
     *
     * @param line String holding cmd.line
     */
    public Arg(final String line) {
        this.line = line;
    }


    /**
     * Factory method.
     *
     * @param cmdLine command line args
     * @return new instance
     */
    public static Arg create(final String cmdLine) {
        return new Arg(cmdLine);
    }

    private String line;

    public void setLine(final String args) {
        line = args;
    }

    public String getLine() {
        return line;
    }

    public String toString() {
        return "Arg{" +
                "line=" + line +
                "}";
    }
}
