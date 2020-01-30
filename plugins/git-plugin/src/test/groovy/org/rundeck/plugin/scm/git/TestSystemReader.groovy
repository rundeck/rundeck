/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.plugin.scm.git

import org.eclipse.jgit.errors.ConfigInvalidException
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.StoredConfig
import org.eclipse.jgit.storage.file.FileBasedConfig
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.util.SystemReader

class TestSystemReader extends SystemReader {
    String hostname = 'rundeck-test'
    private static final class MockConfig extends FileBasedConfig {
        private MockConfig(File cfgLocation, FS fs) {
            super(cfgLocation, fs);
        }

        @Override
        public void load() throws IOException, ConfigInvalidException {
            // Do nothing
        }

        @Override
        public void save() throws IOException {
            // Do nothing
        }

        @Override
        public boolean isOutdated() {
            return false;
        }

        @Override
        public String toString() {
            return "MockConfig";
        }
    }

    FileBasedConfig userGitConfig;
    FileBasedConfig jgitConfig;
    FileBasedConfig systemGitConfig;
    final Map<String, String> values = new HashMap<>();

    TestSystemReader() {
        userGitConfig = new MockConfig(null, null);
        jgitConfig = new MockConfig(null, null);
        systemGitConfig = new MockConfig(null, null);
        setCurrentPlatform()
    }

    @Override
    StoredConfig getJGitConfig() throws ConfigInvalidException, IOException {
        return jgitConfig
    }

    @Override
    StoredConfig getUserConfig() throws ConfigInvalidException, IOException {
        return userGitConfig
    }

    @Override
    StoredConfig getSystemConfig() throws ConfigInvalidException, IOException {
        return systemGitConfig
    }

    @Override
    String getenv(final String variable) {
        return null
    }

    @Override
    FileBasedConfig openUserConfig(final Config parent, final FS fs) {
        return userGitConfig
    }

    @Override
    FileBasedConfig openSystemConfig(final Config parent, final FS fs) {
        return systemGitConfig
    }

    @Override
    FileBasedConfig openJGitConfig(final Config parent, final FS fs) {
        return jgitConfig
    }

    @Override
    long getCurrentTime() {
        System.currentTimeMillis()
    }

    @Override
    int getTimezone(final long when) {
        return getTimeZone().getOffset(when) / (60 * 1000);
    }

    @Override
    String getProperty(final String propertyName) {
        values.get(propertyName)
    }

    /**
     * Assign some properties for the currently executing platform
     */
    public void setCurrentPlatform() {
//        resetOsNames();
        values.put("os.name", System.getProperty("os.name"));
        values.put("file.separator", System.getProperty("file.separator"));
        values.put("path.separator", System.getProperty("path.separator"));
        values.put("line.separator", System.getProperty("line.separator"));
    }
}
