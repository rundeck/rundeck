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

package com.dtolabs.rundeck.jetty.jaas;

import org.apache.log4j.Logger;
import org.mortbay.jetty.security.Credential;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;


/**
 * PropertyFileLoginModule
 *
 *
 */
public class PropertyFileLoginModule extends AbstractLoginModule
{
    public static final String DEFAULT_FILENAME = "realm.properties";
    public static final Map fileMap = new HashMap(); 
    
    private String propertyFileName;
    public static final Logger log = Logger.getLogger(PropertyFileLoginModule.class);
    

    /** 
     * Read contents of the configured property file.
     * 
     * @see javax.security.auth.spi.LoginModule#initialize(javax.security.auth.Subject, javax.security.auth.callback.CallbackHandler, java.util.Map, java.util.Map)
     * @param subject
     * @param callbackHandler
     * @param sharedState
     * @param options
     */
    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map sharedState, Map options)
    {
        super.initialize(subject, callbackHandler, sharedState, options);
        loadProperties((String)options.get("file"));
    }
    
  
    
    public void loadProperties (String filename)
    {
        File propsFile;
        
        if (filename == null)
        {
            propsFile = new File(System.getProperty("user.dir"), DEFAULT_FILENAME);
            //look for a file called realm.properties in the current directory
            //if that fails, look for a file called realm.properties in $jetty.home/etc
            if (!propsFile.exists())
                propsFile = new File(System.getProperty("jetty.home"), DEFAULT_FILENAME);
        }
        else
        {
            propsFile = new File(filename);
            if(!propsFile.exists()) {
                propsFile = new File(System.getProperty("jetty.home"), filename);
            }
        }
        
        //give up, can't find a property file to load
        if (!propsFile.exists())
        {
            log.warn("No property file found: "+propsFile.getAbsolutePath());
            throw new IllegalStateException ("No property file specified in login module configuration file");
        }
            
        
     
        try
        {
            this.propertyFileName = propsFile.getCanonicalPath();
            if (fileMap.get(propertyFileName) != null)
            {
                if (log.isDebugEnabled()) {log.debug("Properties file "+propertyFileName+" already in cache, skipping load");}
                return;
            }
            
            Map userInfoMap = new HashMap();
            Properties props = new Properties();
            props.load(new FileInputStream(propsFile));
            Iterator iter = props.entrySet().iterator();
            while(iter.hasNext())
            {
                
                Map.Entry entry = (Map.Entry)iter.next();
                String username=entry.getKey().toString().trim();
                String credentials=entry.getValue().toString().trim();
                String roles=null;
                int c=credentials.indexOf(',');
                if (c>0)
                {
                    roles=credentials.substring(c+1).trim();
                    credentials=credentials.substring(0,c).trim();
                }

                if (username!=null && username.length()>0 &&
                    credentials!=null && credentials.length()>0)
                {
                    ArrayList roleList = new ArrayList();
                    if(roles!=null && roles.length()>0)
                    {
                        StringTokenizer tok = new StringTokenizer(roles,", ");
                        
                        while (tok.hasMoreTokens())
                            roleList.add(tok.nextToken());
                    }
                    
                    userInfoMap.put(username, (new UserInfo(username, Credential.getCredential(credentials.toString()), roleList)));
                }
            }
            
            fileMap.put(propertyFileName, userInfoMap);
        }
        catch (Exception e)
        {
            log.warn("Error loading properties from file", e);
            throw new RuntimeException(e);
        }
    }

    /** 
     * Don't implement this as we want to pre-fetch all of the
     * users.
     * @see org.mortbay.jetty.plus.jaas.spi.AbstractLoginModule#lazyLoadUser(java.lang.String)
     * @param username
     * @throws Exception
     */
    public UserInfo getUserInfo (String username) throws Exception
    {
        Map userInfoMap = (Map)fileMap.get(propertyFileName);
        if (userInfoMap == null)
            return null;
        return (UserInfo)userInfoMap.get(username);
    }

}
