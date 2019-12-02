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

package org.rundeck.example.plugin;

import com.dtolabs.rundeck.core.jobs.*;
import com.dtolabs.rundeck.core.plugins.JobLifecyclePluginException;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginMetadata;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.jobs.JobOptionImpl;
import com.dtolabs.rundeck.plugins.project.JobLifecyclePlugin;

import java.util.TreeSet;

@Plugin(name = "example", service = ServiceNameConstants.JobLifecycle)
@PluginDescription(title = "Example Job lifecycle Plugin",
                   description = "Adds a new job option before save, and requires a certain value before execution.\n\n"
                                 + "Configure the optionName and secretValue in project configuration.")
@PluginMetadata(key = "faicon", value = "fish")
public class ExampleJobLifecyclePlugin
        implements JobLifecyclePlugin
{

    @PluginProperty(title = "Option Name",
                    description = "Enforce specified option is created",
                    required = true,
                    scope = PropertyScope.Project)
    String optionName;

    @PluginProperty(title = "Secret Value",
                    description = "Enforce option value matches secret value to run a job",
                    required = true,
                    scope = PropertyScope.Project)
    String secretValue;

    @Override
    public JobLifecycleStatus beforeJobExecution(JobPreExecutionEvent event) throws JobLifecyclePluginException {

        String optVal = event.getOptionsValues() != null ? event.getOptionsValues().get(optionName) : null;
        if (!secretValue.equals(optVal)) {
            return JobLifecycleStatusImpl.builder().successful(false).errorMessage(String.format(
                    "The option %s value was incorrect!",
                    optionName
            )).build();
        }
        return null;
    }


    @Override
    public JobLifecycleStatus beforeSaveJob(JobPersistEvent event) throws JobLifecyclePluginException {
        TreeSet<JobOption> newOptions = new TreeSet<>();
        if (event.getOptions() != null) {
            for (JobOption option : event.getOptions()) {
                if (option.getName().equals(optionName)) {
                    //ok
                    return null;
                }
            }
            newOptions.addAll(event.getOptions());
        }
        //create new option
        newOptions.add(
                JobOptionImpl.builder().name(optionName).required(true).build()
        );
        return JobLifecycleStatusImpl.builder().options(newOptions).useNewValues(true).successful(true).build();
    }

}
