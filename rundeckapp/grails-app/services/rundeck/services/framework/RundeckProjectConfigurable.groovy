package rundeck.services.framework

import com.dtolabs.rundeck.core.plugins.configuration.Property

/**
 * defines a set of project-level configuration properties for a grails service/spring bean
 */
interface RundeckProjectConfigurable {
    /**
     * Project configuration category
     * @return
     */
    String getCategory()

    /**
     * List of properties
     * @return
     */
    List<Property> getProjectConfigProperties()

    /**
     * @return a map of config prop names to project config property names
     */
    public Map<String, String> getPropertiesMapping();
}