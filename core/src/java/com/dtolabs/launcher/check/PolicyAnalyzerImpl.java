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

/*
* PolicyAnalyzerImpl.java
* 
* User: greg
* Created: Oct 8, 2009 10:23:00 AM
* $Id$
*/
package com.dtolabs.launcher.check;

import java.util.*;
import java.io.File;

/**
 * PolicyAnalyzerImpl Implements {@link PolicyAnalyzer}, and reports the policy check results to a {@link PolicyAnalyzerListener}
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class PolicyAnalyzerImpl implements PolicyAnalyzer {
    private Checker requiredChecker;
    private Checker optionalChecker;

    /**
     * Create PolicyAnalyzerImpl with a PolicyAnalyzerListener
     * @param listener the listener
     */
    public PolicyAnalyzerImpl(final PolicyAnalyzerListener listener) {
        final PolicyReporter required = new PolicyReporter(true,listener);
        this.requiredChecker = new CheckerImpl(required);
        final PolicyReporter optional = new PolicyReporter(false,listener);
        this.optionalChecker = new CheckerImpl(optional);
    }


    public boolean requireFileExists(final File dir, final boolean directory) {
        return requiredChecker.checkFileExists(dir, directory);
    }

    public boolean expectFileExists(final File dir, final boolean directory) {
        return optionalChecker.checkFileExists(dir, directory);
    }

    public boolean expectPropertyValue(final String key, final String value, final Properties props) {
        return optionalChecker.checkPropertyValue(key, value, props);
    }

    public boolean requirePropertyValue(final String key, final String value, final Properties props) {
        return requiredChecker.checkPropertyValue(key, value, props);
    }

    public int expectPropertyValues(final Properties expectedProps, final Properties props) {
        return optionalChecker.checkPropertyValues(expectedProps, props);
    }

    public int requirePropertyValues(final Properties expectedProps, final Properties props) {
        return requiredChecker.checkPropertyValues(expectedProps, props);
    }

    public int expectPropertiesExist(final Collection<String> keys, final Properties props) {
        return optionalChecker.checkPropertiesExist(keys, props);
    }

    public int requirePropertiesExist(final Collection<String> keys, final Properties props) {
        return requiredChecker.checkPropertiesExist(keys, props);
    }


    Checker getRequiredChecker() {
        return requiredChecker;
    }

    void setRequiredChecker(final Checker requiredChecker) {
        this.requiredChecker = requiredChecker;
    }

    Checker getOptionalChecker() {
        return optionalChecker;
    }

    void setOptionalChecker(final Checker optionalChecker) {
        this.optionalChecker = optionalChecker;
    }

    /**
     * An implementation of {@link com.dtolabs.launcher.check.CheckerListener} that reports policy analysis to a {@link PolicyAnalyzerListener}, based on whether the policy is to
     * require that the check passes or not.  The requirement is passed in calls to the PolicyAnalyzerListener.
     */
    public static class PolicyReporter implements CheckerListener {
        private boolean required;
        private PolicyAnalyzerListener listener ;

        /**
         * Create PolicyReporter
         * @param required if true, require checks to pass
         * @param listener listener to report to
         */
        public PolicyReporter(final boolean required, final PolicyAnalyzerListener listener) {
            this.required = required;
            this.listener = listener;
        }

        public void beginCheckOnFile(final File file) {
            listener.beginCheckOnFile(file);
        }

        public void beginCheckOnDirectory(final File file) {
            listener.beginCheckOnDirectory(file);
        }

        public void beginCheckOnProperties(final File file) {
            listener.beginCheckOnProperties(file);
        }

        public void expectedFile(final File file) {
            listener.passedFile(file);
        }

        public void expectedDirectory(final File file) {
            listener.passedDirectory(file);
        }

        public void expectedPropertyValue(final String key, final String value) {
            listener.passedPropertyValue(key, value);
        }

        public void missingFile(final File file) {
            listener.failedFile(file, true, false, required);
        }

        public void notAFile(final File file) {
            listener.failedFile(file, false, true, required);
        }

        public void missingDirectory(final File file) {
            listener.failedDirectory(file, true, false, required);
        }

        public void notADirectory(final File file) {
            listener.failedDirectory(file, false, true, required);
        }

        public void incorrectPropertyValue(final String key, final String value, final String expected) {
            listener.failedPropertyValue(key, value, expected, required);
        }

        public void missingPropertyValue(final String key, final String expected) {
            listener.failedPropertyValue(key, null, expected, required);
        }

    }
}
