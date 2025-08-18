package com.dtolabs.rundeck.core.config;

import org.rundeck.app.spi.AppService;

/**
 * Provides feature flag information for SPI clients
 */
public interface FeatureInfoService
        extends AppService
{
    /**
     * Return true if the feature is enabled
     *
     * @param feature feature definition
     * @return true if enabled
     */
    boolean featurePresent(FeaturesDefinition feature);

    /**
     * Return true if the feature is enabled
     *
     * @param name feature name
     * @return true if enabled
     */
    boolean featurePresent(String name);

    /**
     * Return true if grails configuration allows given feature
     *
     * @param feature        feature definition
     * @param defaultEnabled default enabled value for the feature, if unset
     * @return true if enabled
     */
    boolean featurePresent(FeaturesDefinition feature, boolean defaultEnabled);

    /**
     * Return true if grails configuration allows given feature
     *
     * @param name           feature name
     * @param defaultEnabled default enabled value for the feature, if unset
     * @return true if enabled
     */
    boolean featurePresent(String name, boolean defaultEnabled);
}
