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
import org.mortbay.jetty.plus.jaas.JAASPrincipal;
import org.mortbay.jetty.plus.jaas.JAASRole;
import org.mortbay.jetty.plus.jaas.callback.ObjectCallback;
//import org.mortbay.jaas.JAASPrincipal;
//import org.mortbay.jaas.JAASRole;
//import org.mortbay.jaas.callback.ObjectCallback;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * AbstractLoginModule
 *
 * Abstract base class for all LoginModules. Subclasses should 
 * just need to implement getUserInfo method.
 *
 */
public abstract class AbstractLoginModule implements LoginModule
{
    public static final Logger log = Logger.getLogger(AbstractLoginModule.class);
    protected CallbackHandler callbackHandler;
    
    private boolean authState = false;
    private boolean commitState = false;
    protected JAASUserInfo currentUser;
    private Subject subject;
    
    
    
    public class JAASUserInfo
    {
        private UserInfo user;
        private Principal principal;
        private List roles;
        
      
        
        public JAASUserInfo (UserInfo u)
        {
            setUserInfo(u);
        }
        
        public String getUserName ()
        {
            return this.user.getUserName();
        }
        
        public Principal getPrincipal()
        {
            return this.principal;
        }
        
        public void setUserInfo (UserInfo u)
        {
            this.user = u;
            this.principal = new JAASPrincipal(u.getUserName());
            this.roles = new ArrayList();
            if (u.getRoleNames() != null)
            {
                Iterator itor = u.getRoleNames().iterator();
                while (itor.hasNext())
                    this.roles.add(new JAASRole((String)itor.next()));
            }
        }
        
        
        public void setJAASInfo (Subject subject)
        {
            subject.getPrincipals().add(this.principal);
            subject.getPrivateCredentials().add(this.user.getCredential());
            subject.getPrincipals().addAll(roles);
        }
        
        public void unsetJAASInfo (Subject subject)
        {
            subject.getPrincipals().remove(this.principal);
            subject.getPrivateCredentials().remove(this.user.getCredential());
            subject.getPrincipals().removeAll(this.roles);
        }
        
        public boolean checkCredential (Object suppliedCredential)
        {
            return this.user.checkCredential(suppliedCredential);
        }
    }
    
    
    
    public Subject getSubject ()
    {
        return this.subject;
    }
    
    public void setSubject (Subject s)
    {
        this.subject = s;
    }
    
    public JAASUserInfo getCurrentUser()
    {
        return this.currentUser;
    }
    
    public void setCurrentUser (JAASUserInfo u)
    {
        this.currentUser = u;
    }
    
    public CallbackHandler getCallbackHandler()
    {
        return this.callbackHandler;
    }
    
    public void setCallbackHandler(CallbackHandler h)
    {
        this.callbackHandler = h; 
    }
    
    public boolean isAuthenticated()
    {
        return this.authState;
    }
    
    public boolean isCommitted ()
    {
        return this.commitState;
    }
    
    public void setAuthenticated (boolean authState)
    {
        this.authState = authState;
    }
    
    public void setCommitted (boolean commitState)
    {
        this.commitState = commitState;
    }
    /** 
     * @see javax.security.auth.spi.LoginModule#abort()
     * @throws LoginException
     */
    public boolean abort() throws LoginException
    {
        this.currentUser = null;
        return (isAuthenticated() && isCommitted());
    }

    /** 
     * @see javax.security.auth.spi.LoginModule#commit()
     * @return
     * @throws LoginException
     */
    public boolean commit() throws LoginException
    {

        if (!isAuthenticated())
        {
            currentUser = null;
            setCommitted(false);
            return false;
        }
        
        setCommitted(true);
        currentUser.setJAASInfo(subject);
        return true;
    }

    
    public Callback[] configureCallbacks ()
    {
     
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("Enter user name");
        callbacks[1] = new ObjectCallback();
        return callbacks;
    }
    
    
    
    public abstract UserInfo getUserInfo (String username) throws Exception;
    
    
    
    /** 
     * @see javax.security.auth.spi.LoginModule#login()
     * @return
     * @throws LoginException
     */
    public boolean login() throws LoginException {
        log.debug("calling login");
        try {
            if (callbackHandler == null) {
                throw new LoginException("No callback handler");
            }

            Callback[] callbacks = configureCallbacks();
            callbackHandler.handle(callbacks);

            String webUserName = ((NameCallback) callbacks[0]).getName();
            Object webCredential = ((ObjectCallback) callbacks[1]).getObject();

            if ((webUserName == null) || (webCredential == null)) {
                setAuthenticated(false);
                return isAuthenticated();
            }

            UserInfo userInfo = getUserInfo(webUserName);

            if (userInfo == null) {
                setAuthenticated(false);
                return isAuthenticated();
            }

            currentUser = new JAASUserInfo(userInfo);
            setAuthenticated(currentUser.checkCredential(webCredential));
            return isAuthenticated();
        }
        catch (IOException e) {
            log.error(e);
            throw new LoginException(e.toString());
        }
        catch (UnsupportedCallbackException e) {
            log.error(e);
            throw new LoginException(e.toString());
        }
        catch (Exception e) {
            log.error(e);
            throw new LoginException(e.toString());
        }
    }

    /** 
     * @see javax.security.auth.spi.LoginModule#logout()
     * @return
     * @throws LoginException
     */
    public boolean logout() throws LoginException
    {
        this.currentUser.unsetJAASInfo(this.subject);
        return true;
    }

    /** 
     * @see javax.security.auth.spi.LoginModule#initialize(javax.security.auth.Subject, javax.security.auth.callback.CallbackHandler, java.util.Map, java.util.Map)
     * @param subject
     * @param callbackHandler
     * @param sharedState
     * @param options
     */
    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map sharedState, Map options)
    {
        this.callbackHandler = callbackHandler;
        this.subject = subject;
    }

}
