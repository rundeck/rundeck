package rundeck.services.feature

/**
 * Manage feature configuration in the 'rundeck.feature.X' namespace, a
 * feature '*' enables all features.
 */
class FeatureService {
    static transactional = false
    def configurationService
    /**
     * Return true if grails configuration allows given feature, or '*' features
     * @param name
     * @return
     */
    def boolean featurePresent(def name) {
        def splat = configurationService.getBoolean('feature.*.enabled', false)
        return splat || configurationService.getBoolean("feature.${name}.enabled", false)
    }
    /**
     * Set an incubator feature toggle on or off
     * @param name
     * @param enable
     */
    def void toggleFeature(def name, boolean enable) {
        configurationService.setBoolean("feature.${name}.enabled", enable)
    }
    /**
     * Set an incubator feature toggle on or off
     * @param name
     * @param enable
     */
    def getFeatureConfig(def name) {
        configurationService.getConfig("feature.${name}.config")
    }
}
