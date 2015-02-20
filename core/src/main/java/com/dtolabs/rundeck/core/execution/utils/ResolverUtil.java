package com.dtolabs.rundeck.core.execution.utils;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.IRundeckProject;
import com.dtolabs.rundeck.core.execution.impl.jsch.JschNodeExecutor;

/**
 * Created by greg on 3/19/15.
 */
public class ResolverUtil {
    /**
     * Resolve a node/project/framework property by first checking node attributes named X, then project properties
     * named "project.X", then framework properties named "framework.X". If none of those exist, return the default
     * value
     */
    public static String resolveProperty(
            final String nodeAttribute,
            final String defaultValue,
            final INodeEntry node,
            final IRundeckProject frameworkProject,
            final Framework framework
    )
    {
        if (null != node.getAttributes().get(nodeAttribute)) {
            return node.getAttributes().get(nodeAttribute);
        } else if (frameworkProject.hasProperty(JschNodeExecutor.PROJ_PROP_PREFIX + nodeAttribute)
                   && !"".equals(frameworkProject.getProperty(JschNodeExecutor.PROJ_PROP_PREFIX + nodeAttribute))) {
            return frameworkProject.getProperty(JschNodeExecutor.PROJ_PROP_PREFIX + nodeAttribute);
        } else if (framework.hasProperty(JschNodeExecutor.FWK_PROP_PREFIX + nodeAttribute)) {
            return framework.getProperty(JschNodeExecutor.FWK_PROP_PREFIX + nodeAttribute);
        } else {
            return defaultValue;
        }
    }

    public static int resolveIntProperty(
            final String attribute,
            final int defaultValue,
            final INodeEntry iNodeEntry,
            final IRundeckProject frameworkProject,
            final Framework framework
    )
    {

        int value = defaultValue;
        final String string = resolveProperty(attribute, null, iNodeEntry, frameworkProject, framework);
        if (null != string) {
            try {
                value = Integer.parseInt(string);
            } catch (NumberFormatException e) {
            }
        }
        return value;
    }

    public static long resolveLongProperty(
            final String attribute,
            final long defaultValue,
            final INodeEntry iNodeEntry,
            final IRundeckProject frameworkProject,
            final Framework framework
    )
    {

        long value = defaultValue;
        final String string = resolveProperty(attribute, null, iNodeEntry, frameworkProject, framework);
        if (null != string) {
            try {
                value = Long.parseLong(string);
            } catch (NumberFormatException e) {
            }
        }
        return value;
    }

    public static boolean resolveBooleanProperty(
            final String attribute,
            final boolean defaultValue,
            final INodeEntry iNodeEntry,
            final IRundeckProject frameworkProject,
            final Framework framework
    )
    {

        boolean value = defaultValue;
        final String string = resolveProperty(attribute, null, iNodeEntry, frameworkProject, framework);
        if (null != string) {
            value = Boolean.parseBoolean(string);
        }
        return value;
    }
}
