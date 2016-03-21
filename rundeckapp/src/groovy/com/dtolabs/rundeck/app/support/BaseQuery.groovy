package com.dtolabs.rundeck.app.support

import grails.validation.Validateable

/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * BaseQuery base for query CommandObject
 * 
 * User: greg
 * Created: Feb 13, 2008 4:18:48 PM
 * $Id$
 */
@Validateable
class BaseQuery {
    Integer max
    Integer offset
    String sortBy
    String sortOrder

    static constraints={
        sortOrder(inList:["ascending","descending"],nullable: true)
        max(min:0,nullable: true)
        offset(min:0,nullable: true)
        sortBy(nullable: true)
    }

    /**
     * Set the pagination properties based on another BaseQuery instance
     */
    public void setPagination(BaseQuery query){
        this.max=query.max
        this.offset=query.offset
        this.sortBy=query.sortBy
        this.sortOrder=query.sortOrder
    }
}
