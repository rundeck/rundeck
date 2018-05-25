/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
 */

package com.dtolabs.rundeck.app.internal.workflow

import groovy.transform.ToString

import java.time.Clock
import java.util.concurrent.TimeUnit

/**
 * Checks a file for size difference and/or elapsed time and can trigger
 * an action if threshold and/or period has elapsed
 * @author greg
 * @since 9/22/17
 */
@ToString(includePackage = false, includeFields = true, includeNames = true)
class PeriodicFileChecker {
    /**
     * The action to trigger with argument of (file size change, time change)
     */
    Closure action
    /**
     * file to check for size changes
     */
    File logfile
    /**
     * Time period unit
     */
    TimeUnit periodUnit = TimeUnit.SECONDS
    /**
     * Time period to cause a trigger
     */
    Long period = 30
    /**
     * minimum time before initial checkpoint
     */
    Long periodThreshold = 30
    /**
     * minimum log file size before initial checkpoint
     */
    Long sizeThreshold = 0
    /**
     * minimum size change between checkpoints
     */
    Long sizeIncrement = 0
    /**
     * Whether to require both thresholds to be met (AND), or only one (OR)
     */
    Behavior thresholdBehavior = Behavior.OR
    /**
     * Whether both time+size increment must be met
     */
    Behavior incrementBehavior = Behavior.OR
    private Date lastUpdate = null
    private long lastSize = 0
    private long periodMS
    private long thresholdMS
    private boolean sizeThresholdMet = false
    private boolean timeThresholdMet = false

    static enum Behavior {
        AND,
        OR

        boolean check(List<Boolean> vals) {
            if (this == AND) {
                return vals.every()
            } else {
                return vals.any()
            }
        }
    }
    Clock clock = Clock.systemUTC()
    /**
     * Trigger a check on the file
     * @return true if the action was called, false otherwise
     */
    boolean triggerCheck() {
        Date current = tick()
        long filesize = logfile.exists() ? logfile.length() : -1

        def thresholdChecks = []
        if (thresholdMS) {
            thresholdChecks << isTimeThreshold(current)
        }
        if (sizeThreshold) {
            thresholdChecks << isSizeThreshold(filesize)
        }
        if (!thresholdBehavior.check(thresholdChecks)) {
            return false
        }

        long timediff = current.time - lastUpdate.time
        long diff = filesize > 0 ? filesize - lastSize : 0

        def periodicChecks = []
        if (periodMS) {
            periodicChecks << isTimeIncrement(timediff)
        }
        if (sizeIncrement) {
            periodicChecks << isSizeCheckpoint(diff)
        }
        if (incrementBehavior.check(periodicChecks)) {
            lastUpdate = current
            lastSize = filesize
            action.call(diff, timediff)
            return true
        }
        return false
    }

    private Date tick() {
        Date current = Date.from clock.instant()
        if (lastUpdate != null) {
            return current
        }
        lastUpdate = current
        periodMS = TimeUnit.MILLISECONDS.convert(period, periodUnit)
        thresholdMS = TimeUnit.MILLISECONDS.convert(periodThreshold, periodUnit)
        current
    }

    private boolean isSizeCheckpoint(long diff) {
        sizeIncrement && diff >= sizeIncrement
    }

    private boolean isSizeThreshold(long filesize) {
        if (!sizeThreshold) {
            sizeThresholdMet = true
        }
        if (!sizeThresholdMet) {
            if (sizeThreshold && filesize >= sizeThreshold) {
                sizeThresholdMet = true
            }
        }
        sizeThresholdMet
    }

    private boolean isTimeIncrement(long timediff) {
        periodMS && timediff >= periodMS
    }

    private boolean isTimeThreshold(Date current) {
        if (!thresholdMS) {
            timeThresholdMet = true
        }
        if (!timeThresholdMet) {
            if (thresholdMS && (current.time - lastUpdate.time) >= thresholdMS) {
                timeThresholdMet = true
            }
        }
        timeThresholdMet
    }

}
