package grails.dev

/**
 * Stands in for a class Grails 8.0.0-M6 no longer ships on the runtime classpath.
 *
 * external-config 2.0.0 accidentally packages external/config/Application.class -- the plugin
 * project's own Grails entry point -- and its plugin descriptor declares that class as a provided
 * artefact. Grails loads it during startup to decide whether it is an artefact, and its static
 * initialiser calls grails.dev.Support.enableAgentIfNotPresent(Class). M6 moved that class out of
 * the runtime classpath when it split CLI-only code into separate -cli artifacts, so the load fails
 * and the application never starts.
 *
 * What the real method does is enable the Grails reloading agent for development. A packaged
 * deployment neither has nor wants it, so doing nothing is the correct behaviour here, not merely a
 * convenient one.
 *
 * Two other approaches were tried and are worse. Stripping the class from the jar turns the failure
 * into ClassNotFoundException, because the descriptor names it. An empty class is not enough either:
 * the call is in the static initialiser, which runs on load. 2.0.0 is the newest release of the
 * plugin, so there is no version to upgrade to.
 *
 * Remove this once external-config ships a build without that stray class, or once Rundeck moves to
 * Spring Boot's own external configuration support and drops the plugin.
 */
class Support {

    /**
     * No-op stand-in for the development reloading agent hook.
     *
     * @param applicationClass the application class the real implementation would instrument
     */
    static void enableAgentIfNotPresent(Class<?> applicationClass) {
    }
}
