/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.dispatcher;

import com.dtolabs.rundeck.core.data.ViewTraverse;
import lombok.Value;

/**
 * @author greg
 * @since 5/1/17
 */
@Value
public class ContextView implements ViewTraverse<ContextView>, Comparable<ContextView> {
    boolean global;
    Integer stepNumber;
    String nodeName;

    public ContextView(Integer step, String nodeName) {
        this.global = step == null && nodeName == null;
        this.stepNumber = step;
        this.nodeName = nodeName;
    }

    public static ContextView global() {
        return new ContextView(null, null);
    }

    public static ContextView step(Integer step) {
        return new ContextView(step, null);
    }

    public static ContextView nodeStep(Integer step, String nodeName) {
        return new ContextView(step, nodeName);
    }

    public static ContextView node(String nodeName) {
        return new ContextView(null, nodeName);
    }

    @Override
    public boolean isWidest() {
        return isGlobal();
    }

    /**
     * @return true if the context has both step and node
     */
    public boolean isNodeStep() {
        return null != getNodeName() && null != getStepNumber();
    }

    /**
     * @return true if the context has only node
     */
    public boolean isNodeOnly() {
        return null != getNodeName() && null == getStepNumber();
    }

    /**
     * @return true if the context has only step
     */
    public boolean isStepOnly() {
        return null != getStepNumber() && null == getNodeName();
    }

    @Override
    public boolean globExpandTo(final ContextView x) {
        return
                //this is global, and x is a node context
                //this is a node context, and so is x
                null == getStepNumber() && x.isNodeOnly()
                ||
                //this is a step context, and x is a sub node step context
                x.isNodeStep() && x.getStepNumber().equals(getStepNumber())
                ;
    }

    @Override
    public ViewTraverse<ContextView> merge(final ContextView source) {
        if (source.isWidest()) {
            return this;
        } else if (null == getNodeName() && null != source.getNodeName()) {
            if (null != getStepNumber()) {
                return nodeStep(getStepNumber(), source.getNodeName());
            } else if (null != source.getStepNumber()) {
                return nodeStep(source.getStepNumber(), source.getNodeName());
            } else {
                return node(source.getNodeName());
            }
        }
        return this;
    }

    @Override
    public boolean isWider(final ContextView source) {
        if (source.isGlobal()) {
            return !isGlobal();
        }
        return

                isNodeStep() && !source.isNodeStep()
                || isStepOnly() && null == source.getStepNumber()
                || isNodeOnly() && source.isGlobal()
                ;
    }

    @Override
    public ContextView widenView() {
        if (isGlobal()) {
            return this;
        }
        if (null != getNodeName() && null != getStepNumber()) {
            return step(getStepNumber());
        }
        return global();
    }

    @Override
    public ContextView getView() {
        return this;
    }

    @Override
    public int compareTo(final ContextView o) {
        if (isGlobal()) {
            return o.isGlobal() ? 0 : -1;
        } else if (o.isGlobal()) {
            return 1;
        }
        if (null != getStepNumber()) {
            if (null != o.getStepNumber()) {
                int compstep = getStepNumber().compareTo(o.getStepNumber());
                if (compstep != 0) {
                    return compstep;
                }
            } else {
                return -1;
            }
            return compareNodeName(o);
        } else if (null != o.getStepNumber()) {
            return 1;
        }

        return compareNodeName(o);
    }

    public int compareNodeName(final ContextView o) {
        if (null != getNodeName()) {
            if (null != o.getNodeName()) {
                return getNodeName().compareTo(o.getNodeName());
            } else {
                return 1;
            }
        } else if (null != o.getNodeName()) {
            return -1;
        }
        return 0;
    }

    public String toString() {
        Integer step = getStepNumber();
        return "ContextView(" +
               (isGlobal() ? "global" : (
                       (null != step ? "step:" + step : "")
                       + (null != getNodeName() ? (null != step ? ", " : "") + "node:" + getNodeName() : "")
               ))
               + ")"
                ;
    }
}
