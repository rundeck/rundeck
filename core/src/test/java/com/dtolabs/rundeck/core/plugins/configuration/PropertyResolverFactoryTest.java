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
