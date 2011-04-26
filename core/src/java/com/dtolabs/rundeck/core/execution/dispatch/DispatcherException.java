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
* DispatcherException.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/22/11 11:51 AM
* 
*/
package com.dtolabs.rundeck.core.execution.dispatch;

import com.dtolabs.rundeck.core.common.INodeEntry;

/**
 * DispatcherException is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class DispatcherException extends Exception {
    private INodeEntry node;

    public DispatcherException() {
    }

    public DispatcherException(String s) {
        super(s);
    }

    public DispatcherException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public DispatcherException(Throwable throwable) {
        super(throwable);
    }

    public DispatcherException(INodeEntry node) {
        this.node = node;
    }

    public DispatcherException(String s, INodeEntry node) {
        super(s);
        this.node = node;
    }

    public DispatcherException(String s, Throwable throwable, INodeEntry node) {
        super(s, throwable);
        this.node = node;
    }

    public DispatcherException(Throwable throwable, INodeEntry node) {
        super(throwable);
        this.node = node;
    }

    public INodeEntry getNode() {
        return node;
    }

    public void setNode(INodeEntry node) {
        this.node = node;
    }
}
