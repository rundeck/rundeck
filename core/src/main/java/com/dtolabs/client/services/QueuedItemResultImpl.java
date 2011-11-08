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
* QueuedItemResultIml.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 19, 2010 4:37:42 PM
* $Id$
*/
package com.dtolabs.client.services;

import com.dtolabs.rundeck.core.dispatcher.QueuedItem;
import com.dtolabs.rundeck.core.dispatcher.QueuedItemResult;

/**
 * QueuedItemResultImpl simple implementation of QueuedItemResult, provides factory methods for creation.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class QueuedItemResultImpl implements QueuedItemResult {
    private boolean successful;
    private String message;
    private QueuedItem item;

    /**
     * Create a successful queued item result.
     *
     * @param message message
     * @param id      ID
     * @param url     URL
     * @param name    item name
     *
     * @return success item
     */
    public static QueuedItemResult successful(final String message, final String id, final String url,
                                              final String name) {
        return new QueuedItemResultImpl(message, id, url, name);
    }

    /**
     * Create a failed queued item result.
     *
     * @param message failure message
     *
     * @return failure item.
     */
    public static QueuedItemResult failed(final String message) {
        return new QueuedItemResultImpl(message);
    }

    /**
     * Constructure with only a failure message, sets the success status to false
     *
     * @param message failure message
     */
    private QueuedItemResultImpl(final String message) {
        this.successful = false;
        this.message = message;
    }

    /**
     * Constructor with a successful result, including id and url.
     *
     * @param message success messsage
     * @param id item id
     * @param url item url
     * @param name item name
     */
    private QueuedItemResultImpl(final String message, final String id, final String url, final String name) {
        this.successful = true;
        this.message = message;
        this.item = createQueuedItem(id, url, name);
    }

    public boolean isSuccessful() {
        return successful;
    }

    /**
     * Set success
     * @param successful success value
     */
    public void setSuccessful(final boolean successful) {
        this.successful = successful;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Set message
     * @param message message string
     */
    public void setMessage(final String message) {
        this.message = message;
    }

    public QueuedItem getItem() {
        return item;
    }


    /**
     * QueuedItem factory method.
     *
     * @param id   id
     * @param url  url
     * @param name name
     *
     * @return QueuedItem
     */
    public static QueuedItem createQueuedItem(final String id, final String url, final String name) {
        return new QueuedItemImpl(id, url, name);
    }

    /**
     * Implementation of {@link QueuedItem}
     */
    static class QueuedItemImpl implements QueuedItem {
        private String id;
        private String url;
        private String name;

        public QueuedItemImpl(final String id, final String url, final String name) {
            this.id = id;
            this.url = url;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public String getUrl() {
            return url;
        }


        public void setUrl(final String url) {
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }
}
