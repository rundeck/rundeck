package rundeck

import rundeck.services.feature.FeatureService

class FeatureTagLib {
    def static namespace = "feature"
    def FeatureService featureService

    static returnObjectForTags = ['isEnabled', 'isDisabled']
    /**
     * Return true if the feature 'name' is enabled
     * @attr name REQUIRED name of feature
     */
    def isEnabled = { attrs, body ->
        if (!attrs.name) {
            throw new Exception("attribute required: 'name'")
        }
        return featureService.featurePresent(attrs.name)
    }
    /**
     * Render body if the feature is enabled
     * @attr name REQUIRED name of feature
     */
    def enabled = { attrs, body ->
        if (isEnabled(attrs, body)) {
            out << body()
        }
    }
    /**
     * Return true if the feature 'name' is disabled
     * @attr name REQUIRED name of feature
     */
    def isDisabled = { attrs, body ->
        return !isEnabled(attrs, body)
    }
    /**
     * Render body if the feature is disabled
     * @attr name REQUIRED name of feature
     */
    def disabled = { attrs, body ->
        if (isDisabled(attrs, body)) {
            out << body()
        }
    }
}
