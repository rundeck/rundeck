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

/*
 * ServiceNameConstants.java
 *
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 11/28/12 3:16 PM
 *
 */
package com.dtolabs.rundeck.plugins;

/**
 * ServiceNameConstants contains the names of defined services
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ServiceNameConstants {
    /**
     * Service Name of the Remote Script Node Step service
     */
    public static final String RemoteScriptNodeStep = "RemoteScriptNodeStep";
    /**
     * Service name of the Workflow Node Step service
     */
    public static final String WorkflowNodeStep     = "WorkflowNodeStep";
    /**
     * Service name of the Workflow Step service
     */
    public static final String WorkflowStep         = "WorkflowStep";
    public static final String WorkflowExecution    = "WorkflowExecution";
    public static final String WorkflowStrategy     = "WorkflowStrategy";

    public static final String NodeExecutor            = "NodeExecutor";
    public static final String FileCopier              = "FileCopier";
    public static final String NodeDispatcher          = "NodeDispatcher";
    public static final String ResourceModelSource     = "ResourceModelSource";
    public static final String ResourceFormatParser    = "ResourceFormatParser";
    public static final String ResourceFormatGenerator = "ResourceFormatGenerator";
    public static final String Notification            = "Notification";
    public static final String StreamingLogReader      = "StreamingLogReader";
    public static final String StreamingLogWriter      = "StreamingLogWriter";
    public static final String LogFileStorage          = "LogFileStorage";
    public static final String ExecutionFileStorage    = "ExecutionFileStorage";
    public static final String StorageConverter        = "StorageConverter";
    public static final String Storage                 = "Storage";
    public static final String Orchestrator            = "Orchestrator";
    public static final String ScmExport               = "ScmExport";
    public static final String ScmImport               = "ScmImport";
    public static final String UI                      = "UI";
    public static final String LogFilter               = "LogFilter";
    public static final String ContentConverter        = "ContentConverter";
    public static final String TourLoader              = "TourLoader";
    public static final String FileUpload              = "FileUpload";
    public static final String OptionValues            = "OptionValues";
    public static final String NodeEnhancer            = "NodeEnhancer";
    public static final String UserGroupSource         = "UserGroupSource";
    public static final String JobPlugin               = "JobPlugin";
    public static final String PasswordUtilityEncrypter = "PasswordUtilityEncrypter";
    public static final String AuditEventListener      = "AuditEventListener";
}
