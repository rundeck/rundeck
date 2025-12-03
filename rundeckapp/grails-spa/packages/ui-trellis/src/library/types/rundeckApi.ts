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

/**
 * Local type definitions migrated from @rundeck/client/dist/lib/models
 * These types are used throughout the application for API responses
 */

export interface Job {
  id?: string;
  name?: string;
  group?: string;
  project?: string;
  description?: string;
  href?: string;
  permalink?: string;
  scheduled?: boolean;
  scheduleEnabled?: boolean;
  enabled?: boolean;
  averageDuration?: number;
  [key: string]: any;
}

export interface ProjectListOKResponseItem {
  name?: string;
  description?: string;
  label?: string;
  url?: string;
  [key: string]: any;
}

export type ProjectListResponse = ProjectListOKResponseItem[];

export interface ExecutionStatusJob {
  id?: string;
  name?: string;
  group?: string;
  project?: string;
  description?: string;
  href?: string;
  permalink?: string;
  [key: string]: any;
}

export interface ExecutionStatusGetResponse {
  id?: number;
  href?: string;
  permalink?: string;
  status?: string;
  customStatus?: string;
  project?: string;
  user?: string;
  serverUUID?: string;
  dateStarted?: any;
  job?: ExecutionStatusJob;
  description?: string;
  argstring?: string;
  successfulNodes?: string[];
  failedNodes?: string[];
  [key: string]: any;
}

export interface ExecutionBulkDeleteResponse {
  failed?: Array<{
    id: string;
    message?: string;
    errorCode?: string;
  }>;
  failedCount?: number;
  successCount?: number;
  requestCount?: number;
  allsuccessful?: boolean;
  [key: string]: any;
}

export interface WorkflowStep {
  exec?: string;
  type?: string;
  description?: string;
  jobref?: WorkflowStepJobref;
  jobId?: string;
  script?: string;
  scriptfile?: string;
  scripturl?: string;
  nodeStep?: string;
  workflow?: WorkflowStep[];
  [key: string]: any;
}

export interface WorkflowStepJobref {
  name?: string;
  group?: string;
  uuid?: string;
  nodeStep?: string;
  importOptions?: boolean;
  [key: string]: any;
}

export interface ExecutionOutputEntry {
  time?: string;
  absolute_time?: string;
  log?: string;
  loghtml?: string;
  level?: string;
  stepctx?: string;
  node?: string;
  metadata?: any;
  [key: string]: any;
}

export interface ExecutionOutput {
  id?: string;
  offset?: string;
  completed?: boolean;
  execCompleted?: boolean;
  hasFailedNodes?: boolean;
  execState?: string;
  lastModified?: string;
  execDuration?: number;
  percentLoaded?: number;
  totalSize?: number;
  empty?: boolean;
  error?: string;
  entries?: ExecutionOutputEntry[];
  [key: string]: any;
}

export interface ExecutionOutputGetResponse extends ExecutionOutput {
  offset: string;
  completed: boolean;
  execCompleted: boolean;
  totalSize: number;
  entries: ExecutionOutputEntry[];
}

export interface JobWorkflowGetResponse {
  id?: string;
  href?: string;
  permalink?: string;
  workflow?: WorkflowStep[];
  [key: string]: any;
}

