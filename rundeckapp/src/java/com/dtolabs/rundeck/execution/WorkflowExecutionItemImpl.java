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

/*
 * WorkflowExecutionItemImpl.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: Mar 16, 2010 10:44:41 AM
 * $Id$
 */
package com.dtolabs.rundeck.execution;

import java.util.Map;

import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.utils.NodeSet;
import com.dtolabs.rundeck.core.utils.NodeSet.Exclude;
import com.dtolabs.rundeck.core.utils.NodeSet.Include;

/**
 * WorkflowExecutionItemImpl is ...
 * 
 * @author Greg Schueler <a
 *         href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class WorkflowExecutionItemImpl implements WorkflowExecutionItem {
	final private IWorkflow workflow;
	final private NodeSet nodeSet;
	final private String user;
	final private int loglevel;
	private boolean keepgoing = false;
	final private String project;
	final private Map<String, Map<String, String>> dataContext;

	public WorkflowExecutionItemImpl(final IWorkflow workflow,
			final NodeSet nodeSet, final String user, final int loglevel,
			final String project,
			final Map<String, Map<String, String>> dataContext) {
		this.workflow = workflow;
		this.nodeSet = nodeSet;
		this.user = user;
		this.loglevel = loglevel;
		this.project = project;
		this.dataContext = dataContext;
	}

	public IWorkflow getWorkflow() {
		return workflow;
	}

	public NodeSet getNodeSet() {
		NodeSet result = nodeSet; // TODO: will evtl. have to clone the object
									// to avoid overwriting during first
									// execution

		Include includes = result.getInclude(); // TODO: find a generic way of
												// replacing everything in
												// includes and excludes
		if (includes.getName() != null) {
			includes.setName(DataContextUtils.replaceDataReferences(
					includes.getName(), getDataContext()));
		}
		if (includes.getTags() != null) {
			includes.setTags(DataContextUtils.replaceDataReferences(
					includes.getTags(), getDataContext()));
		}

		Exclude excludes = result.getExclude();
		if (excludes.getName() != null) {
			excludes.setName(DataContextUtils.replaceDataReferences(
					excludes.getName(), getDataContext()));
		}
		if (excludes.getTags() != null) {
			excludes.setTags(DataContextUtils.replaceDataReferences(
					excludes.getTags(), getDataContext()));
		}

		return result;
	}

	public String getUser() {
		return user;
	}

	public int getLoglevel() {
		return loglevel;
	}

	public boolean isKeepgoing() {
		return keepgoing;
	}

	public void setKeepgoing(boolean keepgoing) {
		this.keepgoing = keepgoing;
	}

	public String getProject() {
		return project;
	}

	public Map<String, Map<String, String>> getDataContext() {
		return dataContext;
	}
}
