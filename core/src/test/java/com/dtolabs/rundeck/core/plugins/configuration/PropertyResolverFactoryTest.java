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

package com.dtolabs.rundeck.core.plugins.configuration;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;

/**
 */
@RunWith(JUnit4.class)
public class PropertyResolverFactoryTest {
    @Test
    public void pluginPropertyPrefix() {
        Assert.assertEquals("plugin.type.name.", PropertyResolverFactory.pluginPropertyPrefix("type", "name"));
    }

    @Test
    public void frameworkPropertyPrefix() {
        Assert.assertEquals("framework.abc", PropertyResolverFactory.frameworkPropertyPrefix("abc"));
    }

    @Test
    public void projectPropertyPrefix() {
        Assert.assertEquals("project.abc", PropertyResolverFactory.projectPropertyPrefix("abc"));
    }

    @Test
    public void createResolver() {
        HashMap<String, String> instanceData = new HashMap<String, String>() {{
            put("a", "instance");
        }};
        HashMap<String, String> projectData = new HashMap<String, String>() {{
            put("a", "project");
        }};
        HashMap<String, String> frameworkData = new HashMap<String, String>() {{
            put("a", "framework");
        }};
        PropertyResolver resolver = PropertyResolverFactory.createResolver(
                PropertyResolverFactory.instanceRetriever(instanceData),
                PropertyResolverFactory.instanceRetriever(projectData),
                PropertyResolverFactory.instanceRetriever(frameworkData)
        );
        Assert.assertEquals("instance", resolver.resolvePropertyValue("a", PropertyScope.InstanceOnly));
        Assert.assertEquals("instance", resolver.resolvePropertyValue("a", PropertyScope.Instance));
        Assert.assertEquals("project", resolver.resolvePropertyValue("a", PropertyScope.ProjectOnly));
        Assert.assertEquals("project", resolver.resolvePropertyValue("a", PropertyScope.Project));
        Assert.assertEquals("framework", resolver.resolvePropertyValue("a", PropertyScope.Framework));
    }

    @Test
    public void createResolverProject() {
        HashMap<String, String> instanceData = new HashMap<String, String>() {{
        }};
        HashMap<String, String> projectData = new HashMap<String, String>() {{
            put("a", "project");
        }};
        HashMap<String, String> frameworkData = new HashMap<String, String>() {{
            put("a", "framework");
        }};
        PropertyResolver resolver = PropertyResolverFactory.createResolver(
                PropertyResolverFactory.instanceRetriever(instanceData),
                PropertyResolverFactory.instanceRetriever(projectData),
                PropertyResolverFactory.instanceRetriever(frameworkData)
        );
        Assert.assertEquals(null, resolver.resolvePropertyValue("a", PropertyScope.InstanceOnly));
        Assert.assertEquals("project", resolver.resolvePropertyValue("a", PropertyScope.Instance));
        Assert.assertEquals("project", resolver.resolvePropertyValue("a", PropertyScope.ProjectOnly));
        Assert.assertEquals("project", resolver.resolvePropertyValue("a", PropertyScope.Project));
        Assert.assertEquals("framework", resolver.resolvePropertyValue("a", PropertyScope.Framework));
    }

    @Test
    public void createResolverFramework() {
        HashMap<String, String> instanceData = new HashMap<String, String>() {{
        }};
        HashMap<String, String> projectData = new HashMap<String, String>() {{
        }};
        HashMap<String, String> frameworkData = new HashMap<String, String>() {{
            put("a", "framework");
        }};
        PropertyResolver resolver = PropertyResolverFactory.createResolver(
                PropertyResolverFactory.instanceRetriever(instanceData),
                PropertyResolverFactory.instanceRetriever(projectData),
                PropertyResolverFactory.instanceRetriever(frameworkData)
        );
        Assert.assertEquals(null, resolver.resolvePropertyValue("a", PropertyScope.InstanceOnly));
        Assert.assertEquals("framework", resolver.resolvePropertyValue("a", PropertyScope.Instance));
        Assert.assertEquals(null, resolver.resolvePropertyValue("a", PropertyScope.ProjectOnly));
        Assert.assertEquals("framework", resolver.resolvePropertyValue("a", PropertyScope.Project));
        Assert.assertEquals("framework", resolver.resolvePropertyValue("a", PropertyScope.Framework));
    }
}
