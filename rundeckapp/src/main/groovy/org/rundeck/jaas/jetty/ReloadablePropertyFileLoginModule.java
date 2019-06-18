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
package org.rundeck.jaas.jetty;

import org.eclipse.jetty.jaas.spi.AbstractLoginModule;
import org.eclipse.jetty.jaas.spi.UserInfo;
import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.security.PropertyUserStore;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ReloadablePropertyFileLoginModule extends AbstractLoginModule {
        public static final String DEFAULT_FILENAME = "realm.properties";

        private static final Logger LOG = LoggerFactory.getLogger(ReloadablePropertyFileLoginModule.class);

        private static ConcurrentHashMap<String, PropertyUserStore> _propertyUserStores = new ConcurrentHashMap<String, PropertyUserStore>();

        private int _refreshInterval = 0;
        private String _filename = DEFAULT_FILENAME;
        private boolean _reloadEnabled = true;
        private boolean _debug = false;


        /**
         * Read contents of the configured property file.
         *
         * @see javax.security.auth.spi.LoginModule#initialize(javax.security.auth.Subject, javax.security.auth.callback.CallbackHandler, java.util.Map,
         *      java.util.Map)
         *
         * @param subject the subject
         * @param callbackHandler the callback handler
         * @param sharedState the shared state map
         * @param options the options map
         */
        @Override
        public void initialize(Subject subject, CallbackHandler callbackHandler, Map< String, ?> sharedState, Map < String, ?> options)
        {
            super.initialize(subject,callbackHandler,sharedState,options);
            setupPropertyUserStore(options);
        }

        private void setupPropertyUserStore(Map<String, ?> options)
        {
            parseConfig(options);

            if (_propertyUserStores.get(_filename) == null)
            {
                PropertyUserStore propertyUserStore = new PropertyUserStore();
                propertyUserStore.setHotReload(_reloadEnabled);
                propertyUserStore.setConfig(_filename);

                PropertyUserStore prev = _propertyUserStores.putIfAbsent(_filename, propertyUserStore);
                if (prev == null)
                {
                    debug("ReloadablePropertyFileLoginModule: setupPropertyUserStore: Starting new PropertyUserStore. PropertiesFile: " + _filename + " refreshInterval: " + _refreshInterval);

                    try
                    {
                        propertyUserStore.start();
                    }
                    catch (Exception e)
                    {
                        LOG.warn("Exception while starting propertyUserStore: ",e);
                    }
                }
            }
        }

        private void parseConfig(Map<String, ?> options)
        {
            String tmp = (String)options.get("file");
            _filename = (tmp == null? DEFAULT_FILENAME : tmp);
            tmp = (String)options.get("refreshInterval");
            _refreshInterval = (tmp == null?_refreshInterval:Integer.parseInt(tmp));
            _debug = options.containsKey("debug") && options.get("debug").equals("true");
        }

        /**
         *
         *
         * @param userName the user name
         * @throws Exception if unable to get the user information
         */
        @Override
        public UserInfo getUserInfo(String userName) throws Exception
        {
            PropertyUserStore propertyUserStore = _propertyUserStores.get(_filename);
            if (propertyUserStore == null)
                throw new IllegalStateException("PropertyUserStore should never be null here!");

            debug("ReloadablePropertyFileLoginModule: Checking PropertyUserStore "+_filename+" for "+userName);
            UserIdentity userIdentity = propertyUserStore.getUserIdentity(userName);
            if (userIdentity==null)
                return null;

            //TODO in future versions change the impl of PropertyUserStore so its not
            //storing Subjects etc, just UserInfo
            Set<Principal> principals = userIdentity.getSubject().getPrincipals();

            List<String> roles = new ArrayList<String>();

            for ( Principal principal : principals )
            {
                if(principal instanceof AbstractLoginService.RolePrincipal) {
                    roles.add(principal.getName());
                }
            }

            Credential credential = (Credential)userIdentity.getSubject().getPrivateCredentials().iterator().next();
            debug("ReloadablePropertyFileLoginModule: Found: " + userName + " in PropertyUserStore "+_filename);
            return new UserInfo(userName, credential, roles);
        }

    public boolean isReloadEnabled() {
        return _reloadEnabled;
    }

    public void setReloadEnabled(final boolean enabled) {
        this._reloadEnabled = enabled;
    }

    public boolean isDebug() {
        return _debug;
    }

    /**
     * Default behavior to emit to System.err
     * @param message
     */
    protected void debug(String message) {
        if(_debug) {
            System.err.println(message);
        }
    }

}
