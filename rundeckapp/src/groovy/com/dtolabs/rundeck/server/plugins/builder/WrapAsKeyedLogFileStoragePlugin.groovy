/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.dtolabs.rundeck.server.plugins.builder

import com.dtolabs.rundeck.core.logging.LogFileStorageException
import com.dtolabs.rundeck.plugins.logging.KeyedLogFileStoragePlugin
import com.dtolabs.rundeck.plugins.logging.LogFileStoragePlugin

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 12/4/13
 * Time: 2:26 PM
 */
class WrapAsKeyedLogFileStoragePlugin implements KeyedLogFileStoragePlugin {
    private LogFileStoragePlugin plugin

    WrapAsKeyedLogFileStoragePlugin(LogFileStoragePlugin plugin) {
        this.plugin = plugin
    }

    @Override
    boolean isAvailable(String key) throws LogFileStorageException {
        if(null!=key){
            throw new UnsupportedOperationException("isAvailable(filekey) unsupported by this plugin")
        }
        return plugin.isAvailable()
    }

    @Override
    boolean store(String key, InputStream stream, long length, Date lastModified) throws IOException, LogFileStorageException {
        if (null != key) {
            throw new UnsupportedOperationException("store(filekey) unsupported by this plugin")
        }
        return plugin.store(stream,length,lastModified)
    }

    @Override
    boolean retrieve(String key, OutputStream stream) throws IOException, LogFileStorageException {
        if (null != key) {
            throw new UnsupportedOperationException("retrieve(filekey) unsupported by this plugin")
        }
        return plugin.retrieve(stream)
    }

    @Override
    void initialize(Map<String, ? extends Object> context) {
        plugin.initialize(context)
    }

    @Override
    boolean isAvailable() throws LogFileStorageException {
        return plugin.isAvailable()
    }

    @Override
    boolean store(InputStream stream, long length, Date lastModified) throws IOException, LogFileStorageException {
        return plugin.store(stream,length,lastModified)
    }

    @Override
    boolean retrieve(OutputStream stream) throws IOException, LogFileStorageException {
        return plugin.retrieve(stream)
    }
}
