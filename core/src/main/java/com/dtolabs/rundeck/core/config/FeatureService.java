package com.dtolabs.rundeck.core.config;

public interface FeatureService {
    /**
     * Return true if grails configuration allows given feature
     * @param feature
     * @return
     */
    boolean featurePresent(Features feature);

    /**
     * Return true if grails configuration allows given feature
     * @param name
     * @return
     */
    boolean featurePresent(String name);

    /**
     * Return true if grails configuration allows given feature
     * @param feature
     * @param defaultEnabled default enabled value for the feature, if unset
     * @return true if enabled
     */
    boolean featurePresent(Features feature, boolean defaultEnabled);

    /**
     * Return true if grails configuration allows given feature
     * @param name
     * @param defaultEnabled default enabled value for the feature, if unset
     * @return true if enabled
     */
    boolean featurePresent(String name, boolean defaultEnabled);

    /**
     * Set an incubator feature toggle on or off
     * @param feature
     * @param enable
     */
    void toggleFeature(Features feature, boolean enable);

    /**
     * Set an incubator feature toggle on or off
     * @param name
     * @param enable
     */
    void toggleFeature(String name, boolean enable);

    Object getFeatureConfig(Features feature);

    Object getFeatureConfig(String name);
}
