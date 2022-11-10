/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.rundeck.plugins;

import com.dtolabs.rundeck.core.encrypter.PasswordUtilityEncrypterPlugin;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher;
import com.dtolabs.rundeck.core.execution.service.FileCopier;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowStrategy;
import com.dtolabs.rundeck.core.plugins.PluggableProviderService;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceFactory;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGenerator;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParser;
import com.dtolabs.rundeck.plugins.audit.AuditEventListenerPlugin;
import com.dtolabs.rundeck.plugins.config.PluginGroup;
import com.dtolabs.rundeck.plugins.file.FileUploadPlugin;
import com.dtolabs.rundeck.plugins.jobs.ExecutionLifecyclePlugin;
import com.dtolabs.rundeck.plugins.logging.*;
import com.dtolabs.rundeck.plugins.logs.ContentConverterPlugin;
import com.dtolabs.rundeck.plugins.nodes.NodeEnhancerPlugin;
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;
import com.dtolabs.rundeck.plugins.option.OptionValuesPlugin;
import com.dtolabs.rundeck.plugins.orchestrator.OrchestratorPlugin;
import com.dtolabs.rundeck.plugins.project.JobLifecyclePlugin;
import com.dtolabs.rundeck.plugins.rundeck.UIPlugin;
import com.dtolabs.rundeck.plugins.scm.ScmExportPluginFactory;
import com.dtolabs.rundeck.plugins.scm.ScmImportPluginFactory;
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin;
import com.dtolabs.rundeck.plugins.step.RemoteScriptNodeStepPlugin;
import com.dtolabs.rundeck.plugins.step.StepPlugin;
import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin;
import com.dtolabs.rundeck.plugins.storage.StoragePlugin;
import com.dtolabs.rundeck.plugins.tours.TourLoaderPlugin;
import com.dtolabs.rundeck.plugins.user.groups.UserGroupSourcePlugin;
import com.dtolabs.rundeck.plugins.webhook.WebhookEventPlugin;
import org.rundeck.core.plugins.PluginProviderServices;
import org.rundeck.core.plugins.PluginTypes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Static list of java interfaces associated with plugin service names, see {@link ServiceNameConstants}
 */
public class ServiceTypes {
    /**
     * Map of Service name to Class
     */
    public static final Map<String, Class<?>> EXECUTION_TYPES;
    public static final Map<String, Class<?>> TYPES;

    static {
        HashMap<String, Class<?>> map = new HashMap<>();
        map.put(ServiceNameConstants.RemoteScriptNodeStep, RemoteScriptNodeStepPlugin.class);
        map.put(ServiceNameConstants.WorkflowNodeStep, NodeStepPlugin.class);
        map.put(ServiceNameConstants.WorkflowStep, StepPlugin.class);
        map.put(ServiceNameConstants.NodeExecutor, NodeExecutor.class);
        map.put(ServiceNameConstants.FileCopier, FileCopier.class);
        map.put(ServiceNameConstants.NodeDispatcher, NodeDispatcher.class);
        map.put(ServiceNameConstants.ResourceModelSource, ResourceModelSourceFactory.class);
        map.put(ServiceNameConstants.ResourceFormatParser, ResourceFormatParser.class);
        map.put(ServiceNameConstants.ResourceFormatGenerator, ResourceFormatGenerator.class);

        HashMap<String, Class<?>> map2 = new HashMap<>(map);
        map2.put(ServiceNameConstants.Notification, NotificationPlugin.class);
        map2.put(ServiceNameConstants.StreamingLogReader, StreamingLogReaderPlugin.class);
        map2.put(ServiceNameConstants.StreamingLogWriter, StreamingLogWriterPlugin.class);
        map2.put(ServiceNameConstants.LogFileStorage, LogFileStoragePlugin.class);
        map2.put(ServiceNameConstants.ExecutionFileStorage, ExecutionFileStoragePlugin.class);
        map2.put(ServiceNameConstants.StorageConverter, StorageConverterPlugin.class);
        map2.put(ServiceNameConstants.Storage, StoragePlugin.class);
        map2.put(ServiceNameConstants.Orchestrator, OrchestratorPlugin.class);
        map2.put(ServiceNameConstants.ScmExport, ScmExportPluginFactory.class);
        map2.put(ServiceNameConstants.ScmImport, ScmImportPluginFactory.class);
        map2.put(ServiceNameConstants.UI, UIPlugin.class);
        map2.put(ServiceNameConstants.LogFilter, LogFilterPlugin.class);
        map2.put(ServiceNameConstants.ContentConverter, ContentConverterPlugin.class);
        map2.put(ServiceNameConstants.TourLoader, TourLoaderPlugin.class);
        map2.put(ServiceNameConstants.FileUpload, FileUploadPlugin.class);
        map2.put(ServiceNameConstants.WorkflowStrategy, WorkflowStrategy.class);
        map2.put(ServiceNameConstants.OptionValues, OptionValuesPlugin.class);
        map2.put(ServiceNameConstants.NodeEnhancer, NodeEnhancerPlugin.class);
        map2.put(ServiceNameConstants.UserGroupSource, UserGroupSourcePlugin.class);
        map2.put(ServiceNameConstants.WebhookEvent, WebhookEventPlugin.class);
        map2.put(ServiceNameConstants.PasswordUtilityEncrypter, PasswordUtilityEncrypterPlugin.class);
        map2.put(ServiceNameConstants.ExecutionLifecycle, ExecutionLifecyclePlugin.class);
        map2.put(ServiceNameConstants.JobLifecycle, JobLifecyclePlugin.class);
        map2.put(ServiceNameConstants.AuditEventListener, AuditEventListenerPlugin.class);
        map2.put(ServiceNameConstants.PluginGroup, PluginGroup.class);

        EXECUTION_TYPES = Collections.unmodifiableMap(map);
        TYPES = Collections.unmodifiableMap(map2);
    }

    private static ServiceLoader<PluginTypes> pluginTypeServiceLoader = ServiceLoader.load(PluginTypes.class);

    private static Map<String, Class<?>> LOADED_TYPES;
    private static final Object synch = new Object();

    public static Map<String, Class<?>> getPluginTypesMap() {
        if (null == LOADED_TYPES) {
            synchronized (synch) {
                if (null == LOADED_TYPES) {
                    HashMap<String, Class<?>> map = new HashMap<>(TYPES);
                    for (PluginTypes pluginTypes : pluginTypeServiceLoader) {
                        map.putAll(pluginTypes.getPluginTypes());
                    }
                    LOADED_TYPES = Collections.unmodifiableMap(map);
                }
            }
        }
        return LOADED_TYPES;
    }

    public static Class<?> getPluginType(String name) {
        return getPluginTypesMap().get(name);
    }

    private static ServiceLoader<PluginProviderServices>
        pluginProviderServiceLoader =
        ServiceLoader.load(PluginProviderServices.class);
    private static final Object providerLoaderSync = new Object();

    /**
     * Get a pluggable service implementation for the given plugin type, if available via {@link ServiceLoader}
     *
     * @param serviceType type
     * @param loader      loader implementation
     * @param <T>
     */
    public static <T> PluggableProviderService<T> getPluginProviderService(
        Class<T> serviceType,
        String serviceName,
        ServiceProviderLoader loader
    )
    {
        synchronized (providerLoaderSync) {
            for (PluginProviderServices pluginProviderServices : pluginProviderServiceLoader) {
                if (pluginProviderServices.hasServiceFor(serviceType, serviceName)) {
                    return pluginProviderServices.getServiceProviderFor(serviceType, serviceName, loader);
                }
            }
        }
        return null;
    }
}
