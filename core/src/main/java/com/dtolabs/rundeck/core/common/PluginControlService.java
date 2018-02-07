package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.common.Framework;

import java.util.Arrays;
import java.util.List;

/**
 * Created by rodrigo on 30-01-18.
 */
public class PluginControlService {

    private Framework framework;

    public PluginControlService(Framework framework){
        this.framework = framework;
    }

    public void checkDisabledPlugin(String projectName, String pluginName) throws PluginDisabledException {

        String disabledPlugins = framework.getProjectProperty(projectName, "disabled.plugins");
        if(disabledPlugins != null && !disabledPlugins.trim().isEmpty()){
            List<String> disabledPluginsList = Arrays.asList(disabledPlugins.split(","));
            boolean isDisabled = disabledPluginsList.contains(pluginName);
            if(isDisabled){
                throw new PluginDisabledException("Plugin : " + pluginName + " is disabled for executions");
            }
        }else{
            return;
        }
    }
}
