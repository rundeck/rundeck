package com.dtolabs.rundeck.app.api.scm

import com.dtolabs.rundeck.app.api.CDataString


/**
 * Created by greg on 10/27/15.
 */
class ScmPluginInputField {

    /**
     * @return descriptive name of the property
     */
    String title

    /**
     * @return property key to use
     */
    String name

    /**
     * @return description of the values of the property
     */
    CDataString description

    /**
     * @return the property type
     */
    String type;

    /**
     * @return true if an empty value is not allowed
     */
    boolean required;

    /**
     * @return the default value of the property, or default select value to select
     */
    String defaultValue;

    /**
     * @return a list of values for a select property
     */
    List<String> values;

    /**
     * @return the scope of this property, i.e. where the value can be retrieved and overridden, or null to indicate
     * the default scope.
     */
    String scope;

    /**
     * @return a map of optional rendering options for the UI
     */
    Map<String, String> renderingOptions;
}
