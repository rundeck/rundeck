/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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
package com.dtolabs.rundeck.core.config;

import lombok.Data;

import java.util.Map;

@Data
public class RundeckConfigBase {

    String executionMode;
    String projectsStorageType;

    Map<String,Object> mail;  //mail is a very dynamic config
    Map<String,Object> storage;  //config for the storage tree
    Map<String,Object> clusterMode;  //config for clustering
    Map<String,Object> pagination; //subproperty contains 'default' which would be an invalid java property name;

    Config config;

    RundeckSecurityConfig security;
    RundeckNodeServiceConfig nodeService;
    RundeckProjectServiceConfig projectService;
    RundeckProjectManagerServiceConfig projectManagerService;
    RundeckLogFileStorageServiceConfig logFileStorageService;
    RundeckReportServiceConfig reportService;
    RepositoryConfig repository;
    RundeckLog4jConfig log4j;
    RundeckLogConfig log;
    RundeckGuiConfig gui;
    RundeckFeatureConfig feature;
    RundeckWebConfig web;
    RundeckAjaxConfig ajax;
    RundeckMetricsConfig metrics;
    RundeckExecutionConfig execution;

    @Data
    public static class RundeckExecutionConfig {

        RetryConfig finalize;
        RetryConfig status;
        ExecutionLogs logs;

        @Data
        public static class RetryConfig {
            Integer retryMax;
            Integer retryDelay;
        }

        @Data
        public static class ExecutionLogs {
            String fileStoragePlugin;
            LogFileStorage fileStorage;
        }

        @Data
        public static class LogFileStorage {
            Boolean cancelOnStorageFailure;
            Integer storageRetryDelay;
            Integer storageRetryCount;
            Integer retrievalRetryDelay;
            Integer retrievalRetryCount;
            Integer remotePendingDelay;

            ConcurrencyLimit storageTasks;
            ConcurrencyLimit retrievalTasks;

            Checkpoint checkpoint;
        }

        @Data
        public static class ConcurrencyLimit {
            Integer concurrencyLimit;
        }
        @Data
        public static class Checkpoint {
            Time time;
            FileSize fileSize;
        }

        @Data
        public static class Time {
            String interval; //time duration e.g. 30s
            String minimum; //time duration e.g. 30s
        }

        @Data
        public static class FileSize {
            Integer minimum;
            Integer increment;
        }
    }

    @Data
    public static class RundeckMetricsConfig {
        Boolean enabled;
        Boolean jmxEnabled;
        Boolean requestFilterEnabled;
        String servletUrlPattern;
        Datasource datasource;

        @Data
        public static class Api {
            Boolean enabled;
            ApiEnabled metrics;
            ApiEnabled ping;
            ApiEnabled threads;
            ApiEnabled healthcheck;
        }

        @Data
        public static class ApiEnabled {
            Boolean enabled;
        }

        @Data
        public static class Datasource {
            Timeout health;
            Timeout ping;
        }

        @Data
        public static class Timeout {
            Integer timeout;
        }
    }

    @Data
    public static class RundeckProjectServiceConfig {
        ProjectExportCache projectExportCache;

        @Data
        public static class ProjectExportCache {
            String spec;
        }
    }

    @Data
    public static class RundeckProjectManagerServiceConfig {
        ProjectCache projectCache;

        @Data
        public static class ProjectCache {
            String spec;
        }
    }

    @Data
    public static class RundeckLogFileStorageServiceConfig {
        Startup startup;
        ResumeIncomplete resumeIncomplete;

        @Data
        public static class Startup {
            String resumeMode;
        }

        @Data
        public static class ResumeIncomplete {
            String strategy;
        }
    }

    @Data
    public static class RundeckReportServiceConfig {
        Startup startup;

        @Data
        public static class Startup {
            Boolean cleanupReports;
        }
    }

    @Data
    public static class RundeckLog4jConfig {

        Log4jConfig config;

        @Data
        public static class Log4jConfig {
            String file;
        }
    }

    @Data
    public static class RundeckLogConfig {
        String dir;
    }

    @Data
    public static class RundeckWebConfig {

        Cookie cookie;
        Jetty jetty;

        @Data
        public static class Cookie {
            Integer localCookieExpiration;
        }

        @Data
        public static class Jetty {
            Servlet servlet;
        }

        @Data
        public static class Servlet {
            Map<String,Object> initParams;
        }
    }

    @Data
    public static class RundeckNodeServiceConfig {

        NodeCache nodeCache;

