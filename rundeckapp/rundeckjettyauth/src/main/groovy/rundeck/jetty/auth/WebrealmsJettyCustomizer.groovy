package rundeck.jetty.auth

import org.eclipse.jetty.security.ConstraintMapping
import org.eclipse.jetty.security.ConstraintSecurityHandler
import org.eclipse.jetty.security.authentication.FormAuthenticator
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.security.Constraint
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.embedded.jetty.JettyServerCustomizer

class WebrealmsJettyCustomizer implements JettyServerCustomizer {

    private static final Logger log = LoggerFactory.getLogger(this)
    public static final String ROLE = 'role'

    private def webrealmsConfig

    WebrealmsJettyCustomizer(def webrealmsConfig) {
        this.webrealmsConfig = webrealmsConfig
    }

    @Override
    void customize(final Server server) {
        if (!(webrealmsConfig && webrealmsConfig.server)) {
            log.debug("No server configuration found in webrealms config. Exiting customization.")
            return
        }

        addLoginServiceBean(server)

        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        if(webrealmsConfig.loginconfig) configureLoginMethod(securityHandler)
        if(webrealmsConfig.securityconstraint) configureUrlPatterns(securityHandler)
        if(webrealmsConfig.securityroles) configureSecurityRoles(securityHandler)

        assignSecurityHandlerToAllContextHandlers(server, securityHandler)
    }

    private Handler assignSecurityHandlerToAllContextHandlers(
            Server server,
            ConstraintSecurityHandler securityHandler
    ) {
            log.debug("Adding security handler to context handler ${server.handler}")
            server.handler.setSecurityHandler(securityHandler)
    }

    private void configureSecurityRoles(securityHandler) {
        webrealmsConfig.securityroles.each { role ->
            if (role.key == ROLE && role.value.name) {
                log.debug("Adding security role: ${role.value.name}")
                securityHandler.addRole(role.value.name)
            }
        }
    }

    private void configureUrlPatterns(ConstraintSecurityHandler securityHandler) {
        webrealmsConfig.securityconstraint.each { securityConstraintDef ->
            def webresourcename = securityConstraintDef.key
            def patternCfg = securityConstraintDef.value
            if (patternCfg && patternCfg.urlpattern) {
                log.debug("Adding constraint mapping for: ${webresourcename} with pattern: ${patternCfg.urlpattern}")

                boolean shouldAuthenticate = patternCfg.authconstraint && patternCfg.authconstraint.rolename
                Constraint securityConstraint = new Constraint()
                securityConstraint.name = webresourcename
                securityConstraint.setAuthenticate(shouldAuthenticate)
                ConstraintMapping mapping = new ConstraintMapping()
                mapping.pathSpec = patternCfg.urlpattern
                mapping.constraint = securityConstraint

                if (shouldAuthenticate) {
                    log.debug("applying role: ${patternCfg.authconstraint.rolename} to mapping: ${webresourcename}")
                    securityConstraint.setRoles(patternCfg.authconstraint.rolename)
                }

                securityHandler.addConstraintMapping(mapping)
            }

        }
    }

    private void configureLoginMethod(ConstraintSecurityHandler securityHandler) {
        securityHandler.setAuthMethod(webrealmsConfig.loginconfig.authmethod)
        securityHandler.setRealmName(webrealmsConfig.loginconfig.realmname)
        if (webrealmsConfig.loginconfig.authmethod == Constraint.__FORM_AUTH &&
            webrealmsConfig.loginconfig.loginpage &&
            webrealmsConfig.loginconfig.errorpage) {
            FormAuthenticator authenticator = new FormAuthenticator(
                    webrealmsConfig.loginconfig.loginpage,
                    webrealmsConfig.loginconfig.errorpage,
                    false
            )
            log.debug("Applying form authenticator to security handler")
            securityHandler.setAuthenticator(authenticator)
        }
    }

    private void addLoginServiceBean(Server server) throws Exception {
        if (!(webrealmsConfig.server.addrealm && webrealmsConfig.server.addrealm.classname)) return
        def configuredLoginServiceClass
        try {
            Class clz = Class.forName(webrealmsConfig.server.addrealm.classname)
            configuredLoginServiceClass = clz.getConstructor(new Class[0]).newInstance(new Object[0])

            try {
                if (configuredLoginServiceClass.class.isAssignableFrom(Class.forName('org.eclipse.jetty.jaas.JAASLoginService'))) {
                    configuredLoginServiceClass.name = webrealmsConfig.server.addrealm.name
                    configuredLoginServiceClass.loginModuleName = webrealmsConfig.server.addrealm.LoginModuleName
                    server.addBean(configuredLoginServiceClass)
                    log.debug("Added login service bean class: ${configuredLoginServiceClass}")
                }
            } catch (Exception e) {
                log.error("Failed to add login service: ${e}", e)
                throw e
            }
        } catch (Exception e) {
            log.error("Failed to add login service: ${e}",e)
            throw e
        }

    }
}

