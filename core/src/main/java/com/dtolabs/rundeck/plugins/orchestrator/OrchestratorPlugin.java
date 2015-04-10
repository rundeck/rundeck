/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.dtolabs.rundeck.plugins.orchestrator;

import java.util.Collection;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;

/**
 * OrchestratorPlugin
 * 
 * Plugin that lets the order of execution be decided. Can also pause if no
 * machine is in a ready state to be updated
 *
 * @author Ashley Taylor
 */
public interface OrchestratorPlugin {

    /**
     * @param context the execution context
     * @param nodes the collection of target nodes
     * @return an Orchestrator instance
     */
    public Orchestrator createOrchestrator(StepExecutionContext context, Collection<INodeEntry> nodes);

}
