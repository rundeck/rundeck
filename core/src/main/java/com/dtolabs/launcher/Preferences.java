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

package com.dtolabs.launcher;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.utils.PropertyUtil;

import java.io.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * give us ability to set all framework/modules.* properties at the cmd line
 * such as --framework.property.name=&lt;value&gt;, which implies:
 * framework.property.name=&lt;value&gt;
 * by generating a preferences.properties file that contains overridden values otherwise default values
 * from new defaults file apply.
 */

public class Preferences {

    public static final String JAVA_HOME = Constants.JAVA_HOME;
    public static final String ENV_JAVA_HOME = System.getProperty("user.java_home");

    // required properties
    public static final String SYSTEM_RDECK_BASE = Constants.getSystemBaseDir();

    // location of default properties file which some of them can be overridden at setup time
    public static final String RUNDECK_DEFAULTS_PROPERTIES_NAME = Constants.getDefaultsPropertiesName();

    /**
     * generate preferences file represented by preferences String
     * @param args arg strings
     * @param preferences prefs
     * @param inputProps input
     * @throws Exception on error
     */
    public static void generate(String args[], String preferences, Properties inputProps) throws Exception {
        String base;
        base = inputProps.getProperty("rdeck.base");
        if ((null == ENV_JAVA_HOME || "".equals(ENV_JAVA_HOME)) && (null == JAVA_HOME || "".equals(JAVA_HOME))){
            throw new Exception("property: java.home, not defined");
        }
        if(null==base) {
            base = SYSTEM_RDECK_BASE;
        }
        if (null == base || "".equals(base))
            throw new Exception("property: rdeck.base, not defined");

        File baseDir = new File(base);
        if (!baseDir.isDirectory()) {
            if (baseDir.exists()) {
                throw new Exception(base + " exists and is not a directory");
            }
            if (!baseDir.mkdirs()) {
                throw new Exception(base + " does not exist and cannot be created");
            }
        }

        // the properties used to generate the preferences.properties file
        Properties systemProperties = System.getProperties();
        Properties defaultProperties = new Properties();

        //
        // bootstrap the rdeck.base, ant.home, and java.home
        //
        String jhome = ENV_JAVA_HOME;
        if(null==jhome) {
            jhome = JAVA_HOME;
        }
        defaultProperties.setProperty("user.java_home", forwardSlashPath(jhome));
        defaultProperties.setProperty("user.java_home.win", backSlashPath(jhome));
        defaultProperties.setProperty("java.home", forwardSlashPath(jhome));
        defaultProperties.setProperty("java.home.win", backSlashPath(jhome));
        defaultProperties.setProperty("rdeck.base", forwardSlashPath(base));
        defaultProperties.setProperty("rdeck.base.win", backSlashPath(base));

        //
        // additional properties needed for successful rdeck setup, based on above
        // bootstrapping properties
        //
        defaultProperties.setProperty("framework.projects.dir", forwardSlashPath(Constants.getFrameworkProjectsDir(
            base)));
        defaultProperties.setProperty("framework.projects.dir.win", backSlashPath(Constants.getFrameworkProjectsDir(
            base)));
        defaultProperties.setProperty("framework.rdeck.base", forwardSlashPath(base));
        defaultProperties.setProperty("framework.rdeck.base.win", backSlashPath(base));
        final String configDir = Constants.getFrameworkConfigDir(base);
        defaultProperties.setProperty("framework.etc.dir", forwardSlashPath(configDir));
        defaultProperties.setProperty("framework.etc.dir.win", backSlashPath(configDir));
        defaultProperties.setProperty("framework.var.dir", forwardSlashPath(Constants.getBaseVar(base)));
        defaultProperties.setProperty("framework.var.dir.win", backSlashPath(Constants.getBaseVar(base)));
        defaultProperties.setProperty("framework.logs.dir", forwardSlashPath(Constants.getFrameworkLogsDir(base)));
        defaultProperties.setProperty("framework.logs.dir.win", backSlashPath(Constants.getFrameworkLogsDir(base)));

        Enumeration propEnum = systemProperties.propertyNames();
        //list of path properties to convert slashes on
        HashSet<String> paths=new HashSet<String>();
        paths.add("user.home");
        while (propEnum.hasMoreElements()) {
            String propName = (String) propEnum.nextElement();
            String propType = propName.split("\\.")[0];
            String value = systemProperties.getProperty(propName);
            if(paths.contains(propName)) {
                value = forwardSlashPath(value);
            }
            defaultProperties.setProperty(propName, value);
        }

        // load the default properties
        loadResourcesDefaults(defaultProperties, RUNDECK_DEFAULTS_PROPERTIES_NAME);
        defaultProperties.putAll(inputProps);
        parseNonReqOptionsAsProperties(defaultProperties, args);

        // expand all keyValues within defaultProperties against keys within same defaultProperties
        // and return new expandedDefaultProperties
        Properties expandedDefaultProperties = PropertyUtil.expand(defaultProperties);

        // parse any --<framework|modules>-<property>-<name>=&lt;value&gt; as
        // <framework|modules>.<property>.<name>=&lt;value&gt; and ensure it is a valid property

        // ensure ${rdeck_base}/etc exists
        File rdeck_base_etc = new File(configDir);
        if (!rdeck_base_etc.isDirectory()) {
            if (!rdeck_base_etc.mkdir()) {
                throw new Exception("Unable to create directory: " + configDir);
            }
        }

        // generate the preferences.properties file into ${rdeck.base}/etc from expandedDefaultProperties
        final FileOutputStream fileOutputStream = new FileOutputStream(preferences);
        try {
            expandedDefaultProperties.store(fileOutputStream, "rdeck setup preferences");
        } finally {
            fileOutputStream.close();
        }

    }

