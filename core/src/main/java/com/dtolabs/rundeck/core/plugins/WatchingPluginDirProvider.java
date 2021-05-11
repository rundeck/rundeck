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
package com.dtolabs.rundeck.core.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class WatchingPluginDirProvider implements PluginDirProvider {

    private final File                               pluginDir;
    private final ScheduledExecutorService           executor             = Executors.newScheduledThreadPool(2);
    private final List<PluginDirChangeEventListener> changeEventListeners = new ArrayList<>();

    public WatchingPluginDirProvider(final File pluginDir) {
        this.pluginDir = pluginDir;
        if(!this.pluginDir.exists()) this.pluginDir.mkdirs();
        startFolderWatch();
    }

    void startFolderWatch() {
        executor.execute(() -> {
            try {
                WatchService watcher = FileSystems.getDefault().newWatchService();
                Path dir = pluginDir.toPath();
                try {
                    WatchKey key = dir.register(watcher,
                                                ENTRY_CREATE,
                                                ENTRY_DELETE,
                                                ENTRY_MODIFY);

                    while(!executor.isShutdown()) {
                        WatchKey tkey;
                        try {
                            tkey = watcher.take();
                        } catch(InterruptedException iex) {
                            System.out.println("plugin dir watcher was interrupted");
                            return;
                        }
                        for(WatchEvent evt : tkey.pollEvents()) {
                            WatchEvent.Kind kind = evt.kind();
                            if(kind == OVERFLOW) continue;

                            String fileName = evt.context().toString();
                            if(fileName.endsWith(".jar") || fileName.endsWith(".zip")){
                                PluginDirChangeEvent event = new PluginDirChangeEvent(fileName,PluginDirChangeType.convertFromWatchKind(kind));
                                for(PluginDirChangeEventListener listener : changeEventListeners) {
                                    listener.onDirChangeEvent(event);
                                }
                            }
                        }
                        if(!key.reset()) {
                            break;
                        }
                    }
                } catch (IOException x) {
                    x.printStackTrace();
                }

            } catch (IOException e) {
                System.err.println("Failed to start plugin dir watcher");
                e.printStackTrace();
            }
        });
    }

    @Override
    public File getPluginDir() {
        return pluginDir;
    }

    @Override
    public void registerDirChangeEventListener(final PluginDirChangeEventListener changeEventListener) {
        changeEventListeners.add(changeEventListener);
    }
}
