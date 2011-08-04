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

package com.dtolabs.client.utils;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;

/**
 * takes a property file name as input and manufactures a Map object via HashMap
 */
public class PropertyMap {

   private HashMap keyvalues = new HashMap();

   public PropertyMap(String propertyFile) throws IOException {
      PropertyResourceBundle prBundle = new PropertyResourceBundle(new FileInputStream(propertyFile));
      
      Enumeration keys = prBundle.getKeys();
      while (keys.hasMoreElements()) {
         String key = (String)keys.nextElement();
         String value = (String)prBundle.handleGetObject(key);
         keyvalues.put(key, value);
      }
   }

   /**
    * return the map of the propertyfile input
    */
   public Map toMap() {
      return this.keyvalues;
   }

   /**
    * return a key's value within the map
    */
   public String get(String key) {
      return (String)this.keyvalues.get(key);
   }

}
