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

package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext;

import java.net.URI;

public class Attribute {

    public Attribute(URI property, String value) {
        this.property = property;
        this.value = value;
    }
    
    public final URI property;
    public final String value;

    public URI getProperty(){
        return property;
    }
    public String getValue(){
        return value;
    }
    @Override
    public String toString() {
        return property.toString() + ":" + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        if (!property.equals(attribute.property)) return false;
        if (!value.equals(attribute.value)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = property.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    public static String propertyKeyForURIBase(Attribute attr, String uriBase) {
        if(attr.getProperty().toString().startsWith(uriBase)) {
            return attr.getProperty().toString().substring(uriBase.length());
        }
        return "<" + attr.getProperty() + ">";
    }


}
