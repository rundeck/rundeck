package rundeck

import rundeck.services.ConfigurationService

class ConfigTagLib {
    static returnObjectForTags = ['getString', 'getInteger', 'getBoolean',]
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

        this."$scope"."$var" = configurationService.getValue(key,String.class,attrs.defaultValue)

        null
    }

    /**
     *
     * @attr config REQUIRED config path string
     * @attr default default value
     */
    def getString={attrs,body->
        if (null==attrs.config) throw new IllegalArgumentException("required [config] attribute for tag <cfg:getString>!")
        return configurationService.getString(attrs.config,attrs.default)
    }
    /**
     *
     * @attr config REQUIRED config path string
     * @attr default REQUIRED default value
     */
    def getInteger={attrs,body->
        if (null==attrs.config) throw new IllegalArgumentException("required [config] attribute for tag <cfg:getInteger>!")
        if (null==attrs.default) throw new IllegalArgumentException("required [default] attribute for tag <cfg:getInteger>!")
        if (!(attrs.default instanceof Integer) ) throw new IllegalArgumentException("[default] attribute must be an integer for tag <cfg:getInteger>!")
        try {
            return configurationService.getInteger(attrs.config, attrs.default)
        } catch (NumberFormatException e) {
            return attrs.default
        }
    }

    /**
     *
     * @attr config REQUIRED config path string
     * @attr default REQUIRED default value
     */
    def getBoolean = {attrs,body->
        if (null==attrs.config) throw new IllegalArgumentException("required [config] attribute for tag <cfg:getBoolean>!")
        if (null==attrs.default) throw new IllegalArgumentException("required [default] attribute for tag <cfg:getBoolean>!")
        return configurationService.getBoolean(attrs.config,attrs.default?true:false)
    }
}
