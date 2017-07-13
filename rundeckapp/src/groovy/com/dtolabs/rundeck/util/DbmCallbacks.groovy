package com.dtolabs.rundeck.util

import liquibase.Liquibase
import liquibase.database.Database
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 * Re-work liquibase migrations to be database-agnostic
 **/
class DbmCallbacks implements GrailsApplicationAware {
    private GrailsApplication grailsApplication

    private static final String MIGRATION_KEY = "AUTO_REWORKED_MIGRATION_KEY"
    private static final String MIGRATION_HEADER = "/* ${MIGRATION_KEY} */"

    // DB-Specific types to liquibase properties mapping
    // see changelog.groovy for defined liquibase properties
    Map<String,String> liquibaseTypesMapping = [
        // start with specific ones, then unspecific ones.
        'type: "varchar(50)"': "type: '\\\${text.type}'",
        'type: "varchar2(500)"': "type: '\\\${text.type}'",
        'type: "varchar"': "type: '\\\${string.type}'",
        'type: "varchar2"': "type: '\\\${string.type}'",
        'type: "bit"': "type: '\\\${boolean.type}\'",
        'type: "number(1,0)"': "type: '\\\${boolean.type}'",
        'type: "bigint"': "type: '\\\${int.type}'",
        'type: "number(19,0)"': "type: '\\\${int.type}'",
        'type: "longtext"': "type: '\\\${clob.type}\'",
        'type: "clob"': "type: '\\\${clob.type}\'",
        'type: "longblob"': "type: '\\\${blob.type}\'",
        'type: "blob"': "type: '\\\${blob.type}\'",
        // regEx (e.g. "varchar(2)" to ${string.type}(2)'. Do not add trailing "'", here!
        '/.*(type: "varchar\\((.*)\\)").*/': "type: '\\\${string.type}",
        '/.*(type: "varchar2\\((.*)\\)").*/': "type: '\\\${string.type}",
        // db features
        'autoIncrement: "true"': "autoIncrement: '\\\${autoIncrement}'"
    ]

    public void setGrailsApplication(GrailsApplication ga) {
        this.grailsApplication = ga
    }

    void beforeStartMigration(Database database) {
        reworkMigrationFiles()
    }

    private void reworkMigrationFiles() {
        def config = grailsApplication.config.grails.plugin.databasemigration
        def changelogLocation = config.changelogLocation ?: 'grails-app/migrations'
        new File(changelogLocation)?.listFiles().each { File it ->
            List updateOnStartFileNames = config.updateOnStartFileNames
            if (updateOnStartFileNames?.contains(it.name)) {
                // do not convert updateOnStart files.
                return
            }
            convertMigrationFile(it)
        }
    }

    private void convertMigrationFile(File migrationFile) {
        def content = migrationFile.text
        if (content.contains(MIGRATION_KEY)) {
            return
        }
        liquibaseTypesMapping.each {
            String pattern = it.key
            String replace = it.value
            if (pattern.startsWith('/')) {
                // Handle regex pattern.
                def newContent = new StringBuffer()
                content.eachLine { String line ->
                    // remove leading and trailing "/"
                    def regEx = pattern[1..-2]
                    def matcher = (line =~ regEx)
                    if (matcher.matches() && matcher.groupCount() == 2) {
                        String replaceFind = matcher[0][1] // this is the found string, e.g. 'type: "varchar(22)"'
                        String replacement = "${replace}(${matcher[0][2]})\'"  // new string, e.g. "type: '${string.type}(22)' "
                        line = line.replace(replaceFind, replacement)
                    }
                    newContent += "${line}\n"
                  }
                  content = newContent
            } else {
                // non-regEx, so replace all in one go.
                content = content.replaceAll(pattern, replace)
            }
        }
        // mark file as already migrated
        content = "${MIGRATION_HEADER}" + "\n" + content
        migrationFile.write(content, 'UTF-8')
        log.warn "*** Converted database migration file ${migrationFile.name} to be database independent"
    }
}

