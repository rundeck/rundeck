package com.dtolabs.rundeck.server.plugins.services;

/**
 * PluginBuilder can produce an instance of a plugin.
 * User: greg
 * Date: 4/16/13
 * Time: 4:48 PM
 */
public interface PluginBuilder<T> {
    public T buildPlugin();
}
