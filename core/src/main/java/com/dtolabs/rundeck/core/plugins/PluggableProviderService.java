package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.ProviderService;

/**
 * ${CLASSNAME} is ...
 * Created by greg
 * Date: 4/12/13
 * Time: 5:46 PM
 */
public interface PluggableProviderService<T> extends ProviderService<T>, PluggableService<T> {
}
