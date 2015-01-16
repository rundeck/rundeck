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
 * ColonyClientState.java
 * 
 * User: greg
 * Created: Jan 13, 2005 3:50:32 PM
 * $Id: ClientState.java 5690 2006-01-14 20:22:27Z connary_scott $
 */
package com.dtolabs.client.utils;


import org.apache.commons.httpclient.HttpState;

import java.util.HashMap;


/**
 * ClientState manages a map of HttpStates, allowing CommanderHttpClientChannel to re-use
 * an existing client session without logging in again.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 5690 $
 */
public class ClientState {
    private ClientState() {

    }

    private static HashMap httpStates = new HashMap();

    /**
     * @return the HTTP State object for the current thread
     */
    public synchronized static HttpState getHttpState() {
        return getState(Thread.currentThread());
    }

    /**
     * Reset the HTTP State object for the current thread
     * @return current state
     */
    public synchronized static HttpState resetHttpState() {
        httpStates.remove(Thread.currentThread());
        return getHttpState();
    }

    private static HttpState getState(Thread t) {
        if (httpStates.containsKey(t)) {
            return (HttpState) httpStates.get(t);
        } else {
            HttpState hs = new HttpState();
            setState(t, hs);
            return hs;
        }
    }

    private static void setState(Thread t, HttpState state) {
        httpStates.put(t, state);
    }
}
