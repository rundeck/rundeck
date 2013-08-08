class WebrealmsGrailsPlugin {

    def DEFAULT_CONFIG_FILE = "DefaultWebrealmsConfig"
    def APP_CONFIG_FILE     = "WebrealmsConfig"

    def version = 0.4
    def dependsOn = [:]

    // TODO Fill in these fields
    def author = "Greg Schueler"
    def authorEmail = "greg@dtosolutions.com"
    def title = "Allows configuration of web.xml to support UserRealms and security constraints"
    def description = '''\
Simply generates definitions in web.xml to define login configuration with particular realm, and security constraints.
'''
    def watchedResources = "**/grails-app/conf/${APP_CONFIG_FILE}.groovy"   
    // URL to the plugin's documentation
    def documentation = "http://grails.org/Webrealms+Plugin"

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }
   
    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)		
    }

    def doWithWebDescriptor = { xml ->
        def config = getConfig()
        if(config){
            def next
            if(config.loginconfig){
                def contextParam = xml."context-param"
                contextParam[contextParam.size() - 1] + {
                    'login-config' {
                        'auth-method'(config.loginconfig.authmethod)
                        'realm-name'(config.loginconfig.realmname)
                        if(config.loginconfig.authmethod=='FORM' && config.loginconfig.loginpage && config.loginconfig.errorpage){
                            'form-login-config' {
                                'form-login-page'(config.loginconfig.loginpage)
                                'form-error-page'(config.loginconfig.errorpage)
                            }
                        }
                    }
                }
//                next = xml.'login-config'
//                if(!next){
//                    next = xml."context-param"
//                }
            }else{
//                next= xml."context-param"
            }
            if(config.securityconstraint){
                def contextParam = xml."context-param"
                contextParam[contextParam.size() - 1] + {
                    def constr=config.securityconstraint
                    constr.each{ sec ->
                        def webresourcename = sec.key
                        def l = sec.value
                        if(l && l.urlpattern){
                            'security-constraint' {
                                'web-resource-collection'{
                                    'web-resource-name'("${webresourcename}")
                                    'url-pattern'(l.urlpattern)
                                }
                                if(l.authconstraint && l.authconstraint.rolename){
                                    'auth-constraint'{
                                        'role-name'(l.authconstraint.rolename)
                                    }
                                }
                            }
                        }

                    }
                }
            }
            if(config.securityroles){
                def contextParam = xml."context-param"
                contextParam[contextParam.size() - 1] + {
                    'security-role'{
                        def secroles=config.securityroles
                        secroles.each{role->
                            if(role.key=='role' && role.value.name){
                                'role-name'(role.value.name)
                            }
                        }
                    }
                }
            }

        }
    }
	                                      
    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }
	
    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    def getConfig = {
        ClassLoader parent = getClass().getClassLoader()
        GroovyClassLoader loader = new GroovyClassLoader(parent)

        def config

        try {
            def defaultConfigFile = loader.loadClass(DEFAULT_CONFIG_FILE)
            //log.info("Loading default config file: "+defaultConfigFile)
            config = new ConfigSlurper().parse(defaultConfigFile)

            try {
                def appConfigFile = loader.loadClass(APP_CONFIG_FILE)
                //log.info("Found application config file: "+appConfigFile)
                def appConfig = new ConfigSlurper().parse(appConfigFile)
                if (appConfig) {
                    //log.info("Merging application config file: "+appConfigFile)
                    config = config.merge(appConfig)
                }
            } catch(ClassNotFoundException e) {
                //log.warn("Did not find application config file: "+APP_CONFIG_FILE)
            }
        } catch(ClassNotFoundException e) {
            //log.error("Did not find default config file: "+DEFAULT_CONFIG_FILE)
        }

        config?.webrealms

    }
}
