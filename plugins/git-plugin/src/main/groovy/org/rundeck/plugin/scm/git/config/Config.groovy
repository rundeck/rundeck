package org.rundeck.plugin.scm.git.config

import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.configuration.PluginAdapterUtility
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import org.rundeck.plugin.scm.git.BuilderUtil

/**
 * Base configuration class, creates plugin properties from annotations and creates instances
 */
class Config {
    Map<String, Object> otherInput
    Map<String, String> rawInput

    /**
     * @param classes
     * @return create a property list by introspection of the classes
     */
    static List<Property> listProperties(Class<?>... classes) {
        classes.collect {
            PluginAdapterUtility.buildFieldProperties(it)
        }.flatten()
    }

    /**
     * Configure a config object given the input
     * @param config config option
     * @param input input values
     * @throws ConfigurationException
     */
    static void configure(Config config, final Map<String, String> input) throws ConfigurationException {
        def unused = PluginAdapterUtility.configureObjectFieldsWithProperties(config, input)
        listProperties(config.class).findAll { it.required }.each { prop ->
            //verify required input
            if (!input[prop.name]) {
                throw new ConfigurationException("${prop.name} cannot be null")
            }
        }
        config.otherInput = unused
        config.rawInput = input
    }

    /**
     * Create a new config object
     * @param clazz config object class
     * @param input input values
     * @return new instance configured with the input
     * @throws ConfigurationException
     */
    static <T extends Config> T create(Class<T> clazz, final Map<String, String> input) throws ConfigurationException {
        T object = clazz.getDeclaredConstructor().newInstance()
        configure(object, input)
        object
    }

    /**
     * Replace an entry in the list with the given property by matching the property name
     * @param list
     * @param newProperty
     * @return
     */
    static List<Property> substituteDefaultValue(List<Property> list, String name, String newDefaultValue) {
        list.collect {
            if (it.name == name) {
                BuilderUtil.property(it) {
                    defaultValue newDefaultValue
                }
            } else {
                it
            }
        }
    }


    @Override
    public String toString() {
        return "${this.class.name}{" +
                "otherInput=" + otherInput +
                ", rawInput=" + rawInput +
                '}';
    }
}
