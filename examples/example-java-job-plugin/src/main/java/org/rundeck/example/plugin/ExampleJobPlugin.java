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

import com.dtolabs.rundeck.core.execution.JobPluginException;
import com.dtolabs.rundeck.core.jobs.JobEventStatus;
import com.dtolabs.rundeck.core.jobs.JobExecutionEvent;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.jobs.JobPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Random;

@Plugin(name = "example", service = ServiceNameConstants.JobPlugin)
@PluginDescription(title = "Example Job Plugin",
                   description = "Adds extra logging before/after the execution, and appends XKCD comic.")
public class ExampleJobPlugin
        implements JobPlugin
{
    public static final int MAGIC = 366;//number of lines in urls.txt


    @PluginProperty(title = "Enabled",
                    description = "If enabled, add some log output before/after the execution, and append XKCD comic.",
                    required = false,
                    defaultValue = "false")
    boolean enabled = false;

    @Override
    public JobEventStatus beforeJobStarts(final JobExecutionEvent event) throws JobPluginException {
        if (enabled) {
            String name = event.getUserName();
            event.getExecutionLogger().log(2, String.format("Beginning the job for %s!", name));
        }
        return null;
    }

    static String randomUrl() {

        InputStream
                resourceAsStream =
                ExampleJobPlugin.class.getClassLoader().getResourceAsStream("org/rundeck/example/plugin/urls.txt");
        if (null == resourceAsStream) {
            return null;
        }
        int rand = new Random(System.currentTimeMillis()).nextInt(MAGIC);
        try (
                BufferedReader is = new BufferedReader(new InputStreamReader(resourceAsStream))
        ) {
            return is.lines().skip(rand).findFirst().orElse(null);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public JobEventStatus afterJobEnds(final JobExecutionEvent event) throws JobPluginException {
        if (enabled) {
            event.getExecutionLogger().log(2, "Finished the job!");
            String name = event.getUserName();
            if (event.getResult().getResult().isSuccess()) {
                event.getExecutionLogger().log(2, String.format("Congrats it was successful, %s!", name));
                String url = randomUrl();
                if (null != url) {
                    HashMap<String, String> meta = new HashMap<>();
                    meta.put("content-data-type", "text/html");
                    event
                            .getExecutionLogger()
                            .log(2, String.format("<img src=\"https://imgs.xkcd.com/comics/%s.png\"/>", url), meta);
                } else {

                    event.getExecutionLogger().log(1, "(Sorry, no fun for you)");
                }
            } else {
                event.getExecutionLogger().log(2, String.format("Sorry, this job failed, %s!", name));
            }
        }
        return null;
    }
}
