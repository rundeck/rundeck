package rundeck

import rundeck.services.ConfigurationService

class ConfigTagLib {
    ConfigurationService configurationService
    def static namespace="cfg"
    static defaultEncodeAs = [taglib:'html']

    def val={attrs,body ->
        out << configurationService.getString(attrs.key.toString(),"")
    }

    /**
     * Sets the variable named by 'var' from the configuration service using the key
     */
    def setVar = { attrs, body ->
        def var = attrs.var
        if (!var) throw new IllegalArgumentException("[var] attribute must be specified to for <cfg:setVar>!")

        def scope = attrs.scope ? SCOPES[attrs.scope] : 'pageScope'
        if (!scope) throw new IllegalArgumentException("Invalid [scope] attribute for tag <cfg:setVar>!")

        String key = attrs.key
        if (!key) throw new IllegalArgumentException("[key] attribute for tag <cfg:setVar>!")

        this."$scope"."$var" = configurationService.getValue(key,attrs.defaultValue)

        null
    }
}