    // load properties file
    private static void loadDefaults(Properties defaultProperties, String propertiesFile) throws IOException {
        FileInputStream fis = new FileInputStream(propertiesFile);
        try {
            defaultProperties.load(fis);
        } finally {
            if(null!=fis){
                fis.close();
            }
        }
    }
    // load properties file
    public static void loadResourcesDefaults(Properties defaultProperties, String propertiesFileName) throws IOException {
        final String resource = Setup.TEMPLATE_RESOURCES_PATH + "/" + propertiesFileName;
        InputStream is = Preferences.class.getClassLoader().getResourceAsStream(
            resource);
        if(null==is) {
            throw new IOException("Unable to load resource: " + resource);
        }
        try {
            defaultProperties.load(is);
        } finally {
            if (null != is) {
                is.close();
            }
        }
    }

    /**
     * Parse arguments that match "--key=value" and populate the Properties with the values.
     * @param defaultProperties the properties
     * @param args the arguments
     * @throws Exception if an error occurs
     */
    public static void parseNonReqOptionsAsProperties(final Properties defaultProperties, final String[] args) throws Exception {

        // loop thru each argument on cmdline
        for (final String argProp : args) {
            //System.out.println("parseNonReqOptionsAsProperties(), argProp: " + argProp);

            // ignore anything that does not start with --
            if (!argProp.startsWith("--")) {
                continue;
            }

            final String propName = convert2PropName(argProp);

            // get the desired property value from cmdline that we want to reset this property with
            // this value is the rhs of the = sign

            final int equalsAt = argProp.indexOf('=');
            final String propValue = argProp.substring(equalsAt + 1);

            if (null == propValue || "".equals(propValue)) {
                throw new Exception("argument: " + argProp + " not valid");
            }

            defaultProperties.setProperty(propName, propValue);
        }

    }

    // parse the property argument and convert from format
    //   --foo.bar...=value
    //   to
    //   format foo.bar...=value
    // also check if it is a valid type of property
    private static String convert2PropName(String argProp) throws Exception {

        //System.out.println("argProp: " + argProp);

        if (!argProp.startsWith("--")) {
            throw new Exception("argument: " + argProp + " does not start with --");
        }
        if (argProp.indexOf("=") == -1) {
            throw new Exception("argument: " + argProp + " does not contain an = sign");
        }

        // remove hyphens
        String argProperty = argProp.substring(2);


        if (null == argProperty || "".equals(argProperty))
            throw new Exception("argument: " + argProp + " not valid");

        // get the lhs of =, the property name
        String argPropName = argProperty.split("=")[0];


        if (null == argPropName || "".equals(argPropName))
            throw new Exception("argument: " + argProp + " not valid");

        // get the rhs of =, the property value
        int equalsAt = argProperty.indexOf('=');
        String argPropValue = argProperty.substring(equalsAt + 1);

        if (null == argPropValue || "".equals(argPropValue))
            throw new Exception("argument: " + argProp + " not valid");

        //System.out.println("argPropName: " + argPropName);

        // ensure this type of prop is one that we will handle
        //String[] dotSplit = argPropName.split("\\.");
        //String propType = dotSplit[0];

        //if (Arrays.binarySearch(PREFS_ALLOWED_PROP_TYPES, propType) < 0) {
            //StringBuffer allowedProps = new StringBuffer();
            //for (int i = 0; i < PREFS_ALLOWED_PROP_TYPES.length; i++) {
                //allowedProps.append(PREFS_ALLOWED_PROP_TYPES[i]);
                //allowedProps.append(" ");
            //}
            //throw new Exception("argument: " + argProp + " not allowed, " +
                    //allowedProps.toString());
        //}
        return argPropName;

    }

    public static String forwardSlashPath(String input) {
        if (System.getProperties().get("file.separator").equals("\\")) {
            return input.replaceAll("\\\\", "/");
        }
        return input;
    }

    public static String backSlashPath(String input) {
        if (System.getProperties().get("file.separator").equals("\\")) {
            return input.replaceAll(Pattern.quote("/"), Matcher.quoteReplacement("\\"));
        }
        return input;
    }
}
