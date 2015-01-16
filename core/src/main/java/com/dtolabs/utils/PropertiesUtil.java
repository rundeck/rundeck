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
 * PropertiesUtil.java
 * 
 * User: greg
 * Created: Sep 22, 2006 12:31:05 PM
 * $Id: PropertiesUtil.java 7769 2008-02-07 00:50:23Z gschueler $
 */
package com.dtolabs.utils;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Collection;


/**
 * PropertiesUtil provides some utility methods for Properties.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 7769 $
 */
public class PropertiesUtil {
    /**
     * Returns the Properties formatted as a String
     *
     * @param props properties
     *
     * @return String format from the Properties
     *
     * @throws java.io.IOException if an error occurs
     */
    public static String stringFromProperties(Properties props) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
        props.store(baos, null);

        String propsString;
        propsString = URLEncoder.encode(baos.toString("ISO-8859-1"), "ISO-8859-1");

        return propsString;
    }

    /**
     * Convert a String into a Properties object
     *
     * @param propString properties string
     *
     * @return properties
     *
     * @throws IOException if an error occurs
     */
    public static Properties propertiesFromString(String propString) throws IOException {
        Properties props = new Properties();


        String pstring = URLDecoder.decode(propString, "ISO-8859-1");


        props.load(new ByteArrayInputStream(pstring.getBytes()));
        return props;
    }

    /**
     * Adder represents an object that can have properties added to it.
     *
     * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
     * @version $Revision: 7769 $
     */
    public static interface Adder {
        /**
         * Add a property.
         * @param name name
         * @param value value
         */
        public void addProperty(String name, String value);

        /**
         * Add all the input Properties.
         * @param properties properties
         */
        public void addProperties(Properties properties);
    }


    /**
     * Reader represents an object that can produce Properties.
     *
     * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
     * @version $Revision: 7769 $
     */
    public static interface Producer {
        /**
         * @return the properties.
         */
        public Properties readProperties();
    }

    /**
     * Returns a Collection of all property values that have keys with a certain prefix.
     * @param props the Properties to reaqd
     * @param prefix the prefix
     * @return Collection of all property values with keys with a certain prefix
     */
    public static Collection listPropertiesWithPrefix(Properties props, String prefix) {
        final HashSet set = new HashSet();
        for (Iterator i = props.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            if (key.startsWith(prefix)) {
                set.add(props.getProperty(key));
            }
        }
        return set;
    }
    /**
     * A Producer that given an input set of properties and a specific prefix, produces another set of properties
     * from all input properties that have that prefix and removes the prefix.
     */
    public static class PrefixProducer implements Producer {
        private String prefix;
        private Properties props;

        public PrefixProducer(Properties props, String prefix) {
            this.prefix = prefix;
            this.props = props;
        }

        public Properties readProperties() {
            Properties newprops = new Properties();
            for (Iterator i = props.keySet().iterator(); i.hasNext();) {
                String key = (String) i.next();
                if (key.startsWith(prefix)) {
                    String newkey = key.substring(prefix.length());

                    newprops.setProperty(newkey, props.getProperty(key));
                }
            }
            return newprops;
        }
    }

    /**
     * A Adder that adds new properties to the input Properties object using a given prefix to prepend to the
     * name of all new properties.
     */
    public static class PrefixAdder implements Adder {
        private String prefix;
        private Properties props;

        public PrefixAdder(Properties props, String prefix) {
            this.prefix = prefix;
            this.props = props;
        }

        public void addProperty(String name, String value) {
            props.put(prefix + name, value);
        }

        public void addProperties(Properties properties) {
            for (Iterator i = properties.keySet().iterator(); i.hasNext();) {
                String key = (String) i.next();
                String value = properties.getProperty(key);
                addProperty(key, value);
            }
        }

        public Adder withPrefix(String prefix) {
            return new PrefixAdder(props, this.prefix + prefix);
        }
    }

}