        @Data
        public static class NodeCache {
            Boolean firstLoadAsynch;
            Boolean enabled;
            String spec;
        }
    }

    @Data
    public static class Config {
        String location;  //can contain the customized location of rundeck config dir
        Map<String,Object> storage; //config for the config storage tree
    }

    @Data
    public static class StorageProviderConfig {
        String type;
        String path;
        Boolean removePathPrefix;
        Map<String,Object> config;
    }

    @Data
    public static class RepositoryConfig {

        Artifacts artifacts;
        Plugins plugins;

        @Data
        public static class Artifacts {
            Map<String,StorageProviderConfig> provider;
        }

        @Data
        public static class Plugins {
            Map<String,StorageProviderConfig> provider;
        }

    }

    @Data
    public static class RundeckFeatureConfig {

        Boolean enableAll;
        Repository repository = new Repository();
        Enabled optionValuesPlugin = new Enabled();
        Enabled webhooks = new Enabled();
        Enabled emailCSSFramework = new Enabled();
        Enabled enhancedNodes = new Enabled();
        Enabled cleanExecutionsHistoryJob = new Enabled();
        Enabled workflowDynamicStepSummaryGUI = new Enabled();
        Enabled legacyProjectNodesUi = new Enabled();
        Enabled jobLifecyclePlugin = new Enabled();
        Enabled executionLifecyclePlugin = new Enabled();

        @Data
        public static class Enabled {
            Boolean enabled;

            public Enabled() { this(false); }
            public Enabled(final Boolean enabled) {
                this.enabled = enabled;
            }
        }

        @Data
        public static class Repository {
            Boolean enabled = true;
            Boolean syncOnBootstrap = false;
            RepositoryInstalledPlugins installedPlugins;
        }

        @Data
        public static class RepositoryInstalledPlugins {
            String storageTreePath;
        }
    }

    @Data
    public static class RundeckSecurityConfig {

        Integer maxSessions;
        Boolean enforceMaxSessions;
        Boolean useHMacRequestTokens;
        Boolean syncLdapUser;
        String requiredRole;
        String jaasRolePrefix;

        ApiCookieAccess apiCookieAccess;
        Authorization authorization;
        Csrf csrf;
        Ldap ldap;
        HttpHeaders headers;

        @Data
        public static class Ldap {
            String bindPassword;
        }

        @Data
        public static class Authorization {
            ContainerPrinciple containerPrinciple;
            Container container;
            Preauthenticated preauthenticated;
        }
        @Data
        public static class ApiCookieAccess {
            private Boolean enabled;
        }
        @Data
        public static class ContainerPrinciple {
            private Boolean enabled;
        }
        @Data
        public static class Container {
            private Boolean enabled;
        }
        @Data
        public static class Preauthenticated {
            private Boolean enabled;
            private String attributeName;
            private String userNameHeader;
            private String userRolesHeader;
            private String delimiter;
            private String redirectUrl;
            private Boolean redirectLogout;
        }
        @Data
        public static class Csrf {
            CsrfReferer referer;
        }
        @Data
        public static class CsrfReferer {
            String filterMethod;
            Boolean allowApi;
            Boolean requireHttps;
        }
        @Data
        public static class HttpHeaders {
            Boolean enabled;
            Map<String,Object> provider; //very complex structure
        }
    }

    @Data
    public static class RundeckGuiConfig {
        String instanceName;
        String startpage;
        String logo;
        String logoHires;
        Boolean clusterIdentityInHeader;
        Boolean clusterIdentityInFooter;

        Execution execution;
        PaginateJobs paginatejobs;
        StaticUserResources staticUserResources;
        Login login;
        Job job;

        @Data
        public static class Job {
            Description description;
        }
        @Data
        public static class Description {
            Boolean disableHTML;
        }
        @Data
        public static class Execution {
            Tail tail;
            Logs logs;
        }
        @Data
        public static class Logs {
            Compacted compacted;
        }
        @Data
        public static class Compacted {
            Boolean disabled;
        }
        @Data
        public static class Tail {
            Map<String,Integer> lines;
        }

        @Data
        public static class PaginateJobs {
            Boolean enabled;
            String maxPerPage;
        }

        @Data
        public static class StaticUserResources {
            Boolean enabled;
        }
        @Data
        public static class Login {
            String welcome;
            String welcomeHtml;
        }
    }

    @Data
    public static class RundeckAjaxConfig {
        String compression;
        ExecutionState executionState;

        @Data
        public static class ExecutionState {
            Compression compression;
        }
        @Data
        public static class Compression {
            Integer nodeThreshold;
        }
    }
}
