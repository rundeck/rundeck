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
* DefaultDispatchedScriptExecutionItem.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 10, 2010 11:13:02 AM
* $Id$
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.dispatcher.IDispatchedScript;

/**
 * DefaultDispatchedScriptExecutionItem is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
class DispatchedScriptExecutionItemImpl implements DispatchedScriptExecutionItem{
    private IDispatchedScript dispatchedScript;

    public DispatchedScriptExecutionItemImpl(final IDispatchedScript dispatchedScript) {
        this.dispatchedScript = dispatchedScript;
    }

    public IDispatchedScript getDispatchedScript() {
        return dispatchedScript;
    }

    public void setDispatchedScript(final IDispatchedScript dispatchedScript) {
        this.dispatchedScript = dispatchedScript;
    }

    public String getType() {
        return null;
    }
}
