String savedUserDir;

/**
 * WORKAROUND FOR GRAILS BUG: http://jira.grails.org/browse/MAVEN-154
 */
eventTestPhasesStart = {args ->
    println "Setting user.dir (${basedir})"
    savedUserDir = System.getProperty("user.dir")
    System.setProperty("user.dir", "${basedir}")
}

/**
 * WORKAROUND FOR GRAILS BUG: http://jira.grails.org/browse/MAVEN-154
 */
eventTestPhasesEnd = {args ->
    if (savedUserDir)
    {
        println "Restoring user.dir (${savedUserDir})"
        System.setProperty("user.dir", savedUserDir)
    }
}