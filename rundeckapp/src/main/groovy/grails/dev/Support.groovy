package grails.dev

/**
 * Placeholder for a class Grails 8.0.0-M6 no longer ships.
 *
 * external-config 2.0.0 accidentally packages external/config/Application.class -- the plugin
 * project's own Grails entry point -- and its plugin descriptor declares that class as a provided
 * artefact. Grails loads it during startup to decide whether it is an artefact, and loading fails
 * because its main() body carries an `ldc` of grails.dev.Support, which M6 moved out of the runtime
 * classpath when it split CLI-only code into separate -cli artifacts.
 *
 * The class only has to resolve, not to do anything: the reference sits in main(), which a deployed
 * application never calls, and the call itself goes through a Groovy call site that is resolved when
 * invoked rather than when the class is loaded. An empty class is enough to let loading succeed.
 *
 * Stripping the class from the jar was tried first and does not work -- the descriptor names it, so
 * removing it turns the failure into ClassNotFoundException. 2.0.0 is the newest release of the
 * plugin, so there is no version to upgrade to.
 *
 * Remove this once external-config ships a build without that stray class, or once Rundeck moves to
 * Spring Boot's own external configuration support and drops the plugin.
 */
class Support {
}
