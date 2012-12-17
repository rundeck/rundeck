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
* ContextStack.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/27/12 10:21 AM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow;

import org.apache.commons.lang.StringUtils;

import java.util.*;


/**
 * A simple stack, with factory methods for making mutated copies
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ContextStack<T> {
    private List<T> stack;

    private ContextStack() {
        stack = new ArrayList<T>();
    }

    private ContextStack(final List<T> stack) {
        this.stack = new ArrayList<T>(stack);
    }

    private ContextStack(final T item) {
        this.stack = new ArrayList<T>(1);
        push(item);
    }

    /**
     * push a value
     */
    public void push(final T value) {
        if(null!=value){
            stack.add(value);
        }
    }


    /**
     * Pop a value.
     *
     * @throws IllegalStateException if no values remain
     */
    public T pop() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("stack is empty");
        }
        return stack.remove(stack.size() - 1);
    }
    /**
     * Peek at the top value
     *
     * @throws IllegalStateException if no values remain
     */
    public T peek() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("stack is empty");
        }
        return stack.get(stack.size() - 1);
    }


    /**
     * Return a new stack based with the same contents
     */
    public ContextStack<T> copy() {
        return new ContextStack<T>(this.stack);
    }
    /**
     * Return a new stack based with the same contents and one value pushed
     */
    public ContextStack<T> copyPush(final T value) {
        final ContextStack<T> stack1 = copy();
        stack1.push(value);
        return stack1;
    }

    /**
     * Return a new stack with the same contents but pop a value
     */
    public ContextStack<T> copyPop() {
        final ContextStack<T> stack1 = copy();
        stack1.pop();
        return stack1;
    }

    /**
     * Return the stack size
     */
    public int size() {
        return stack.size();
    }

    /**
     * Create a new stack with a single item
     */
    public static <T> ContextStack<T> create(final T value) {
        return new ContextStack<T>(value);
    }

    /**
     * Return a copy of the stack
     */
    public List<T> stack() {
        return new ArrayList<T>(stack);
    }

    /**
     * Joins the context stack items with ":"
     */
    public String toString() {
        return StringUtils.join(stack, ":");
    }
}
