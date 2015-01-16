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
* Pair.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/12/11 3:27 PM
* 
*/
package com.dtolabs.rundeck.core.utils;

/**
 * Pair implementation that disallows nulls, and provides setters.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PairImpl<T, W> implements Pair<T, W> {
    private T first;
    private W second;

    public PairImpl(final T first, final W second) {
        if (null == first) {
            throw new NullPointerException("first");
        }
        if (null == second) {
            throw new NullPointerException("second");
        }
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    /**
     * Set first item
     * @param first first item
     */
    public void setFirst(final T first) {
        if (null == first) {
            throw new NullPointerException("first");
        }
        this.first = first;
    }

    public W getSecond() {
        return second;
    }

    /**
     * Set second item
     * @param second second item
     */
    public void setSecond(final W second) {
        if (null == second) {
            throw new NullPointerException("second");
        }
        this.second = second;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PairImpl)) {
            return false;
        }

        final PairImpl pair = (PairImpl) o;

        if (!first.equals(pair.first)) {
            return false;
        }
        if (!second.equals(pair.second)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = first.hashCode();
        result = 31 * result + second.hashCode();
        return result;
    }
}
