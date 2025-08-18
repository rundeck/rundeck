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
package com.rundeck.repository.client.events

import com.rundeck.repository.events.RepositoryEventEmitter
import com.rundeck.repository.events.RepositoryEventListener
import com.rundeck.repository.events.RepositoryUpdateEvent


class RepositoryEventManager implements RepositoryEventEmitter {

    List<RepositoryEventListener> listeners = []

    @Override
    void emit(final RepositoryUpdateEvent event) {
        listeners.each {
            try {
                it.onEvent(event)
            } catch(Exception ex) {
                //log listener failed when processing event
                ex.printStackTrace()
            }
        }
    }
}
